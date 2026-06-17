#!/bin/bash
# 反馈时间监控 —— 实测 fastTest 和 fullTest 耗时，确保在承诺范围内。
#
# 使用方式：
#   chmod +x benchmark-feedback.sh
#   ./benchmark-feedback.sh
#
# 承诺 SLA：
#   fastTest（切片测试）≤ 3s
#   全量 test            ≤ 30s
#   integrationTest      ≤ 120s

set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "========================================="
echo "  反馈时间基准测试"
echo "========================================="
echo ""

cd "$(git rev-parse --show-toplevel 2>/dev/null || echo '.')"

# 预热（避免冷 JVM 影响首次测量）
echo "--- 预热 ---"
./gradlew compileJava > /dev/null 2>&1 || true

# ====== fastTest ======
echo "--- fastTest（切片测试）---"
START=$(date +%s%N)
./gradlew fastTest 2>&1 | tail -3
END=$(date +%s%N)
FAST_MS=$(( (END - START) / 1000000 ))
FAST_S=$(echo "scale=1; $FAST_MS / 1000" | bc)

if [ "$FAST_MS" -le 3000 ]; then
    echo -e "${GREEN}✅ fastTest: ${FAST_S}s (SLA ≤ 3s)${NC}"
else
    echo -e "${RED}❌ fastTest: ${FAST_S}s (SLA ≤ 3s exceeded!)${NC}"
fi

# ====== test（全量） ======
echo ""
echo "--- test（全量单元+切片测试）---"
START=$(date +%s%N)
./gradlew test 2>&1 | tail -3
END=$(date +%s%N)
TEST_MS=$(( (END - START) / 1000000 ))
TEST_S=$(echo "scale=1; $TEST_MS / 1000" | bc)

if [ "$TEST_MS" -le 30000 ]; then
    echo -e "${GREEN}✅ test: ${TEST_S}s (SLA ≤ 30s)${NC}"
elif [ "$TEST_MS" -le 60000 ]; then
    echo -e "${YELLOW}⚠️  test: ${TEST_S}s (SLA ≤ 30s exceeded, but < 60s)${NC}"
else
    echo -e "${RED}❌ test: ${TEST_S}s (SLA ≤ 30s exceeded!)${NC}"
fi

# ====== integrationTest ======
echo ""
echo "--- integrationTest（集成测试, 可跳过）---"
START=$(date +%s%N)
./gradlew integrationTest 2>&1 | tail -3
END=$(date +%s%N)
INT_MS=$(( (END - START) / 1000000 ))
INT_S=$(echo "scale=1; $INT_MS / 1000" | bc)

if [ "$INT_MS" -le 120000 ]; then
    echo -e "${GREEN}✅ integrationTest: ${INT_S}s (SLA ≤ 120s)${NC}"
else
    echo -e "${YELLOW}⚠️  integrationTest: ${INT_S}s (SLA ≤ 120s exceeded)${NC}"
fi

echo ""
echo "========================================="
echo "  基准测试完成"
echo "========================================="
