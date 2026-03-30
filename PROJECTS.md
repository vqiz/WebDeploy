# WebDeploy – Projektanleitung

## Voraussetzungen

- WebDeploy installiert (`webdeploy --version`)
- Server konfiguriert (`webdeploy --setup-server` oder manuell unter `~/.webdeploy/servers/<name>`)
- SSH-Key für den Server vorhanden

---

## 1. Server einmalig einrichten

Falls noch nicht geschehen, lege den Server an:

```bash
webdeploy --setup-server
```

Oder erstelle manuell die Datei `~/.webdeploy/servers/<servername>` (JSON):

```json
{
  "name": "productionserver",
  "host": "123.456.789.0",
  "user": "root",
  "keypath": "/Users/dein-name/.ssh/id_rsa_server"
}
```

---

## 2. Projekt initialisieren

Navigiere in das Projektverzeichnis und führe aus:

```bash
cd /pfad/zu/meinem/projekt
webdeploy --init
```

Dies erstellt eine `webdeploy.config` im aktuellen Verzeichnis.

---

## 3. `webdeploy.config` konfigurieren

Die Konfigurationsdatei ist JSON. Hier sind alle Felder:

```json
{
  "servername": "<servername>",
  "projectname": "<projektname>",
  "needsbackend": true,
  "enabledomain": true,
  "domain": "meine-domain.de",

  "buildCommand": "<lokaler Build-Befehl>",
  "uploadPath": "<lokaler Build-Output-Ordner>",

  "backendBuildCommand": "<lokaler Build-Befehl (Backend)>",
  "backendArtifactPath": "<lokaler Pfad zum Hochladen>",
  "backendDeployPath": "<Zielpfad auf dem Server>",
  "backendWorkingDirectory": "<Arbeitsverzeichnis auf dem Server (für den Service)>",
  "backendRunCommand": "<Startbefehl auf dem Server>",
  "backendServiceName": "<Name des Systemd-Services>",

  "backendProxyPath": "/",
  "backendProxyTarget": "http://localhost:3000",
  "clientMaxBodySize": "50M",
  "backendFilePath": ""
}
```

> **Hinweis:** `backendWorkingDirectory` ist optional. Wenn nicht gesetzt, wird `backendDeployPath` als Arbeitsverzeichnis verwendet.

---

## 4. Konfiguration nach Projekttyp

### Static Site (nur HTML/CSS/JS, kein Server)

Kein Backend nötig. Nur Dateien hochladen.

```json
{
  "servername": "productionserver",
  "projectname": "meine-static-site",
  "needsbackend": false,
  "enabledomain": true,
  "domain": "meine-domain.de",
  "buildCommand": "",
  "uploadPath": "dist",
  "backendBuildCommand": "",
  "backendArtifactPath": "",
  "backendDeployPath": "",
  "backendWorkingDirectory": "",
  "backendRunCommand": "",
  "backendServiceName": "",
  "backendProxyPath": "",
  "backendProxyTarget": "",
  "clientMaxBodySize": "10M",
  "backendFilePath": ""
}
```

---

### React / Vite (Static Build)

```json
{
  "servername": "productionserver",
  "projectname": "mein-react-app",
  "needsbackend": false,
  "enabledomain": true,
  "domain": "meine-domain.de",
  "buildCommand": "npm run build",
  "uploadPath": "dist",
  "backendBuildCommand": "",
  "backendArtifactPath": "",
  "backendDeployPath": "",
  "backendWorkingDirectory": "",
  "backendRunCommand": "",
  "backendServiceName": "",
  "backendProxyPath": "",
  "backendProxyTarget": "",
  "clientMaxBodySize": "10M",
  "backendFilePath": ""
}
```

**Wie es funktioniert:**
- `npm run build` baut den `dist`-Ordner lokal
- Der `dist`-Ordner wird auf den Server hochgeladen
- Nginx liefert die statischen Dateien direkt aus

---

### Next.js (SSR – Server Side Rendering)

```json
{
  "servername": "productionserver",
  "projectname": "mein-next-app",
  "needsbackend": true,
  "enabledomain": true,
  "domain": "meine-domain.de",
  "buildCommand": "npm run build",
  "uploadPath": ".next",
  "backendBuildCommand": "npm run build",
  "backendArtifactPath": ".next",
  "backendDeployPath": "/var/www/mein-next-app/.next",
  "backendWorkingDirectory": "/var/www/mein-next-app",
  "backendRunCommand": "npm start",
  "backendServiceName": "mein-next-app",
  "backendProxyPath": "/",
  "backendProxyTarget": "http://localhost:3000",
  "clientMaxBodySize": "50M",
  "backendFilePath": ""
}
```

**Wichtig:** `backendWorkingDirectory` muss auf den **Parent-Ordner** zeigen (wo `package.json` liegt), nicht auf `.next`.

**Was auf dem Server vorhanden sein muss:**
- `/var/www/mein-next-app/package.json`
- `/var/www/mein-next-app/node_modules/` (nach `npm install`)

> Beim ersten Deploy: Manuell `npm install` auf dem Server ausführen oder `backendRunCommand` auf `npm install && npm start` setzen.

---

### Express.js / Node.js API

```json
{
  "servername": "productionserver",
  "projectname": "meine-api",
  "needsbackend": true,
  "enabledomain": true,
  "domain": "api.meine-domain.de",
  "buildCommand": "",
  "uploadPath": ".",
  "backendBuildCommand": "",
  "backendArtifactPath": ".",
  "backendDeployPath": "/var/www/meine-api",
  "backendWorkingDirectory": "/var/www/meine-api",
  "backendRunCommand": "node server.js",
  "backendServiceName": "meine-api",
  "backendProxyPath": "/",
  "backendProxyTarget": "http://localhost:4000",
  "clientMaxBodySize": "50M",
  "backendFilePath": ""
}
```

---

### NestJS / TypeScript Backend

```json
{
  "servername": "productionserver",
  "projectname": "mein-nest-app",
  "needsbackend": true,
  "enabledomain": true,
  "domain": "api.meine-domain.de",
  "buildCommand": "npm run build",
  "uploadPath": "dist",
  "backendBuildCommand": "npm run build",
  "backendArtifactPath": "dist",
  "backendDeployPath": "/var/www/mein-nest-app",
  "backendWorkingDirectory": "/var/www/mein-nest-app",
  "backendRunCommand": "node dist/main.js",
  "backendServiceName": "mein-nest-app",
  "backendProxyPath": "/",
  "backendProxyTarget": "http://localhost:3000",
  "clientMaxBodySize": "20M",
  "backendFilePath": ""
}
```

---

### Spring Boot (JAR)

```json
{
  "servername": "productionserver",
  "projectname": "mein-spring-app",
  "needsbackend": true,
  "enabledomain": true,
  "domain": "api.meine-domain.de",
  "buildCommand": "mvn clean package -DskipTests",
  "uploadPath": "target/app.jar",
  "backendBuildCommand": "mvn clean package -DskipTests",
  "backendArtifactPath": "target/app.jar",
  "backendDeployPath": "/var/www/mein-spring-app",
  "backendWorkingDirectory": "/var/www/mein-spring-app",
  "backendRunCommand": "java -jar app.jar",
  "backendServiceName": "mein-spring-app",
  "backendProxyPath": "/",
  "backendProxyTarget": "http://localhost:8080",
  "clientMaxBodySize": "20M",
  "backendFilePath": ""
}
```

---

## 5. Projekt deployen

```bash
webdeploy --deploy
```

**Was passiert beim Deploy:**
1. `buildCommand` wird lokal ausgeführt
2. `uploadPath` / `backendArtifactPath` wird auf den Server hochgeladen (in Releases-Struktur unter `/var/www/webdeploy/<projektname>/releases/`)
3. Ein Symlink `current` → neuester Release wird gesetzt
4. Der öffentliche Pfad `backendDeployPath` zeigt auf `current`
5. Systemd-Service wird konfiguriert, aktiviert und neu gestartet
6. Nginx wird (beim ersten Mal) via `--setupproject` konfiguriert

---

## 6. Erstes Deployment eines neuen Projekts

Beim ersten Mal immer zuerst:

```bash
webdeploy --setupproject
```

Dies konfiguriert Nginx und richtet den Systemd-Service ein.  
Danach reicht `webdeploy --deploy` für alle weiteren Updates.

---

## 7. Nützliche Befehle

```bash
webdeploy --deploy           # Deployment starten
webdeploy --setupproject     # Nginx + Systemd einrichten (einmalig)
webdeploy --status           # Nginx Status anzeigen
webdeploy --logs             # Nginx Error-Logs anzeigen
webdeploy --rollback         # Auf vorherigen Release zurückrollen
webdeploy --restart          # Nginx neu starten
webdeploy --ssh              # Interaktive SSH-Session öffnen
webdeploy --sftp             # Cyberduck/FTP öffnen
webdeploy --help             # Alle Befehle anzeigen
```

---

## 8. Serverstruktur (automatisch angelegt)

```
/var/www/webdeploy/<projektname>/
├── releases/
│   ├── 1706000000000/   ← älterer Release
│   └── 1706000060000/   ← aktueller Release
└── current -> releases/1706000060000/  ← Symlink

/var/www/<projektname>/  ← öffentlicher Pfad (Symlink auf current)
/etc/nginx/sites-available/<projektname>
/etc/nginx/sites-enabled/<projektname>
/etc/systemd/system/<projektname>.service
```

---

## Häufige Fehler

### `npm error Missing script: "start"`
→ `backendWorkingDirectory` zeigt in den falschen Ordner (z.B. ins `.next`-Verzeichnis statt in den Projekt-Root). Setze `backendWorkingDirectory` explizit auf den Ordner mit `package.json`.

### Service startet aber Seite lädt nicht
→ Prüfe ob `backendProxyTarget` den richtigen Port hat (z.B. `http://localhost:3000`).

### Upload erfolgreich aber kein Update sichtbar
→ Prüfe ob `backendDeployPath` den richtigen Pfad hat. Next.js erwartet z.B. `.next` im Projektordner.
