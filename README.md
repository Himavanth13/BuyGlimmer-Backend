# BuyGlimmer Backend

Spring Boot 3 backend scaffold for the BuyGlimmer storefront.

## Covered API flows

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/users/profile`
- `POST /api/v1/users/profile/update`
- `POST /api/v1/categories`
- `POST /api/v1/products`
- `POST /api/v1/products/{id}`
- `POST /api/v1/wishlist`
- `POST /api/v1/wishlist/toggle`
- `POST /api/v1/cart`
- `POST /api/v1/cart/items`
- `POST /api/v1/cart/items/{cartItemId}`
- `POST /api/v1/orders/checkout`
- `POST /api/v1/orders`
- `POST /api/v1/orders/{orderId}`

## Project structure

- `controller`: REST entry points
- `dto`: request and response contracts
- `service`: application layer and in-memory domain logic
- `exception`: centralized API error handling
- root package classes: application bootstrap and typed properties
- `config`: OpenAPI configuration

## Run locally

```bash
mvn spring-boot:run
```

Swagger UI will be available at `/swagger-ui.html`.

## Notes

This scaffold follows the same Spring Boot layout used in the referenced `sabbpegold` backend, but the domain is adapted to the BuyGlimmer frontend module.

The current implementation is wired through stored procedures using Spring JDBC. For local development it uses H2 aliases as procedures so the structure is runnable out of the box and can be ported later to MySQL or PostgreSQL procedures with the same repository boundaries.