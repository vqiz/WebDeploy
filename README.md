# WebDeploy

WebDeploy is a powerful Java CLI tool for deploying web applications to remote servers via SSH/SFTP. It automates the entire process: server setup (Nginx, SSH keys), project configuration, file deployment, and SSL certificate management.

## Features

- **Automated Server Setup**: Installs Nginx, configures firewall/services, and sets up SSH key authentication with a single command.
- **Nginx Management**: Automatically configures Nginx virtual hosts (vhosts) for your projects.
- **SSL Automation**: One-command SSL setup using Certbot (Let's Encrypt).
- **Seamless Deployment**: Deploys artifacts via SFTP and reloads Nginx automatically.
- **Management Tools**: Check status, view logs, restart services, and SSH into your server directly from the CLI.

## Installation

### Prerequisites
- Java 17+
- Gradle (optional, if building from source)
- A remote Linux server (Ubuntu/Debian recommended)

### Building
```bash
gradle build
gradle jar
```
The executable jar will be in `build/libs/`.

## Usage

Run the tool using `java -jar WebDeploy.jar` or via Gradle:

### 1. Setup a New Server
Connects to a fresh server, installs dependencies, and secures it.
```bash
java -jar WebDeploy.jar --setup --host=<ip-address> --password=<root-password> [--user=<user>]
```

### 2. Configure a Project
Run this in your project's root directory. It links the project to a configured server and sets up Nginx/Domain settings.
```bash
java -jar WebDeploy.jar --setupproject
```
*Prompts will ask for Server Name, Project Name, Domain, etc.*

### 3. Deploy
Uploads files from `./dist` to the server and ensures Nginx is configured.
```bash
java -jar WebDeploy.jar --deploy
```

### 4. Setup SSL
Generates an SSL certificate for your configured domain.
```bash
java -jar WebDeploy.jar --ssl
```

## Available Commands

| Command | Description |
| :--- | :--- |
| `--setup` | Initialize a new server (Nginx, SSH Keys). |
| `--setupproject` | Configure the current directory as a deployable project. |
| `--deploy` | Upload files and configure Nginx. |
| `--ssl` | Install Certbot and generate SSL certificates. |
| `--status` | Show Nginx service status and disk usage. |
| `--logs` | Stream the last 50 lines of Nginx error logs. |
| `--restart` | Restart the Nginx web server. |
| `--ssh` | Open an interactive SSH session to the server. |
| `--test-connection` | Verify SSH connectivity to the server. |
| `--listservers` | List configured servers on this machine. |
| `--help` | Show help and usage information. |
| `--version` | Display version information. |
| `--backup` | Backup the remote `/var/www` directory to local. |
| `--delete-project` | Remove project files and config from remote server. |
| `--rollback` | Rollback to the previous deployment. |
| `--audit` | Show deployment history. |
| `--db-backup` | Backup the database (MySQL/PostgreSQL support). |
| `--db-restore` | Restore the database from a file. |
| `--monitor` | Real-time server resource monitoring (htop). |
| `--clear-cache` | Clear Nginx and application caches. |
| `--env-set` | Set a remote environment variable. |
| `--env-get` | Get a remote environment variable. |
| `--env-list` | List all remote environment variables. |
| `--firewall-status` | Check UFW firewall status. |
| `--firewall-allow` | Allow a specific port via UFW. |
| `--firewall-deny` | Deny a specific port via UFW. |
| `--cron-list` | List all cron jobs for the user. |
| `--cron-add` | Add a new cron job. |
| `--cron-remove` | Remove a cron job. |
| `--user-add` | Add a new system user. |
| `--user-remove` | Remove a system user. |
| `--disk-cleanup` | Clean up old logs and temp files. |
| `--update-server` | Run `apt-get update && upgrade`. |
| `--install-pkg` | Install a specific package via apt. |
| `--service-start` | Start a system service. |
| `--service-stop` | Stop a system service. |
| `--service-restart` | Restart a system service. |
| `--git-pull` | Pull the latest changes from Git on the server. |
| `--npm-install` | Run `npm install` in the project directory. |
| `--pm2-list` | List running Node.js processes (PM2). |
| `--pm2-restart` | Restart a PM2 process. |
| `--pm2-logs` | View PM2 logs. |
| `--whoami` | Check current user context on remote. |
| `--reboot` | Reboot the remote server. |
| `--shutdown` | Shutdown the remote server. |
| `--info` | Show server hardware info (CPU, RAM). |
| `--network` | Show network usage stats. |
| `--docker-ps` | List running Docker containers. |
| `--maintenance` | Toggle maintenance mode page. |
| `--uptime` | Show server uptime. |
| `--kernel` | Show kernel version. |
| `--fail2ban-install` | Install Fail2Ban. |
| `--fail2ban-status` | Check Fail2Ban status. |
| `--check-ports` | Check open ports (netstat/ss). |
| `--access-logs` | Stream Nginx access logs. |
| `--journal-failed` | Show failed systemd services. |
| `--composer-install` | Run `composer install` (PHP). |
| `--composer-update` | Run `composer update` (PHP). |
| `--artisan-cmd` | Run `php artisan` command. |
| `--pip-install` | Run `pip install -r requirements.txt`. |
| `--venv-create` | Create Python venv. |
| `--zip-remote` | Zip a remote folder. |
| `--unzip-remote` | Unzip a remote file. |
| `--mv` | Move a remote file. |
| `--cp` | Copy a remote file. |
| `--chown` | Change file owner (recursive). |
| `--chmod` | Change file permissions (recursive). |
| `--dig` | DNS lookup for project domain. |
| `--whois` | Whois lookup for project domain. |
| `--run-script` | Execute a custom script on server. |

## Configuration

- **Server Configs**: Stored in `servers/<servername>` (local to the tool).
- **Project Config**: Stored in `webdeploy.config` (in your project root).

## License

MIT
