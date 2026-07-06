# CHANGELOG

Tüm önemli değişiklikler bu dosyada listelenecektir.

Projenin sürüm yönetimi [SemVer](https://semver.org/spec/v2.0.0.html) (Semantic Versioning) standartlarına uygun olarak yapılmaktadır.

---

## [1.5.0-Phase5] - 2026-07-06

### Eklendi
- **Eklenti (.zpack) & JS Sandbox Orkestrasyonu (Backend):**
  - **Eklenti Yükleyici:** ZIP sıkıştırılmış eklenti paketlerini parse edip `.agents/plugins/` dizinine çıkaran ve etkinleştiren `PluginManager` geliştirildi.
  - **JavaScript Sandbox Yorumlayıcı:** Eklenti veya yetenek betiklerini Docker node sandbox konteyneri içinde izole parametrelerle çalıştıran `JsSandboxExecutor` entegre edildi.
  - **Uç Noktalar:** `/install`, `/plugins` listeleme ve durum güncelleme API rotaları `AgentRoutes` altına eklendi.
- **Gelişmiş Arayüz (UI/UX) & MCP Onay Akışı (Android):**
  - **Mesaj Kutusuna Entegre Kod Modu:** Kod Modu seçici kontrolü üst menüden kaldırılarak mesaj yazma kutusunun (capsule) sol köşesine yerleştirildi. Aktif edildiğinde temaya uygun gri/beyaz `Icons.Sharp.Code` simgesi ve mini bir `KOD` rozeti belirir.
  - **Slash Suggester (Yetenek Listesi):** Kullanıcı mesaj alanına `/` (slash) karakterini girdiğinde, kayıtlı tüm yetenekleri listeleyen ve filtreleyen premium monokrom `SkillsSuggestionPopup` açılır.
  - **Aktif Yetenek Kapsülü:** Bir yetenek seçildiğinde mesaj yazma alanının içinde şık bir active skill kapsülü (`[ ⚡ skill_name ]`) belirir ve gönderilen mesaja o yeteneğin prompt yönergeleri otomatik eklenir.
  - **Yenilenen Kota & Workspace Kartı:** Çalışma alanı ve kota kartı, ekranın ortasında yer kaplamayacak ve metin taşması yapmayacak şekilde `3 kolonlu` grid/tag düzenine dönüştürüldü.
  - **Monokrom Tema Optimizasyonu:** Uygulamadaki tüm neon yeşil arayüz renkleri, saf siyah AMOLED temayla uyumlu monokrom gri ve beyaz tonlarıyla değiştirildi (ikonlar, butonlar, diyalog çerçeveleri ve tablolar dahil).
  - **Logosuz Kartlar:** Eklenti ve yetenek kartları gereksiz logo resimlerinden arındırılarak minimalist monokrom sistem simgeleriyle optimize edildi.
  - **McpConsentDialog:** Ajan yerel rehbere veya takvime erişmeye çalıştığında açılan, onay veya red yetkisi sunan premium AMOLED siyah & beyaz arayüz penceresi.
  - **Canlı Tool Çipleri:** Giriş alanının üstünde konumlanan, aktif aracın durumuna göre pulsing border animasyonlu visual live tool indicator.

## [1.4.0-Phase4] - 2026-07-06

### Eklendi
- **Bellek (Memory) & Kota (Quota) Yönetimi (Backend):**
  - **Veritabanı Şeması:** Ajanın başarılı komut ve kararlarını vektör benzeri formatta tutan `AgentMemory` ve kullanıcı başına kaynak takibi yapan `UserQuotas` tabloları eklendi.
  - **Memory & Quota Servisleri:** Kota kontrollerini yürüten `QuotaManager` ve başarılı komutları kaydeden `MemoryManager` servisleri geliştirildi.
  - **Dinamik Skill Yükleyici:** Projedeki `.agents/skills/<skill_name>/SKILL.md` kurallarını okuyup parser'dan geçiren ve LLM Planner sistem talimatlarına enjekte eden `PromptSkillLoader` entegre edildi.
- **Docker Sandbox Güvenlik Sıkılaştırması (Backend):**
  - Konteyner başlatılırken RAM kullanım sınırı (`1024m`) ve CPU işlemci sınırı (`1.0` - 1 Core) getirilerek sunucu kaynakları koruma altına alındı.
- **Ajan Bellek ve Kota Paneli (Android):**
  - Kod Modu ekranına, çalışma alanı bilgisinin hemen altına kalan Token limitini, kalan CPU saniyesini ve aktif bellek kaydı adedini gösteren şık monokrom statü widget'ı eklendi.

## [1.3.0-Phase3] - 2026-07-06

### Eklendi
- **Asenkron Arka Plan Kuyruğu & Bildirimler (Backend):**
  - **Asenkron Kuyruk Servisi:** Kotlin Coroutines `Channel` tabanlı in-memory asenkron görev kuyruğu (`AgentQueueProcessor`) eklendi.
  - **Asenkron POST Uç Noktası:** `/execute` isteği kuyruğa delege edilerek anında `status = "enqueued"` cevabı dönecek şekilde güncellendi.
  - **Simüle FCM Bildirimleri:** Görev bittiğinde, onay gerektiğinde veya başarısız olduğunda bildirim basan `NotificationManager` eklendi.
  - **Ekran Görüntüsü Desteği:** Sandbox'taki arayüz testleri tamamlandığında veritabanına otomatik `screenshot` türünde artifact yazan simülasyon mekanizması kuruldu.
- **Asenkron Polling & Tarayıcı Görüntüleri (Android):**
  - Adım yürütüldüğünde sunucuyu asenkron sorgulayan (2 saniyede bir polling) ve ajan durumu `running` olduğu sürece UI'ı güncelleyen `pollAgentSessionStatus` mekanizması.
  - **Tarayıcı Ekran Görüntüsü Kartı:** Ajanın ürettiği screenshot URL'lerini Coil kütüphanesiyle görselleştiren neon çerçeveli premium `ScreenshotCard` eklendi.

## [1.2.0-Phase2] - 2026-07-06

### Eklendi
- **Canlı Takip & Artifact Altyapısı (Backend):**
  - **Veritabanı Tablosu:** Planlar, diff çıktıları ve logları saklamak için `Artifacts` tablosu oluşturuldu ve auto-migrate listesine eklendi.
  - **Artifact Servisi:** Ajan planı ve terminal loglarını veritabanına kaydeden ve getiren `ArtifactManager` servisi eklendi.
  - **GET Uç Noktası:** Oturum bazlı artifact'leri sorgulayan `/api/v1/agent/session/{sessionId}/artifacts` route eklendi.
- **Canlı Takip & Sekmeli Görünüm (Android):**
  - Kod modunda "Konsol" ve "Çıktılar" (Artifacts) arasında pürüzsüz geçiş sağlayan monokrom sliding indicator tab bar arayüzü entegre edildi.
  - **Plan Checklist Kartı:** Ajanın oluşturduğu planı checkbox'lar ile interaktif listeye döken `PlanChecklistCard` eklendi.
  - **Kod Değişikliği Diff Kartı:** Yapılan kod satırı ekleme/silmelerini monokrom yeşil/kırmızı renklendirme ile gösteren mobil uyumlu `DiffViewerCard` eklendi.
- **Zero-Config Geliştirme (Sunucu & İstemci):**
  - `gradlew.bat` dosyaları güncellenerek sistemde Java tanımlı olmasa dahi kök dizindeki yerel JDK'yı otomatik olarak `JAVA_HOME` olarak tanımlayan yapı kuruldu.

## [1.1.0-Phase1] - 2026-07-06

### Eklendi
- **Kod Modu Altyapısı (Backend):**
  - **Docker Sandbox Servisi:** Java `ProcessBuilder` tabanlı izole ve kaynak sınırlı `node:18-alpine` konteyner yönetimi (`DockerSandboxManager`).
  - **Agent Loop & ReAct Altyapısı:** LLM planlayıcı ve adım adım sandbox komut yürütücü (`AgentLoopManager`).
  - **Ktor Uç Noktaları:** `/api/v1/agent/session` üzerinden session başlatma, yürütme ve durdurma API uçları (`AgentRoutes`).
- **Kod Modu Arayüzü (Android):**
  - Segmented sliding control ile Sohbet ve Kod Modu arasında pürüzsüz geçiş.
  - Çalışma alanı seçici dialog ekranı (`WorkspaceSelectionDialog`).
  - Otonom adımları izleyen zaman çizelgesi ve monokrom Linux terminal log konsolu (`AgentTerminalPanel`).
  - Kritik veya tehlikeli komutlar için mobil onay kartı (Human-in-the-loop).

## [1.0.0] - 2026-07-06

### Eklendi
- **Android Mobil İstemci (`zeka-android`):**
  - Jetpack Compose tabanlı AMOLED dostu "Pure Black" & "Graphite" monokrom görsel kimlik.
  - Room DB ile sohbet geçmişi ve konfigürasyon önbellekleme desteği.
  - Android Keystore tabanlı `EncryptedSharedPreferences` ile şifreli API anahtarı depolama.
  - Yerel Text-To-Speech (TTS) ve Speech-To-Text (STT) entegrasyonu.
  - MCP, Yetenek (Skills) ve Eklenti (Plugins) katalog arayüzleri.
- **Ktor Backend Sunucusu (`zeka-backend`):**
  - Kotlin Ktor tabanlı, Exposed ORM kullanan veri katmanı.
  - JWT tabanlı oturum yönetimi ve Redis istek hız sınırlayıcı (Rate limiter).
  - MinIO (S3) dosya depolama sistemi.
  - Kullanıcı API anahtarları için **AES-256-GCM** şifreleme/deşifreleme güvenlik servisi.
- **CI/CD & Kubernetes:**
  - GitHub Actions ile otomatik backend test/build ve Android debug APK derleme hatları.
  - Tek tıkla yerel veya uzak cluster kurulumu sağlayan `deploy-k8s.ps1` PowerShell betiği.

### Değişti
- Standart aniden açılan Compose Dialog'ları kaldırılarak yerine aşağıdan yukarıya pürüzsüzce kayan `AnimatedVisibility` overlay panelleri yerleştirildi.
- Yetenek ve Eklenti kartları MCP marka logolarından bağımsız hale getirilerek işlevlerine uygun monokrom sistem simgeleriyle özelleştirildi.
