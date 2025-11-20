# cURL Examples for Elux Product API

## Base URL
```bash
export API_URL="http://localhost:8080"
```

## Health Check

Check if the API is running:
```bash
curl "$API_URL/health"
```

Expected response:
```json
{
  "status": "UP"
}
```

## List Products by Country

### Sweden (25% VAT)
```bash
curl "$API_URL/products?country=Sweden"
```

### Germany (19% VAT)
```bash
curl "$API_URL/products?country=Germany"
```

### France (20% VAT)
```bash
curl "$API_URL/products?country=France"
```

Example response:
```json
[
  {
    "id": "laptop-se-1",
    "name": "Premium Laptop",
    "basePrice": 15000.0,
    "country": "Sweden",
    "discounts": [],
    "finalPrice": 18750.0
  }
]
```

## Apply Discounts

### Apply a single discount
```bash
curl -X PUT "$API_URL/products/laptop-se-1/discount" \
  -H "Content-Type: application/json" \
  -d '{
    "discountId": "summer-sale",
    "percent": 10.0
  }'
```

Expected response:
```json
{
  "id": "laptop-se-1",
  "name": "Premium Laptop",
  "basePrice": 15000.0,
  "country": "Sweden",
  "discounts": [
    {
      "discountId": "summer-sale",
      "percent": 10.0
    }
  ],
  "finalPrice": 16875.0
}
```

**Calculation:**
- Base: 15000
- After 10% discount: 15000 × 0.90 = 13500
- With 25% VAT: 13500 × 1.25 = 16875

### Apply multiple discounts
```bash
# First discount
curl -X PUT "$API_URL/products/laptop-se-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "summer-sale", "percent": 10.0}'

# Second discount
curl -X PUT "$API_URL/products/laptop-se-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "loyalty-bonus", "percent": 5.0}'
```

After both discounts:
```json
{
  "finalPrice": 15937.5,
  "discounts": [
    {"discountId": "summer-sale", "percent": 10.0},
    {"discountId": "loyalty-bonus", "percent": 5.0}
  ]
}
```

**Calculation:**
- Base: 15000
- After 15% total discount: 15000 × 0.85 = 12750
- With 25% VAT: 12750 × 1.25 = 15937.5

### Test idempotency (applying same discount twice)
```bash
# First application
curl -X PUT "$API_URL/products/phone-de-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "black-friday", "percent": 20.0}'

# Second application (should return same result)
curl -X PUT "$API_URL/products/phone-de-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "black-friday", "percent": 20.0}'
```

Both requests return the same product state (idempotent).

## Test Concurrent Requests

Test concurrency safety by sending multiple requests simultaneously:

```bash
# Using GNU Parallel (install with: apt-get install parallel)
seq 1 10 | parallel -j 10 "curl -X PUT '$API_URL/products/tablet-fr-1/discount' \
  -H 'Content-Type: application/json' \
  -d '{\"discountId\": \"flash-sale\", \"percent\": 15.0}' \
  -s"
```

Or using a simple bash loop:
```bash
for i in {1..10}; do
  curl -X PUT "$API_URL/products/tablet-fr-1/discount" \
    -H "Content-Type: application/json" \
    -d '{"discountId": "concurrent-test", "percent": 15.0}' \
    -s -o /dev/null &
done
wait

# Verify discount applied only once
curl "$API_URL/products?country=France" | jq '.[] | select(.id=="tablet-fr-1") | .discounts'
```

## Error Cases

### Missing country parameter
```bash
curl "$API_URL/products"
```

Response (400 Bad Request):
```json
{
  "error": "bad_request",
  "message": "Country parameter is required"
}
```

### Invalid country
```bash
curl "$API_URL/products?country=Spain"
```

Response (400 Bad Request):
```json
{
  "error": "invalid_country",
  "message": "Unsupported country: Spain"
}
```

### Invalid discount percent
```bash
curl -X PUT "$API_URL/products/laptop-se-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "invalid", "percent": 150.0}'
```

Response (400 Bad Request):
```json
{
  "error": "invalid_discount",
  "message": "Discount percent must be between 0 and 100"
}
```

### Product not found
```bash
curl -X PUT "$API_URL/products/nonexistent/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "test", "percent": 10.0}'
```

Response (404 Not Found):
```json
{
  "error": "not_found",
  "message": "Product not found"
}
```

## Pretty Print with jq

Install jq for better JSON formatting:
```bash
# Ubuntu/Debian
sudo apt-get install jq

# macOS
brew install jq
```

Usage:
```bash
curl "$API_URL/products?country=Sweden" | jq '.'

# Filter specific fields
curl "$API_URL/products?country=Sweden" | jq '.[] | {id, name, finalPrice}'

# Get only product with discounts
curl "$API_URL/products?country=Sweden" | jq '.[] | select(.discounts | length > 0)'
```

## Complete Workflow Example

```bash
# 1. Check API health
curl "$API_URL/health"

# 2. List Swedish products
curl "$API_URL/products?country=Sweden" | jq '.'

# 3. Apply first discount to a laptop
curl -X PUT "$API_URL/products/laptop-se-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "early-bird", "percent": 15.0}' | jq '.'

# 4. Apply second discount
curl -X PUT "$API_URL/products/laptop-se-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "newsletter-subscriber", "percent": 5.0}' | jq '.'

# 5. View updated product with both discounts
curl "$API_URL/products?country=Sweden" | jq '.[] | select(.id=="laptop-se-1")'

# 6. Try to apply first discount again (idempotent)
curl -X PUT "$API_URL/products/laptop-se-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "early-bird", "percent": 15.0}' | jq '.'
```

## Performance Testing with Apache Bench

Test API performance:
```bash
# Install Apache Bench
sudo apt-get install apache2-utils

# Test GET endpoint
ab -n 1000 -c 10 "$API_URL/products?country=Sweden"

# Test PUT endpoint (create a file with JSON body)
echo '{"discountId":"perf-test","percent":10.0}' > discount.json
ab -n 100 -c 10 -p discount.json -T "application/json" "$API_URL/products/laptop-se-1/discount"
```

## Monitoring Logs

If running with Docker:
```bash
# View application logs
docker logs -f elux-product-api

# View database logs
docker logs -f elux-postgres
```

## Clean Up Test Data

To reset the database:
```bash
docker-compose down -v
docker-compose up -d
```

This will delete all data and restart with fresh sample products.
