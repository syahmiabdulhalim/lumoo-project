#!/bin/bash
# LUMOO Restore Script
# Restores MySQL database from a backup file
#
# Usage:
#   ./scripts/restore.sh /backups/lumoo/db/lumoo_db_20260101_020000.sql.gz
#   ./scripts/restore.sh latest   — restores most recent backup

set -euo pipefail

DB_CONTAINER="${DB_CONTAINER:-lumoo-db}"
BACKUP_DIR="${BACKUP_DIR:-/backups/lumoo}"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

if [ "${1:-}" = "latest" ]; then
    BACKUP_FILE=$(ls -t "$BACKUP_DIR/db"/*.sql.gz 2>/dev/null | head -1)
    if [ -z "$BACKUP_FILE" ]; then
        log "ERROR: No backup files found in $BACKUP_DIR/db/"
        exit 1
    fi
    log "Using latest backup: $(basename "$BACKUP_FILE")"
else
    BACKUP_FILE="${1:-}"
fi

if [ -z "$BACKUP_FILE" ] || [ ! -f "$BACKUP_FILE" ]; then
    echo "Usage: $0 <backup-file.sql.gz>"
    echo "       $0 latest"
    exit 1
fi

log "Verifying backup integrity..."
gzip -t "$BACKUP_FILE" || { log "ERROR: Backup file is corrupted"; exit 1; }

log "WARNING: This will overwrite the current database!"
read -rp "Continue? (yes/no): " confirm
[ "$confirm" = "yes" ] || { log "Aborted."; exit 0; }

log "Restoring from $(basename "$BACKUP_FILE")..."
gunzip -c "$BACKUP_FILE" | docker exec -i "$DB_CONTAINER" \
    mysql -u lumoo_user -plumoo_pass lumoo

log "Restore complete."
log "Restart the app container: docker compose restart app"
