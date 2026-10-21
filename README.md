# DriveOS Alpha 0.2

Eksperimentalna Android infotainment aplikacija za aftermarket Android head unit uređaje (uključujući Junsun) i Android emulatore.

## Što je novo
- Tamni landscape dashboard s navigacijskom i medijskom zonom
- **Demo Mode** s primjerom navigacijskih uputa, simuliranom brzinom 65 km/h i demonstracijskim medijskim tipkama
- Pregled instaliranih aplikacija: Maps, Waze, Spotify i YouTube Music
- Otvaranje instaliranih aplikacija ili poveznica na Play Store kada nedostaju
- Dijagnostika modela, Android verzije i rezolucije te dijeljenje izvještaja
- Siguran izlaz; ne mijenja se HOME launcher

**Važno:** Demo player nema zvuk i ne kontrolira stvarnu reprodukciju. Demo brzina nije GPS podatak. Live način trenutačno otvara vanjske aplikacije; ne ugrađuje Google Maps ili Waze u dashboard.

## APK
U kartici **Actions** odaberite zadnji uspješni **Build DriveOS APK** pa pod **Artifacts** preuzmite ZIP s debug APK-om. Na mobilnom uređaju/emulatoru instalirajte `app-debug.apk`.

## Build
GitHub Actions koristi JDK 17, Gradle 8.9 i Android SDK 35. Aplikacija podržava Android API 23+ (Android 6.0+).

Testirajte dok je vozilo parkirano. Još nije testirano na stvarnom Junsun MCU-u niti su implementirane kontrole volana, poziva i prava medijska integracija.
