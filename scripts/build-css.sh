#!/bin/bash
# Builds src/main/resources/static/css/tailwind.css from the classes used in templates (and Java).
# Uses the pinned Tailwind v3 standalone CLI (no Node). Runs automatically in the Maven build
# (generate-resources); run it by hand with --watch while editing templates.
#   ./scripts/build-css.sh           one minified build
#   ./scripts/build-css.sh --watch   rebuild on every template change
set -euo pipefail
cd "$(dirname "$0")/.."

VERSION=v3.4.17
case "$(uname -s)-$(uname -m)" in
  Linux-x86_64)  ASSET=tailwindcss-linux-x64;   SHA=7d24f7fa191d2193b78cd5f5a42a6093e14409521908529f42d80b11fde1f1d4 ;;
  Linux-aarch64) ASSET=tailwindcss-linux-arm64; SHA=69b1378b8133192d7d2feb12a116fa12d035594f58db3eff215879e4ad8cf39b ;;
  Darwin-arm64)  ASSET=tailwindcss-macos-arm64; SHA=a1d0c7985759accca0bf12e51ac1dcbf0f6cf2fffb62e6e0f62d091c477a10a3 ;;
  Darwin-x86_64) ASSET=tailwindcss-macos-x64;   SHA=6cbdad74be776c087ffa5e9a057512c54898f9fe8828d3362212dfe32fc933a3 ;;
  *) echo "build-css: unsupported platform $(uname -s)-$(uname -m)" >&2; exit 1 ;;
esac

CACHE="${TAILWIND_CACHE:-$HOME/.cache/lumoo}"
BIN="$CACHE/$ASSET-$VERSION"
if [ ! -x "$BIN" ]; then
  mkdir -p "$CACHE"
  URL="https://github.com/tailwindlabs/tailwindcss/releases/download/$VERSION/$ASSET"
  echo "build-css: downloading Tailwind $VERSION ($ASSET)"
  if command -v curl >/dev/null; then curl -sSfL -o "$BIN.tmp" "$URL"; else wget -qO "$BIN.tmp" "$URL"; fi
  echo "$SHA  $BIN.tmp" | sha256sum -c --quiet - || { echo "build-css: checksum mismatch, refusing to run" >&2; rm -f "$BIN.tmp"; exit 1; }
  chmod +x "$BIN.tmp" && mv "$BIN.tmp" "$BIN"
fi

OUT=src/main/resources/static/css/tailwind.css
if [ "${1:-}" = "--watch" ]; then
  exec "$BIN" -c tailwind.config.js -i src/main/css/tailwind.input.css -o "$OUT" --watch
fi
"$BIN" -c tailwind.config.js -i src/main/css/tailwind.input.css -o "$OUT" --minify 2>&1 | grep -v "^$" | grep -vi "browserslist" || true
[ -s "$OUT" ] || { echo "build-css: $OUT was not produced" >&2; exit 1; }
echo "build-css: wrote $OUT ($(wc -c < "$OUT") bytes)"
