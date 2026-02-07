# 🚀 WebDeploy

**The All-in-One CLI for Seamless Java & Web Deployments.**

WebDeploy automates your entire deployment workflow—from server provisioning and security hardening to atomic, zero-downtime deployments with instant rollbacks.

---

## ✨ Key Features

- **⚡️ Instant Server Setup**: Provision a fresh Linux server (Nginx, Firewall, SSH Hardening) in one command.
- **🔐 Automatic Security**: Auto-generates SSH keys with correct permissions (`chmod 600`) and configures passwordless login.
- **🌍 Global Access**: Manage any server from anywhere using `webdeploy --ssh <server>`.
- **🔄 Atomic Deployments**: Zero-downtime updates using symlinked releases. Access via IP or Domain always serves the latest version.
- **⏪ Instant Rollback**: Something went wrong? Revert to the previous version instantly with `--rollback`.
- **🛠 Build Automation**: Define local build commands (e.g., `npm run build`) that run automatically before deployment.
- **📦 Smart Uploads**: Configure exactly which folder to upload (e.g., `dist`, `build`, or `public`).

---

## 📥 Installation

### 1. Download & Build
Clone the repository and build the executable JAR:
```bash
git clone https://github.com/yourusername/WebDeploy.git
cd WebDeploy
./gradlew build
# Flatten dependencies and create executable
# (Refer to build script or use provided 'bin' distribution)
```

### 2. Install Globally
Copy the executable to your user bin directory for global access:
```bash
mkdir -p ~/.webdeploy/bin
cp WebDeploy.jar ~/.webdeploy/bin/webdeploy
chmod +x ~/.webdeploy/bin/webdeploy
# Add to PATH (in .zshrc or .bashrc)
export PATH="$HOME/.webdeploy/bin:$PATH"
```

---

## 📖 Usage Guide

### 1. Provision a New Server
Connect to a fresh VPS and turn it into a production-ready web server.
```bash
webdeploy --setup --host=1.2.3.4 --password=root_password
```
*Prompts will ask for a Server Name (e.g., `prod-server`). SSH keys are auto-generated and secured.*

### 2. Connect via SSH
Access your validated servers instantly from any directory.
```bash
webdeploy --ssh prod-server
# Or run a single command remotely
webdeploy --ssh prod-server "htop"
```

### 3. Initialize a Project
Run this in your local project root.
```bash
cd my-web-app
webdeploy --setupproject
```
**Configuration Wizard prompts:**
- **Server Name**: Choose one of your provisioned servers.
- **Domain**: (Optional) e.g., `myapp.com`.
- **Build Command**: (Optional) e.g., `npm install && npm run build`.
- **Upload Folder**: (Default: `dist`) The local folder containing production assets.

### 4. Deploy 🚀
Build, upload, and switch live.
```bash
webdeploy --deploy
```
**What happens:**
1. Runs your **Build Command** locally.
2. Creates a new release directory on the server (`releases/<timestamp>`).
3. Uploads the **Upload Folder** content.
4. Atomically updates the `current` symlink.
5. Reloads Nginx.

### 5. Rollback ⏪
Revert to the previous release if bugs are found.
```bash
webdeploy --rollback
```
*Instantly points the `current` symlink to the previous working release.*

---

## ⚙️ Configuration Reference

Your project configuration is stored in `webdeploy.config`.

```json
{
  "servername": "prod-server",
  "projectname": "my-app",
  "enabledomain": true,
  "domain": "example.com",
  "buildCommand": "npm install && npm run build",
  "uploadPath": "build"
}
```

| Field | Description |
| :--- | :--- |
| `servername` | Name of the target server (from `--setup`). |
| `projectname` | Unique identifier for the project on the server. |
| `enabledomain` | `true` to configure Nginx for a domain, `false` for IP only. |
| `domain` | The domain name (e.g., `example.com`). |
| `buildCommand` | **(New)** Bash command to run locally before upload. |
| `uploadPath` | **(New)** Local directory to upload (relative to project root). |

---

## 🛠 Command Reference

| Command | Description |
| :--- | :--- |
| `--setup` | Initialize a new server remote. |
| `--ssh <server>` | SSH into a configured server. |
| `--setupproject` | Write `webdeploy.config` for the current folder. |
| `--editproject` | Modify existing project config. Supports flags like `--server`, `--domain`, `--build-command`, `--upload-path`, etc. |
| `--deploy` | Build and deploy the project. |
| `--rollback` | Revert the live site to the previous release. |
| `--status` | Show Nginx status. |
| `--logs` | View Nginx error logs. |
| `--monitor` | Launch `htop` on the remote server. |
| `--diskspace` | Show available disk space. |
| `--setupdomain` | Configure a domain and SSL for an existing project. |
| `--help` | Show all available commands. |

---

## 🆘 Troubleshooting

**"No config file found"**
- Ensure you are running `--deploy` inside a project directory with `webdeploy.config`.

**"Permission denied (publickey)"**
- Run `chmod 600 ~/.ssh/id_rsa_<servername>` (Note: New setups enforce this automatically).

**"Command not found"**
- Ensure `~/.webdeploy/bin` is in your `$PATH`.

---
*Built with ❤️ by the Google DeepMind Team*
