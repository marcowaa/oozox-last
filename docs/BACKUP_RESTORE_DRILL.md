# OOZOX Backup, Off-Server Sync & Restore Drill Runbook

## 1. Automated Encrypted Backup + Off-Server Sync
Every backup runs `mongodump --archive --gzip` piped through OpenSSL **AES-256-CBC** (`-pbkdf2 -iter 200000`) using `/opt/oozox/.backup.key` (`BACKUP_ENCRYPTION_PASSPHRASE`), then uploads the encrypted archive off-server via either:
- **rclone** (`BACKUP_RCLONE_REMOTE=remote:oozox-backups/mongo`), or
- **AWS S3 / S3-compatible** (`BACKUP_S3_URI=s3://oozox-backups/mongo`).

Failures in backup or off-server upload immediately trigger a Telegram ops alert via `ALERT_TELEGRAM_BOT_TOKEN` and `ALERT_TELEGRAM_CHAT_ID`.

---

## 2. Throwaway Database Restore Drill (Non-Destructive)
Before trusting any backup archive in production, run a restore drill into an isolated throwaway database (`oozox_restore_drill`) while the live database (`oozox_prod_db`) stays untouched:

```bash
# 1. Create a fresh encrypted backup (or download one from off-server storage)
./scripts/deploy-prod.sh backup
LATEST_BACKUP="$(ls -t /opt/oozox/backups/*.archive.gz.enc | head -n1)"

# 2. Restore into throwaway DB using --target-db (interactive prompt requires typing RESTORE)
./scripts/deploy-prod.sh restore "$LATEST_BACKUP" --target-db oozox_restore_drill

# 3. Inspect restored collections & document counts in the throwaway DB
docker exec -i oozox_mongo_prod mongosh --quiet \
  --username "$MONGO_ROOT_USER" \
  --password "$MONGO_ROOT_PASSWORD" \
  --authenticationDatabase admin \
  --eval '
    const db = db.getSiblingDB("oozox_restore_drill");
    db.getCollectionNames().forEach(c => print(c + ": " + db.getCollection(c).countDocuments()));
  '

# 4. Clean up the throwaway database after verifying counts
docker exec -i oozox_mongo_prod mongosh --quiet \
  --username "$MONGO_ROOT_USER" \
  --password "$MONGO_ROOT_PASSWORD" \
  --authenticationDatabase admin \
  --eval 'db.getSiblingDB("oozox_restore_drill").dropDatabase()'
```

---

## 3. Full Production Disaster Recovery Restore
Only run without `--target-db` during disaster recovery:
```bash
./scripts/deploy-prod.sh restore /opt/oozox/backups/oozox_mongo_YYYYMMDD_HHMMSS.archive.gz.enc
```
The script requires typing `RESTORE` interactively to prevent accidental overwrites.

---

## 4. Uptime Monitoring & Alerting
1. Set `ALERT_TELEGRAM_BOT_TOKEN`, `ALERT_TELEGRAM_CHAT_ID`, `LOG_LEVEL=info`, and optionally `SENTRY_DSN` in `.env.production`.
2. Install the 5-minute uptime check + daily 03:00 backup cron jobs:
   ```bash
   ./scripts/deploy-prod.sh install-backup-script
   ```
3. Run an ad-hoc check anytime:
   ```bash
   ./scripts/deploy-prod.sh monitor
   ```
