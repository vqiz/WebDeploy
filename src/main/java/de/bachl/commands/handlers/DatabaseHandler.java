/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands.handlers;

import com.jcraft.jsch.Session;
import de.bachl.Config.ProjectConfig;
import de.bachl.commands.Command;
import de.bachl.commands.CommandRegistry;
import de.bachl.commands.CommandUtils;
import de.bachl.utils.Log;

import java.util.HashMap;

public class DatabaseHandler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() { return "--db-install-mysql"; }
            public String getDescription() { return "Install MySQL server."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                Log.info("Installing MySQL Server...");
                CommandUtils.sendCommand("sudo apt-get update && sudo DEBIAN_FRONTEND=noninteractive apt-get install -y mysql-server", session, true);
                CommandUtils.sendCommand("sudo systemctl start mysql && sudo systemctl enable mysql", session, true);
                Log.info("MySQL installed. Run: sudo mysql_secure_installation");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--db-install-pgsql"; }
            public String getDescription() { return "Install PostgreSQL server."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                Log.info("Installing PostgreSQL...");
                CommandUtils.sendCommand("sudo apt-get update && sudo apt-get install -y postgresql postgresql-contrib", session, true);
                CommandUtils.sendCommand("sudo systemctl start postgresql && sudo systemctl enable postgresql", session, true);
                Log.info("PostgreSQL installed. Default user: postgres");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--db-install-redis"; }
            public String getDescription() { return "Install Redis server."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                Log.info("Installing Redis...");
                CommandUtils.sendCommand("sudo apt-get update && sudo apt-get install -y redis-server", session, true);
                CommandUtils.sendCommand("sudo systemctl start redis-server && sudo systemctl enable redis-server", session, true);
                Log.info("Redis installed and running.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--db-install-mongodb"; }
            public String getDescription() { return "Install MongoDB 7.x."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                Log.info("Installing MongoDB 7.0...");
                CommandUtils.sendCommand(
                    "curl -fsSL https://pgp.mongodb.com/server-7.0.asc | sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor",
                    session, true);
                CommandUtils.sendCommand(
                    "echo 'deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse' | sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list",
                    session, true);
                CommandUtils.sendCommand("sudo apt-get update && sudo apt-get install -y mongodb-org", session, true);
                CommandUtils.sendCommand("sudo systemctl start mongod && sudo systemctl enable mongod", session, true);
                Log.info("MongoDB installed and running.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--db-status"; }
            public String getDescription() { return "Check database status (usage: --db-status mysql|pgsql|redis|mongodb)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String type = args.get("--db-status");
                if (type == null || type.equals("true") || type.isEmpty()) {
                    Log.error("Specify database type: mysql, pgsql, redis, or mongodb.");
                    return;
                }
                String service;
                switch (type.toLowerCase()) {
                    case "mysql":   service = "mysql";        break;
                    case "pgsql":   service = "postgresql";   break;
                    case "redis":   service = "redis-server"; break;
                    case "mongodb": service = "mongod";       break;
                    default:
                        Log.error("Unknown type: " + type + ". Use mysql, pgsql, redis, or mongodb.");
                        return;
                }
                CommandUtils.sendCommand("sudo systemctl status " + service + " --no-pager", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--db-list"; }
            public String getDescription() { return "List databases (usage: --db-list mysql|pgsql|redis|mongodb)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String type = args.get("--db-list");
                if (type == null || type.equals("true") || type.isEmpty()) {
                    Log.error("Specify database type: mysql, pgsql, redis, or mongodb.");
                    return;
                }
                switch (type.toLowerCase()) {
                    case "mysql":
                        CommandUtils.sendCommand("mysql -e 'SHOW DATABASES;'", session, true);
                        break;
                    case "pgsql":
                        CommandUtils.sendCommand("sudo -u postgres psql -l", session, true);
                        break;
                    case "redis":
                        CommandUtils.sendCommand("redis-cli INFO keyspace", session, true);
                        break;
                    case "mongodb":
                        CommandUtils.sendCommand("mongosh --eval 'db.adminCommand({listDatabases:1})' --quiet 2>/dev/null || mongo --eval 'db.adminCommand({listDatabases:1})' --quiet", session, true);
                        break;
                    default:
                        Log.error("Unknown type: " + type + ". Use mysql, pgsql, redis, or mongodb.");
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--db-create"; }
            public String getDescription() { return "Create a database (usage: --db-create mysql:mydb or pgsql:mydb)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String value = args.get("--db-create");
                if (value == null || value.equals("true") || !value.contains(":")) {
                    Log.error("Usage: --db-create type:dbname (e.g. mysql:myapp or pgsql:myapp).");
                    return;
                }
                String[] parts = value.split(":", 2);
                String type = parts[0].trim().toLowerCase();
                String dbName = parts[1].trim();

                switch (type) {
                    case "mysql":
                        CommandUtils.sendCommand("mysql -e 'CREATE DATABASE IF NOT EXISTS `" + dbName + "`;'", session, true);
                        break;
                    case "pgsql":
                        CommandUtils.sendCommand("sudo -u postgres createdb " + dbName, session, true);
                        break;
                    case "mongodb":
                        CommandUtils.sendCommand(
                            "mongosh --eval 'db.getSiblingDB(\"" + dbName + "\").createCollection(\"init\")' --quiet 2>/dev/null || true",
                            session, true);
                        break;
                    default:
                        Log.error("Unknown type: " + type + ". Use mysql, pgsql, or mongodb.");
                }
                Log.info("Database '" + dbName + "' created (" + type + ").");
            }
        });
    }
}
