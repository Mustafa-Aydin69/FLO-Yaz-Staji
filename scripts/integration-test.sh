#!/usr/bin/env bash
# 4 servisi docker-compose ile ayaga kaldirip Search -> Cart -> Payment -> Inventory
# akisini uctan uca otomatik test eder. Basarisiz bir adimda non-zero exit code doner.
set -u
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

SEARCH_URL="http://localhost:${SEARCH_SERVICE_PORT:-8081}"
CART_URL="http://localhost:${CART_SERVICE_PORT:-8082}"
PAYMENT_URL="http://localhost:${PAYMENT_SERVICE_PORT:-8083}"
INVENTORY_URL="http://localhost:${INVENTORY_SERVICE_PORT:-8084}"

COMPOSE_ARGS=(-f infra/docker-compose.yml)
if [ -f .env ]; then
  COMPOSE_ARGS+=(--env-file .env)
fi
compose() {
  docker compose "${COMPOSE_ARGS[@]}" "$@"
}

fail() {
  echo "FAIL: $1"
  compose down
  exit 1
}

assert_contains() {
  local haystack="$1" needle="$2" step="$3"
  if [[ "$haystack" != *"$needle"* ]]; then
    fail "$step - beklenen '$needle' bulunamadi. Yanit: $haystack"
  fi
}

wait_healthy() {
  local url="$1" name="$2" timeout=60 elapsed=0
  until curl -s -o /dev/null -w "%{http_code}" "$url/health" 2>/dev/null | grep -q 200; do
    sleep 2
    elapsed=$((elapsed + 2))
    if [ "$elapsed" -ge "$timeout" ]; then
      fail "$name /health'e $timeout sn icinde 200 donmedi"
    fi
  done
}

echo "==> docker compose up -d --build"
compose up -d --build || fail "docker compose up basarisiz oldu"

echo "==> Servislerin healthy olmasi bekleniyor"
wait_healthy "$SEARCH_URL" "search-service"
wait_healthy "$CART_URL" "cart-service"
wait_healthy "$PAYMENT_URL" "payment-service"
wait_healthy "$INVENTORY_URL" "inventory-service"
echo "    4 servis de hazir."

echo "==> 1) Search: urun aramasi"
SEARCH_RESP=$(curl -s "$SEARCH_URL/search?q=nike")
assert_contains "$SEARCH_RESP" "Nike" "Search /search?q=nike"

echo "==> 2) Inventory: odeme oncesi stok durumu"
STOCK_BEFORE=$(curl -s "$INVENTORY_URL/inventory/1")
RESERVED_BEFORE=$(echo "$STOCK_BEFORE" | grep -o '"reservedCount":[0-9]*' | grep -o '[0-9]*$')

echo "==> 3) Cart: sepet olustur"
CART_RESP=$(curl -s -X POST "$CART_URL/cart" -H "Content-Type: application/json" -d '{"userId":"integration-test"}')
assert_contains "$CART_RESP" "cartId" "Cart POST /cart"
CART_ID=$(echo "$CART_RESP" | grep -o '"cartId":"[^"]*"' | head -1 | cut -d'"' -f4)
[ -n "$CART_ID" ] || fail "cartId cikartilamadi: $CART_RESP"
echo "    cartId=$CART_ID"

echo "==> 4) Cart: urun ekle (Search dogrulamasi tetiklenir)"
ITEM_RESP=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$CART_URL/cart/$CART_ID/items" \
  -H "Content-Type: application/json" -d '{"productId":1,"quantity":1}')
echo "$ITEM_RESP" | grep -q "HTTP_CODE:200" || fail "Cart POST /cart/$CART_ID/items 200 donmedi: $ITEM_RESP"
assert_contains "$ITEM_RESP" "Air Runner X1" "Cart item add - urun adi"

echo "==> 5) Payment: odeme yap (Cart + Inventory zinciri)"
PAYMENT_RESP=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$PAYMENT_URL/payment" \
  -H "Content-Type: application/json" -d "{\"cartId\":\"$CART_ID\"}")
echo "$PAYMENT_RESP" | grep -q "HTTP_CODE:201" || fail "Payment POST /payment 201 donmedi: $PAYMENT_RESP"
assert_contains "$PAYMENT_RESP" "\"status\":\"SUCCESS\"" "Payment sonucu"

echo "==> 6) Inventory: odeme sonrasi stok rezervasyonu arttmi"
STOCK_AFTER=$(curl -s "$INVENTORY_URL/inventory/1")
RESERVED_AFTER=$(echo "$STOCK_AFTER" | grep -o '"reservedCount":[0-9]*' | grep -o '[0-9]*$')
if [ "$RESERVED_AFTER" -ne "$((RESERVED_BEFORE + 1))" ]; then
  fail "reservedCount artmadi (once=$RESERVED_BEFORE, sonra=$RESERVED_AFTER)"
fi
echo "    reservedCount: $RESERVED_BEFORE -> $RESERVED_AFTER"

echo "==> Tum adimlar basarili. Temizleniyor..."
compose down
echo "OK: uctan uca entegrasyon testi gecti."
