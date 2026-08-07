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
flo-observability-sim/
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
git clone https://github.com/Mustafa-Aydin69/flo-observability-sim.git
cd flo-observability-sim
docker-compose up --build
```

Tüm servisler ayağa kalktıktan sonra:

| Arayüz | URL |
|---|---|
| Jaeger UI | http://localhost:16686 |
| Prometheus UI | http://localhost:9090 |
| Grafana | http://localhost:3000 |
| Alertmanager | http://localhost:9093 |

## Örnek Kullanım Senaryoları

1. **Sipariş Darboğazı Tespiti** — Bir isteğin hangi serviste ne kadar zaman harcadığını `trace_id` ile Jaeger üzerinden tespit etme. Detay: [`docs/senaryo-darbogaz.md`](docs/senaryo-darbogaz.md)
2. **Kampanya Trafik Simülasyonu** — Locust ile ani yük artışı simüle edip sistemin darboğaz noktalarını Grafana dashboard'larıyla gözlemleme. Detay: [`docs/senaryo-kampanya-trafik.md`](docs/senaryo-kampanya-trafik.md)

## Geliştirme Planı

Projenin 20 günlük, gün gün ilerleyen detaylı faz planı için [`FAZLAR.md`](FAZLAR.md) dosyasına bakın.

## Sınırlamalar

- Servisler arası veri gerçek bir veritabanı yerine mock/in-memory veri ile simüle edilir
- Log toplama katmanı bu sürümde basit tutulmuştur, kapsamlı bir log pipeline'ı içermez
- Gecikme/hata enjeksiyon mekanizmaları yalnızca test amaçlıdır, varsayılan olarak kapalıdır

## Lisans

Bu proje bir staj/okul çalışması kapsamında geliştirilmiştir.