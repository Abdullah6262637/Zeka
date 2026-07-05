# Zeka → Mobil Antigravity Dönüşüm Planı
## "Kod Modu" Merkezli Mimari ve Yol Haritası

---

## 0. Temel Tasarım Kararı: İki Mod

Zeka'yı tek bir sohbet uygulaması olmaktan çıkarıp iki net moda ayırıyoruz. Bu, hem kullanıcı deneyimini basit tutar hem de agentic (otonom) özelliklerin karmaşıklığını normal sohbeti bozmadan izole eder.

| | **Sohbet Modu (mevcut)** | **Kod Modu (yeni)** |
|---|---|---|
| Varsayılan durum | Açılışta aktif | Üstteki anahtar/buton ile açılır |
| Davranış | Soru-cevap, MCP araçları (GitHub okuma, Notion vb.) | Ajan planlar, sandbox'ta kod yazar/çalıştırır, terminal ve tarayıcı kullanır |
| Onay gerekliliği | Yok | Riskli her adımda insan onayı (varsayılan) |
| Çıktı biçimi | Düz metin/markdown cevap | **Artifact**'lar: Görev Planı, Uygulama Planı, Terminal Çıktısı, Ekran Görüntüsü |
| Arka plan çalışma | Yok | Var — görev kapanınca da backend'de sürebilir, push bildirimle haber verir |
| Kaynak erişimi | Yok | Workspace'e bağlı repo/klasör, sandbox dosya sistemi |

**Neden bu ayrım şart:** Antigravity'nin karmaşıklığının tamamını (sandbox, terminal, tarayıcı, çoklu ajan) her sohbete bulaştırmak hem güvenlik riskini hem bilişsel yükü artırır. Kod Modu, kullanıcının bilinçli olarak "şimdi bana bir görevi otonom yürüt" dediği, ayrı bir arayüz ve izin seti olan bir alan olmalı.

---

## 1. Kod Modu'na Basınca Ne Olmalı? (UX Akışı)

1. Kullanıcı sohbet ekranındaki **"Kod Modu"** anahtarına basar.
2. Sistem önce bir **Workspace seçtirir**: "Yeni proje", "GitHub reposu bağla" veya "Var olan workspace'i aç".
3. Workspace seçildikten sonra ekran ikiye ayrılır (mobilde sekme/tab olarak):
   - **Sohbet/Görev sekmesi**: kullanıcı doğal dilde görev tanımlar ("Bu repoda login ekranındaki hatayı düzelt")
   - **Artifact/Görev Panosu sekmesi**: ajanın ürettiği plan, kod diff'i, terminal çıktısı, ekran görüntüleri buradan izlenir
4. Ajan görevi alt adımlara böler → **Görev Planı** artifact'ı üretir → kullanıcıdan "Devam Et" onayı ister.
5. Onay sonrası ajan sandbox'ta çalışır (kod yazar, komut çalıştırır, gerekiyorsa headless tarayıcıda test eder).
6. Her kritik adımda (dosya silme, dış API çağrısı, bağımlılık kurulumu, git push) **onay kartı** çıkar.
7. Görev bitince kullanıcı telefon kilitliyken bile arka planda ilerleme olur; tamamlanınca **push bildirimi** gelir.
8. Kullanıcı Artifact üzerine yorum bırakabilir ("bu fonksiyonu değiştir"), ajan görevi durdurmadan bunu işler.

---

## 2. Mimari: Neyin Nerede Çalışacağı

Mobilde gerçek kod çalıştırma/terminal/tarayıcı mümkün olmadığından **tüm agentic yürütme backend'de**, mobil uygulama sadece **kumanda + izleme paneli** olacak.

```
[Android - Zeka App]
   |  (REST/SSE, WebSocket)
   v
[Zeka Backend - Ktor]
   |-- Agent Orchestrator (yeni)
   |     |-- Planner (LLM: görevi alt görevlere böler)
   |     |-- Executor / Agent Loop (ReAct benzeri döngü)
   |     |-- Tool Registry (terminal, dosya, browser, MCP)
   |-- Sandbox Manager (yeni)
   |     |-- Docker-in-Docker veya Firecracker microVM
   |     |-- Her workspace = izole konteyner
   |-- Artifact Store (MinIO üzerine inşa)
   |-- Task Queue (Redis + BullMQ benzeri, veya Kafka)
   |-- Notification Service (FCM push)
   |-- Headless Browser Service (Playwright, konteynerde)
```

### Yeni Backend Bileşenleri (detay)

**a) Agent Orchestrator**
- `Planner`: kullanıcı isteğini alır, LLM'e "şu görevi adımlara böl, JSON şemasında dön" der.
- `Agent Loop`: klasik plan → tool_call → gözlem → güncelle plan döngüsü. Zaman aşımı, maksimum adım sayısı, sonsuz döngü koruması olmalı.
- `Tool Registry`: her aracın (terminal, dosya okuma/yazma, git, browser, MCP servisleri) JSON şema tanımı, izin seviyesi (otomatik / onay gerekli / yasak) burada tutulur.

**b) Sandbox Manager**
- Her workspace için izole, kaynak sınırlı (CPU/RAM/disk limitli) bir konteyner.
- Konteyner dışına ağ erişimi varsayılan kapalı; sadece izin verilen domain'lere (npm registry, pip index vb.) açık.
- Konteyner ömrü: görev bitince veya X dakika hareketsizlikte otomatik durur (maliyet kontrolü).

**c) Artifact Store**
- Şema: `artifact_id, workspace_id, task_id, type (plan/diff/screenshot/log/video), content_ref (MinIO path), created_at, status`
- Mobil taraf bunu sayfalı şekilde çeker, tipe göre farklı kart render eder (diff viewer, resim, log terminali).

**d) Task/Job Sistemi**
- Uzun süren görevler mobil bağlantısından bağımsız backend'de sürmeli → Redis/queue + worker mimarisi.
- Görev durumları: `planned, awaiting_approval, running, paused, completed, failed`.

**e) Bildirim Servisi**
- Onay gerektiğinde veya görev bittiğinde FCM push. Derin link ile doğrudan ilgili Artifact ekranına yönlendirme.

---

## 3. Mobil (Android) Tarafında Yapılacaklar

1. **Kod Modu anahtarı ve Workspace seçim ekranı** (yeni Compose ekranları)
2. **Görev Panosu (Manager View mobil karşılığı)**: aktif/bekleyen/tamamlanan görevlerin kart listesi, durum rozetleri
3. **Artifact bileşenleri**:
   - Görev Planı kartı (checklist görünümü, tamamlanan adımlar işaretli)
   - Kod Diff görüntüleyici (syntax highlight, +/- satır renklendirme — mevcut açık kaynak diff kütüphaneleri değerlendirilebilir)
   - Terminal/log görüntüleyici (mono font, kaydırılabilir, ANSI renk desteği)
   - Ekran görüntüsü/video artifact görüntüleyici
4. **Onay kartı bileşeni**: "Ajan şunu yapmak istiyor: `rm -rf node_modules && npm install`. Onaylıyor musun?" + Onayla/Reddet/Düzenle
5. **WebSocket/SSE bağlantı yönetimi**: görev ilerlemesini canlı akıtmak için (Retrofit zaten var, OkHttp WebSocket eklenebilir)
6. **Arka plan senkronizasyon**: WorkManager ile periyodik durum kontrolü + FCM entegrasyonu
7. **GitHub/Git entegrasyonu**: OAuth ile repo bağlama, commit/push işlemlerini onay akışına dahil etme

---

## 4. Güvenlik — Kod Modu'nun Olmazsa Olmazları

Antigravity'nin piyasaya çıkışından 24 saat sonra ajan yapılandırması manipüle edilerek kötü amaçlı kod çalıştırma açığı bulunmuştu. Bunu baştan tasarıma dahil edin:

- **Varsayılan olarak her yıkıcı/geri alınamaz işlem onay ister**: dosya silme, `git push --force`, dış ağ isteği, ödeme/kimlik bilgisi kullanımı, bağımlılık kurulumu.
- **Sandbox = tek güven sınırı**: sandbox'tan gerçek kullanıcı cihazına veya backend'in ana sistemine hiçbir kanal açılmamalı.
- **Prompt injection savunması**: MCP'den veya taranan bir web sayfasından gelen içerik asla "sistem talimatı" gibi işlenmemeli; ajan tool sonuçlarını her zaman "güvenilmeyen veri" etiketiyle işlemeli.
- **Ağ allowlist**: sandbox konteynerlerinin çıkış trafiği varsayılan kapalı, sadece paket yöneticileri gibi bilinen domain'lere izinli.
- **Denetim kaydı (audit log)**: ajanın attığı her adım, kullandığı komut, verdiği onaylar kalıcı loglanmalı — hem güvenlik hem hata ayıklama için.
- **Kaynak/kota limiti**: kullanıcı başına eşzamanlı sandbox sayısı, CPU/RAM/süre kotası (maliyet + kötüye kullanım kontrolü).

---

## 5. Fazlı Yol Haritası

### Faz 0 — Repo Hijyeni (2 hafta)
- APK'yı repodan çıkarıp GitHub Releases'e taşı
- `k8s-secrets.yaml` gibi dosyaları temizle, gerçek sır varsa git geçmişinden sil, `.env.example` şablonları bırak
- CI/CD kur (GitHub Actions: lint, build, test)
- Sürüm etiketleme (semver) ve CHANGELOG başlat

### Faz 1 — Sandbox + Basit Agent Loop (4-6 hafta)
- Backend'de Docker tabanlı sandbox altyapısı (tek workspace, tek konteyner)
- Planner + tek adımlı Agent Loop: kullanıcı görev verir → plan üretilir → tek komut çalıştırılır → sonuç gösterilir
- Mobilde: Kod Modu anahtarı, basit görev ekranı, tek tip "işlem çıktısı" kartı
- Onay mekanizması (her komut için basit onay/red)

### Faz 2 — Artifacts ve Görev Panosu (4-6 hafta)
- Artifact veri modeli + MinIO entegrasyonu tam devreye alınır
- Görev Planı, Diff, Log artifact tipleri
- Mobilde Görev Panosu ekranı (birden fazla görev, durum takibi)
- Workspace/GitHub repo bağlama akışı

### Faz 3 — Asenkron Orkestrasyon (6-8 hafta)
- Görevlerin backend'de bağımsız sürmesi (queue + worker)
- FCM push bildirim + derin link
- Çoklu görev paralel çalıştırma (aynı workspace'te veya farklı workspace'lerde)
- Headless browser tool (Playwright) — web projelerinde otomatik test + ekran görüntüsü artifact'ı

### Faz 4 — Bellek/Öğrenme ve Olgunlaştırma (devam eden)
- pgvector ile "başarılı çözümler / mimari kararlar" bilgi tabanı
- Skills sistemi: kullanıcı veya organizasyon bazlı, projeye özel talimat paketleri (Antigravity'deki Agent Skills mantığına benzer: `SKILL.md` + destek dosyaları, ihtiyaç anında yüklenen)
- Güvenlik denetimi / sandbox escape testleri (üçüncü parti pentest)
- Kapalı beta → geri bildirim → Play Store yayını

---

## 6. Öncelik Matrisi (Neye Önce Yatırım Yapmalı)

| Bileşen | Etki | Zorluk | Öncelik |
|---|---|---|---|
| Sandbox altyapısı | Çok yüksek | Yüksek | 1 |
| Basit Agent Loop | Çok yüksek | Orta | 1 |
| Onay/güvenlik katmanı | Çok yüksek | Orta | 1 |
| Artifact sistemi | Yüksek | Orta | 2 |
| Görev Panosu (mobil) | Yüksek | Orta | 2 |
| Asenkron/push bildirim | Orta-Yüksek | Orta | 3 |
| Headless browser tool | Orta | Yüksek | 3 |
| Çoklu ajan paralel çalışma | Orta | Yüksek | 4 |
| Bellek/öğrenme katmanı | Orta | Yüksek | 4 |

---

## 7. Riskler ve Dikkat Edilmesi Gerekenler

- **Maliyet**: sandbox konteynerleri ve LLM tool-call döngüleri (özellikle uzun ajan döngülerinde) ciddi API/altyapı maliyeti yaratır — kullanıcı başına kota şart.
- **Mobil bağlantı kopuklukları**: görev backend'de sürerken kullanıcı offline olabilir; state senkronizasyonu buna dayanıklı tasarlanmalı (idempotent job durumu, yeniden bağlanınca senkron).
- **Güven inşası**: kullanıcı otonom kod çalıştırmaya güvenmeli — bu yüzden Artifact'lar (şeffaflık) ve onay akışı, "hız"dan daha öncelikli tasarım hedefi olmalı.
- **Kapsam sürünmesi**: Faz 1'i "sadece tek komut çalıştırıp sonucu göster" ile sınırlı tutup, çoklu ajan/asenkron gibi karmaşık kısımları sonraki fazlara ertelemek kritik — aksi halde proje hiç bitmeyen bir "büyük patlama" haline gelir.

---

*Bu plan, mevcut Zeka mimarisi (Kotlin/Compose Android + Ktor/PostgreSQL/Redis/MinIO backend) üzerine inşa edilecek şekilde tasarlanmıştır. Faz 1'e başlamadan önce sandbox teknolojisi seçimi (Docker-in-Docker vs. Firecracker vs. gVisor) için ayrı bir teknik değerlendirme (ADR) yapılması önerilir.*
