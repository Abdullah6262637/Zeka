# CHANGELOG

Tüm önemli değişiklikler bu dosyada listelenecektir.

Projenin sürüm yönetimi [SemVer](https://semver.org/spec/v2.0.0.html) (Semantic Versioning) standartlarına uygun olarak yapılmaktadır.

---

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
