# Server Documentation

This document contains critical information about the server setup, hosted websites, and configuration details.

> [!CAUTION]
> **SENSITIVE DATA**: This file contains root credentials. Share securely.

## Server Access

| Type | Detail |
| :--- | :--- |
| **IP Address** | `89.144.42.43` |
| **User** | `root` |
| **Password** | `S1MXB5MvfUabmwfN` |
| **SSH Command** | `ssh root@89.144.42.43` |

---

## Hosted Websites

The server utilizes **Nginx** as a reverse proxy and **PM2** to manage Node.js applications.

### 1. Forstbetrieb Kornfeldner (Active Client)

*   **Domains**: `forstbetrieb-kronfeldner.de`, `www.forstbetrieb-kronfeldner.de`
*   **Type**: SPA (Single Page Application) + Node.js Backend API
*   **Directory**: `/var/www/html/kronfeld`
    *   **Frontend**: Static files (HTML, JS, CSS) served directly by Nginx from this folder.
    *   **Backend**: `server.cjs` located in this folder.
*   **Backend Port**: `3001` (Proxied via Nginx `/api`)
*   **PM2 Process Name**: `kronfeld-api` (ID: 1)

#### Configuration Snippets
**Nginx (`/etc/nginx/sites-enabled/forstbetrieb-kronfeldner.de`)**:
```nginx
# Root directory for static assets
root /var/www/html/kronfeld;

# API Proxy to Node.js backend
location /api {
    proxy_pass http://localhost:3001;
    ...
}

# SPA Fallback (Unknown routes go to index.html)
location / {
    try_files $uri $uri/ /index.html;
}
```

---

### 2. Dominic Bachl / Bachl Systems (Portfolio)

*   **Domains**: `bachl-systems.de`, `dominicbachl.com` (likely redirected or aliased)
*   **Type**: Node.js Application (Next.js or similar)
*   **Directory**: `/var/www/dominic-portfolio`
*   **Port**: `3000` (Proxied via Nginx root `/`)
*   **PM2 Process Name**: `dominic-portfolio` (ID: 0)

#### Configuration Snippets
**Nginx (`/etc/nginx/sites-enabled/bachl-systems.de`)**:
```nginx
# Proxy everything to localhost:3000
location / {
    proxy_pass http://127.0.0.1:3000;
    ...
}
```

---

## Deployment & Maintenance

### Checking Process Status
The server uses `pm2` to keep Node.js apps alive.
```bash
pm2 list          # Check status
pm2 logs [id]     # View logs (e.g., pm2 logs 1)
pm2 restart [id]  # Restart app (e.g., pm2 restart kronfeld-api)
```

### File Structure Overview
```
/
├── var/
│   └── www/
│       ├── html/
│       │   └── kronfeld/         # Forstbetrieb Kornfeldner (Subject Project)
│       └── dominic-portfolio/    # Other running project
└── etc/
    └── nginx/
        └── sites-enabled/        # Nginx Configs
```

### Important Notes
*   **SSL**: Managed automatically via Let's Encrypt (Certbot).
*   **Uploads**: `client_max_body_size` is set to `500M` for `forstbetrieb-kronfeldner.de` to support video uploads.
*   **Caching**: Aggressive caching (1 year) is enabled for `/assets/` on the Kronfeld site.
