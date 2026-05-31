#!/bin/bash
# LUMOO Daily Backup Script
# Backs up: MySQL database + uploads volume
# Storage: local /backups/ dir (mount to DigitalOcean Spaces or external)
#
# Usage:
#   ./scripts/backup.sh              — full backup
#   ./scripts/backup.sh --db-only    — database only
#   ./scripts/backup.sh --verify     — verify last backup
#
# Cron (run daily at 2:30am):
#   30 2 * * * /opt/lumoo/scripts/backup.sh >> /var/log/lumoo-backup.log 2>&1

set -euo pipefail

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
DATE=$(date +%Y%m%d)
BACKUP_DIR="${BACKUP_DIR:-/backups/lumoo}"
KEEP_DAILY=7
KEEP_WEEKLY=4
DB_CONTAINER="${DB_CONTAINER:-lumoo-db}"
APP_CONTAINER="${APP_CONTAINER:-lumoo-app}"

mkdir -p "$BACKUP_DIR/db" "$BACKUP_DIR/uploads" "$BACKUP_DIR/logs"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$BACKUP_DIR/logs/backup.log"; }

# ── Database backup ───────────────────────────────────────────────────────────
backup_db() {
    log "Starting MySQL backup..."
    local out="$BACKUP_DIR/db/lumoo_db_${TIMESTAMP}.sql.gz"

    docker exec "$DB_CONTAINER" \
        mysqldump -u lumoo_user -plumoo_pass \
        --single-transaction \
        --routines \
        --triggers \
        --add-drop-table \
        lumoo 2>/dev/null | gzip > "$out"

    local size
    size=$(du -sh "$out" | cut -f1)
    log "DB backup done: $(basename "$out") ($size)"
}

# ── Uploads backup ────────────────────────────────────────────────────────────
backup_uploads() {
    log "Starting uploads backup..."
    local out="$BACKUP_DIR/uploads/lumoo_uploads_${TIMESTAMP}.tar.gz"

    docker run --rm \
        --volumes-from "$APP_CONTAINER" \
        alpine tar czf - /app/uploads > "$out" 2>/dev/null

    local size
    size=$(du -sh "$out" | cut -f1)
    log "Uploads backup done: $(basename "$out") ($size)"
}

# ── Cleanup old backups ───────────────────────────────────────────────────────
cleanup() {
    log "Cleaning up old backups (keep ${KEEP_DAILY} daily, ${KEEP_WEEKLY} weekly)..."

    # Keep weekly backups (Sunday = day 0)
    find "$BACKUP_DIR/db" -name "*.sql.gz" -mtime +7 ! -newer "$BACKUP_DIR/db/lumoo_db_$(date -d 'last sunday' +%Y%m%d 2>/dev/null || date +%Y%m%d)_000000.sql.gz" 2>/dev/null | head -n -$((KEEP_WEEKLY)) | xargs -r rm -f

    # Remove daily backups older than KEEP_DAILY days
    find "$BACKUP_DIR/db"      -name "*.sql.gz"  -mtime +"$KEEP_DAILY" -delete 2>/dev/null || true
    find "$BACKUP_DIR/uploads" -name "*.tar.gz"  -mtime +"$KEEP_DAILY" -delete 2>/dev/null || true
    find "$BACKUP_DIR/logs"    -name "backup.log" -size +50M -delete 2>/dev/null || true
}

# ── Verify last backup ────────────────────────────────────────────────────────
verify() {
    local latest_db
    latest_db=$(ls -t "$BACKUP_DIR/db"/*.sql.gz 2>/dev/null | head -1)
    if [ -z "$latest_db" ]; then
        log "ERROR: No database backup found!"
        exit 1
    fi
    local size
    size=$(du -sh "$latest_db" | cut -f1)
    log "Latest DB backup: $(basename "$latest_db") ($size)"

    if gzip -t "$latest_db" 2>/dev/null; then
        log "Backup integrity: OK"
    else
        log "ERROR: Backup file is corrupted!"
        exit 1
    fi
}

# ── Optional: push to DigitalOcean Spaces ────────────────────────────────────
push_to_spaces() {
    if [ -z "${DO_SPACES_KEY:-}" ] || [ -z "${DO_SPACES_SECRET:-}" ]; then
        return 0
    fi
    log "Uploading to DigitalOcean Spaces..."
    # Requires: apt-get install s3cmd
    # Configure: s3cmd --configure  (or set DO_SPACES_* env vars)
    s3cmd put "$BACKUP_DIR/db/lumoo_db_${TIMESTAMP}.sql.gz" \
        "s3://${DO_SPACES_BUCKET:-lumoo-backups}/db/lumoo_db_${TIMESTAMP}.sql.gz" \
        --access_key="$DO_SPACES_KEY" \
        --secret_key="$DO_SPACES_SECRET" \
        --host="${DO_SPACES_REGION:-sgp1}.digitaloceanspaces.com" \
        --host-bucket="%(bucket)s.${DO_SPACES_REGION:-sgp1}.digitaloceanspaces.com" \
        2>/dev/null && log "Uploaded to Spaces: lumoo_db_${TIMESTAMP}.sql.gz" || log "Spaces upload failed (continuing)"
}

# ── Main ──────────────────────────────────────────────────────────────────────
case "${1:-}" in
    --verify)   verify; exit 0 ;;
    --db-only)  backup_db; cleanup ;;
    *)
        backup_db
        backup_uploads
        cleanup
        push_to_spaces
        log "Backup complete."
        ;;
esac
