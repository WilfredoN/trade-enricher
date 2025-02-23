# Trade Enricher Service

A Spring WebFlux-based service that enriches trade data with product information stored in Redis.

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Redis server running on port 6379

## Running the Application

1. Start Redis server locally:
```bash
redis-server
```

2. Build the application:
```bash
mvn clean package
```

3. Run the application:
```bash
java -jar target/trade-enricher-0.0.1-SNAPSHOT.jar
```

The application will automatically load product data from the configured CSV file (default: largeSizeProduct.csv) into Redis on startup. To change it - modify the application.properties.

## API Documentation

### Process Trade Data

Endpoint: `POST /api/v1/trade`

Supports three content types:
- `application/json`
- `application/xml`
- `text/csv`

#### Example Requests

JSON:
```json
{
  "date": "20240120",
  "productId": "5",
  "currency": "USD",
  "price": 100.50
}
```

XML:
```xml
<trade>
    <date>20240120</date>
    <productId>5</productId>
    <currency>USD</currency>
    <price>100.50</price>
</trade>
```

CSV:
```csv
date,productId,currency,price
20240120,5,USD,100.50
```

## Configuration

Key configurations in `application.properties`:

```properties
spring.data.redis.port=6379 # Redis server port
product.loader.file=largeSizeProduct.csv # Product data file
spring.task.execution.pool.core-size=8 # Thread pool settings
spring.task.execution.pool.max-size=16 # Thread pool settings
spring.task.execution.pool.queue-capacity=10000 # Thread pool settings
```

## Future Improvements

1. **Performance Optimizations**
   - Implement batch processing for trade enrichment
   - Add Redis connection pooling
   - Fine-tune thread pool settings

2. **Resilience**
   - Add circuit breakers for Redis operations
   - Implement retry mechanisms
   - Add health check endpoints

3. **Monitoring & Observability**
   - Add Prometheus metrics
   - Implement distributed tracing
   - Enhanced logging and monitoring

4. **Security**
   - Add authentication and authorization
   - Input validation and sanitization
   - Rate limiting

5. **Testing**
   - Add more unit tests
   - Integration tests with TestContainers
   - Performance tests

6. **Documentation**
   - OpenAPI/Swagger documentation
   - More detailed API documentation
   - Architecture diagrams

7. **Features**
   - Support for more input/output formats
   - Bulk operation endpoints
   - Real-time trade processing via WebSocket
   - Product data management API
