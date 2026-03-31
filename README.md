# WebDeploy v2.0

**CLI tool for deploying and managing web applications on remote Linux servers.**

Deploy frontends, backends, configure Nginx, SSL, databases, and monitor servers — all from your local machine via a single command.

---

## Features

- Zero-downtime deployments with symlinked releases and instant rollback
- Automated server provisioning: Nginx, SSH key hardening, UFW firewall
- Multi-project support — host multiple sites (frontend + backend) on one server
- Backend support: Java (systemd), Node.js (PM2 or nohup), any language
- Auto SSL via Let's Encrypt / Certbot
- Full Nginx management: config, gzip, CORS, rate limiting, basic auth, HTTP/2
- PM2 process manager integration
- Database installation and management (MySQL, PostgreSQL, Redis, MongoDB)
- System monitoring: CPU, memory, processes, disk I/O, network
- Safe config writing via base64 encoding (no quote injection)
- 172 tests, all passing — no production server used for testing

---

## Installation

```bash
git clone https://github.com/yourusername/WebDeploy.git
cd WebDeploy
./gradlew jar
```

Install globally:
```bash
mkdir -p ~/.webdeploy/bin
cp build/libs/WebDeploy.jar ~/.webdeploy/bin/webdeploy.jar

# Create wrapper script
echo '#!/bin/bash\njava -jar ~/.webdeploy/bin/webdeploy.jar "$@"' > ~/.webdeploy/bin/webdeploy
chmod +x ~/.webdeploy/bin/webdeploy

# Add to PATH in .zshrc / .bashrc
export PATH="$HOME/.webdeploy/bin:$PATH"
```

---

## Quick Start

### 1. Provision a server
```bash
webdeploy --setup --host=1.2.3.4 --password=root_password
# Optional: custom SSH port
webdeploy --setup --host=1.2.3.4 --password=root_password --sshport=2222
```
Installs Nginx, generates SSH keypair, disables password auth, optionally configures UFW.

### 2. Initialize a project
```bash
cd my-react-app
webdeploy --setupproject
# With template for faster setup:
webdeploy --setupproject --template=react    # npm run build / dist
webdeploy --setupproject --template=vue      # npm run build / dist
webdeploy --setupproject --template=node     # backend-focused
webdeploy --setupproject --template=spring   # ./gradlew build
webdeploy --setupproject --template=static   # no build / .
```

### 3. Deploy
```bash
webdeploy --deploy
```
Runs build command → uploads to timestamped release → updates symlink → reloads Nginx → optional health check.

### 4. Rollback
```bash
webdeploy --rollback
```

### 5. Domain + SSL
```bash
webdeploy --setupdomain
# Or non-interactively:
webdeploy --setupdomain --domain=example.com --project=myapp --server=prod
```

---

## Configuration

### Server config — `~/.webdeploy/servers/<name>`
```json
{
  "name": "prod-server",
  "host": "1.2.3.4",
  "user": "root",
  "sshPort": 22,
  "keypath": "/Users/you/.ssh/id_rsa_prod-server"
}
```

### Project config — `./webdeploy.config`
```json
{
  "servername": "prod-server",
  "projectname": "my-app",
  "buildCommand": "npm run build",
  "uploadPath": "dist",
  "enabledomain": true,
  "domain": "example.com",
  "keepReleases": 5,
  "preDeployCommand": "npm run lint",
  "postDeployCommand": "echo deployed",
  "healthCheckUrl": "https://example.com/health",
  "needsbackend": true,
  "backendBuildCommand": "mvn clean package",
  "backendArtifactPath": "target/app.jar",
  "backendDeployPath": "/var/www/backend",
  "backendRunCommand": "java -jar app.jar",
  "backendServiceName": "my-app-backend",
  "backendProxyPath": "/api",
  "backendProxyTarget": "http://localhost:8080",
  "clientMaxBodySize": "50M",
  "nginxCustomBlock": "location /static { expires 1y; }"
}
```

| Field | Description |
|---|---|
| `servername` | Server name from `--setup` |
| `projectname` | Unique project identifier on the server |
| `buildCommand` | Local command run before upload |
| `uploadPath` | Local folder to upload (default: `dist`) |
| `keepReleases` | Number of releases to keep (default: 5) |
| `preDeployCommand` | Run locally before deploy starts |
| `postDeployCommand` | Run locally after deploy completes |
| `healthCheckUrl` | URL to verify after deploy |
| `enabledomain` | Whether domain + Nginx vhost is configured |
| `domain` | Domain name (e.g. `example.com`) |
| `needsbackend` | Whether a backend service is deployed |
| `backendBuildCommand` | Local command to build backend |
| `backendArtifactPath` | Local path to backend artifact |
| `backendDeployPath` | Remote path for backend files |
| `backendRunCommand` | Command for systemd ExecStart |
| `backendServiceName` | systemd service name |
| `backendProxyPath` | Nginx proxy location (e.g. `/api`) |
| `backendProxyTarget` | Backend URL (e.g. `http://localhost:8080`) |
| `clientMaxBodySize` | Nginx upload limit (default: `10M`) |
| `nginxCustomBlock` | Custom nginx location block |

---

## Command Reference

### Setup
| Command | Description |
|---|---|
| `--setup` | Provision server: Nginx + SSH keys + optional UFW |
| `--setupproject` | Init project config wizard |
| `--setupproject --template=<t>` | Init with template: `react`, `vue`, `node`, `spring`, `static` |
| `--setupdomain` | Configure domain + SSL for existing project |
| `--editproject` | Edit project config via flags |
| `--listservers` | List configured servers |

### Deployment
| Command | Description |
|---|---|
| `--deploy` | Full deploy: build → upload → symlink → reload |
| `--rollback` | Revert to previous release |
| `--releases` | List all releases with current marker |
| `--deploy-status` | Show current release info |
| `--health-check` | HTTP check against `healthCheckUrl` or domain |
| `--cleanup-releases` | Remove old releases beyond `keepReleases` |
| `--backup` | Download remote project as `.tar.gz` |
| `--delete-project` | Remove project from server |

### Nginx / Web
| Command | Description |
|---|---|
| `--status` | Nginx status |
| `--restart` | Restart Nginx |
| `--nginx-reload` | Reload Nginx config |
| `--nginx-test` | Test Nginx config validity |
| `--nginx-config` | Show project Nginx config |
| `--nginx-sites` | List all enabled sites |
| `--nginx-install` | Install Nginx |
| `--nginx-optimize` | Apply performance tuning |
| `--logs` | Nginx error log (last 50 lines) |
| `--access-logs` | Nginx access log (last 50 lines) |
| `--maintenance` | Toggle maintenance mode page |
| `--gzip-enable` | Enable gzip compression |
| `--cors-enable` | Add CORS headers to project |
| `--ratelimit <rate>` | Configure rate limiting (e.g. `10r/s`) |
| `--basicauth <user:pass>` | Enable HTTP basic auth |
| `--www-redirect` | Add www → non-www redirect |
| `--custom-errors` | Setup custom 404 / 50x pages |
| `--max-body-size <size>` | Set upload size limit |
| `--clear-cache` | Reload Nginx + clear project cache |

### Security
| Command | Description |
|---|---|
| `--firewall-status` | UFW status |
| `--firewall-enable` | Enable UFW |
| `--firewall-disable` | Disable UFW |
| `--firewall-list` | List UFW rules |
| `--firewall-reset` | Reset all UFW rules |
| `--firewall-allow <port>` | Allow port through UFW |
| `--firewall-deny <port>` | Deny port |
| `--setup-ufw-defaults` | Enable UFW with web defaults (22, 80, 443) |
| `--fail2ban-install` | Install fail2ban |
| `--fail2ban-status` | fail2ban status |
| `--fail2ban-unban <ip>` | Unban an IP address |
| `--ssl-renew` | Force SSL certificate renewal |
| `--ssl-status <domain>` | Check SSL certificate expiry |
| `--last-logins` | Show last 20 logins |
| `--auth-log` | Show auth log (last 100 lines) |
| `--ssh-config` | Show SSH daemon config |
| `--check-ports` | List open ports |

### System
| Command | Description |
|---|---|
| `--ssh <server>` | Interactive SSH session |
| `--monitor` | Launch htop |
| `--uptime` | Server uptime |
| `--info` | CPU / memory / disk summary |
| `--cpu` | CPU usage |
| `--memory` | Memory usage |
| `--processes` | Top 20 processes by CPU |
| `--load` | System load average |
| `--network` | Network interfaces |
| `--hostname` | Show hostname |
| `--timezone <tz>` | Show or set timezone |
| `--os-info` | OS and kernel details |
| `--kernel` | Kernel version |
| `--whoami` | Current remote user |
| `--diskspace` | Disk usage |
| `--disk-cleanup` | Clean apt cache + journals |
| `--service-start <name>` | Start service |
| `--service-stop <name>` | Stop service |
| `--service-restart <name>` | Restart service |
| `--service-status <name>` | Service status |
| `--service-enable <name>` | Enable service on boot |
| `--service-disable <name>` | Disable service on boot |
| `--service-list` | List all active services |
| `--reboot` | Reboot server |
| `--shutdown` | Shutdown server |
| `--update-server` | apt-get update + upgrade |
| `--install-pkg <name>` | Install a package |
| `--user-add <name>` | Add system user |
| `--user-remove <name>` | Remove system user |
| `--test-connection` | Verify SSH connectivity |
| `--ping <host>` | Ping from server |
| `--env-vars` | Show remote environment variables |

### Monitoring
| Command | Description |
|---|---|
| `--top` | Process snapshot |
| `--htop` | Interactive htop session |
| `--disk-io` | Disk I/O stats |
| `--network-stats` | Network statistics |
| `--open-files` | Open file descriptor count |
| `--system-temp` | CPU temperature |
| `--connections` | Active network connections |
| `--journal-errors` | Recent systemd errors |
| `--syslog` | Syslog (last 50 lines) |
| `--journal-failed` | Failed systemd units |

### PM2
| Command | Description |
|---|---|
| `--pm2-install` | Install PM2 globally |
| `--pm2-list` | List PM2 processes |
| `--pm2-start <script>` | Start process with PM2 |
| `--pm2-stop <name>` | Stop process |
| `--pm2-restart <name>` | Restart process |
| `--pm2-delete <name>` | Delete process |
| `--pm2-logs <name>` | Show logs |
| `--pm2-save` | Save process list |
| `--pm2-startup` | Configure PM2 auto-start |
| `--pm2-status <name>` | Show process details |
| `--pm2-env <name>` | Show process environment |

### Database
| Command | Description |
|---|---|
| `--db-install-mysql` | Install MySQL server |
| `--db-install-pgsql` | Install PostgreSQL |
| `--db-install-redis` | Install Redis |
| `--db-install-mongodb` | Install MongoDB 7.x |
| `--db-status <type>` | Check DB service status |
| `--db-list <type>` | List databases |
| `--db-create <type:name>` | Create database |
| `--db-backup` | Dump database to `/tmp` |

### Files
| Command | Description |
|---|---|
| `--ls <path>` | List remote directory |
| `--cat-remote <path>` | Show remote file content |
| `--tail-remote <path>` | Tail remote file |
| `--upload <local:remote>` | Upload file or directory |
| `--download <remote:local>` | Download file |
| `--zip-remote <path>` | Zip remote folder |
| `--unzip-remote <path>` | Unzip remote file |
| `--mv <src dest>` | Move remote file |
| `--cp <src dest>` | Copy remote file |
| `--chmod <perms path>` | Change permissions |
| `--chown <owner path>` | Change owner |
| `--sftp` | Open Cyberduck SFTP (macOS) |

### Environment & Cron
| Command | Description |
|---|---|
| `--env-set <KEY=VAL>` | Set environment variable in `.env` |
| `--env-get` | Show project `.env` |
| `--env-list` | List `.env` contents |
| `--cron-list` | List cron jobs |
| `--cron-add <expr>` | Add cron job |
| `--cron-remove <pattern>` | Remove matching cron job |

### Config
| Command | Description |
|---|---|
| `--show-config` | Print project config |
| `--show-server <name>` | Print server config (password masked) |
| `--remove-server <name>` | Delete server config |
| `--edit-server <name>` | Update server field (`--host`, `--user`, `--sshport`) |

### DNS
| Command | Description |
|---|---|
| `--dig` | DNS lookup for project domain |
| `--whois` | Whois lookup for project domain |

---

## Multi-Site Setup

One server can host multiple independent projects:

```bash
# Project 1 — React frontend + Java backend
cd ~/projects/shop
webdeploy --setupproject --template=react
# answer: project=shop, domain=shop.example.com, backend=yes ...
webdeploy --deploy

# Project 2 — Static site
cd ~/projects/blog
webdeploy --setupproject --template=static
# answer: project=blog, domain=blog.example.com ...
webdeploy --deploy
```

Each project gets its own:
- Nginx vhost at `/etc/nginx/sites-available/<project>`
- Releases at `/var/www/webdeploy/<project>/releases/`
- Current symlink at `/var/www/webdeploy/<project>/current`
- Public root at `/var/www/html/<project>` → symlink
- systemd service for backend (optional)

---

## Directory Structure on Server

```
/var/www/webdeploy/<project>/
├── releases/
│   ├── 1743350000000/   (older)
│   └── 1743360000000/   (current)
└── current → releases/1743360000000/

/var/www/html/<project> → /var/www/webdeploy/<project>/current

/etc/nginx/sites-available/<project>
/etc/nginx/sites-enabled/<project> → sites-available/<project>

/etc/systemd/system/<project>-backend.service
```

---

## Troubleshooting

**"No config file found"**
Run `--deploy` inside a directory containing `webdeploy.config`.

**"Server configuration not found"**
Run `--listservers` to see configured servers. Re-run `--setup` if missing.

**"Permission denied (publickey)"**
Check `chmod 600 ~/.ssh/id_rsa_<servername>`. New setups enforce this automatically.

**"Command not found: webdeploy"**
Ensure `~/.webdeploy/bin` is in your `$PATH`.

**Deployment succeeded but site shows old content**
Run `--rollback` then re-deploy. Check `--nginx-test` and `--status`.

---

## Building & Testing

```bash
# Build
./gradlew jar

# Run all tests (no server required)
./gradlew test

# Build + test
./gradlew build
```

Test suite: 172 tests, JUnit 5 + Mockito, zero network calls.

---

## License

Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved.
