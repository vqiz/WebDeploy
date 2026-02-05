# AGENTS.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview
WebDeploy is a Java CLI tool for deploying web applications to remote servers via SSH/SFTP. It automates server setup (nginx installation, SSH key generation) and handles file uploads with command execution.

## Build and Development Commands

### Building the project
```bash
gradle build
```

### Running the application
```bash
gradle run --args="<arguments>"
```

### Building a JAR
```bash
gradle jar
```
The output JAR will be in `build/libs/`

### Running tests
```bash
gradle test
```

### Cleaning build artifacts
```bash
gradle clean
```

## Application Commands

The application accepts the following command-line arguments:

- **Setup a new server**: `--setup --host=<host> --password=<password> [--user=<user>]`
  - Installs nginx, generates SSH keys, and saves server configuration
  - Creates config file in `servers/<servername>`
  
- **Setup project**: `--setupproject`
  - Interactive setup linking a project to a configured server
  - Creates `webdeploy.config` in the project directory

- **Deploy**: `--deploy`
  - Uploads files from `./dist` to `/var/www/html/<project-name>` on the configured server
  - Automatically skips `.git`, `node_modules`, and `build` directories

- **List servers**: `--listservers`
  - Shows all configured server connections

## Architecture

### Package Structure
- **`de.bachl`** - Root package containing main entry point
  - `WebDeploy.java` - Main class, parses arguments and delegates to services
  
- **`de.bachl.Config`** - Configuration management
  - `Config.java` - Server configuration model (SSH credentials, host, keypath)
  - `ProjectConfig.java` - Project configuration model (server name, project name, backend settings)
  - `ConfigProvider.java` - Reads/writes JSON config files using Gson
  - `FileWriter.java` - File I/O utility

- **`de.bachl.setup`** - Setup and deployment services
  - `SetupProvider.java` - Server initialization: nginx install, SSH key generation, authorized_keys setup
  - `ProjectProvider.java` - Interactive project configuration wizard
  - `DeployService.java` - Main deployment logic: SFTP upload and SSH command execution

- **`de.bachl.utils`** - CLI utilities
  - `ArgsConstructor.java` - Parses command-line arguments into HashMap
  - `ArgsDefiner.java` - Routes arguments to appropriate service handlers
  - `ArgsLister.java` - Lists available server configurations
  - `Log.java` - Logging utility

### Key Technical Details

#### SSH/SFTP Implementation
- Uses JSch library for all SSH/SFTP operations
- `DeployService.connectSSH()` - Establishes SSH session using key-based authentication
- `DeployService.connect()` - Establishes SFTP session, supports both key and password auth
- `DeployService.recursiveUpload()` - Recursively uploads directories, creates remote directories as needed

#### Authentication Flow
`SetupProvider.setupAuth()` generates RSA keypair locally, writes to `~/.ssh/id_rsa_<servername>`, appends public key to server's `authorized_keys`, then disables password authentication by modifying `/etc/ssh/sshd_config`

#### Configuration Storage
- Server configs: `servers/<servername>` (JSON files in working directory)
- Project config: `webdeploy.config` (JSON file in project root)
- SSH keys: `~/.ssh/id_rsa_<servername>` and `~/.ssh/id_rsa_<servername>.pub`

### Important Constraints
- Expects built project files in `./dist` directory before deployment
- Default build command is "npm run build" (defined in `Config.java`)
- Remote deployment path is hardcoded to `/var/www/html/<project-name>`
- Server setup assumes Debian/Ubuntu-based systems (uses `apt-get`)

## Dependencies
- **JSch 0.1.55** - SSH/SFTP client
- **Gson 2.10.1** - JSON serialization
- **MongoDB BSON 4.11.1** - BSON utilities (check if actually used)
- **Lombok 1.18.30** - Reduces boilerplate with @Data, @AllArgsConstructor
- **JUnit Jupiter 5.9.1** - Testing framework
