# Elux Product API

A Kotlin/Ktor-based REST API service for managing country-specific products with VAT calculations and concurrency-safe discount management.

## Features

- **Country-based Product Listings**: Query products by country with automatic VAT calculations
- **VAT Support**: Sweden (25%), Germany (19%), France (20%)
- **Discount Management**: Apply discounts to products with idempotency and concurrency safety
- **PostgreSQL Database**: Persistent storage with database-level constraints
- **Concurrency Safe**: Database unique constraints prevent duplicate discount applications
- **Docker Support**: Complete docker-compose setup for easy deployment

## Tech Stack

- **Language**: Kotlin 1.9.20
- **Framework**: Ktor 2.3.6
- **Database**: PostgreSQL 15 (Production), H2 (Testing)
- **ORM**: Exposed
- **Build Tool**: Gradle 8.4
- **Testing**: Kotlin Test, Ktor Test

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Java 17+ (if running locally without Docker)

### Running with Docker Compose

1. Clone the repository:
```bash
git clone <repository-url>
cd elux-coding-test
```

2. Start the application and database:
```bash
docker-compose up -d
```

3. The API will be available at `http://localhost:8080`

4. Check health:
```bash
curl http://localhost:8080/health
```

### Running Locally

1. Start PostgreSQL:
```bash
docker-compose up -d postgres
```

2. Build the project:
```bash
./gradlew build
```

3. Run the application:
```bash
./gradlew run
```

Or run the JAR:
```bash
java -jar build/libs/elux-product-api-all-1.0.0.jar
```

## API Endpoints

### GET /products?country={country}

List all products in a specific country with calculated final prices.

**Query Parameters:**
- `country` (required): Country name (Sweden, Germany, or France)

**Response:**
```json
[
  {
    "id": "p1",
    "name": "Laptop",
    "basePrice": 1000.0,
    "country": "Sweden",
    "discounts": [
      {
        "discountId": "summer-sale",
        "percent": 10.0
      }
    ],
    "finalPrice": 1125.0
  }
]
```

**Formula:** `finalPrice = basePrice × (1 - totalDiscount%) × (1 + VAT%)`

**Example:**
```bash
curl "http://localhost:8080/products?country=Sweden"
```

### PUT /products/{id}/discount

Apply a discount to a product. This endpoint is idempotent - applying the same discount multiple times will only create it once.

**Path Parameters:**
- `id`: Product ID

**Request Body:**
```json
{
  "discountId": "summer-sale",
  "percent": 10.0
}
```

**Response:**
```json
{
  "id": "p1",
  "name": "Laptop",
  "basePrice": 1000.0,
  "country": "Sweden",
  "discounts": [
    {
      "discountId": "summer-sale",
      "percent": 10.0
    }
  ],
  "finalPrice": 1125.0
}
```

**Example:**
```bash
curl -X PUT "http://localhost:8080/products/p1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "summer-sale", "percent": 10.0}'
```

### GET /health

Health check endpoint.

**Example:**
```bash
curl http://localhost:8080/health
```

## Testing

Run all tests:
```bash
./gradlew test
```

Run specific test:
```bash
./gradlew test --tests "ApplicationTest.testConcurrentDiscountApplication"
```

View test report:
```bash
open build/reports/tests/test/index.html
```

### Test Coverage

- ✅ Product listing by country
- ✅ Discount application
- ✅ Idempotency (same discount applied multiple times)
- ✅ Concurrency safety (10 concurrent discount applications)
- ✅ VAT calculation
- ✅ Error handling (missing parameters, invalid values)

## Database Schema

### Products Table
| Column    | Type   | Description          |
|-----------|--------|----------------------|
| id        | VARCHAR(100) | Primary key    |
| name      | VARCHAR(255) | Product name   |
| base_price| DOUBLE | Price before VAT/discount |
| country   | VARCHAR(50)  | Country       |

### Discounts Table
| Column     | Type   | Description          |
|------------|--------|----------------------|
| id         | INTEGER | Auto-increment primary key |
| product_id | VARCHAR(100) | Foreign key to products |
| discount_id| VARCHAR(100) | Unique discount identifier |
| percent    | DOUBLE | Discount percentage  |

**Unique Constraint:** `(product_id, discount_id)` ensures a discount can only be applied once to a product.

## Configuration

Configuration is managed through `src/main/resources/application.conf`:

```hocon
database {
    jdbcUrl = "jdbc:postgresql://localhost:5432/elux_products"
    driver = "org.postgresql.Driver"
    user = "elux_user"
    password = "elux_pass"
    maxPoolSize = 10
}
```

Environment variables override configuration:
- `DATABASE_URL`: Database JDBC URL
- `DATABASE_USER`: Database username
- `DATABASE_PASSWORD`: Database password
- `PORT`: Server port (default: 8080)

## Project Structure

```
elux-coding-test/
├── build.gradle.kts          # Build configuration
├── docker-compose.yml         # Docker orchestration
├── Dockerfile                 # Application container
├── src/
│   ├── main/
│   │   ├── kotlin/com/elux/
│   │   │   ├── Application.kt       # Main entry point
│   │   │   ├── model/
│   │   │   │   └── Models.kt        # Data classes
│   │   │   ├── database/
│   │   │   │   ├── DatabaseFactory.kt
│   │   │   │   └── Tables.kt        # Database schema
│   │   │   ├── service/
│   │   │   │   └── ProductService.kt # Business logic
│   │   │   ├── routes/
│   │   │   │   └── ProductRoutes.kt  # API endpoints
│   │   │   └── util/
│   │   │       └── VATCalculator.kt  # VAT calculations
│   │   └── resources/
│   │       ├── application.conf     # Configuration
│   │       └── logback.xml          # Logging config
│   └── test/
│       └── kotlin/com/elux/
│           └── ApplicationTest.kt   # Tests
└── ARCHITECTURE.md           # Architecture documentation
```

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed architecture documentation, including:
- System design decisions
- Concurrency handling
- Database constraints
- Flow diagrams

## Development

### Building

```bash
./gradlew build
```

### Running Tests

```bash
./gradlew test
```

### Creating Fat JAR

```bash
./gradlew buildFatJar
```

The JAR will be created at `build/libs/elux-product-api-all-1.0.0.jar`

## License

This project is created for the Elux coding test.

