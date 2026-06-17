#!/bin/bash
# 环境自检脚本 —— 验证本地开发基础设施是否就绪
#
# 使用方式：
#   chmod +x env-check.sh
#   ./env-check.sh
#
# 检查项：
#   1. Docker 是否运行
#   2. docker compose 服务健康状态
#   3. MySQL 连接
#   4. Redis 连接
#   5. WireMock 可访问
#
# 退出码：0 = 全部通过，1 = 有失败项

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

PASS=0
FAIL=0

pass() { echo -e "  ${GREEN}✅ $1${NC}"; PASS=$((PASS+1)); }
fail() { echo -e "  ${RED}❌ $1${NC}"; FAIL=$((FAIL+1)); }
warn() { echo -e "  ${YELLOW}⚠️  $1${NC}"; }

echo "========================================="
echo "  Java Harness 环境自检"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================="
echo ""

# ====== 1. Docker ======
echo "--- Docker ---"
if docker info > /dev/null 2>&1; then
    pass "Docker 运行中"
else
    fail "Docker 未运行，请先启动 Docker"
    echo ""
    echo "结果: ${PASS} 通过, ${FAIL} 失败"
    exit 1
fi

# ====== 2. Docker Compose 服务 ======
echo "--- docker compose 服务 ---"

# 查找 docker compose 文件位置
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"
if [ ! -f "$COMPOSE_FILE" ]; then
    # 尝试从项目根目录
    COMPOSE_FILE="$(git rev-parse --show-toplevel 2>/dev/null || echo '.')/.harness/devops/docker-compose.yml"
fi

if [ ! -f "$COMPOSE_FILE" ]; then
    fail "找不到 docker-compose.yml"
    exit 1
fi

# 检查容器运行状态
check_container() {
    local name=$1
    local health_label=$2
    if docker compose -f "$COMPOSE_FILE" ps --status running "$name" 2>/dev/null | grep -q "$name"; then
        if [ -n "$health_label" ]; then
            local health
            health=$(docker inspect --format='{{.State.Health.Status}}' "$name" 2>/dev/null || echo "no-healthcheck")
            if [ "$health" = "healthy" ] || [ "$health" = "no-healthcheck" ]; then
                pass "$name 运行中 (health: $health)"
            else
                warn "$name 运行中但 health=$health"
            fi
        else
            pass "$name 运行中"
        fi
    else
        fail "$name 未运行 → docker compose -f $COMPOSE_FILE up -d $name"
    fi
}

check_container "harness-mysql" "yes"
check_container "harness-redis" "yes"
check_container "harness-wiremock" "no"

# ====== 3. MySQL 连接 ======
echo "--- MySQL ---"
if docker compose -f "$COMPOSE_FILE" exec -T mysql mysqladmin ping -h localhost -u root -proot --silent 2>/dev/null; then
    pass "MySQL 连接正常 (root@localhost:3306/app_db)"
else
    fail "MySQL 连接失败"
fi

# ====== 4. Redis 连接 ======
echo "--- Redis ---"
if docker compose -f "$COMPOSE_FILE" exec -T redis redis-cli PING 2>/dev/null | grep -q "PONG"; then
    pass "Redis 连接正常 (localhost:6379)"
else
    fail "Redis 连接失败"
fi

# ====== 5. WireMock ======
echo "--- WireMock ---"
WIREMOCK_URL="http://localhost:8089/__admin/mappings"
if curl -s -o /dev/null -w "%{http_code}" "$WIREMOCK_URL" 2>/dev/null | grep -q "200"; then
    # 检查 stub 是否加载
    STUB_COUNT=$(curl -s "$WIREMOCK_URL" 2>/dev/null | python3 -c "import sys,json; print(len(json.load(sys.stdin).get('mappings',[])))" 2>/dev/null || echo "?")
    pass "WireMock 可访问 (localhost:8089, ${STUB_COUNT} stubs 已加载)"
else
    warn "WireMock 不可访问 (localhost:8089) — 外部 API stub 不可用"
fi

# ====== 汇总 ======
echo ""
echo "========================================="
echo -e "  结果: ${GREEN}${PASS} 通过${NC}, ${RED}${FAIL} 失败${NC}"
echo "========================================="

if [ "$FAIL" -gt 0 ]; then
    echo ""
    echo "修复建议："
    echo "  1. 启动所有服务：docker compose -f $COMPOSE_FILE up -d"
    echo "  2. 等待 MySQL 就绪：sleep 10"
    echo "  3. 重新运行自检：./env-check.sh"
    exit 1
fi

echo ""
echo "环境就绪，可以启动应用："
echo "  ./gradlew bootRun --args='--spring.profiles.active=local'"
exit 0
