# BuyGlimmer Backend

Spring Boot 3 backend scaffold for the BuyGlimmer storefront.

## Fintech Wrapper Contract

### Request

```json
{
	"token": "string",
	"requestId": "string",
	"data": {}
}
```

### Response

```json
{
	"requestId": "string",
	"status": "SUCCESS/FAILED",
	"message": "string",
	"data": {}
}
```

## Stored Procedure APIs (POST only)

### Product

- `POST /api/v1/products/list` -> `sp_get_products`
- `POST /api/v1/products/detail` -> `sp_get_product`
- `POST /api/v1/products/search` -> `sp_search_products`

### Cart

- `POST /api/v1/cart/add` -> `sp_add_to_cart`
- `POST /api/v1/cart/get` -> `sp_get_cart`
- `POST /api/v1/cart/update` -> `sp_update_cart_item`
- `POST /api/v1/cart/remove` -> `sp_remove_cart_item`

### Orders

- `POST /api/v1/orders/create` -> `sp_create_order`, then `sp_add_order_items`
- `POST /api/v1/orders/list` -> `sp_get_orders`
- `POST /api/v1/orders/detail` -> `sp_get_order_detail`

### User

- `POST /api/v1/user/profile` -> `sp_get_profile`
- `POST /api/v1/user/update` -> `sp_update_profile`

### Address

- `POST /api/v1/address/add` -> `sp_add_address`

### Coupon

- `POST /api/v1/coupons/validate` -> `sp_validate_coupon`

## Project structure

- `controller`: REST entry points (wrapper based)
- `service`: business orchestration layer
- `repository`: JDBC CallableStatement + stored procedures only
- `dto`: request/response contracts
- `util`: response factory and DB call utility
- `exception`: centralized wrapper error handling
- `config`: OpenAPI and H2 stored procedure aliases

## Run locally

```bash
mvn spring-boot:run
```

Swagger UI will be available at `/swagger-ui.html`.

## Pre-release checklist

Run the pre-release gate script to verify critical schema-compatibility endpoints before smoke tests:

```powershell
powershell -ExecutionPolicy Bypass -File .\pre_release_check.ps1
```

To run only compatibility probes (skip smoke):

```powershell
powershell -ExecutionPolicy Bypass -File .\pre_release_check.ps1 -SkipSmoke
```

## Notes

The fintech API layer is implemented with strict `POST` endpoints, request/response wrappers, and JDBC `CallableStatement` stored procedure calls only.