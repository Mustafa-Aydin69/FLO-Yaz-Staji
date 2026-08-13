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

### Şu Anki Durum (Gün 6)

Yukarıdaki hedef mimari (OTel Collector, Jaeger, Prometheus, Grafana, Alertmanager) henüz kurulmadı — bunlar Faz 8'den itibaren ekleniyor. Şu an çalışan zincir:

```
[ Search :8081 ] <-- [ Cart :8082 ] <-- [ Payment :8083 ] --> [ Inventory :8084 ]
      ^                                        |
      |________________ ürün doğrulaması ______|  (Cart, ürün eklerken Search'e sorar)
```

**4 servis de çalışır durumda ve uçtan uca bir sipariş akışını (arama → sepet → ödeme → stok rezervasyonu) destekliyor.** Ödeme, sepetteki her ürün için Inventory'den stok rezerve ediyor; stok yetersizse ya tüm işlem geri alınıyor (`409`) ya da (istek üzerine) sadece stoku olan ürünlerden tahsilat yapılıyor. **Docker Compose entegrasyonu tamamlandı** — `docker-compose up --build` ile 4 servis tek komutla, doğru sırayla (`depends_on` + healthcheck) ve container network üzerinden birbirini servis adıyla bularak ayağa kalkıyor.

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

### Projeyi çalıştırma

1. Reponu klonla ve dizine gir:
   ```bash
   git clone https://github.com/Mustafa-Aydin69/FLO-Yaz-Staji.git
   cd FLO-Yaz-Staji
   ```
2. *(Opsiyonel)* Port/URL'leri özelleştirmek istersen `.env.example`'ı `.env` olarak kopyala:
   ```bash
   cp .env.example .env
   ```
   Kopyalamazsan `infra/docker-compose.yml` içindeki varsayılan değerler (8081-8084 portları, container network URL'leri) kullanılır — proje `.env` olmadan da doğrudan çalışır.
3. 4 servisi tek komutla ayağa kaldır:
   ```bash
   docker compose -f infra/docker-compose.yml up --build
   ```
   Servisler sırayla (`depends_on` + healthcheck ile) ayağa kalkar: önce Search ve Inventory, onlar `healthy` olunca Cart, o da `healthy` olunca Payment.
4. Servislerin ayakta olduğunu doğrula:

   | Servis | Health check |
   |---|---|
   | Search Service | http://localhost:8081/health |
   | Cart Service | http://localhost:8082/health |
   | Payment Service | http://localhost:8083/health |
   | Inventory Service | http://localhost:8084/health |

   Detaylı endpoint listesi için aşağıdaki [API Dokümantasyonu](#api-dokümantasyonu) bölümüne bakabilirsin.
5. *(Opsiyonel)* Tüm zinciri (arama → sepet → ödeme → stok) otomatik doğrulamak için:
   ```bash
   ./scripts/integration-test.sh
   ```
   Bu script servisleri kendi başına ayağa kaldırıp test eder ve sonunda `docker compose down` ile temizler.
6. Durdurmak için:
   ```bash
   docker compose -f infra/docker-compose.yml down
   ```

> **Not:** Jaeger, Prometheus, Grafana ve Alertmanager arayüzleri bu fazda henüz mevcut değil — bunlar Faz 8'den itibaren eklenecek (bkz. [Geliştirme Planı](FAZLAR.md)).

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

Cart Service'e bağımlıdır: ödeme oluştururken `GET /cart/{cartId}` ile sepet içeriğini ve `totalAmount`'ı çeker (varsayılan `cart-service.base-url=http://localhost:8082`). Cart Service erişilemezse `502` döner. Ayrıca Inventory Service'e bağımlıdır (aşağıya bakın). Banka çağrısı `BankApiClient` ile mock'lanır (sabit ~300ms gecikme, şimdilik her zaman başarılı).

| Method | Endpoint | Açıklama |
|---|---|---|
| GET | `/health` | Servis sağlık kontrolü |
| POST | `/payment` | Sepet için ödeme oluşturur. Body: `{"cartId": "...", "continueWithAvailable": false}` (`continueWithAvailable` opsiyonel). Sepet bulunamazsa `404`, sepet tutarı `0` veya negatifse `400`, stok yetersizse `409` (aşağıya bakın) |
| GET | `/payment/{paymentId}` | Ödeme durumunu döner. Ödeme bulunamazsa `404` |

Örnek:
```bash
curl -X POST http://localhost:8083/payment -H "Content-Type: application/json" -d '{"cartId":"{cartId}"}'
curl http://localhost:8083/payment/{paymentId}
```

### Inventory Service (`localhost:8084`)

Başlangıç stok verisi Search Service'in ürün kataloğuyla eşleşecek şekilde tanımlanmıştır (`stock.json`). Payment Service ödeme sırasında bu servisi çağırır.

| Method | Endpoint | Açıklama |
|---|---|---|
| GET | `/health` | Servis sağlık kontrolü |
| GET | `/inventory/{productId}` | Ürünün stok/rezervasyon durumunu döner. Ürün bulunamazsa `404` |
| POST | `/inventory/{productId}/reserve` | Belirtilen miktarı rezerve eder (`reservedCount` artırılır). Body: `{"quantity": 2}`. Yetersiz stokta `409`, `quantity <= 0` ise `400` |
| POST | `/inventory/{productId}/release` | Rezervasyonu iade eder (`reservedCount` azaltılır). Body: `{"quantity": 2}`. `quantity <= 0` ise `400` |

Örnek:
```bash
curl http://localhost:8084/inventory/1
curl -X POST http://localhost:8084/inventory/1/reserve -H "Content-Type: application/json" -d '{"quantity":2}'
curl -X POST http://localhost:8084/inventory/1/release -H "Content-Type: application/json" -d '{"quantity":2}'
```

**Payment Service'in Inventory entegrasyonu:** `POST /payment` isteği artık sepetteki her ürün için ayrı ayrı rezervasyon deniyor. Herhangi bir ürünün stoku yetersizse, o ana kadar rezerve edilenler otomatik olarak geri alınır (`release`) ve `409 Conflict` döner (all-or-nothing). İstek body'sine opsiyonel `"continueWithAvailable": true` eklenirse, Payment Service sadece stoku olan ürünlerden tahsilat yapar ve tutarı buna göre yeniden hesaplar.

## Servisler Arası İletişim Standartları

Tüm servis-servis HTTP çağrıları (`SearchServiceClient`, `CartServiceClient`, `InventoryServiceClient`) ortak `services/common` modülü üzerinden aşağıdaki kurallara uyar:

- **Timeout:** Her çağrıda 3 saniyelik connect + read timeout uygulanır (`RestClientFactory`).
- **Retry:** Sadece bağlantı/timeout kaynaklı hatalarda (`ResourceAccessException`) 1 kez otomatik retry yapılır. `404`/`409` gibi iş mantığı hata kodları retry edilmez (deterministik oldukları için tekrar denemek anlamsızdır, POST'larda yan etkiye de yol açabilir).
- **Erişilemezlik:** Downstream servise hiç ulaşılamazsa (timeout, bağlantı reddi, DNS hatası) çağıran servis `502 Bad Gateway` döner, mesajda hangi servisin erişilemez olduğu belirtilir (örn. `"Search service unavailable: ..."`).
- **Correlation ID:** Her istek bir `X-Request-ID` header'ı taşır — client vermezse sunucu otomatik bir UUID üretir. Bu ID, o isteğin tetiklediği tüm alt servis çağrılarına otomatik olarak taşınır ve her serviste log satırına yazılır (`[requestId] METHOD path -> status (süre)`), böylece tek bir isteğin tüm servislerdeki izini sürmek mümkündür.
- **Hata response formatı:** Tüm servisler aynı JSON şemasını kullanır: `{"timestamp", "status", "error", "message", "path"}` (Spring Boot varsayılanı, `server.error.include-message: always` ile).
- **Config:** Servis URL'leri kod içinde sabitlenmez; `.env`/`.env.example` üzerinden okunur (local geliştirme için `*_URL`, Docker Compose network'ü için `*_DOCKER_URL`).

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

### Gün 5 Durumu — Inventory Service ve Zincirin Tamamlanması

- `Stock` veri modeli ve `stock.json` (Search Service'in ürün kataloğuyla eşleşen 25 kayıt) eklendi; `StockRepository` açılışta bunu belleğe yüklüyor
- `GET /inventory/{productId}`, `POST /inventory/{productId}/reserve`, `POST /inventory/{productId}/release` endpoint'leri eklendi; yetersiz stokta `409`, geçersiz miktarda `400`
- Payment Service, Inventory Service ile entegre edildi: ödeme öncesi sepetteki her ürün için ayrı ayrı rezervasyon deniyor
- Stok yetersizliğinde varsayılan davranış all-or-nothing: o ana kadar rezerve edilenler geri alınır (`release`), `409` döner; opsiyonel `continueWithAvailable: true` ile sadece stoku olan ürünlerden tahsilat yapılıp tutar yeniden hesaplanıyor
- İstek loglama filtresi (`RequestLoggingFilter`) Inventory Service'e de uygulandı
- `InventoryControllerTest` (6 case) ve `PaymentControllerTest`'e eklenen 2 yeni case (stok yetersizliği + kısmi ödeme) ile toplam senaryolar test edildi, tamamı geçiyor
- Search → Cart → Payment → Inventory tam zinciri 4 servis local'de ayrı ayrı çalıştırılarak uçtan uca manuel doğrulandı (curl ile); ayrıca stok yetersizliği edge case'i (all-or-nothing ve kısmi ödeme) manuel test edildi
- Bu testler sırasında hata yanıtlarında (`404`/`400`/`409`/`502`) mesaj alanının boş geldiği fark edildi; tüm servislerin `application.yml`'ine `server.error.include-message: always` eklenerek düzeltildi
- Docker-compose entegrasyonu ve tek komutla ayağa kalkma doğrulaması bilinçli olarak Faz 6-7'ye ertelendi (Search/Cart/Payment'ta olduğu gibi)

### Gün 6 Durumu — Servisler Arası İletişimin Sağlamlaştırılması

- Faz 2/3/4/5'ten ertelenen Docker maddeleri tamamlandı: 4 servisin de Dockerfile'ı `docker build` ile test edildi, `docker-compose.yml`'e eklendi, aynı container network'ünde servis adlarıyla birbirini bulabildikleri doğrulandı, `docker-compose up --build` ile tek komutla tam zincir (Search → Cart → Payment → Inventory) uçtan uca test edildi
- Yeni bir Maven modülü olan `services/common` eklendi; 4 serviste birebir tekrar eden `RequestLoggingFilter` ve servis-servis HTTP çağrı kalıpları (404→boş sonuç, erişilemezse `502`) buraya taşındı
- Tüm servis-servis `RestClient`'lara ortak bir fabrika (`RestClientFactory`) üzerinden 3 saniyelik connect+read timeout eklendi; sadece bağlantı/timeout hatalarında (iş mantığı hata kodlarında değil) 1 kez retry yapan ortak bir mekanizma eklendi
- Servis URL'leri `.env`/`.env.example` üzerinden merkezi hale getirildi (Docker network içi URL'ler için ayrı değişkenler: `SEARCH_SERVICE_DOCKER_URL` vb.)
- Her isteğe bir `X-Request-ID` correlation ID'si eklendi (client vermezse otomatik üretiliyor); bu ID servisler arası tüm çağrılarda otomatik taşınıyor ve loglara yazılıyor — tek bir isteğin 4 servis logunda da aynı ID ile izlenebildiği doğrulandı
- Hata response formatının (Spring Boot varsayılan `timestamp`/`status`/`error`/`message`/`path` şeması) zaten 4 serviste birebir tutarlı olduğu doğrulandı, ek bir soyutlamaya gerek görülmedi
- Circuit breaker eklenmesi değerlendirildi; bu ölçekte (henüz metrik/dashboard ve chaos enjeksiyonu yok) gereksiz karmaşıklık olacağına karar verilip Faz 15'e ertelendi
- `docker-compose.yml`'e `depends_on` (`condition: service_healthy`) ve `/health` tabanlı healthcheck'ler eklendi; servislerin doğru sırayla ayağa kalktığı doğrulandı
- Bir servis bağımlı olduğu servis hiç ayakta değilken çağrıldığında (`--no-deps` ile bilerek test edildi) sınırlı sürede (~5 sn) temiz bir `502` döndüğü, isteğin sonsuza kadar asılı kalmadığı doğrulandı
- `scripts/integration-test.sh` eklendi: 4 servisi `docker compose` ile ayağa kaldırıp uçtan uca akışı otomatik test eden, adım adım assertion yapan bir script

### Gün 8 Durumu — Observability: Tracing Kurulumu (Search Service)

- Search Service'e `opentelemetry-instrumentation-bom` (2.16.0) + `opentelemetry-spring-boot-starter` eklendi; root `pom.xml`'de bu BOM import'u `spring-boot-dependencies`'den önce sıralandı (aksi halde OTel çekirdek kütüphaneleri ile bazı alt bağımlılıkları arasında versiyon çakışması oluşup servis açılışta hata veriyordu)
- `otel.service.name=search-service` resource attribute'u ile `TracerProvider` kuruldu; Spring MVC otomatik instrumentation'ı (`otel.instrumentation.spring-webmvc.enabled`) her `/search`, `/products/{id}` isteği için otomatik bir `SERVER` span'i üretiyor
- Trace exporter olarak `logging` (konsola basan `LoggingSpanExporter`) ayarlandı; metrics/logs exporter'ları bu fazın kapsamı dışında olduğu için `none` bırakıldı
- `SearchController`'daki kataloğu filtreleme adımı, `search.query` ve `search.result_count` attribute'larını taşıyan manuel bir `filter-catalog` child span'ine sarıldı; her istekte aynı `trace_id` altında üretildiği doğrulandı
- Hata durumunda `filter-catalog` span'i `ERROR` status'una geçiriliyor ve exception kaydediliyor (`recordException` + `setStatus`); bunu kalıcı olarak doğrulayan `InMemorySpanExporter` tabanlı bir unit test (`SearchControllerTracingTest`) eklendi
- Tüm OTel ayarları (`OTEL_SERVICE_NAME`, `OTEL_TRACES_EXPORTER`, `OTEL_METRICS_EXPORTER`, `OTEL_LOGS_EXPORTER`, `OTEL_INSTRUMENTATION_SPRING_WEBMVC_ENABLED`) `.env.example` üzerinden environment variable'larla parametrize edilebiliyor
- Jaeger/OTel Collector henüz kurulmadı — trace'ler şu an yalnızca Search Service'in kendi konsoluna basılıyor; Collector entegrasyonu Faz 10'da, diğer servislere (Cart/Payment/Inventory) tracing yayılması Faz 9'da yapılacak

### Gün 9 Durumu — Observability: Tracing Cart, Payment, Inventory Servislerine Yayıldı

- Cart/Payment/Inventory servislerine Search Service ile aynı OTel kurulumu uygulandı (`opentelemetry-spring-boot-starter`, `otel.service.name` her serviste kendi adına — `cart-service`/`payment-service`/`inventory-service`, `logging` trace exporter, env var parametrizasyonu)
- **Kritik bug bulundu ve düzeltildi:** Servisler arası çağrılarda (`RestClientFactory`) `RestClient.builder()` doğrudan çağrılıyordu, bu Spring'in auto-configure ettiği (ve OTel/Observation instrumentation'ını içeren) `RestClient.Builder` bean'ini bypass edip `traceparent` header'ının hiç taşınmamasına yol açıyordu — her servis çağrısı kendi başına yeni bir trace başlatıyordu. Üç client sınıfı (`SearchServiceClient`, `CartServiceClient`, `InventoryServiceClient`) Spring-yönetimli `RestClient.Builder`'ı inject edecek şekilde düzeltildi; düzeltme sonrası tüm zincirde (Cart→Search, Payment→Cart, Payment→Inventory) tek bir `trace_id`'nin korunduğu gerçek çoklu-servis testleriyle doğrulandı
- Her serviste kritik bir iş adımına manuel child span eklendi: Cart'ta `update-cart` (sepet güncelleme), Payment'ta `bank-charge` (`BankApiClient` çağrısı), Inventory'de `reserve-stock`/`release-stock` (stok ayarlama) — hepsi `search.*` ile aynı desende (`<servis>.<alan>` attribute isimlendirmesi, hata durumunda `ERROR` status + exception kaydı)
- Hata senaryoları (stok yetersizliği, banka çağrısı hatası — `BankApiClient` şu an hiç başarısız olmadığından mock'lanarak) `InMemorySpanExporter` tabanlı kalıcı unit testlerle (`InventoryControllerTracingTest`, `PaymentControllerTracingTest`) doğrulandı
- Tüm servislerin `@WebMvcTest`'lerine, constructor'a eklenen `OpenTelemetry` bağımlılığı için `OpenTelemetry.noop()` test config'i proaktif olarak eklendi (Search Service'te Faz 8'de keşfedilen regresyonun tekrarlanmaması için)
- `mvn verify` (tüm reactor) ile 29/29 test geçtiği, spotless format kontrolünün temiz olduğu doğrulandı
- 4 servis aynı anda çalışırken tek bir isteğin trace'ini takip etmenin (her servisin ayrı konsol logunu elle karşılaştırmak) ne kadar zahmetli olduğu gözlemlendi — bu, Faz 10-11'deki OTel Collector/Jaeger ihtiyacının somut gerekçesi

### Gün 10 Durumu — OTel Collector Kurulumu ve Trace Propagation Testi

```
[ Search :8081 ]
[ Cart :8082    ]  --OTLP/HTTP (4318)-->  [ OTel Collector ]
[ Payment :8083 ]                          |   receivers: otlp (grpc:4317, http:4318)
[ Inventory :8084]                          |   processors: batch (5s / 1024 span)
                                            |   exporters: debug (verbosity: detailed)
                                            |   extensions: health_check (:13133)
                                            v
                                   flo-otel-collector-1 logları
                              (docker compose logs -f otel-collector)
```

- `infra/otel-collector-config.yaml` oluşturuldu: `otlp` receiver (gRPC 4317 + HTTP 4318), `debug` exporter, `batch` processor (5s timeout, 1024 span batch size) ve `health_check` extension (`0.0.0.0:13133`)
- Collector, `infra/docker-compose.yml`'e servis olarak eklendi; config dosyası read-only volume ile `/etc/otelcol-contrib/config.yaml`'a mount ediliyor
- 4 servisin `OTEL_EXPORTER_OTLP_ENDPOINT` değeri konsol yerine `http://otel-collector:4318`'e yönlendirildi
- Uçtan uca bir sipariş akışı (cart oluştur → ürün ekle → ödeme yap) tetiklenip Collector logları incelendi: **trace propagation her kök istek içinde doğru çalışıyor**, ama mimaride "sipariş" tek bir HTTP isteği olmadığından (3 bağımsız kök istek: `POST /cart`, `POST /cart/{id}/items`, `POST /payment`) tek bir trace_id 4 servisin tamamını kapsamıyor — bunun yerine her kök isteğin kendi trace_id'si, o istek içindeki tüm alt-çağrılarda (örn. ürün ekle → cart+search, ödeme → payment+cart+inventory) tutarlı şekilde taşınıyor; kopma tespit edilmedi
- Collector image'ı (`otel/opentelemetry-collector-contrib`) distroless olduğundan (konteynerde `sh`/`wget`/`curl` yok) diğer servislerdeki gibi Docker-native healthcheck kurulamadı; bunun yerine resmi `health_check` extension'ı eklenip `curl http://localhost:13133/` ile `200 OK` doğrulandı — `depends_on` bu servis için `service_started` olarak kaldı

## Sınırlamalar

- Servisler arası veri gerçek bir veritabanı yerine mock/in-memory veri ile simüle edilir
- Log toplama katmanı bu sürümde basit tutulmuştur, kapsamlı bir log pipeline'ı içermez
- Gecikme/hata enjeksiyon mekanizmaları yalnızca test amaçlıdır, varsayılan olarak kapalıdır

## Lisans

Bu proje bir staj/okul çalışması kapsamında geliştirilmiştir.