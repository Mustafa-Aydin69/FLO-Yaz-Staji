# FLO Perakende Observability Simülatörü — Staj Faz Planı

**Toplam süre:** 20 gün
**Kural:** Her faz = 1 gün. Her günde en az bir somut kod/yapılandırma işlemi üretilecek; her gün en az 10-15 alt adımdan oluşacak.

Mimari referans: Mock E-Ticaret Servisleri (Search, Cart, Payment, Inventory) → OpenTelemetry SDK → OTel Collector → (Traces/Logs: Jaeger/Tempo, Metrics: Prometheus) → Grafana.

---

## FAZ 1 — Gün 1: Proje İskeleti ve Repo Kurulumu

1. Git reposu oluştur (`flo-observability-sim`), `.gitignore` ekle (Python/Node standart)
2. Klasör yapısını oluştur: `services/search`, `services/cart`, `services/payment`, `services/inventory`, `infra/`, `docs/`
3. Dil/framework kararını ver (örn. Python + FastAPI) ve her servis klasörüne boş bir `main.py` iskeleti koy
4. Sanal ortam (venv) veya Node için `package.json` başlat, temel bağımlılıkları (`fastapi`, `uvicorn` veya `express`) kur
5. Kök dizine boş bir `docker-compose.yml` iskeleti ekle (henüz servis tanımları yok)
6. Her servis için ayrı `Dockerfile` iskeleti oluştur (multi-stage değil, basit tek aşamalı)
7. README.md dosyasını başlat: proje amacı, mimari diyagramı (metin/ASCII), kurulum adımları başlığı
8. `.env.example` dosyası oluştur (port numaraları, servis URL'leri için placeholder)
9. Ortak bir `requirements.txt` / `package.json` bağımlılık listesi taslağı hazırla
10. Her servis için `/health` endpoint'i yaz ve local'de tek tek çalıştırıp test et (curl ile)
11. Basit bir `Makefile` veya `scripts/run_all.sh` ile tüm servisleri paralel başlatmayı dene
12. Postman/Insomnia veya `.http` dosyası ile temel health-check isteklerini kaydet
13. Git ilk commit'i at, `main` branch koruması (opsiyonel) düşün
14. Gün sonu: README'ye "Gün 1 durumu" notu ekle

---

## FAZ 2 — Gün 2: Search Service Temel Mantığı

1. Search Service için basit bir ürün kataloğu (mock JSON veri, 20-30 ayakkabı kaydı) oluştur
2. `GET /search?q=` endpoint'ini yaz — isim/kategori bazlı basit filtreleme
3. `GET /products/{id}` endpoint'i ile tekil ürün detay dönüşü ekle
4. Response modeli için Pydantic (veya benzeri) şema tanımla (id, name, brand, price, stock)
5. Hatalı/boş sorgu senaryosu için 400 response davranışı ekle
6. Servisi ayrı bir portta (örn. 8001) çalıştır ve manuel test et
7. Basit bir logging middleware ekle (istek geldiğinde path + süreyi logla)
8. `requirements.txt`/`package.json`'a eklenen yeni bağımlılıkları işle
9. Dockerfile'ı Search Service için tamamla, `docker build` ile image'ı test et
10. docker-compose.yml içine Search Service'i ekle, `docker-compose up search` ile ayağa kaldır
11. Container içinden health-check ve search endpoint'ini curl ile doğrula
12. Basit unit test(ler) yaz (en az 2-3 test case: bulunan ürün, bulunamayan ürün, boş query)
13. Kod stilini gözden geçir (linter/formatter varsa çalıştır)
14. README'ye Search Service API dokümantasyonu ekle

---

## FAZ 3 — Gün 3: Cart Service Temel Mantığı

1. Sepet veri modelini tasarla (cart_id, user_id, items[], created_at)
2. In-memory veya basit dict tabanlı bir "sahte veritabanı" katmanı oluştur (ileride Redis'e geçiş için soyutlanmış)
3. `POST /cart` — yeni sepet oluşturma endpoint'i
4. `POST /cart/{cart_id}/items` — sepete ürün ekleme (Search Service'e HTTP isteğiyle ürün doğrulaması)
5. `GET /cart/{cart_id}` — sepet içeriğini görüntüleme
6. `DELETE /cart/{cart_id}/items/{item_id}` — üründen çıkarma
7. Search Service'e yapılan çağrı için basit bir HTTP client wrapper fonksiyonu yaz
8. Search Service kapalıyken Cart Service'in davranışını test et (hata yönetimi/fallback)
9. Dockerfile'ı tamamla, docker-compose'a Cart Service'i ekle
10. İki servisin aynı Docker network'ünde birbirini servis adıyla (örn. `http://search:8001`) çağırabildiğini doğrula
11. Sepet oluşturma → ürün ekleme → görüntüleme akışını uçtan uca manuel test et
12. Unit testler ekle (en az 3 case: geçerli ekleme, geçersiz ürün id, boş sepet görüntüleme)
13. Logging middleware'i Cart Service'e de uygula
14. README'yi güncelle: Cart Service API + Search Service bağımlılığı notu

---

## FAZ 4 — Gün 4: Payment Service Temel Mantığı

1. Ödeme veri modelini tasarla (payment_id, cart_id, amount, status, created_at)
2. `POST /payment` endpoint'i — cart_id alıp Cart Service'ten sepet toplamını çekme
3. Cart Service'e yapılan HTTP çağrısını wrapper fonksiyon olarak yaz
4. Mock "banka API" çağrısını simüle eden ayrı bir fonksiyon yaz (henüz hatasız, sabit gecikmeli)
5. Ödeme sonucu response modelini tanımla (success/failed, transaction_id)
6. `GET /payment/{payment_id}` — ödeme durumu sorgulama endpoint'i
7. Dockerfile'ı tamamla, docker-compose'a Payment Service'i ekle
8. Cart → Payment akışını uçtan uca test et (sepet oluştur, ürün ekle, ödeme yap)
9. Servisler arası hata senaryosu test et (Cart Service kapalıyken Payment ne yapıyor?)
10. Logging middleware'i Payment Service'e uygula
11. Unit testler yaz (en az 3 case: başarılı ödeme, geçersiz cart_id, sıfır tutarlı sepet)
12. Response sürelerini manuel olarak gözlemle (ileride latency injection için baseline)
13. Kod tekrarını azaltmak için ortak bir `services/common/` modülü çıkarmayı değerlendir (HTTP client, logging, config)
14. README'yi güncelle: Payment Service API dokümantasyonu

---

## FAZ 5 — Gün 5: Inventory Service ve Zincirin Tamamlanması

1. Stok veri modelini tasarla (product_id, stock_count, reserved_count)
2. Search Service'teki mock ürün listesiyle tutarlı bir stok veri seti oluştur
3. `GET /inventory/{product_id}` — mevcut stok sorgulama
4. `POST /inventory/{product_id}/reserve` — ödeme onaylandığında stok düşürme
5. `POST /inventory/{product_id}/release` — ödeme başarısızsa stok iade
6. Payment Service'i güncelle: ödeme başarılıysa Inventory Service'e reserve çağrısı yap
7. Ödeme başarısız senaryosunda Inventory'e release çağrısı yapılmasını sağla (telafi mantığı)
8. Dockerfile + docker-compose entegrasyonu
9. Search → Cart → Payment → Inventory tam zincirini uçtan uca manuel test et
10. Stok yetersizse ödeme akışının nasıl davrandığını test et (edge case)
11. Logging middleware'i Inventory Service'e uygula
12. Unit testler yaz (en az 3 case)
13. Tüm servislerin docker-compose ile tek komutla (`docker-compose up`) ayağa kalktığını doğrula
14. README'ye tam mimari diyagramı ve "4 servis şu an çalışıyor" durum notu ekle
15. Gün sonu: Postman/`.http` koleksiyonunu tam akış için güncelle

---

## FAZ 6 — Gün 6: Servisler Arası İletişimin Sağlamlaştırılması

1. Tüm servis-servis HTTP çağrılarını tek bir ortak `http_client.py`/`httpClient.js` modülünde topla
2. Timeout ve retry mantığı ekle (örn. 3 sn timeout, 1 retry)
3. Servis URL'lerini `.env`/config dosyasından okuyacak şekilde merkezi hale getir
4. Correlation/request ID üretimi ekle (henüz OTel yok, elle bir `X-Request-ID` header'ı taşı)
5. Hata durumlarında tutarlı bir JSON error response formatı standardize et (tüm servislerde aynı şema)
6. Circuit breaker mantığının gerekip gerekmediğini değerlendir, gerekirse basit bir versiyon ekle
7. docker-compose'a `depends_on` ve basit healthcheck tanımları ekle
8. Servislerin birbirini beklemeden (henüz hazır değilken) çağrılması senaryosunu test et
9. Load edilen `.env` değerlerinin her serviste doğru okunduğunu doğrula
10. Entegrasyon testi yaz: 4 servisi aynı anda ayağa kaldırıp uçtan uca akışı otomatik test eden bir script
11. README'ye "Servisler arası iletişim standartları" bölümü ekle
12. Kod tabanını gözden geçir, tekrar eden kodu `common/` modülüne taşı
13. Git commit + değişiklik notları

---

## FAZ 7 — Gün 7: Docker Compose Ağının Sağlamlaştırılması

1. docker-compose.yml'de tüm servisler için sabit bir custom network tanımla
2. Servis isimlerinin DNS olarak çalıştığını doğrula (container-to-container)
3. Port çakışmalarını kontrol et, gerekirse portları yeniden düzenle
4. Volume mount'ları değerlendir (log dosyaları için, geliştirme sırasında hot-reload için)
5. `docker-compose.override.yml` ile dev/prod ayrımı yapılıp yapılmayacağına karar ver
6. Healthcheck'leri her serviste `/health` endpoint'ine bağla (compose healthcheck direktifi)
7. `docker-compose up -d` sonrası tüm container'ların `healthy` durumuna geçtiğini doğrula
8. Container loglarını `docker-compose logs -f` ile izleyip anormallik var mı kontrol et
9. Kaynak limitleri (CPU/memory) için ilk taslak sınırlar ekle
10. Compose dosyasını `infra/docker-compose.yml` altına taşıyıp kök dizinden çalıştırmayı test et
11. Tek komutla tüm sistemin ayağa kalkıp health-check'lerin geçtiğini doğrulayan bir smoke-test script'i yaz
12. README'ye "Projeyi çalıştırma" bölümünü net adımlarla güncelle
13. Git commit, bu noktayı bir milestone olarak etiketle (`v0.1-services-ready`)

---

## FAZ 8 — Gün 8: OpenTelemetry SDK Entegrasyonu — Search Service

1. OpenTelemetry SDK ve otomatik instrumentation paketlerini Search Service'e kur
2. `TracerProvider` kurulumunu yap (service.name = "search-service" resource attribute'u ile)
3. FastAPI/Express için otomatik instrumentation middleware'ini bağla
4. Konsola trace çıktısı basan basit bir `ConsoleSpanExporter` ile ilk testi yap
5. Manuel bir span oluşturma örneği ekle (örn. kataloğu filtreleme adımını ayrı span yap)
6. Span attribute'ları ekle (örn. query, result_count)
7. Search endpoint'ine istek atıp konsolda trace/span çıktısını gözlemle
8. `trace_id` ve `span_id`'nin her istekte farklı üretildiğini doğrula
9. Hata durumunda span'in `ERROR` status'una geçtiğini test et (kasıtlı bir exception fırlat)
10. OTel konfigürasyonunu environment variable'lardan okuyacak şekilde parametrize et
11. Bu adımları `common/` modülüne taşıyıp diğer servislerde tekrar kullanılabilir hale getir
12. README'ye "Observability — Tracing kurulumu (Search Service)" notu ekle
13. Git commit

---

## FAZ 9 — Gün 9: OpenTelemetry SDK Entegrasyonu — Cart, Payment, Inventory

1. Gün 8'de oluşturulan ortak OTel modülünü Cart Service'e uygula
2. Aynı modülü Payment Service'e uygula
3. Aynı modülü Inventory Service'e uygula
4. Her serviste `service.name` resource attribute'unun doğru servisi yansıttığını doğrula
5. Servisler arası HTTP çağrılarında `traceparent` header'ının otomatik taşınıp taşınmadığını kontrol et (auto-instrumentation ile genelde otomatik)
6. Search → Cart → Payment → Inventory zincirinde tek bir isteğin aynı `trace_id`'yi taşıdığını konsol loglarından doğrula
7. Her serviste kritik iş adımlarına (örn. Payment'te "banka çağrısı" adımına) manuel child span ekle
8. Span attribute standardı belirle (örn. tüm servislerde `http.method`, `http.status_code` tutarlı olsun)
9. Hatalı senaryoları (stok yetersiz, ödeme başarısız) tetikleyip span'lerin error durumunu doğru yansıttığını test et
10. Trace çıktısını konsoldan okumanın zorlaştığı noktayı not al (Jaeger/Tempo ihtiyacının gerekçesi)
11. Unit/entegrasyon testlerinin OTel eklenmesinden sonra hâlâ geçtiğini doğrula
12. README güncelle: 4 servisin tamamında tracing aktif
13. Git commit, milestone (`v0.2-tracing-instrumented`)

---

## FAZ 10 — Gün 10: OTel Collector Kurulumu ve Trace Propagation Testi

1. OpenTelemetry Collector için `otel-collector-config.yaml` dosyasını oluştur
2. Receiver olarak OTLP (gRPC/HTTP) tanımla
3. Şimdilik `logging` exporter ile Collector'ın veriyi aldığını doğrula
4. Collector'ı docker-compose'a servis olarak ekle
5. 4 mikroservisin OTel exporter endpoint'lerini Collector'a yönlendir (console yerine)
6. `docker-compose up` sonrası Collector loglarında trace verisinin aktığını gözlemle
7. Uçtan uca bir isteği (Search→Cart→Payment→Inventory) tetikleyip Collector loglarında tek `trace_id` altında 4 servisin span'lerini ara
8. Trace propagation kopması olursa (farklı trace_id'ler) sebebini debug et ve düzelt
9. Collector için batch processor ekleyerek performansı iyileştir
10. Collector healthcheck'ini docker-compose'a ekle
11. Collector konfigürasyonunu `infra/otel-collector-config.yaml` altında versiyonla
12. README'ye Collector mimarisi ve veri akışı diyagramını ekle
13. Git commit

---

## FAZ 11 — Gün 11: Jaeger/Tempo Kurulumu ve Trace Görselleştirme

1. Jaeger (all-in-one) veya Grafana Tempo image'ını docker-compose'a ekle
2. OTel Collector'ın trace exporter'ını Jaeger/Tempo'ya yönlendir (Collector config güncelle)
3. Jaeger UI'a (örn. `localhost:16686`) tarayıcıdan erişip arayüzü keşfet
4. Uçtan uca bir sipariş isteği tetikleyip Jaeger UI'da `trace_id` ile arama yap
5. 4 servisin span'lerinin tek bir trace timeline'ında göründüğünü doğrula
6. Her span'in süresini (duration) inceleyip hangi servisin en çok zaman aldığını gözlemle
7. Kasıtlı olarak Payment Service'e sabit bir gecikme (örn. `time.sleep(0.5)`) ekleyip Jaeger'da bunun görünürlüğünü test et, sonra kaldır
8. Hata senaryosunda (stok yok) span'in Jaeger UI'da kırmızı/error olarak işaretlendiğini doğrula
9. Trace arama filtrelerini (servis adı, süre, hata durumu) dene
10. Jaeger/Tempo veri saklama (retention) ayarlarını gözden geçir, geliştirme için makul bir değer belirle
11. README'ye "Trace görselleştirme — Jaeger kullanımı" bölümü ekle, örnek ekran görüntüsü placeholder'ı koy
12. Git commit, milestone (`v0.3-tracing-visualized`)

---

## FAZ 12 — Gün 12: Prometheus Kurulumu ve Metrik Toplama

1. Her mikroservise `/metrics` endpoint'i ekle (prometheus_client / prom-client kütüphanesi ile)
2. Temel RED metriklerini tanımla: `http_requests_total` (Counter), `http_request_duration_seconds` (Histogram)
3. Her endpoint için method/path/status_code label'larını doğru şekilde ekle
4. Prometheus image'ını docker-compose'a ekle, `prometheus.yml` scrape config dosyasını oluştur
5. Scrape target olarak 4 servisin `/metrics` endpoint'lerini tanımla (servis adı + port)
6. Prometheus UI'a (`localhost:9090`) erişip target'ların `UP` durumunda olduğunu doğrula
7. Basit bir PromQL sorgusu çalıştır (örn. `rate(http_requests_total[1m])`)
8. Servislere birkaç manuel istek atıp metriklerin arttığını Prometheus'ta gözlemle
9. Custom bir iş metriği ekle (örn. Payment Service'te `payments_success_total`, `payments_failed_total`)
10. Inventory Service'te `stock_level` gauge metriği ekle
11. Prometheus scrape interval'ını (örn. 5s-15s) değerlendir ve ayarla
12. Prometheus config dosyasını `infra/prometheus.yml` altında versiyonla
13. README'ye "Metrics — Prometheus kurulumu" bölümü ekle
14. Git commit

---

## FAZ 13 — Gün 13: Grafana Kurulumu ve Veri Kaynağı Bağlantısı

1. Grafana image'ını docker-compose'a ekle
2. Grafana'ya `localhost:3000` üzerinden giriş yap, ilk kurulum adımlarını tamamla
3. Prometheus'u Grafana'da veri kaynağı (Data Source) olarak ekle
4. Jaeger/Tempo'yu Grafana'da veri kaynağı olarak ekle (Tempo kullanılıyorsa native destek daha kolay)
5. Grafana provisioning dosyalarını (`datasources.yaml`) oluşturup veri kaynaklarının otomatik yüklenmesini sağla
6. İlk basit paneli oluştur: toplam istek sayısı (tüm servisler)
7. Servis bazlı ayrı bir panel ekle (örn. dropdown/variable ile servis seçimi)
8. Panel zaman aralığını (time range) ve otomatik yenilemeyi (auto-refresh) test et
9. Grafana kullanıcı/parola yönetimini `.env` üzerinden parametrize et (default admin/admin'i değiştir)
10. Dashboard'ı JSON olarak export edip `infra/grafana/dashboards/` altına kaydet
11. Provisioning ile dashboard'ın container yeniden başlatıldığında otomatik yüklendiğini doğrula
12. README'ye "Dashboard — Grafana kurulumu" bölümü ekle
13. Git commit, milestone (`v0.4-dashboards-connected`)

---

## FAZ 14 — Gün 14: RED Method Dashboard'ının Tamamlanması

1. Her servis için Rate paneli oluştur (`rate(http_requests_total[1m])`, servis bazlı ayrım)
2. Her servis için Error Rate paneli oluştur (5xx oranı / toplam istek oranı)
3. Her servis için Duration (p50/p95/p99 latency) paneli oluştur (histogram_quantile ile)
4. Payment Service için özel "ödeme başarı/başarısızlık oranı" paneli ekle
5. Inventory Service için "güncel stok seviyesi" paneli ekle
6. Genel bir "sistem sağlığı" özet paneli (tüm servislerin health durumu) oluştur
7. Panel eşik renklerini (yeşil/sarı/kırmızı) hata oranına göre ayarla
8. Dashboard'a değişkenler (variable) ekle: servis seçimi, zaman aralığı
9. Panelleri mantıklı bir düzende gruplandır (Rate/Errors/Duration ayrı satırlar halinde)
10. Test amaçlı birkaç istek daha atıp panellerin gerçek zamanlı güncellendiğini doğrula
11. Dashboard'ı tekrar JSON olarak export edip versiyonla
12. README'ye dashboard'ın ekran görüntüsü placeholder'ı ve panel açıklamaları ekle
13. Git commit

---

## FAZ 15 — Gün 15: Gecikme ve Hata Enjeksiyonu

1. Search Service'e rastgele %5 olasılıkla ekstra gecikme (örn. 300-800ms) ekleyen bir middleware/fonksiyon yaz
2. Bu davranışı environment variable ile açılıp kapanabilir hale getir (`CHAOS_ENABLED=true/false`)
3. Payment Service'e rastgele %3 olasılıkla HTTP 500 veya "POS timeout" hatası döndüren mantık ekle
4. Enjeksiyon oranlarını config'den okunacak şekilde parametrize et (sabit kodlamaktan kaçın)
5. Chaos davranışı aktifken sistemin genel akışının (Search→Cart→Payment→Inventory) hâlâ çalıştığını doğrula
6. Birkaç yüz manuel/scriptli istek atıp gecikme ve hata oranlarının beklenen yüzdelere yakın gerçekleştiğini doğrula
7. Jaeger'da enjekte edilen gecikmenin ilgili span'de göründüğünü kontrol et
8. Prometheus/Grafana'da error rate panelinin bu enjeksiyonu yakaladığını doğrula
9. Payment hata senaryosunda Inventory'e `release` çağrısının doğru tetiklendiğini tekrar test et (telafi mantığı hâlâ çalışıyor mu)
10. Enjeksiyon parametrelerini README'de dokümante et (nasıl açılır/kapanır, oranlar nasıl değiştirilir)
11. Bu davranışı test ortamı dışında (production benzeri) kapatmanın önemini not olarak ekle
12. Git commit, milestone (`v0.5-chaos-injection`)

---

## FAZ 16 — Gün 16: İş Senaryosu — Sipariş Darboğazı Tespiti

1. "Payment → Bank API" adımını simüle eden fonksiyona kasıtlı, belirgin bir gecikme (örn. 3-4 sn) ekle (geçici test amaçlı)
2. Bu gecikmeli senaryoda uçtan uca bir sipariş isteği gönder
3. Jaeger UI'da ilgili `trace_id`'yi bul, hangi span'in toplam süreyi domine ettiğini tespit et
4. Bulguyu (darboğazın Payment→Bank-API adımında olduğunu) bir mini-rapor olarak `docs/senaryo-darbogaz.md` dosyasına yaz
5. Grafana'da bu gecikmenin p95/p99 latency panelinde nasıl göründüğünü ekran görüntüsüyle belgele
6. Aynı senaryoyu chaos enjeksiyonu (Gün 15) ile karşılaştır, ikisinin dashboard'da nasıl ayırt edildiğini analiz et
7. Test amaçlı eklenen sabit gecikmeyi kaldır (yalnızca env-flag ile açılabilir chaos mekanizması kalsın)
8. Senaryo dokümanına "bu tespiti observability olmadan yapmak neden zor olurdu" kısmını ekle
9. Bir "runbook" taslağı yaz: benzer bir yavaşlık production'da yaşanırsa hangi adımlar izlenir (trace_id bul → Jaeger'da ara → en uzun span'i tespit et → ilgili servisi incele)
10. README'ye bu senaryoyu örnek kullanım vakası olarak ekle
11. Git commit

---

## FAZ 17 — Gün 17: Yük Testi Aracının Kurulumu (Locust veya k6)

1. Locust veya k6 aracını projeye ekle (tercih ettiğin dile göre karar ver)
2. Temel bir yük testi senaryosu yaz: kullanıcı arama yapar → sepete ekler → ödeme yapar
3. Test senaryosunu ayrı bir `load-test/` klasöründe organize et
4. Locust/k6'yı docker-compose'a (opsiyonel, ayrı bir servis olarak) veya local çalıştırılacak şekilde entegre et
5. Küçük bir yük testi (örn. 10 eşzamanlı kullanıcı, 1 dakika) çalıştırıp sistemin genel davranışını gözlemle
6. Test sırasında Grafana dashboard'ını canlı izle, metriklerin arttığını doğrula
7. Test sonuçlarını (Locust/k6 raporu) `docs/load-test-results/` altına kaydet
8. Yük testi sırasında karşılaşılan hataları (varsa) not al, kaynağını araştır
9. Yük testi parametrelerini (kullanıcı sayısı, süre, ramp-up) kolayca değiştirilebilir hale getir
10. README'ye "Yük testi nasıl çalıştırılır" bölümü ekle
11. Git commit

---

## FAZ 18 — Gün 18: Kampanya/Flaş İndirim Trafik Simülasyonu

1. Yük testi senaryosunu büyüt: 1000 isteklik ani bir trafik patlaması simülasyonu tasarla
2. Locust/k6 script'inde "spike" (ani yük artışı) senaryosu için ayrı bir profil oluştur
3. Chaos enjeksiyonunu (Gün 15) aktif ederek yüksek trafik altında hata oranlarının nasıl değiştiğini gözlemle
4. Testi çalıştır, Grafana'da `http_requests_total` ve `http_request_duration_seconds` metriklerinin "coştuğunu" doğrula
5. Bu yoğun trafik sırasında Jaeger'da yavaşlayan/hata veren trace'leri filtreleyerek incele
6. Sistemin hangi servisinin bu yükte en çok zorlandığını (en yüksek latency/error) tespit et
7. Bulguları `docs/senaryo-kampanya-trafik.md` dosyasına yaz (bottleneck neresi, neden)
8. Grafana dashboard'ında bu spike senaryosunu gösteren bir ekran görüntüsü/export al
9. Test sonrası sistemin normale döndüğünü (metriklerin baseline'a indiğini) doğrula
10. Bu senaryoyu README'de ikinci örnek kullanım vakası olarak ekle
11. Git commit, milestone (`v0.6-load-tested`)

---

## FAZ 19 — Gün 19: Alertmanager Kurulumu ve Otomatik Alarm

1. Prometheus Alertmanager image'ını docker-compose'a ekle
2. `alertmanager.yml` konfigürasyon dosyasını oluştur (temel receiver tanımı)
3. Prometheus'a bir alert rule ekle: Payment Service HTTP 500 oranı 5 dakikalık pencerede %2'yi geçerse tetiklensin
4. Alert rule'u Prometheus'un `rules/` dosyasına yaz, Prometheus config'de rule dosyasını tanımla
5. Slack webhook (veya alternatif olarak e-posta/log tabanlı bir mock receiver) entegrasyonunu Alertmanager'a ekle
6. Chaos enjeksiyon oranını geçici olarak yükseltip (örn. %3 → %10) alarmı bilerek tetikle
7. Alarmın Alertmanager UI'da (`localhost:9093`) göründüğünü doğrula
8. Slack/webhook bildiriminin gerçekten ulaştığını test et
9. Alarm çözüldüğünde (error rate normale döndüğünde) "resolved" bildiriminin gittiğini doğrula
10. Chaos oranını normale (%3) döndür
11. İkinci bir basit alert kuralı daha ekle (örn. bir servis 1 dakikadan uzun süre `/health` cevabı vermiyorsa)
12. README'ye "Alerting — Alertmanager kurulumu" bölümü ekle
13. Git commit, milestone (`v0.7-alerting-ready`)

---

## FAZ 20 — Gün 20: Dokümantasyon, Sunum Hazırlığı ve Kapanış

1. Tüm mimariyi özetleyen final bir diyagram çiz (servisler, OTel Collector, Prometheus, Jaeger, Grafana, Alertmanager)
2. README.md dosyasını baştan sona gözden geçir, eksik/güncel olmayan bölümleri düzelt
3. `docker-compose up` ile projenin sıfırdan (temiz bir ortamda) sorunsuz ayağa kalktığını son kez doğrula
4. Tüm Grafana dashboard'larını JSON olarak export edip `infra/grafana/dashboards/` içinde güncel tut
5. İki senaryo dokümanını (darboğaz tespiti, kampanya trafik simülasyonu) son haliyle gözden geçir
6. Projenin "3 Pillars of Observability" (Traces/Metrics/Logs) açısından neyi kapsayıp neyi kapsamadığını netleştir
7. Bilinen kısıtları/eksikleri (örn. log toplama katmanının basit kaldığı, gerçek bir veritabanı yerine mock veri kullanıldığı) bir "Sınırlamalar" bölümünde listele
8. Staj/okul raporu için gerekli ekran görüntülerini (Jaeger trace örneği, Grafana dashboard, Alertmanager alarm örneği) topla
9. Kısa bir demo akışı senaryosu yaz: "şu isteği at → şurada trace'i göster → şu paneli göster → alarmı tetikle"
10. Proje kod tabanını son kez temizle (kullanılmayan kod, TODO'lar, gereksiz loglar)
11. `v1.0` etiketiyle son bir Git commit/tag at
12. Sunum/rapor için proje özetini (1-2 paragraf, teknik olmayan dille) hazırla
13. FAZLAR.md'deki tüm günleri gözden geçirip fiilen tamamlanan/tamamlanmayan adımları işaretle (retrospektif)
14. Sonraki adımlar (opsiyonel geliştirmeler: gerçek log pipeline'ı, canary deployment simülasyonu vb.) için bir "gelecek fikirler" listesi bırak

---

## Notlar

- Her gün sonunda mutlaka bir Git commit atılmalı; commit mesajı o günün ana çıktısını özetlemeli.
- Bir günün adımları bitmeden bir sonraki güne geçilmemeli; sıkışma olursa aynı fazda ek gün eklenebilir (örn. Faz 10a, 10b).
- Chaos/enjeksiyon mekanizmaları varsayılan olarak kapalı (`CHAOS_ENABLED=false`) tutulmalı, yalnızca ilgili test günlerinde açılmalı.