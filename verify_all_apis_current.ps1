$ErrorActionPreference = 'Stop'

$base = 'http://localhost:8080'
$ts = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$email = "full.$ts@example.com"
$phone = '9' + ($ts.ToString().Substring(1,9))
$customerId = $null
$addressId = $null
$productId = '38e45a96-1f70-11f1-9651-ed7fb304f8d2'
$variantId = '38fdce2b-1f70-11f1-9651-ed7fb304f8d2'

function Wrap($token, $data) {
  @{ token = $token; requestId = ('req-' + [guid]::NewGuid()); data = $data } | ConvertTo-Json -Depth 12
}

function CallApi($name, $path, $token, $data) {
  try {
    $r = Invoke-RestMethod -Method Post -Uri ($base + $path) -ContentType 'application/json' -Body (Wrap $token $data)
    [pscustomobject]@{
      Api = $name
      Path = $path
      Http = '200'
      Status = $r.status
      Message = $r.message
      Data = $r.data
    }
  }
  catch {
    $body = ''
    if ($_.Exception.Response) {
      $sr = New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
      $body = $sr.ReadToEnd()
    }
    [pscustomobject]@{
      Api = $name
      Path = $path
      Http = 'ERR'
      Status = 'FAILED'
      Message = ($_.Exception.Message + ' ' + $body).Trim()
      Data = $null
    }
  }
}

$rows = @()

# Auth flow
$register = CallApi 'AuthRegister' '/api/v1/auth/register' '' @{ name='Full Check User'; email=$email; password='StrongPass@123'; phone=$phone }
$rows += $register
$token = $register.Data.token
$customerId = $register.Data.user.id

$login = CallApi 'AuthLogin' '/api/v1/auth/login' '' @{ email=$email; password='StrongPass@123' }
$rows += $login
if ($login.Data -and $login.Data.token) { $token = $login.Data.token }
$forgot = CallApi 'AuthForgotPassword' '/api/v1/auth/forgot-password' '' @{ email=$email }
$rows += $forgot
$resetToken = $forgot.Data.resetToken
if ($resetToken) {
  $rows += CallApi 'AuthResetPassword' '/api/v1/auth/reset-password' '' @{ email=$email; resetToken=$resetToken; newPassword='StrongPass@456' }
} else {
  $rows += [pscustomobject]@{ Api='AuthResetPassword'; Path='/api/v1/auth/reset-password'; Http='ERR'; Status='FAILED'; Message='resetToken not returned by forgot-password'; Data=$null }
}

# Public product APIs
$rows += CallApi 'ProductsList' '/api/v1/products/list' '' @{}
$rows += CallApi 'ProductsDetail' '/api/v1/products/detail' '' @{ productId=$productId }
$rows += CallApi 'ProductsSearch' '/api/v1/products/search' '' @{ keyword='shirt' }

# Wishlist
$rows += CallApi 'WishlistList' '/api/v1/wishlist/list' $token @{ customerId=$customerId }
$rows += CallApi 'WishlistToggle' '/api/v1/wishlist/toggle' $token @{ customerId=$customerId; productId=$productId }

# Cart APIs (public/guest-capable, using customer actor here)
$cartAdd = CallApi 'CartAdd' '/api/v1/cart/add' '' @{ customerId=$customerId; productId=$productId; variantId=$variantId; quantity=1 }
$rows += $cartAdd
$cartGet = CallApi 'CartGet' '/api/v1/cart/get' '' @{ customerId=$customerId }
$rows += $cartGet

$cartItemId = $null
if ($cartGet.Data -is [System.Array] -and $cartGet.Data.Count -gt 0) {
  $cartItemId = $cartGet.Data[0].cartItemId
} elseif ($cartGet.Data -and $cartGet.Data.items -and $cartGet.Data.items.Count -gt 0) {
  $cartItemId = $cartGet.Data.items[0].cartItemId
}

if ($cartItemId) {
  $rows += CallApi 'CartUpdate' '/api/v1/cart/update' '' @{ cartItemId=$cartItemId; quantity=2; customerId=$customerId }
  $rows += CallApi 'CartRemove' '/api/v1/cart/remove' '' @{ cartItemId=$cartItemId; customerId=$customerId }
} else {
  $rows += [pscustomobject]@{ Api='CartUpdate'; Path='/api/v1/cart/update'; Http='ERR'; Status='FAILED'; Message='cartItemId not found from CartGet'; Data=$null }
  $rows += [pscustomobject]@{ Api='CartRemove'; Path='/api/v1/cart/remove'; Http='ERR'; Status='FAILED'; Message='cartItemId not found from CartGet'; Data=$null }
}

# Address / coupon / user
$address = CallApi 'AddressAdd' '/api/v1/address/add' $token @{ customerId=$customerId; type='HOME'; addressLine='12 Test Street'; city='Hyderabad'; state='Telangana'; pincode='500001'; isDefault=$false }
$rows += $address
if ($address.Data -and $address.Data.addressId) { $addressId = $address.Data.addressId }
$rows += CallApi 'CouponValidate' '/api/v1/coupons/validate' $token @{ customerId=$customerId; couponCode='SAVE10'; orderAmount=1500.00 }
$rows += CallApi 'UserProfile' '/api/v1/user/profile' $token @{ customerId=$customerId }
$rows += CallApi 'UserUpdate' '/api/v1/user/update' $token @{ customerId=$customerId; name='Ravi Kumar'; email=('ravi.' + $ts + '@example.com'); mobile=('8' + ($ts.ToString().Substring(1,9))) }

# Orders
$order = $null
if ($addressId) {
  $order = CallApi 'OrdersCreate' '/api/v1/orders/create' $token @{ customerId=$customerId; addressId=$addressId; couponCode='SAVE10'; paymentMethod='UPI'; items=@(@{ variantId=$variantId; quantity=1; price=999.00 }) }
  $rows += $order
} else {
  $rows += [pscustomobject]@{ Api='OrdersCreate'; Path='/api/v1/orders/create'; Http='ERR'; Status='FAILED'; Message='addressId not returned from AddressAdd'; Data=$null }
}
$orderId = $order.Data.orderId

$rows += CallApi 'OrdersList' '/api/v1/orders/list' $token @{ customerId=$customerId }
if ($orderId) {
  $rows += CallApi 'OrdersDetail' '/api/v1/orders/detail' $token @{ orderId=$orderId }
  $rows += CallApi 'OrdersInstantBuy' '/api/v1/orders/instant-buy' $token @{ customerId=$customerId; addressId=$addressId; variantId=$variantId; quantity=1; price=999.00; couponCode='SAVE10'; paymentMethod='UPI' }
} else {
  $rows += [pscustomobject]@{ Api='OrdersDetail'; Path='/api/v1/orders/detail'; Http='ERR'; Status='FAILED'; Message='orderId not returned from OrdersCreate'; Data=$null }
  $rows += [pscustomobject]@{ Api='OrdersInstantBuy'; Path='/api/v1/orders/instant-buy'; Http='ERR'; Status='FAILED'; Message='orderId context missing'; Data=$null }
}

# Invoice
$invoice = $null
if ($orderId) {
  $invoice = CallApi 'InvoiceGenerate' '/api/v1/invoices/generate' $token @{ orderId=$orderId; customerId=$customerId; billingEmail=$email }
  $rows += $invoice
} else {
  $rows += [pscustomobject]@{ Api='InvoiceGenerate'; Path='/api/v1/invoices/generate'; Http='ERR'; Status='FAILED'; Message='orderId not available from OrdersCreate'; Data=$null }
}

$invoiceId = $invoice.Data.invoiceId
if ($invoiceId) {
  $rows += CallApi 'InvoiceDetail' '/api/v1/invoices/detail' $token @{ invoiceId=$invoiceId }
  $rows += CallApi 'InvoiceEmail' '/api/v1/invoices/email' $token @{ invoiceId=$invoiceId; recipientEmail=$email }
} else {
  $rows += [pscustomobject]@{ Api='InvoiceDetail'; Path='/api/v1/invoices/detail'; Http='ERR'; Status='FAILED'; Message='invoiceId not returned from InvoiceGenerate'; Data=$null }
  $rows += [pscustomobject]@{ Api='InvoiceEmail'; Path='/api/v1/invoices/email'; Http='ERR'; Status='FAILED'; Message='invoiceId not returned from InvoiceGenerate'; Data=$null }
}

if ($orderId) {
  $rows += CallApi 'InvoiceByOrder' '/api/v1/invoices/by-order' $token @{ orderId=$orderId }
} else {
  $rows += [pscustomobject]@{ Api='InvoiceByOrder'; Path='/api/v1/invoices/by-order'; Http='ERR'; Status='FAILED'; Message='orderId not available from OrdersCreate'; Data=$null }
}

# Notifications
$rows += CallApi 'EmailSend' '/api/v1/notifications/email/send' $token @{ customerId=$customerId; toEmail=$email; subject='Hello'; messageType='ORDER_UPDATE'; body='Test mail body' }
$rows += CallApi 'EmailHistory' '/api/v1/notifications/email/history' $token @{ customerId=$customerId }

# Delivery
$delivery = $null
if ($orderId) {
  $delivery = CallApi 'DeliveryCreate' '/api/v1/delivery/create' $token @{ customerId=$customerId; orderId=$orderId; courierName='BlueDart'; trackingNumber=('TRK-' + $ts); estimatedDeliveryDate='2026-03-25' }
  $rows += $delivery
} else {
  $rows += [pscustomobject]@{ Api='DeliveryCreate'; Path='/api/v1/delivery/create'; Http='ERR'; Status='FAILED'; Message='orderId not available from OrdersCreate'; Data=$null }
}

$deliveryId = $delivery.Data.deliveryId
if ($deliveryId) {
  $rows += CallApi 'DeliveryDetail' '/api/v1/delivery/detail' $token @{ customerId=$customerId; deliveryId=$deliveryId }
  $rows += CallApi 'DeliveryUpdateStatus' '/api/v1/delivery/update-status' $token @{ customerId=$customerId; deliveryId=$deliveryId; status='IN_TRANSIT'; currentLocation='Hyderabad'; remarks='Dispatched' }
} else {
  $rows += [pscustomobject]@{ Api='DeliveryDetail'; Path='/api/v1/delivery/detail'; Http='ERR'; Status='FAILED'; Message='deliveryId not returned from DeliveryCreate'; Data=$null }
  $rows += [pscustomobject]@{ Api='DeliveryUpdateStatus'; Path='/api/v1/delivery/update-status'; Http='ERR'; Status='FAILED'; Message='deliveryId not returned from DeliveryCreate'; Data=$null }
}

# Returns and refunds
$ret = $null
if ($orderId) {
  $ret = CallApi 'ReturnsCreate' '/api/v1/returns/create' $token @{ orderId=$orderId; customerId=$customerId; reason='SIZE_ISSUE'; comments='Need smaller size' }
  $rows += $ret
} else {
  $rows += [pscustomobject]@{ Api='ReturnsCreate'; Path='/api/v1/returns/create'; Http='ERR'; Status='FAILED'; Message='orderId not available from OrdersCreate'; Data=$null }
}

$returnId = $ret.Data.returnId
if ($returnId) {
  $rows += CallApi 'ReturnsDetail' '/api/v1/returns/detail' $token @{ customerId=$customerId; returnId=$returnId }
} else {
  $rows += [pscustomobject]@{ Api='ReturnsDetail'; Path='/api/v1/returns/detail'; Http='ERR'; Status='FAILED'; Message='returnId not returned from ReturnsCreate'; Data=$null }
}
$rows += CallApi 'ReturnsList' '/api/v1/returns/list' $token @{ customerId=$customerId }

$refund = $null
if ($returnId) {
  $refund = CallApi 'RefundsCreate' '/api/v1/refunds/create' $token @{ customerId=$customerId; returnId=$returnId; paymentId='PAY-SAMPLE-001'; amount=999.00; reason='RETURN_APPROVED' }
  $rows += $refund
} else {
  $rows += [pscustomobject]@{ Api='RefundsCreate'; Path='/api/v1/refunds/create'; Http='ERR'; Status='FAILED'; Message='returnId not available from ReturnsCreate'; Data=$null }
}

$refundId = $refund.Data.refundId
if ($refundId) {
  $rows += CallApi 'RefundsDetail' '/api/v1/refunds/detail' $token @{ customerId=$customerId; refundId=$refundId }
} else {
  $rows += [pscustomobject]@{ Api='RefundsDetail'; Path='/api/v1/refunds/detail'; Http='ERR'; Status='FAILED'; Message='refundId not returned from RefundsCreate'; Data=$null }
}
$rows += CallApi 'RefundsList' '/api/v1/refunds/list' $token @{ customerId=$customerId }

$rows | ConvertTo-Json -Depth 10 | Set-Content -Path .\api_test_results_full.json
$rows | Select-Object Api, Path, Http, Status, Message | Tee-Object -FilePath .\api_test_results_full.txt | Format-Table -AutoSize

$failed = @($rows | Where-Object { $_.Http -ne '200' -or $_.Status -ne 'SUCCESS' })
if ($failed.Count -gt 0) {
  $failed | ConvertTo-Json -Depth 10 | Set-Content -Path .\api_test_failures_full.json
  Write-Host "`nFAILED endpoint count: $($failed.Count)"
  exit 1
}

Write-Host "`nAll APIs passed."
exit 0
