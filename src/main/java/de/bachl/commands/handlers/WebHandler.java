/* Copyright (c) 2026 Dominic Bachl IT Solutions & Consulting. All rights reserved. */

package de.bachl.commands.handlers;

import java.util.HashMap;

import com.jcraft.jsch.Session;

import de.bachl.Config.ProjectConfig;
import de.bachl.commands.Command;
import de.bachl.commands.CommandRegistry;
import de.bachl.commands.CommandUtils;
import de.bachl.services.NginxService;
import de.bachl.utils.Log;

public class WebHandler {

    public void register(CommandRegistry registry) {

        registry.register(new Command() {
            public String getCommand() { return "--restart"; }
            public String getDescription() { return "Restart Nginx."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo systemctl restart nginx", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--status"; }
            public String getDescription() { return "Nginx status."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("systemctl status nginx --no-pager", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--logs"; }
            public String getDescription() { return "Tail Nginx error.log."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("tail -n 50 /var/log/nginx/error.log", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--access-logs"; }
            public String getDescription() { return "Tail Nginx access.log."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("tail -n 50 /var/log/nginx/access.log", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--journal-failed"; }
            public String getDescription() { return "Show failed systemd units."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("journalctl -p 3 -xb", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--clear-cache"; }
            public String getDescription() { return "Reload Nginx and clear project cache."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo systemctl reload nginx", session, true);
                if (config != null) {
                    String path = "/var/www/html/" + config.getProjectname();
                    CommandUtils.sendCommand("cd " + path + " && (php artisan optimize:clear || true)", session, true);
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--maintenance"; }
            public String getDescription() { return "Toggle maintenance mode page."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Project config required.");
                    return;
                }
                String path = "/var/www/html/" + config.getProjectname() + "/maintenance.html";
                boolean exists = false;
                try {
                    CommandUtils.sendCommand("ls " + path, session, false);
                    exists = true;
                } catch (Exception e) {
                    exists = false;
                }
                if (exists) {
                    Log.info("Disabling maintenance mode...");
                    CommandUtils.sendCommand("rm " + path, session, true);
                } else {
                    Log.info("Enabling maintenance mode...");
                    CommandUtils.sendCommand("echo '<h1>Maintenance Mode</h1>' > " + path, session, true);
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--max-body-size"; }
            public String getDescription() { return "Set Nginx client_max_body_size (usage: --max-body-size 500M)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Project config required.");
                    return;
                }
                String size = args.get("--max-body-size");
                if (size == null || size.equals("true") || size.isEmpty()) {
                    Log.error("Please specify size (e.g. 500M).");
                    return;
                }
                config.setClientMaxBodySize(size);
                new de.bachl.Config.ConfigProvider().setupProject(config);
                Log.info("Updated project config with client_max_body_size: " + size);
                new NginxService().setupSite(session, config);
                Log.success("Nginx configuration updated.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--nginx-test"; }
            public String getDescription() { return "Test Nginx configuration syntax."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo nginx -t", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--nginx-config"; }
            public String getDescription() { return "Show Nginx config for current project or list all sites."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config != null && config.getProjectname() != null) {
                    CommandUtils.sendCommand("cat /etc/nginx/sites-available/" + config.getProjectname(), session, true);
                } else {
                    CommandUtils.sendCommand("ls /etc/nginx/sites-enabled/", session, true);
                }
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--nginx-sites"; }
            public String getDescription() { return "List Nginx enabled sites."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("ls -la /etc/nginx/sites-enabled/", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--nginx-install"; }
            public String getDescription() { return "Install Nginx on the server."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                new NginxService().install(session);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--nginx-reload"; }
            public String getDescription() { return "Reload Nginx without downtime."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                CommandUtils.sendCommand("sudo systemctl reload nginx", session, true);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--gzip-enable"; }
            public String getDescription() { return "Enable gzip compression in Nginx."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String gzipConfig = "gzip on;\n" +
                        "gzip_vary on;\n" +
                        "gzip_min_length 1024;\n" +
                        "gzip_proxied any;\n" +
                        "gzip_comp_level 6;\n" +
                        "gzip_types text/plain text/css text/xml text/javascript " +
                        "application/json application/javascript application/xml+rss " +
                        "application/atom+xml image/svg+xml;\n";
                String b64 = java.util.Base64.getEncoder()
                        .encodeToString(gzipConfig.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                CommandUtils.sendCommand("echo '" + b64 + "' | base64 -d | sudo tee /etc/nginx/conf.d/gzip.conf > /dev/null", session, true);
                CommandUtils.sendCommand("sudo systemctl reload nginx", session, true);
                Log.success("Gzip compression enabled.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--ratelimit"; }
            public String getDescription() { return "Set Nginx rate limit (usage: --ratelimit 10r/s)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String rate = args.get("--ratelimit");
                if (rate == null || rate.equals("true") || rate.isEmpty()) {
                    rate = "10r/s";
                }
                String rateLimitConfig = "limit_req_zone $binary_remote_addr zone=weblimit:10m rate=" + rate + ";\n";
                String b64 = java.util.Base64.getEncoder()
                        .encodeToString(rateLimitConfig.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                CommandUtils.sendCommand("echo '" + b64 + "' | base64 -d | sudo tee /etc/nginx/conf.d/ratelimit.conf > /dev/null", session, true);
                CommandUtils.sendCommand("sudo systemctl reload nginx", session, true);
                Log.success("Rate limiting configured at " + rate + ".");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--nginx-optimize"; }
            public String getDescription() { return "Apply Nginx performance optimizations."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String perfConfig = "worker_processes auto;\n" +
                        "worker_rlimit_nofile 65535;\n\n" +
                        "events {\n" +
                        "    worker_connections 65535;\n" +
                        "    use epoll;\n" +
                        "    multi_accept on;\n" +
                        "}\n\n" +
                        "http {\n" +
                        "    keepalive_timeout 65;\n" +
                        "    keepalive_requests 100;\n" +
                        "    sendfile on;\n" +
                        "    tcp_nopush on;\n" +
                        "    tcp_nodelay on;\n" +
                        "    open_file_cache max=200000 inactive=20s;\n" +
                        "    open_file_cache_valid 30s;\n" +
                        "    open_file_cache_min_uses 2;\n" +
                        "    open_file_cache_errors on;\n" +
                        "}\n";
                String b64 = java.util.Base64.getEncoder()
                        .encodeToString(perfConfig.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                CommandUtils.sendCommand("echo '" + b64 + "' | base64 -d | sudo tee /etc/nginx/conf.d/performance.conf > /dev/null", session, true);
                CommandUtils.sendCommand("sudo nginx -t && sudo systemctl reload nginx", session, true);
                Log.success("Nginx performance optimizations applied.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--www-redirect"; }
            public String getDescription() { return "Add www redirect server block to project Nginx config."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null || config.getDomain() == null || config.getDomain().isEmpty()) {
                    Log.error("Project config with domain required.");
                    return;
                }
                String domain = config.getDomain();
                String redirectBlock = "server {\n" +
                        "    listen 80;\n" +
                        "    server_name www." + domain + ";\n" +
                        "    return 301 $scheme://" + domain + "$request_uri;\n" +
                        "}\n";
                String remotePath = "/etc/nginx/sites-available/" + config.getProjectname() + "-www-redirect";
                String b64 = java.util.Base64.getEncoder()
                        .encodeToString(redirectBlock.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                CommandUtils.sendCommand("echo '" + b64 + "' | base64 -d | sudo tee " + remotePath + " > /dev/null", session, true);
                CommandUtils.sendCommand("sudo ln -sf " + remotePath + " /etc/nginx/sites-enabled/", session, true);
                CommandUtils.sendCommand("sudo systemctl reload nginx", session, true);
                Log.success("www redirect configured.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--basicauth"; }
            public String getDescription() { return "Add HTTP basic auth (usage: --basicauth user:password)."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                String value = args.get("--basicauth");
                if (value == null || value.equals("true") || !value.contains(":")) {
                    Log.error("Usage: --basicauth user:password");
                    return;
                }
                if (config == null) {
                    Log.error("Project config required.");
                    return;
                }
                String[] parts = value.split(":", 2);
                String user = parts[0];
                String pass = parts[1];
                CommandUtils.sendCommand("sudo apt-get install -y apache2-utils", session, true);
                CommandUtils.sendCommand("sudo htpasswd -bc /etc/nginx/.htpasswd-" + config.getProjectname() + " " + user + " " + pass, session, true);
                Log.success("Basic auth configured for user: " + user);
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--cors-enable"; }
            public String getDescription() { return "Add CORS headers to project Nginx config."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Project config required.");
                    return;
                }
                Log.info("CORS headers should be added to the Nginx site config. Re-running Nginx setup with CORS block.");
                String corsBlock = "add_header 'Access-Control-Allow-Origin' '*' always;\n" +
                        "        add_header 'Access-Control-Allow-Methods' 'GET, POST, PUT, DELETE, OPTIONS' always;\n" +
                        "        add_header 'Access-Control-Allow-Headers' 'Authorization, Content-Type' always;";
                config.setNginxCustomBlock(corsBlock);
                new NginxService().setupSite(session, config);
                Log.success("CORS headers added to Nginx config.");
            }
        });

        registry.register(new Command() {
            public String getCommand() { return "--custom-errors"; }
            public String getDescription() { return "Write custom 404 and 50x error pages to project root."; }
            public void execute(Session session, HashMap<String, String> args, ProjectConfig config) throws Exception {
                if (config == null) {
                    Log.error("Project config required.");
                    return;
                }
                String root = "/var/www/html/" + config.getProjectname();
                String page404 = "<!DOCTYPE html><html><head><title>404 Not Found</title></head><body><h1>404</h1><p>Page not found.</p></body></html>";
                String page50x = "<!DOCTYPE html><html><head><title>Server Error</title></head><body><h1>Oops</h1><p>Something went wrong. Please try again later.</p></body></html>";
                String b64_404 = java.util.Base64.getEncoder().encodeToString(page404.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                String b64_50x = java.util.Base64.getEncoder().encodeToString(page50x.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                CommandUtils.sendCommand("echo '" + b64_404 + "' | base64 -d | sudo tee " + root + "/404.html > /dev/null", session, true);
                CommandUtils.sendCommand("echo '" + b64_50x + "' | base64 -d | sudo tee " + root + "/50x.html > /dev/null", session, true);
                Log.success("Custom error pages written.");
            }
        });
    }
}
