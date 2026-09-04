#!/bin/bash
# =============================================================================
# init.sh - AI Agent environment bootstrap
# Run at the start of every Agent session: bash init.sh
# =============================================================================
# Configuration
APP_PORT=8081
HEALTH_CHECK_URL="http://localhost:8081/"
STARTUP_COMMAND="mvn spring-boot:run -Dspring-boot.run.profiles=dev"
LOG_FILE="logs/dev_server.log"
HEALTH_TIMEOUT=60
# =============================================================================

set -euo pipefail
echo "[init] Bootstrapping hm-dianping..."

# 1. Verify required tools
MISSING=()
command -v java >/dev/null 2>&1 || MISSING+=("java")
command -v mvn >/dev/null 2>&1 || MISSING+=("mvn")
command -v curl >/dev/null 2>&1 || MISSING+=("curl")
command -v lsof >/dev/null 2>&1 || MISSING+=("lsof")
command -v nohup >/dev/null 2>&1 || MISSING+=("nohup")
if [ ${#MISSING[@]} -gt 0 ]; then
  echo "[ERROR] Missing tools: ${MISSING[*]}. Install them and re-run."
  exit 1
fi
echo "[init] ✓ Tools OK"

# 2. Clear port if occupied
PORT_PID=$(lsof -t -i:"$APP_PORT" 2>/dev/null || true)
if [ -n "$PORT_PID" ]; then
  echo "[init] Releasing port $APP_PORT (PID $PORT_PID)..."
  kill -9 "$PORT_PID" 2>/dev/null || true
  sleep 1
fi

# 3. Start dev server in background
mkdir -p logs
nohup $STARTUP_COMMAND > "$LOG_FILE" 2>&1 &
echo "[init] Server starting (PID $!, logs -> $LOG_FILE)"

# 4. Health check loop
echo "[init] Waiting for server (timeout: ${HEALTH_TIMEOUT}s)..."
COUNT=0
until [ $COUNT -ge $HEALTH_TIMEOUT ]; do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_CHECK_URL" 2>/dev/null || echo "000")
  [ "$STATUS" = "200" ] && { echo "[init] ✓ Server ready at $HEALTH_CHECK_URL"; break; }
  sleep 1
  COUNT=$((COUNT+1))
  [ $((COUNT % 10)) -eq 0 ] && echo "[init]   Waiting... ${COUNT}s"
done
if [ $COUNT -ge $HEALTH_TIMEOUT ]; then
  echo "[ERROR] Server not healthy after ${HEALTH_TIMEOUT}s - check $LOG_FILE"
  exit 1
fi

# 5. Git context
echo ""
echo "[init] -- Git status --------------------------------"
git status -s
echo ""
echo "[init] -- Recent commits -----------------------------"
git log -n 3 --oneline
echo ""
echo "[init] ✓ Environment ready."
