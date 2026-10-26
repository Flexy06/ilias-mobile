# ILIAS für Android (HKA)

Eine native Android-App für [ILIAS der Hochschule Karlsruhe](https://ilias.h-ka.de) – mit Material 3 Expressive, dynamischen Farben und **dauerhafter Anmeldung**.

## Funktionen

- **Bleibt angemeldet:** Login einmal in der App, danach meldet sie sich automatisch im Hintergrund neu an, sobald die ILIAS-Sitzung abläuft. Das Passwort liegt AES-verschlüsselt mit einem Schlüssel aus dem Android-Keystore auf dem Gerät.
- **Native Navigation:** Untere Leiste mit Übersicht, Kurse, Kalender und Mail. Jeder Tab behält seinen eigenen Verlauf. Nochmal auf den aktiven Tab tippen springt zu dessen Startseite.
- **Wie eine App:** Splash-Screen, Edge-to-Edge, Themed Icon, Wavy-Ladebalken, Pull-to-Refresh, Zurück-Geste.
- **Downloads:** Dateien landen über den Android-Downloadmanager in `Download/ILIAS/`.
- **Uploads:** Datei-Upload für Abgaben funktioniert über die Android-Dateiauswahl.
- **Dark Mode:** ILIAS-Seiten werden auf Wunsch automatisch abgedunkelt.
- **Links:** `ilias.h-ka.de`-Links lassen sich direkt in der App öffnen (Android-Einstellungen → Apps → ILIAS → Standardmäßig öffnen → Link hinzufügen). Externe Links öffnen in einem Custom Tab.

## Installation

Die neueste APK gibt es unter [Releases](../../releases/latest). Jeder Push auf `main` baut automatisch eine neue, signierte Version.

## Selbst bauen

```bash
./gradlew assembleDebug
```

Android Studio (Narwhal oder neuer), JDK 17.
