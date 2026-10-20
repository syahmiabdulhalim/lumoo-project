#!/bin/bash
# Verifies the Flyway migrations against a real MySQL 8.4 (prod engine) in a throwaway container.
#   1. Runs V1+V2, inserts duplicate cart/review rows, runs V3+V4 and checks the dedup result.
#   2. Runs the Spring context + repository tests against MySQL (Flyway + Hibernate validate).
#
# Usage: ./scripts/verify-mysql-migrations.sh        (needs Docker running)
set -euo pipefail

cd "$(dirname "$0")/.."
DOCKER="${DOCKER:-docker}"
command -v "$DOCKER" >/dev/null 2>&1 || DOCKER="/mnt/c/Program Files/Docker/Docker/resources/bin/docker.exe"  # WSL + Docker Desktop
NAME="lumoo-migration-check-$$"
PORT="${PORT:-33061}"
MIG=src/main/resources/db/migration

cleanup() { "$DOCKER" rm -f "$NAME" >/dev/null 2>&1 || true; }
trap cleanup EXIT

echo "▶ Starting MySQL 8.4 on port $PORT..."
"$DOCKER" run -d --name "$NAME" -p "$PORT:3306" \
  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=lumoo -e MYSQL_USER=lumoo -e MYSQL_PASSWORD=lumoo \
  mysql:8.4 >/dev/null
until "$DOCKER" exec "$NAME" mysqladmin ping -uroot -proot --silent >/dev/null 2>&1; do sleep 2; done
sleep 3

sql()  { "$DOCKER" exec -i "$NAME" mysql -uroot -proot --batch --skip-column-names lumoo; }
file() { sql < "$1"; }

echo "▶ Step 1: V1, V2, duplicate rows, then V3, V4 on raw MySQL"
file "$MIG/V1__baseline.sql"
file "$MIG/V2__email_verification_required.sql"
sql <<'SQL'
INSERT INTO users (id, email, role, verified, suspended, email_verified, email_verification_required) VALUES (1, 'b@test.com', 'USER', 0, 0, 1, 0);
INSERT INTO products (id, name, price) VALUES (1, 'Cement', 100);
INSERT INTO cart_items (user_id, product_id, quantity, name, price) VALUES (1, 1, 2, 'Cement', 100), (1, 1, 3, 'Cement', 100);
INSERT INTO reviews (user_id, product_id, rating) VALUES (1, 1, 5), (1, 1, 4);
SQL
file "$MIG/V3__order_indexes_and_unique_cart_review.sql"
file "$MIG/V4__order_version_column.sql"
CART=$(echo "SELECT COUNT(*), SUM(quantity) FROM cart_items;" | sql)
REV=$(echo "SELECT COUNT(*), MAX(rating) FROM reviews;" | sql)
[ "$CART" = "1	5" ] || { echo "✗ cart dedup wrong: $CART (expected 1 row, qty 5)"; exit 1; }
[ "$REV" = "1	5" ]  || { echo "✗ review dedup wrong: $REV (expected the first review kept)"; exit 1; }
echo "  ✓ duplicates merged (cart qty 2+3=5, earliest review kept)"

echo "▶ Step 2: Spring Boot + Flyway + Hibernate validate on a fresh MySQL database"
echo "DROP DATABASE lumoo; CREATE DATABASE lumoo; GRANT ALL ON lumoo.* TO 'lumoo'@'%';" | "$DOCKER" exec -i "$NAME" mysql -uroot -proot
./mvnw -B -q test \
  -Dtest='LumooApplicationTests,MigrationConstraintsTest,OrderRepositoryQueriesTest,LoginIntegrationTest' \
  -Dspring.datasource.url="jdbc:mysql://localhost:$PORT/lumoo?useSSL=false&allowPublicKeyRetrieval=true" \
  -Dspring.datasource.username=lumoo -Dspring.datasource.password=lumoo
echo "✓ Migrations verified on MySQL 8.4"
