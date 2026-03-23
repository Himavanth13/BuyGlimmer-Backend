$ErrorActionPreference='Stop'

$base='http://localhost:8080'
$ts=[DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$email="full.$ts@example.com"
$phone="9" + ($ts.ToString().Substring(1,9))
$customerId='39225d99-1f70-11f1-9651-ed7fb304f8d2'
$addressId='3929205e-1f70-11f1-9651-ed7fb304f8d2'
$productId='38e45a96-1f70-11f1-9651-ed7fb304f8d2'
$variantId='38fdce2b-1f70-11f1-9651-ed7fb304f8d2'

function Wrap($token,$data){
  @{token=$token;requestId=('req-'+[guid]::NewGuid());data=$data} | ConvertTo-Json -Depth 12
}

function CallApi($name,$path,$token,$data){
  try {
    $r=Invoke-RestMethod -Method Post -Uri ($base+$path) -ContentType 'application/json' -Body (Wrap $token $data)
    [pscustomobject]@{Api=$name;Http='200';Status=$r.status;Message=$r.message;Data=$r.data}
  } catch {
    $body=''
    if($_.Exception.Response){
      $sr=New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
      $body=$sr.ReadToEnd()
    }
    [pscustomobject]@{Api=$name;Http='ERR';Status='FAILED';Message=($_.Exception.Message + ' ' + $body);Data=$null}
  }
}

$rows=@()

$reg=CallApi 'AuthRegister' '/api/v1/auth/register' '' @{name='Full Check User';email=$email;password='StrongPass@123';phone=$phone}
$rows+=$reg
$token=$reg.Data.token

# 1) Auth
$rows+=CallApi 'AuthLogin' '/api/v1/auth/login' '' @{email=$email;password='StrongPass@123'}

# 2) Product catalog
$rows+=CallApi 'ProductsList' '/api/v1/products/list' $token @{}
$rows+=CallApi 'ProductsDetail' '/api/v1/products/detail' $token @{productId=$productId}
$rows+=CallApi 'ProductsSearch' '/api/v1/products/search' $token @{keyword='shirt'}

# 3) Cart flow
$rows+=CallApi 'CartAdd' '/api/v1/cart/add' $token @{customerId=$customerId;productId=$productId;variantId=$variantId;quantity=1}
$cartGet=CallApi 'CartGet' '/api/v1/cart/get' $token @{customerId=$customerId}
$rows+=$cartGet

$cartItemId=$null
if($cartGet.Data -is [System.Array] -and $cartGet.Data.Count -gt 0){
  $cartItemId=$cartGet.Data[0].cartItemId
} elseif($cartGet.Data -and $cartGet.Data.items -and $cartGet.Data.items.Count -gt 0){
  $cartItemId=$cartGet.Data.items[0].cartItemId
}

if($cartItemId){
  $rows+=CallApi 'CartUpdate' '/api/v1/cart/update' $token @{cartItemId=$cartItemId;quantity=2}
  $rows+=CallApi 'CartRemove' '/api/v1/cart/remove' $token @{cartItemId=$cartItemId}
} else {
  $rows+=[pscustomobject]@{Api='CartUpdate';Http='ERR';Status='FAILED';Message='cartItemId not found from CartGet';Data=$null}
  $rows+=[pscustomobject]@{Api='CartRemove';Http='ERR';Status='FAILED';Message='cartItemId not found from CartGet';Data=$null}
}

# 4) Address, coupon, user
$rows+=CallApi 'AddressAdd' '/api/v1/address/add' $token @{customerId=$customerId;type='HOME';addressLine='12 Test Street';city='Hyderabad';state='Telangana';pincode='500001';isDefault=$false}
$rows+=CallApi 'CouponValidate' '/api/v1/coupons/validate' $token @{customerId=$customerId;couponCode='SAVE10';orderAmount=1500.00}
$rows+=CallApi 'UserProfile' '/api/v1/user/profile' $token @{customerId=$customerId}
$rows+=CallApi 'UserUpdate' '/api/v1/user/update' $token @{customerId=$customerId;name='Ravi Kumar';email=('ravi.'+$ts+'@example.com');mobile='9876543222'}

# 5) Orders
$order=CallApi 'OrdersCreate' '/api/v1/orders/create' $token @{customerId=$customerId;addressId=$addressId;couponCode='SAVE10';paymentMethod='UPI';items=@(@{variantId=$variantId;quantity=1;price=999.00})}
$rows+=$order
$orderId=$order.Data.orderId

$rows+=CallApi 'OrdersList' '/api/v1/orders/list' $token @{customerId=$customerId}
if($orderId){
  $rows+=CallApi 'OrdersDetail' '/api/v1/orders/detail' $token @{orderId=$orderId}
} else {
  $rows+=[pscustomobject]@{Api='OrdersDetail';Http='ERR';Status='FAILED';Message='orderId not available from OrdersCreate';Data=$null}
}

$rows+=CallApi 'OrdersInstantBuy' '/api/v1/orders/instant-buy' $token @{customerId=$customerId;addressId=$addressId;variantId=$variantId;quantity=1;price=999.00;couponCode='SAVE10';paymentMethod='UPI'}

# 6) Payments
$payment=CallApi 'PaymentsCreate' '/api/v1/payments/create' $token @{orderId=$orderId;method='UPI';gatewayTxnId=('TXN-'+$ts);amount=999.00}
$rows+=$payment
$paymentId=$payment.Data.paymentId
$gatewayTxnId=$payment.Data.gatewayTxnId
if(-not $gatewayTxnId){ $gatewayTxnId=('TXN-'+$ts) }

$rows+=CallApi 'PaymentsVerify' '/api/v1/payments/verify' $token @{paymentId=$paymentId;gatewayTxnId=$gatewayTxnId;status='SUCCESS'}

# 7) Delivery
$delivery=CallApi 'DeliveryCreate' '/api/v1/delivery/create' $token @{orderId=$orderId;courierName='BlueDart';trackingNumber=('TRK-'+$ts);estimatedDeliveryDate='2026-03-25'}
$rows+=$delivery
$deliveryId=$delivery.Data.deliveryId

$rows+=CallApi 'DeliveryDetail' '/api/v1/delivery/detail' $token @{deliveryId=$deliveryId}
$rows+=CallApi 'DeliveryUpdateStatus' '/api/v1/delivery/update-status' $token @{deliveryId=$deliveryId;status='IN_TRANSIT';currentLocation='Hyderabad';remarks='Dispatched'}

$invoice=CallApi 'InvoiceGenerate' '/api/v1/invoices/generate' $token @{orderId=$orderId;customerId=$customerId;billingEmail=$email}
$rows+=$invoice
$invoiceId=$invoice.Data.invoiceId

$rows+=CallApi 'InvoiceDetail' '/api/v1/invoices/detail' $token @{invoiceId=$invoiceId}
$rows+=CallApi 'InvoiceByOrder' '/api/v1/invoices/by-order' $token @{orderId=$orderId}
$rows+=CallApi 'InvoiceEmail' '/api/v1/invoices/email' $token @{invoiceId=$invoiceId;recipientEmail=$email}

$rows+=CallApi 'EmailSend' '/api/v1/notifications/email/send' $token @{customerId=$customerId;toEmail=$email;subject='Hello';messageType='ORDER_UPDATE';body='Test mail body'}
$rows+=CallApi 'EmailHistory' '/api/v1/notifications/email/history' $token @{customerId=$customerId}

$ret=CallApi 'ReturnsCreate' '/api/v1/returns/create' $token @{orderId=$orderId;customerId=$customerId;reason='SIZE_ISSUE';comments='Need smaller size'}
$rows+=$ret
$returnId=$ret.Data.returnId

$rows+=CallApi 'ReturnsDetail' '/api/v1/returns/detail' $token @{returnId=$returnId}
$rows+=CallApi 'ReturnsList' '/api/v1/returns/list' $token @{customerId=$customerId}

$refund=CallApi 'RefundsCreate' '/api/v1/refunds/create' $token @{returnId=$returnId;paymentId=$paymentId;amount=999.00;reason='RETURN_APPROVED'}
$rows+=$refund
$refundId=$refund.Data.refundId

$rows+=CallApi 'RefundsDetail' '/api/v1/refunds/detail' $token @{refundId=$refundId}
$rows+=CallApi 'RefundsList' '/api/v1/refunds/list' $token @{customerId=$customerId}

$rows | Select-Object Api,Http,Status,Message | Format-Table -AutoSize
