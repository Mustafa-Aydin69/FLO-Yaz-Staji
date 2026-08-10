# CLAUDE.md

Bu dosya, bu repoda çalışırken Claude Code'un uyması gereken kuralları tanımlar.

## Git / GitHub Kuralları

**Commit ve push işlemlerini Claude yapmaz — bunları Mustafa kendisi yapar.**

### Çalışma akışı

1. Claude, `git add` / `git commit` / `git push` komutlarını **çalıştırmaz**. Kod değişikliklerini yapar, gerekirse test eder, ama commit/push adımını kullanıcıya bırakır.
2. Bir çalışma birimi tamamlandığında, Claude şunları içeren bir **commit geri bildirimi** sunar. Çalışma birimi, bir FAZ'ın tamamı değil, o FAZ içindeki **tek bir numaralı alt madde**dir (örn. Faz 5'in "1. Stok veri modelini tasarla" maddesi) — madde tek başına derlenmeyen/çalışmayan küçük bir parça olsa bile, geri bildirim yine de sunulur:
   - Değişikliklerin kısa özeti (ne değişti, neden)
   - Önerilen commit mesajı (sade, teknik, normal bir geliştirici commit'i formatında — örn. `feat: add payment service health check`)
   - Varsa dikkat edilmesi gereken noktalar (örn. `.env`/secret içeren dosyaların stage edilmemesi)
3. Commit'i ve push'u Mustafa kendi git kimliğiyle (`Mustafa-Aydin69`) kendisi çalıştırır.
4. Commit mesajı önerilerinde AI/Claude'a referans veren ifade, imza veya trailer (`Co-Authored-By: Claude`, `Generated with Claude Code` vb.) **bulunmaz** — mesajlar öneri niteliğinde, teknik ve normal commit diliyle yazılır.

## Geliştirme Akışı — FAZLAR.md

FAZLAR.md'deki maddeler **tek tek, sırayla** uygulanır — birden fazla madde aynı anda birden yapılmaz. Her madde bitince:

1. Claude o maddeye ait değişiklikleri tamamlar.
2. Claude yukarıdaki commit geri bildirimini sunar.
3. Mustafa commit/push işlemini yapar ve bir sonraki maddeye geçilir.

### Uygulamaya başlamadan önce izin

**Claude, bir FAZ'ın alt adımlarını (1-14 gibi numaralı listeyi) tek seferde, onay almadan kod olarak uygulamaz.** Mustafa "maddeleri planlayalım", "getirir misin", "bakalım" gibi bir şey söylediğinde bu bir **planlama/tartışma daveti**dir, uygulama talimatı değildir — plan netleşse bile Claude kodu yazmaya başlamadan önce açıkça "uygulayayım mı?" diye sormalı veya Mustafa'nın net bir "başla/devam et/uygula" onayını beklemelidir.

- Bir FAZ'ın planı birlikte netleştirildikten sonra bile, Claude uygulamaya geçmeden önce kısa bir onay ister.
- Onay alınmadan dosya oluşturma/düzenleme yapılmaz.
- Belirsizlik varsa (istek planlama mı yoksa uygulama emri mi?), Claude planlama olarak yorumlar ve sorar.

## Proje Bağlamı

FLO Perakende Observability Simülatörü — Java Spring Boot ile yazılan mikroservisler (Search, Cart, Payment, Inventory), OpenTelemetry ile distributed tracing (Jaeger), Prometheus + Grafana ile metrik/dashboard, Locust ile yük testi, Alertmanager ile alarm. Detaylı faz planı için `FAZLAR.md` dosyasına bak.