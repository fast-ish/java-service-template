#!/bin/bash
# =============================================================================
# Service Verification Script
# =============================================================================
# Run this after starting your service to verify everything is working.
# Usage: ./scripts/verify.sh [port]
# =============================================================================

set -e

PORT=${1:-8080}
MGMT_PORT=${2:-8081}
BASE_URL="http://localhost:$PORT"
MGMT_URL="http://localhost:$MGMT_PORT"

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo ""
echo "=========================================="
echo "  Service Verification"
echo "=========================================="
echo ""

check() {
    local name=$1
    local url=$2
    local expected=${3:-200}

    printf "  %-30s" "$name..."

    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")

    if [ "$response" = "$expected" ]; then
        echo -e "${GREEN}✓ OK${NC}"
        return 0
    else
        echo -e "${RED}✗ FAILED (HTTP $response)${NC}"
        return 1
    fi
}

echo "Health Checks:"
echo "--------------"
check "Liveness" "$MGMT_URL/health/liveness"
check "Readiness" "$MGMT_URL/health/readiness"

echo ""
echo "API Endpoints:"
echo "--------------"
check "Status" "$BASE_URL/api/v1/status"
check "Info" "$BASE_URL/api/v1/info"

echo ""
echo "Documentation:"
echo "--------------"
check "OpenAPI Spec" "$BASE_URL/api-docs"
check "Swagger UI" "$BASE_URL/swagger-ui.html" 302

echo ""
echo "Metrics:"
echo "--------"
check "Prometheus" "$MGMT_URL/prometheus"

{%- if values.featureFlags %}
echo ""
echo "Feature Flags:"
echo "--------------"
check "Togglz Console" "$BASE_URL/togglz-console" 302
{%- endif %}

echo ""
echo "=========================================="

# Summary
failed=0
if ! check "Final Health Check" "$MGMT_URL/health" >/dev/null 2>&1; then
    failed=1
fi

if [ $failed -eq 0 ]; then
    echo -e "${GREEN}"
    echo "  ✓ All checks passed!"
    echo -e "${NC}"
    echo "  Your service is ready at: $BASE_URL"
    echo "  API docs available at: $BASE_URL/swagger-ui.html"
    echo ""
else
    echo -e "${RED}"
    echo "  ✗ Some checks failed"
    echo -e "${NC}"
    echo "  Check the logs: make deps-logs"
    echo ""
    exit 1
fi
