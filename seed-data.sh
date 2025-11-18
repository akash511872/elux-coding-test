#!/bin/bash

# Seed Script for Elux Product API
# This script adds sample products to the database for testing

API_URL="http://localhost:8080"

echo "🌱 Seeding sample products..."

# Note: Products are created programmatically via the service layer
# This script demonstrates how to use the API to apply discounts

echo ""
echo "📦 Adding discounts to products..."

# Sweden products - discounts
curl -X PUT "$API_URL/products/laptop-se-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "summer-sale", "percent": 15.0}' \
  -s -o /dev/null -w "Summer Sale discount: %{http_code}\n"

curl -X PUT "$API_URL/products/laptop-se-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "loyalty-bonus", "percent": 5.0}' \
  -s -o /dev/null -w "Loyalty Bonus discount: %{http_code}\n"

# Germany products - discounts
curl -X PUT "$API_URL/products/phone-de-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "clearance", "percent": 20.0}' \
  -s -o /dev/null -w "Clearance discount: %{http_code}\n"

# France products - discounts
curl -X PUT "$API_URL/products/tablet-fr-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "new-customer", "percent": 10.0}' \
  -s -o /dev/null -w "New Customer discount: %{http_code}\n"

echo ""
echo "✅ Sample discounts added!"
echo ""
echo "🔍 Test the API:"
echo "  curl '$API_URL/products?country=Sweden'"
echo "  curl '$API_URL/products?country=Germany'"
echo "  curl '$API_URL/products?country=France'"
