# FLO Observability Sim

Perakende e-ticaret altyapısında dağıtık mikroservis mimarisinin uçtan uca izlenebilirliğini (observability) simüle eden bir proje. Arama, sepet, ödeme ve stok servisleri arasındaki istek akışı; distributed tracing, metrik toplama ve alarm mekanizmalarıyla gözlemlenir.

## Amaç

Kampanya dönemlerinde yüksek trafik alan e-ticaret sistemlerinde tek bir sipariş adımı (Arama → Sepet → Ödeme → Stok) birden fazla mikroservisten geçer. Bu servislerden birindeki gecikme veya sessiz hata, tüm akışı bozabilir. Bu proje, bu tür sorunları tespit etmeye yarayan endüstri standardı bir observability altyapısını (3 Pillars: Traces, Metrics, Logs) sıfırdan kurar ve gerçekçi arıza/yük senaryolarıyla test eder.

## Mimari

```
[ Mock E-Ticaret Servisleri ] --(OTel SDK)--> [ OpenTelemetry Collector ]
   (Search, Cart, Payment,                              |
    Inventory)                            +-------------+-------------+
                                           |                           |
                                    (Traces & Logs)               (Metrics)
                                           v                           v
                                   [ Jaeger / Tempo ]           [ Prometheus ]
                                           |                           |
                                           +-------------+-------------+
                                                         |
                                                         v
                                                [ Grafana Paneli ]
                                                         |
                                                         v
                                                [ Alertmanager ]
```

### Servisler

| Servis | Sorumluluk |
|---|---|
| **Search Service** | Ürün arama ve kataloğu; rastgele %5 gecikme enjeksiyonu içerir |
| **Cart Service** | Sepet oluşturma/güncelleme; Search Service'e ürün doğrulaması için istek atar |
| **Payment Service** | Ödeme işleme; rastgele %3 hata (HTTP 500 / POS timeout) enjeksiyonu içerir |
| **Inventory Service** | Stok sorgulama, ödeme sonucuna göre stok düşürme/iade etme |

## Teknoloji Yığını

- **Servisler:** Java, Spring Boot
- **Tracing:** OpenTelemetry (Java Agent + Collector), Jaeger
- **Metrics:** Micrometer, Prometheus
- **Dashboard:** Grafana
- **Alarm:** Prometheus Alertmanager
- **Yük testi:** Locust
- **Konteynerizasyon:** Docker, Docker Compose

## Proje Yapısı

```
FLO-Yaz-Staji/
├── services/
│   ├── search-service/
│   ├── cart-service/
│   ├── payment-service/
│   └── inventory-service/
├── infra/
│   ├── docker-compose.yml
│   ├── otel-collector-config.yaml
│   ├── prometheus.yml
│   ├── alertmanager.yml
│   └── grafana/
│       └── dashboards/
├── load-test/
├── docs/
│   ├── senaryo-darbogaz.md
│   └── senaryo-kampanya-trafik.md
└── Mustik/
    ├── FAZLAR.md
    └── CLAUDE.md
```

## Kurulum ve Çalıştırma

### Gereksinimler
- Docker & Docker Compose
- Java 17+ (yalnızca lokal geliştirme için)
- Maven veya Gradle

### Ayağa kaldırma

```bash
git clone https://github.com/Mustafa-Aydin69/FLO-Yaz-Staji.git
cd FLO-Yaz-Staji
docker-compose up --build
```

Tüm servisler ayağa kalktıktan sonra:

| Arayüz | URL |
|---|---|
| Jaeger UI | http://localhost:16686 |
| Prometheus UI | http://localhost:9090 |
| Grafana | http://localhost:3000 |
| Alertmanager | http://localhost:9093 |

## API Dokümantasyonu

### Search Service (`localhost:8081`)

| Method | Endpoint | Açıklama |
|---|---|---|
| GET | `/health` | Servis sağlık kontrolü |
| GET | `/search?q={terim}` | İsim, marka veya kategoriye göre ürün arama (case-insensitive). `q` boş/whitespace ise `400` döner |
| GET | `/products/{id}` | Tekil ürün detayı. Ürün bulunamazsa `404` döner |

Örnek:
```bash
curl "http://localhost:8081/search?q=nike"
curl "http://localhost:8081/products/1"
```

### Cart Service (`localhost:8082`)

Search Service'e bağımlıdır: sepete ürün eklerken `GET /products/{id}` ile ürün doğrulaması yapar (varsayılan `search-service.base-url=http://localhost:8081`). Search Service erişilemezse `502` döner.

| Method | Endpoint | Açıklama |
|---|---|---|
| GET | `/health` | Servis sağlık kontrolü |
| POST | `/cart` | Yeni (boş) sepet oluşturur. Body: `{"userId": "..."}` (opsiyonel) |
| GET | `/cart/{cartId}` | Sepet içeriğini (kalemler + `totalAmount`) döner. Sepet bulunamazsa `404` |
| POST | `/cart/{cartId}/items` | Sepete ürün ekler. Body: `{"productId": 1, "quantity": 2}`. Ürün veya sepet bulunamazsa `404`, `quantity <= 0` ise `400` |
| DELETE | `/cart/{cartId}/items/{productId}` | Üründen çıkarır. Sepet veya kalem bulunamazsa `404` |

Örnek:
```bash
curl -X POST http://localhost:8082/cart -H "Content-Type: application/json" -d '{"userId":"demo"}'
curl -X POST http://localhost:8082/cart/{cartId}/items -H "Content-Type: application/json" -d '{"productId":1,"quantity":2}'
curl http://localhost:8082/cart/{cartId}
curl -X DELETE http://localhost:8082/cart/{cartId}/items/1
```

### Payment Service (`localhost:8083`)

Cart Service'e bağımlıdır: ödeme oluştururken `GET /cart/{cartId}` ile sepetin `totalAmount`'ını çeker (varsayılan `cart-service.base-url=http://localhost:8082`). Cart Service erişilemezse `502` döner. Banka çağrısı `BankApiClient` ile mock'lanır (sabit ~300ms gecikme, şimdilik her zaman başarılı).

| Method | Endpoint | Açıklama |
|---|---|---|
| GET | `/health` | Servis sağlık kontrolü |
| POST | `/payment` | Sepet için ödeme oluşturur. Body: `{"cartId": "..."}`. Sepet bulunamazsa `404`, sepet tutarı `0` veya negatifse `400` |
| GET | `/payment/{paymentId}` | Ödeme durumunu döner. Ödeme bulunamazsa `404` |

Örnek:
```bash
curl -X POST http://localhost:8083/payment -H "Content-Type: application/json" -d '{"cartId":"{cartId}"}'
curl http://localhost:8083/payment/{paymentId}
```

## Örnek Kullanım Senaryoları

1. **Sipariş Darboğazı Tespiti** — Bir isteğin hangi serviste ne kadar zaman harcadığını `trace_id` ile Jaeger üzerinden tespit etme. Detay: [`docs/senaryo-darbogaz.md`](docs/senaryo-darbogaz.md)
2. **Kampanya Trafik Simülasyonu** — Locust ile ani yük artışı simüle edip sistemin darboğaz noktalarını Grafana dashboard'larıyla gözlemleme. Detay: [`docs/senaryo-kampanya-trafik.md`](docs/senaryo-kampanya-trafik.md)

## Geliştirme Planı

Projenin 20 günlük, gün gün ilerleyen detaylı faz planı için [`FAZLAR.md`](FAZLAR.md) dosyasına bakın.

### Gün 1 Durumu — Proje İskeleti ve Repo Kurulumu

- Git reposu kuruldu, `.gitignore` eklendi
- `services/` altında 4 servis için Spring Boot (Java 21, Maven) iskeleti oluşturuldu: search-service, cart-service, payment-service, inventory-service
- Root `pom.xml` üzerinden ortak bağımlılık yönetimi (Spring Boot BOM) sağlandı
- Kök dizinde boş `docker-compose.yml` iskeleti ve her serviste `Dockerfile` eklendi
- `.env.example` ile port/URL placeholder'ları tanımlandı
- Her serviste `/health` endpoint'i eklendi; 4 servis de local'de tek tek ayağa kaldırılıp curl ile doğrulandı (search:8081, cart:8082, payment:8083, inventory:8084)
- `scripts/run_all.sh` ile tüm servisleri paralel başlatma eklendi
- `docs/health-checks.http` ile health-check istek koleksiyonu oluşturuldu

### Gün 2 Durumu — Search Service Temel Mantığı

- Mock ürün kataloğu (`products.json`, 25 ayakkabı kaydı) eklendi; `ProductRepository` uygulama açılışında bu veriyi belleğe yüklüyor
- `GET /search?q=` endpoint'i eklendi — isim/marka/kategori bazlı case-insensitive filtreleme, boş/whitespace `q` için `400` davranışı
- `GET /products/{id}` endpoint'i eklendi — bulunamayan ürün için `404`
- İstek loglama filtresi eklendi (`RequestLoggingFilter`): her istekte method + path + status + süre loglanıyor
- `SearchControllerTest` ile 5 unit test yazıldı, tamamı geçiyor
- Root `pom.xml`'e Spotless (Google Java Format) formatlama kontrolü eklendi; 4 servisteki mevcut kod da bu standarda göre yeniden biçimlendirildi

### Gün 3 Durumu — Cart Service Temel Mantığı

- `Cart`/`CartItem` veri modelleri ve `ConcurrentHashMap` tabanlı in-memory `CartRepository` eklendi
- `POST /cart`, `GET /cart/{cartId}`, `POST /cart/{cartId}/items`, `DELETE /cart/{cartId}/items/{productId}` endpoint'leri eklendi
- Search Service entegrasyonu: `RestClient` tabanlı `SearchServiceClient` ile ürün doğrulaması yapılıyor; ürün bulunamazsa `404`, Search Service'e erişilemezse `502` dönüyor
- Search Service kapalıyken ve açıkken uçtan uca akışlar (sepet oluştur → ürün ekle → görüntüle) manuel olarak doğrulandı
- İstek loglama filtresi (`RequestLoggingFilter`) Cart Service'e de uygulandı
- `CartControllerTest` ile 8 unit test yazıldı, tamamı geçiyor

### Gün 4 Durumu — Payment Service Temel Mantığı

- `Cart` modeline `totalAmount` alanı eklendi (Cart Service, Faz 3'e küçük bir düzeltme); `CartController` her sepet güncellemesinde bunu yeniden hesaplıyor
- `Payment`/`PaymentStatus`/`CreatePaymentRequest` veri modelleri ve `ConcurrentHashMap` tabanlı in-memory `PaymentRepository` eklendi
- `POST /payment`, `GET /payment/{paymentId}` endpoint'leri eklendi
- Cart Service entegrasyonu: `RestClient` tabanlı `CartServiceClient` ile sepet tutarı çekiliyor; sepet bulunamazsa `404`, Cart Service'e erişilemezse `502` dönüyor
- Mock banka çağrısı (`BankApiClient`): sabit ~300ms gecikmeli, şimdilik her zaman başarılı, rastgele `transactionId` üretiyor
- Sepet tutarı `0` veya negatifse ödeme `400 Bad Request` ile reddediliyor (banka çağrısı hiç yapılmıyor)
- İstek loglama filtresi (`RequestLoggingFilter`) Payment Service'e de uygulandı
- `PaymentControllerTest` ile 5 unit test yazıldı (başarılı ödeme, geçersiz cartId, sıfır tutarlı sepet, ödeme sorgulama — bulundu/bulunamadı), tamamı geçiyor

## Sınırlamalar

- Servisler arası veri gerçek bir veritabanı yerine mock/in-memory veri ile simüle edilir
- Log toplama katmanı bu sürümde basit tutulmuştur, kapsamlı bir log pipeline'ı içermez
- Gecikme/hata enjeksiyon mekanizmaları yalnızca test amaçlıdır, varsayılan olarak kapalıdır

## Lisans

Bu proje bir staj/okul çalışması kapsamında geliştirilmiştir.