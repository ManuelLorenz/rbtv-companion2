# RBTV Companion

Native Android companion for Rocket Beans TV with three sections: Uploadplan, aktuelle Uploads und RBTV-News. Kein Forum. Dark Theme mit orangefarbenen Akzenten.

## APK über GitHub Actions erstellen

1. Lade **alle Inhalte dieses Projektordners** in das Stammverzeichnis deines GitHub-Repositories hoch. Wichtig: `build.gradle`, `settings.gradle`, `app/` und `.github/` müssen direkt im Repository-Stamm liegen. Nicht den ZIP-Ordner als Unterordner hochladen.
2. Öffne auf GitHub den Reiter **Actions** und wähle **Build RBTV Companion APK**.
3. Falls der Workflow noch nicht automatisch gestartet ist, wähle **Run workflow**.
4. Nach erfolgreichem Lauf öffne den Workflow-Lauf und lade unter **Artifacts** `RBTV-Companion-Android-17-APK` herunter. Darin liegt `app-debug.apk`.
5. Öffne die APK auf deinem Android-Gerät und bestätige gegebenenfalls die Installation aus dieser Quelle.

Der Workflow verwendet Android Gradle Plugin 9.1.1, Gradle 9.3.1, Java 17 und Android SDK API 37. AGP 9 bringt Kotlin-Unterstützung bereits integriert mit.

## Datenaktualisierung

Die App aktualisiert die Daten beim Öffnen und periodisch im Hintergrund (Android kann Hintergrundjobs verzögern). Bei fehlender Verbindung bleibt der letzte lokale Datenstand verfügbar.

## Hinweis

Community-Projekt; nicht offiziell von Rocket Beans Entertainment GmbH.
