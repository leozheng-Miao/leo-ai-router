# 生产上线步骤

## 1. 域名

推荐使用两个域名：

- 前端：`www.example.com`
- 后端：`api.example.com`

DNS 解析：

- `www.example.com` A 记录指向服务器公网 IP
- `api.example.com` A 记录指向服务器公网 IP

当前已生成私密文件 `deploy/.env.production`，里面保留真实密钥，只剩两个域名占位：

- `YOUR_FRONTEND_DOMAIN`
- `YOUR_BACKEND_DOMAIN`

真实域名确定后，执行：

```bash
sh scripts/configure-production-domains.sh www.example.com api.example.com
```

脚本会同时替换：

- `FRONTEND_DOMAIN`
- `BACKEND_DOMAIN`
- `VITE_API_BASE_URL`
- `VITE_OPENAPI_SCHEMA_URL`
- `LEO_FRONTEND_BASE_URL`
- `LEO_CORS_ALLOWED_ORIGINS`
- Stripe、支付宝、微信回调 URL

## 2. 是否需要 Docker

建议使用 Docker。原因：

- MySQL、Redis、后端、前端、Nginx 版本可固定
- 服务器重启后可自动拉起
- MySQL 和 Redis 默认不暴露公网端口
- 数据通过 Docker volume 持久化

不使用 Docker 也可以，但需要手动安装 Java 21、Node 22、MySQL 8、Redis 7、Nginx、Certbot，并手动维护 systemd。

## 3. 数据库导出

本地导出当前结构和基础数据：

```bash
DB_PASSWORD=clx740329 sh scripts/export-deploy-db.sh
```

生成文件：

```text
deploy/mysql/init/01-schema-and-seed.sql
```

该文件已被 `.gitignore` 忽略，里面会包含当前基础数据和模型提供者配置，不要提交到 Git。

## 4. 服务器准备

服务器开放端口：

- `80`
- `443`

安装 Docker 和 Compose 插件后，把项目上传到服务器，例如：

```bash
cd /opt
git clone <your-repo-url> leo-ai-router-backend
cd /opt/leo-ai-router-backend
```

如果通过 Git 上传，`deploy/.env.production` 和 `deploy/mysql/init/01-schema-and-seed.sql` 需要手动复制到服务器。

## 5. 首次 HTTP 启动

首次先用 HTTP 配置启动，便于 Certbot 签发证书：

```bash
vi deploy/.env.production
docker compose --env-file deploy/.env.production -f docker-compose.prod.yml up -d --build
```

确认 HTTP 可访问：

```bash
curl -I http://www.example.com
curl -I http://api.example.com/api/health/
```

## 6. 签发 HTTPS 证书

替换命令中的邮箱和域名：

```bash
docker compose --env-file deploy/.env.production -f docker-compose.prod.yml run --rm certbot certonly \
  --webroot \
  --webroot-path /var/www/certbot \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email \
  -d www.example.com \
  -d api.example.com
```

该命令会生成一个包含前后端域名的证书，证书目录以第一个域名为准。签发成功后，修改 `deploy/.env.production`：

```env
NGINX_CONF_TEMPLATE=./deploy/nginx/https.conf.template
```

重启 Nginx：

```bash
docker compose --env-file deploy/.env.production -f docker-compose.prod.yml up -d nginx
```

## 7. 验证

```bash
curl -I https://www.example.com
curl -I https://api.example.com/api/health/
docker compose --env-file deploy/.env.production -f docker-compose.prod.yml ps
docker compose --env-file deploy/.env.production -f docker-compose.prod.yml logs --tail=200 backend
```

## 8. 证书续期

可配置服务器 crontab：

```bash
0 3 * * * cd /opt/leo-ai-router-backend && docker compose --env-file deploy/.env.production -f docker-compose.prod.yml run --rm certbot renew && docker compose --env-file deploy/.env.production -f docker-compose.prod.yml exec nginx nginx -s reload
```

## 9. 生产注意事项

- MySQL、Redis 不要开放公网端口
- `deploy/.env.production` 不要提交 Git
- `deploy/mysql/init/01-schema-and-seed.sql` 只在 MySQL volume 为空时自动导入
- 支付宝生产环境使用 `https://openapi.alipay.com/gateway.do`
- Stripe webhook 地址需要在 Stripe 后台配置为后端域名对应地址
- 前端构建时会固化 `VITE_API_BASE_URL`，修改后需要重新构建 frontend 镜像
