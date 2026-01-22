#!/bin/bash

# ============================================
# Bike Rental API Test Script (extended)
# ============================================

# Настройки
BASE_URL="${1:-http://localhost:8080}"
TOKEN=""
ADMIN_TOKEN="${ADMIN_TOKEN:-}"
ADMIN_USERNAME="${ADMIN_USERNAME:-}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-}"
PROMPT_ADMIN="${PROMPT_ADMIN:-0}" # set 1 to prompt for admin creds (interactive)
BICYCLE_TYPE="${BICYCLE_TYPE:-MOUNTAIN}" # sent as JSON field "type" to /api/v1/bicycles (safe default)
ENABLE_DELETES="${ENABLE_DELETES:-0}"   # set 1 to enable DELETE tests (dangerous)

# Цвета
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Счётчики
PASSED=0
FAILED=0
SKIPPED=0

print_skip() {
    local name="$1"
    local reason="$2"
    echo -e "${BLUE}SKIP${NC} $name${reason:+ — $reason}"
    ((SKIPPED++))
}

# Функция для вывода результата
print_result() {
    local name="$1"
    local status="$2"
    local response="$3"

    if [ "$status" -ge 200 ] && [ "$status" -lt 300 ]; then
        echo -e "${GREEN}✓ PASS${NC} [$status] $name"
        ((PASSED++))
    elif [[ "$name" =~ expected[[:space:]]+([0-9]{3}) ]] && [ "$status" -eq "${BASH_REMATCH[1]}" ]; then
        echo -e "${GREEN}PASS${NC} [$status] $name"
        ((PASSED++))
    elif [ "$status" -ge 400 ] && [ "$status" -lt 500 ] && [[ "$name" == *"should fail"* ]]; then
        echo -e "${GREEN}✓ PASS${NC} [$status] $name (expected failure)"
        ((PASSED++))
    else
        echo -e "${RED}✗ FAIL${NC} [$status] $name"
        echo -e "${YELLOW}  Response: $response${NC}"
        ((FAILED++))
    fi
}

# Функция для HTTP запросов
api_call() {
    local method="$1"
    local endpoint="$2"
    local data="$3"
    local auth="$4"

    local headers="-H \"Content-Type: application/json\" -H \"Accept: application/json\""
    if [ -n "$auth" ]; then
        headers="$headers -H \"Authorization: Bearer $auth\""
    fi

    if [ -n "$data" ]; then
        response=$(eval "curl -s -w '\n%{http_code}' -X $method $headers -d '$data' '$BASE_URL$endpoint'")
    else
        response=$(eval "curl -s -w '\n%{http_code}' -X $method $headers '$BASE_URL$endpoint'")
    fi

    body=$(echo "$response" | head -n -1)
    status=$(echo "$response" | tail -n 1)

    echo "$status|$body"
}

# Утилита: безопасно извлечь поле из JSON (если jq есть)
json_get() {
    local json="$1"
    local jq_expr="$2"
    if command -v jq >/dev/null 2>&1; then
        echo "$json" | jq -r "$jq_expr // empty" 2>/dev/null
    else
        echo ""
    fi
}

echo ""
echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   Bike Rental API Test Suite${NC}"
echo -e "${BLUE}   Base URL: $BASE_URL${NC}"
echo -e "${BLUE}============================================${NC}"
echo ""

# ============================================
# 1. AUTH TESTS
# ============================================
echo -e "${YELLOW}--- Authentication Tests ---${NC}"

# Генерируем уникальное имя пользователя
TIMESTAMP=$(date +%s)
TEST_USER="testuser_$TIMESTAMP"
TEST_PASS="password123"

USER_ID=""

# Register
result=$(api_call "POST" "/api/v1/auth/register" "{\"username\":\"$TEST_USER\",\"password\":\"$TEST_PASS\"}")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
print_result "Register new user" "$status" "$body"

# Login
result=$(api_call "POST" "/api/v1/auth/login" "{\"username\":\"$TEST_USER\",\"password\":\"$TEST_PASS\"}")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
print_result "Login user" "$status" "$body"

# Извлекаем токен
TOKEN=$(json_get "$body" '.access_token')
if [ -z "$TOKEN" ]; then
    TOKEN=$(json_get "$body" '.accessToken')
fi
if [ -z "$TOKEN" ]; then
    TOKEN=$(echo "$body" | grep -oE '"access_token"\s*:\s*"[^"]*"' | head -1 | cut -d'"' -f4)
fi
if [ -z "$TOKEN" ]; then
    TOKEN=$(echo "$body" | grep -oE '"accessToken"\s*:\s*"[^"]*"' | head -1 | cut -d'"' -f4)
fi

# Извлекаем USER_ID из login response (часто поле "user" или "userId" или "id")
USER_ID=$(json_get "$body" '.user_id')
if [ -z "$USER_ID" ]; then
    USER_ID=$(json_get "$body" '.userId')
fi
if [ -z "$USER_ID" ]; then
    USER_ID=$(json_get "$body" '.id')
fi
if [ -z "$USER_ID" ]; then
    USER_ID=$(echo "$body" | grep -oE '"user_id"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
fi
if [ -z "$USER_ID" ]; then
    USER_ID=$(echo "$body" | grep -oE '"user"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
fi
if [ -z "$USER_ID" ]; then
    USER_ID=$(echo "$body" | grep -oE '"userId"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
fi
if [ -z "$USER_ID" ]; then
    USER_ID=$(echo "$body" | grep -oE '"id"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
fi

if [ -n "$TOKEN" ]; then
    echo -e "${GREEN}  Token received: ${TOKEN:0:20}...${NC}"
else
    echo -e "${RED}  Failed to get token!${NC}"
fi

if [ -n "$USER_ID" ]; then
    echo -e "${GREEN}  User ID from login: $USER_ID${NC}"
else
    echo -e "${YELLOW}  User ID not found in login response (will try /auth/info)${NC}"
fi

# Get user info
result=$(api_call "GET" "/api/v1/auth/info" "" "$TOKEN")
status=$(echo "$result" | cut -d'|' -f1)
info_body=$(echo "$result" | cut -d'|' -f2-)
print_result "Get user info (authenticated)" "$status" "$info_body"

# Если USER_ID не нашли в login — пробуем из /auth/info
if [ -z "$USER_ID" ] && [ "$status" -ge 200 ] && [ "$status" -lt 300 ]; then
    USER_ID=$(echo "$info_body" | grep -oE '"id"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
    if [ -z "$USER_ID" ]; then
        USER_ID=$(echo "$info_body" | grep -oE '"user"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
    fi
    if [ -z "$USER_ID" ]; then
        USER_ID=$(echo "$info_body" | grep -oE '"userId"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
    fi
fi

if [ -n "$USER_ID" ]; then
    echo -e "${BLUE}  Final USER_ID: $USER_ID${NC}"
else
    echo -e "${RED}  Failed to determine USER_ID (rental create will not work)${NC}"
fi

# Get user info without token (should fail)
result=$(api_call "GET" "/api/v1/auth/info" "" "")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
print_result "Get user info without token - should fail" "$status" "$body"

# Duplicate registration (should fail)
result=$(api_call "POST" "/api/v1/auth/register" "{\"username\":\"$TEST_USER\",\"password\":\"$TEST_PASS\"}")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
print_result "Duplicate registration - should fail" "$status" "$body"

echo ""

# ============================================
# 1.5 ADMIN AUTH (optional)
# ============================================
ADMIN_ENABLED=0

if [ "$PROMPT_ADMIN" -eq 1 ] && [ -z "$ADMIN_TOKEN" ] && [ -z "$ADMIN_USERNAME" ] && [ -t 0 ]; then
    echo -e "${YELLOW}--- Admin Login (optional) ---${NC}"
    read -r -p "Admin username (leave blank to skip admin tests): " ADMIN_USERNAME
    if [ -n "$ADMIN_USERNAME" ]; then
        read -r -s -p "Admin password: " ADMIN_PASSWORD
        echo ""
    fi
fi

if [ -n "$ADMIN_TOKEN" ]; then
    ADMIN_ENABLED=1
    echo -e "${GREEN}  Using ADMIN_TOKEN from env (${ADMIN_TOKEN:0:20}...)${NC}"
elif [ -n "$ADMIN_USERNAME" ] && [ -n "$ADMIN_PASSWORD" ]; then
    echo -e "${YELLOW}--- Admin Login (optional) ---${NC}"
    result=$(api_call "POST" "/api/v1/auth/login" "{\"username\":\"$ADMIN_USERNAME\",\"password\":\"$ADMIN_PASSWORD\"}")
    status=$(echo "$result" | cut -d'|' -f1)
    admin_body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Login admin" "$status" "$admin_body"

    ADMIN_TOKEN=$(json_get "$admin_body" '.access_token')
    if [ -z "$ADMIN_TOKEN" ]; then
        ADMIN_TOKEN=$(echo "$admin_body" | grep -oE '"access_token"\s*:\s*"[^"]*"' | head -1 | cut -d'"' -f4)
    fi

    if [ -n "$ADMIN_TOKEN" ]; then
        ADMIN_ENABLED=1
        echo -e "${GREEN}  Admin token received: ${ADMIN_TOKEN:0:20}...${NC}"
    else
        echo -e "${RED}  Failed to get admin token (admin-only tests will be skipped/negative)${NC}"
    fi
else
    echo -e "${BLUE}  Admin credentials not provided (set ADMIN_USERNAME/ADMIN_PASSWORD or ADMIN_TOKEN).${NC}"
fi



# ============================================
# 2. STATION TESTS
# ============================================
echo -e "${YELLOW}--- Station Tests ---${NC}"

# Create station
STATION_ID=""
if [ "$ADMIN_ENABLED" -eq 1 ]; then
    result=$(api_call "POST" "/api/v1/stations" "{\"name\":\"Test Station $TIMESTAMP\",\"coordinates\":{\"latitude\":55.75,\"longitude\":37.62}}" "$ADMIN_TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Create station (admin)" "$status" "$body"

    STATION_ID=$(json_get "$body" '.id')
    if [ -z "$STATION_ID" ]; then
        STATION_ID=$(echo "$body" | grep -oE '"id"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
    fi
    echo -e "${BLUE}  Station ID: $STATION_ID${NC}"
else
    result=$(api_call "POST" "/api/v1/stations" "{\"name\":\"Test Station $TIMESTAMP\",\"coordinates\":{\"latitude\":55.75,\"longitude\":37.62}}" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Create station - should fail" "$status" "$body"
fi

# Get all stations
result=$(api_call "GET" "/api/v1/stations" "" "$TOKEN")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
print_result "Get all stations" "$status" "$body"

# Get station by ID
if [ -n "$STATION_ID" ]; then
    result=$(api_call "GET" "/api/v1/stations/$STATION_ID" "" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Get station by ID" "$status" "$body"

    # NEW: Get bicycles by station
    result=$(api_call "GET" "/api/v1/stations/$STATION_ID/bicycles" "" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Get bicycles by station" "$status" "$body"
else
    print_skip "Get station by ID" "no station created"
    print_skip "Get bicycles by station" "no station created"
fi

echo ""

# ============================================
# 3. BICYCLE TESTS
# ============================================
echo -e "${YELLOW}--- Bicycle Tests ---${NC}"

# Create bicycle
BICYCLE_ID=""
if [ "$ADMIN_ENABLED" -eq 1 ] && [ -n "$STATION_ID" ]; then
    result=$(api_call "POST" "/api/v1/bicycles" "{\"model\":\"Test Bike $TIMESTAMP\",\"type\":\"$BICYCLE_TYPE\",\"stationId\":$STATION_ID}" "$ADMIN_TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Create bicycle (admin)" "$status" "$body"

    BICYCLE_ID=$(json_get "$body" '.id')
    if [ -z "$BICYCLE_ID" ]; then
        BICYCLE_ID=$(echo "$body" | grep -oE '"id"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
    fi
    echo -e "${BLUE}  Bicycle ID: $BICYCLE_ID${NC}"
elif [ "$ADMIN_ENABLED" -ne 1 ]; then
    # dummy stationId to validate RBAC (USER must be forbidden)
    result=$(api_call "POST" "/api/v1/bicycles" "{\"model\":\"Test Bike $TIMESTAMP\",\"type\":\"$BICYCLE_TYPE\",\"stationId\":1}" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Create bicycle - should fail" "$status" "$body"
else
    print_skip "Create bicycle (admin)" "no station created"
fi

# Get all bicycles
result=$(api_call "GET" "/api/v1/bicycles" "" "$TOKEN")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
print_result "Get all bicycles" "$status" "$body"

# Get bicycle by ID
if [ -n "$BICYCLE_ID" ]; then
    result=$(api_call "GET" "/api/v1/bicycles/$BICYCLE_ID" "" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Get bicycle by ID" "$status" "$body"
else
    print_skip "Get bicycle by ID" "no bicycle created"
fi

# NEW: Needs service
result=$(api_call "GET" "/api/v1/bicycles/needs-service" "" "$TOKEN")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
print_result "Get bicycles needing service" "$status" "$body"

echo ""

# ============================================
# 4. PAYMENT TESTS
# ============================================
echo -e "${YELLOW}--- Payment Tests ---${NC}"

# Add balance
result=$(api_call "POST" "/api/v1/payments" "{\"amount\":1000}" "$TOKEN")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
print_result "Add balance (payment)" "$status" "$body"

# Get payments
result=$(api_call "GET" "/api/v1/payments" "" "$TOKEN")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
print_result "Get all payments" "$status" "$body"

echo ""

# ============================================
# 5. RENTAL TESTS
# ============================================
echo -e "${YELLOW}--- Rental Tests ---${NC}"

echo -e "${BLUE}  User ID: $USER_ID${NC}"

# NEW: Payments by user
if [ -n "$USER_ID" ]; then
    result=$(api_call "GET" "/api/v1/payments/user/$USER_ID" "" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Get payments by user" "$status" "$body"
fi
# ============================================
# Create rental (correct payload for your backend)
# ============================================
echo -e "${YELLOW}--- Create Rental ---${NC}"

if [ -z "${USER_ID:-}" ] || [ -z "${BICYCLE_ID:-}" ] || [ -z "${STATION_ID:-}" ]; then
    if [ "$ADMIN_ENABLED" -eq 1 ]; then
        echo -e "${RED}✗ FAIL${NC} Create rental: missing IDs (USER_ID=${USER_ID:-}, BICYCLE_ID=${BICYCLE_ID:-}, STATION_ID=${STATION_ID:-})"
        ((FAILED++))
    else
        print_skip "Create rental" "missing IDs (need admin to create station/bicycle)"
    fi
else
    # Attempt 1: OpenAPI-style (most likely correct): user, bicycle, start_station
    payload="{\"user\":$USER_ID,\"bicycle\":$BICYCLE_ID,\"start_station\":$STATION_ID}"
    result=$(api_call "POST" "/api/v1/rentals" "$payload" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)

    if [ "$status" -ge 200 ] && [ "$status" -lt 300 ]; then
        print_result "Create rental" "$status" "$body"
        RENTAL_ID=$(echo "$body" | grep -oE '"id"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
        echo -e "${BLUE}  Rental ID: $RENTAL_ID${NC}"
    else
        # Optional fallback: some codebases use startStation or startStationId but NOT userId.
        # Try a second variant only if the first failed.
        payload2="{\"user\":$USER_ID,\"bicycle\":$BICYCLE_ID,\"startStation\":$STATION_ID}"
        result2=$(api_call "POST" "/api/v1/rentals" "$payload2" "$TOKEN")
        status2=$(echo "$result2" | cut -d'|' -f1)
        body2=$(echo "$result2" | cut -d'|' -f2-)

        if [ "$status2" -ge 200 ] && [ "$status2" -lt 300 ]; then
            print_result "Create rental (fallback payload)" "$status2" "$body2"
            RENTAL_ID=$(echo "$body2" | grep -oE '"id"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
            echo -e "${BLUE}  Rental ID: $RENTAL_ID${NC}"
        else
            echo -e "${RED}✗ FAIL${NC} Create rental"
            echo -e "${YELLOW}  Attempt 1 payload: $payload${NC}"
            echo -e "${YELLOW}  Attempt 1 status:  $status${NC}"
            echo -e "${YELLOW}  Attempt 1 body:    $body${NC}"
            echo -e "${YELLOW}  Attempt 2 payload: $payload2${NC}"
            echo -e "${YELLOW}  Attempt 2 status:  $status2${NC}"
            echo -e "${YELLOW}  Attempt 2 body:    $body2${NC}"
            ((FAILED++))
        fi
    fi
fi

echo ""


# Get all rentals
result=$(api_call "GET" "/api/v1/rentals" "" "$TOKEN")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
print_result "Get all rentals" "$status" "$body"

# NEW: Get rentals by user
if [ -n "$USER_ID" ]; then
    result=$(api_call "GET" "/api/v1/rentals/user/$USER_ID" "" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Get rentals by user" "$status" "$body"
fi

# NEW: Get rental by ID
if [ -n "$RENTAL_ID" ]; then
    result=$(api_call "GET" "/api/v1/rentals/$RENTAL_ID" "" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Get rental by ID" "$status" "$body"
fi

# NEW: Cancel rental (may fail depending on status)
#if [ -n "$RENTAL_ID" ]; then
#    result=$(api_call "POST" "/api/v1/rentals/$RENTAL_ID/cancel" "" "$TOKEN")
#    status=$(echo "$result" | cut -d'|' -f1)
#    body=$(echo "$result" | cut -d'|' -f2-)
#    print_result "Cancel rental" "$status" "$body"
#fi

# Complete rental
if [ -n "$RENTAL_ID" ] && [ -n "$STATION_ID" ]; then
    sleep 1  # Ждём 1 секунду чтобы была стоимость
    result=$(api_call "PUT" "/api/v1/rentals/$RENTAL_ID/complete" "{\"end_station\":$STATION_ID}" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Complete rental" "$status" "$body"
fi

echo ""






# ============================================
# 6. TECHNICIAN TESTS
# ============================================
echo -e "${YELLOW}--- Technician Tests ---${NC}"

# Create technician
TECHNICIAN_ID=""
if [ "$ADMIN_ENABLED" -eq 1 ]; then
    result=$(api_call "POST" "/api/v1/technicians" "{\"name\":\"John Tech $TIMESTAMP\",\"phone\":\"+7999123456\",\"specialization\":\"Mountain bikes\"}" "$ADMIN_TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Create technician (admin)" "$status" "$body"

    TECHNICIAN_ID=$(json_get "$body" '.id')
    if [ -z "$TECHNICIAN_ID" ]; then
        TECHNICIAN_ID=$(echo "$body" | grep -oE '"id"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
    fi
    echo -e "${BLUE}  Technician ID: $TECHNICIAN_ID${NC}"

    # Get all technicians
    result=$(api_call "GET" "/api/v1/technicians" "" "$ADMIN_TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Get all technicians (admin)" "$status" "$body"

    # NEW: Get technician by ID
    if [ -n "$TECHNICIAN_ID" ]; then
        result=$(api_call "GET" "/api/v1/technicians/$TECHNICIAN_ID" "" "$ADMIN_TOKEN")
        status=$(echo "$result" | cut -d'|' -f1)
        body=$(echo "$result" | cut -d'|' -f2-)
        print_result "Get technician by ID (admin)" "$status" "$body"
    else
        print_skip "Get technician by ID (admin)" "no technician created"
    fi
else
    # Validate RBAC (USER must be forbidden)
    result=$(api_call "GET" "/api/v1/technicians" "" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Get all technicians - should fail" "$status" "$body"

    result=$(api_call "POST" "/api/v1/technicians" "{\"name\":\"John Tech $TIMESTAMP\",\"phone\":\"+7999123456\",\"specialization\":\"Mountain bikes\"}" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Create technician - should fail" "$status" "$body"
fi

echo ""

# ============================================
# 7. REPAIR TESTS
# ============================================
echo -e "${YELLOW}--- Repair Tests ---${NC}"

# Create repair
REPAIR_ID=""
if [ "$ADMIN_ENABLED" -eq 1 ]; then
    if [ -n "$BICYCLE_ID" ] && [ -n "$TECHNICIAN_ID" ]; then
        result=$(api_call "POST" "/api/v1/repairs" "{\"bicycleId\":$BICYCLE_ID,\"technicianId\":$TECHNICIAN_ID,\"description\":\"Test repair $TIMESTAMP\"}" "$ADMIN_TOKEN")
        status=$(echo "$result" | cut -d'|' -f1)
        body=$(echo "$result" | cut -d'|' -f2-)
        print_result "Create repair (admin)" "$status" "$body"

        REPAIR_ID=$(json_get "$body" '.id')
        if [ -z "$REPAIR_ID" ]; then
            REPAIR_ID=$(echo "$body" | grep -oE '"id"\s*:\s*[0-9]+' | head -1 | grep -oE '[0-9]+')
        fi
        echo -e "${BLUE}  Repair ID: $REPAIR_ID${NC}"
    else
        print_skip "Create repair (admin)" "missing bicycle/technician IDs"
    fi
else
    # Validate RBAC (USER must be forbidden)
    if [ -n "$BICYCLE_ID" ] && [ -n "$TECHNICIAN_ID" ]; then
        result=$(api_call "POST" "/api/v1/repairs" "{\"bicycleId\":$BICYCLE_ID,\"technicianId\":$TECHNICIAN_ID,\"description\":\"Test repair $TIMESTAMP\"}" "$TOKEN")
        status=$(echo "$result" | cut -d'|' -f1)
        body=$(echo "$result" | cut -d'|' -f2-)
        print_result "Create repair - should fail" "$status" "$body"
    else
        print_skip "Create repair - should fail" "missing bicycle/technician IDs"
    fi
fi

# Get all repairs
result=$(api_call "GET" "/api/v1/repairs" "" "${ADMIN_TOKEN:-$TOKEN}")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
if [ "$ADMIN_ENABLED" -eq 1 ]; then
    print_result "Get all repairs (admin)" "$status" "$body"
else
    print_result "Get all repairs - should fail" "$status" "$body"
fi

# NEW: Get scheduled repairs
result=$(api_call "GET" "/api/v1/repairs/scheduled" "" "${ADMIN_TOKEN:-$TOKEN}")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
if [ "$ADMIN_ENABLED" -eq 1 ]; then
    print_result "Get scheduled repairs (admin)" "$status" "$body"
else
    print_result "Get scheduled repairs - should fail" "$status" "$body"
fi

# NEW: Get repair by ID
if [ -n "$REPAIR_ID" ]; then
    result=$(api_call "GET" "/api/v1/repairs/$REPAIR_ID" "" "${ADMIN_TOKEN:-$TOKEN}")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    if [ "$ADMIN_ENABLED" -eq 1 ]; then
        print_result "Get repair by ID (admin)" "$status" "$body"
    else
        print_result "Get repair by ID - should fail" "$status" "$body"
    fi
else
    print_skip "Get repair by ID" "no repair created"
fi

# Complete repair
if [ -n "$REPAIR_ID" ]; then
    result=$(api_call "PUT" "/api/v1/repairs/$REPAIR_ID/complete" "" "${ADMIN_TOKEN:-$TOKEN}")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    if [ "$ADMIN_ENABLED" -eq 1 ]; then
        print_result "Complete repair (admin)" "$status" "$body"
    else
        print_result "Complete repair - should fail" "$status" "$body"
    fi
else
    print_skip "Complete repair" "no repair created"
fi

echo ""

# ============================================
# 8. ADMIN REQUESTS TESTS (NEW)
# ============================================
echo -e "${YELLOW}--- Admin Requests Tests ---${NC}"

# Get current role
if [ "$ADMIN_ENABLED" -eq 1 ]; then
    result=$(api_call "GET" "/api/v1/admin-requests/role" "" "$ADMIN_TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Get current role (admin)" "$status" "$body"

    # Create admin request (may already exist -> 409 is OK)
    result=$(api_call "POST" "/api/v1/admin-requests/create" "{\"username\":\"$TEST_USER\",\"description\":\"Request admin role $TIMESTAMP\"}" "$ADMIN_TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    if [ "$status" -eq 409 ]; then
        print_result "Create admin request (expected 409)" "$status" "$body"
    else
        print_result "Create admin request (admin)" "$status" "$body"
    fi

    result=$(api_call "GET" "/api/v1/admin-requests/pending" "" "$ADMIN_TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Get pending admin requests (admin)" "$status" "$body"

    # May be 200 or 404 depending on whether request exists
    if [ -n "$USER_ID" ]; then
        result=$(api_call "GET" "/api/v1/admin-requests/pending/user/$USER_ID" "" "$ADMIN_TOKEN")
        status=$(echo "$result" | cut -d'|' -f1)
        body=$(echo "$result" | cut -d'|' -f2-)
        if [ "$status" -eq 404 ]; then
            print_result "Get pending admin request by user (expected 404)" "$status" "$body"
        else
            print_result "Get pending admin request by user (admin)" "$status" "$body"
        fi
    else
        print_skip "Get pending admin request by user (admin)" "no USER_ID"
    fi

    # Approve/reject with fake id (should fail: usually 404)
    FAKE_REQ_ID=999999999
    result=$(api_call "PATCH" "/api/v1/admin-requests/pending/$FAKE_REQ_ID/approve" "" "$ADMIN_TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Approve admin request (fake id) - should fail" "$status" "$body"

    result=$(api_call "PATCH" "/api/v1/admin-requests/pending/$FAKE_REQ_ID/reject" "" "$ADMIN_TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Reject admin request (fake id) - should fail" "$status" "$body"
else
    result=$(api_call "GET" "/api/v1/admin-requests/role" "" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Get current role - should fail" "$status" "$body"

    result=$(api_call "POST" "/api/v1/admin-requests/create" "{\"username\":\"$TEST_USER\",\"description\":\"Request admin role $TIMESTAMP\"}" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Create admin request - should fail" "$status" "$body"

    result=$(api_call "GET" "/api/v1/admin-requests/pending" "" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Get pending admin requests - should fail" "$status" "$body"

    FAKE_REQ_ID=999999999
    result=$(api_call "PATCH" "/api/v1/admin-requests/pending/$FAKE_REQ_ID/approve" "" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Approve admin request (fake id) - should fail" "$status" "$body"

    result=$(api_call "PATCH" "/api/v1/admin-requests/pending/$FAKE_REQ_ID/reject" "" "$TOKEN")
    status=$(echo "$result" | cut -d'|' -f1)
    body=$(echo "$result" | cut -d'|' -f2-)
    print_result "Reject admin request (fake id) - should fail" "$status" "$body"
fi

echo ""

# ============================================
# 9. OPTIONAL DELETE TESTS (NEW, DANGEROUS)
# ============================================
echo -e "${YELLOW}--- Delete Tests (optional) ---${NC}"
if [ "$ENABLE_DELETES" -eq 1 ]; then
    echo -e "${YELLOW}  ENABLE_DELETES=1, running DELETE endpoints${NC}"

    if [ -n "$BICYCLE_ID" ]; then
        result=$(api_call "DELETE" "/api/v1/bicycles/$BICYCLE_ID" "" "${ADMIN_TOKEN:-$TOKEN}")
        status=$(echo "$result" | cut -d'|' -f1)
        body=$(echo "$result" | cut -d'|' -f2-)
        if [ "$ADMIN_ENABLED" -eq 1 ]; then
            print_result "Delete bicycle (admin)" "$status" "$body"
        else
            print_result "Delete bicycle - should fail" "$status" "$body"
        fi
    fi

    if [ -n "$TECHNICIAN_ID" ]; then
        result=$(api_call "DELETE" "/api/v1/technicians/$TECHNICIAN_ID" "" "${ADMIN_TOKEN:-$TOKEN}")
        status=$(echo "$result" | cut -d'|' -f1)
        body=$(echo "$result" | cut -d'|' -f2-)
        if [ "$ADMIN_ENABLED" -eq 1 ]; then
            print_result "Delete technician (admin)" "$status" "$body"
        else
            print_result "Delete technician - should fail" "$status" "$body"
        fi
    fi

    if [ -n "$STATION_ID" ]; then
        result=$(api_call "DELETE" "/api/v1/stations/$STATION_ID" "" "${ADMIN_TOKEN:-$TOKEN}")
        status=$(echo "$result" | cut -d'|' -f1)
        body=$(echo "$result" | cut -d'|' -f2-)
        if [ "$ADMIN_ENABLED" -eq 1 ]; then
            print_result "Delete station (admin)" "$status" "$body"
        else
            print_result "Delete station - should fail" "$status" "$body"
        fi
    fi
else
    echo -e "${BLUE}  Skipped (set ENABLE_DELETES=1 to enable)${NC}"
fi

echo ""

# ============================================
# 10. SWAGGER/OPENAPI TEST
# ============================================
echo -e "${YELLOW}--- OpenAPI Tests ---${NC}"

result=$(api_call "GET" "/api/v1/api-docs" "" "")
status=$(echo "$result" | cut -d'|' -f1)
body=$(echo "$result" | cut -d'|' -f2-)
print_result "Get OpenAPI docs" "$status" "$body"

echo ""

# ============================================
# SUMMARY
# ============================================
echo -e "${BLUE}============================================${NC}"
echo -e "${BLUE}   Test Results${NC}"
echo -e "${BLUE}============================================${NC}"
echo -e "${GREEN}Passed: $PASSED${NC}"
echo -e "${RED}Failed: $FAILED${NC}"
echo -e "${BLUE}Skipped: $SKIPPED${NC}"
TOTAL=$((PASSED + FAILED + SKIPPED))
echo -e "Total:  $TOTAL"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All tests passed! ✓${NC}"
    exit 0
else
    echo -e "${RED}Some tests failed! ✗${NC}"
    exit 1
fi
