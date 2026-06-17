#!/bin/bash
# 环境重置脚本 —— 完全重建本地开发环境
#
# 使用方式：
#   chmod +x env-reset.sh
#   ./env-reset.sh
#
# 做的事：
#   1. 停止并删除所有容器、网络、数据卷
#   2. 重新启动所有服务
#   3. 等待 MySQL 就绪
#   4. 运行环境自检
#
# 警告：会删除所有本地数据！生产环境不要用。

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"

if [ ! -f "$COMPOSE_FILE" ]; then
    echo -e "${RED}❌ 找不到 $COMPOSE_FILE${NC}"
    exit 1
fi

echo "========================================="
echo "  环境重置"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================="
echo ""

# 1. 停止并清理
echo "--- 停止并删除所有容器 + 数据卷 ---"
docker compose -f "$COMPOSE_FILE" down -v --remove-orphans 2>/dev/null || true
echo -e "${GREEN}✅ 旧环境已清理${NC}"

# 2. 重新启动
echo ""
echo "--- 重新启动服务 ---"
docker compose -f "$COMPOSE_FILE" up -d

# 3. 等待 MySQL 就绪
echo ""
echo "--- 等待 MySQL 就绪 ---"
for i in $(seq 1 30); do
    if docker compose -f "$COMPOSE_FILE" exec -T mysql mysqladmin ping -h localhost -u root -proot --silent 2>/dev/null; then
        echo -e "${GREEN}✅ MySQL 就绪 (${i}s)${NC}"
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo -e "${RED}❌ MySQL 启动超时${NC}"
        docker compose -f "$COMPOSE_FILE" logs mysql --tail 20
        exit 1
    fi
    sleep 1
done

# 4. 等待 Redis 就绪
echo "--- 等待 Redis 就绪 ---"
for i in $(seq 1 10); do
    if docker compose -f "$COMPOSE_FILE" exec -T redis redis-cli PING 2>/dev/null | grep -q "PONG"; then
        echo -e "${GREEN}✅ Redis 就绪 (${i}s)${NC}"
        break
    fi
    sleep 1
done

# 5. 运行环境自检
echo ""
echo "--- 环境自检 ---"
if [ -f "${SCRIPT_DIR}/env-check.sh" ]; then
    bash "${SCRIPT_DIR}/env-check.sh"
else
    echo -e "${YELLOW}⚠️  env-check.sh 不存在，跳过${NC}"
fi

echo ""
echo "========================================="
echo -e "  ${GREEN}✅ 环境重置完成${NC}"
echo "========================================="
echo ""
echo "可以启动应用："
echo "  ./gradlew bootRun --args='--spring.profiles.active=local'"
