$ts=[DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$email="smoke.$ts@example.com"
$phone="9"+($ts.ToString().Substring(1,9))
$reg=Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/auth/register' -ContentType 'application/json' -Body (@{token='';requestId='req-'+[guid]::NewGuid();data=@{name='Smoke User';email=$email;password='StrongPass@123';phone=$phone}}|ConvertTo-Json -Depth 6)
$token=$reg.data.token
$customerId='39225d99-1f70-11f1-9651-ed7fb304f8d2'
$productId='38e45a96-1f70-11f1-9651-ed7fb304f8d2'
$variantId='38fdce2b-1f70-11f1-9651-ed7fb304f8d2'
$addressId='3929205e-1f70-11f1-9651-ed7fb304f8d2'

function CallStatus($name,$path,$data){
  try{
    $r=Invoke-RestMethod -Method Post -Uri ("http://localhost:8080"+$path) -ContentType 'application/json' -Body (@{token=$token;requestId='req-'+[guid]::NewGuid();data=$data}|ConvertTo-Json -Depth 10)
    "${name}: $($r.status) - $($r.message)"
  } catch {
    if($_.Exception.Response -ne $null){
      $sr=New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
      "${name}: FAILED - " + $sr.ReadToEnd()
    } else {
      "${name}: FAILED - $($_.Exception.Message)"
    }
  }
}

$lines = @()
$lines += "Register: $($reg.status) - $($reg.message)"
$lines += CallStatus 'ProductsList' '/api/v1/products/list' @{}
$lines += CallStatus 'ProductsDetail' '/api/v1/products/detail' @{productId=$productId}
$lines += CallStatus 'ProductsSearch' '/api/v1/products/search' @{keyword='shirt'}
$lines += CallStatus 'CartAdd' '/api/v1/cart/add' @{customerId=$customerId;productId=$productId;variantId=$variantId;quantity=1}
$lines += CallStatus 'CartGet' '/api/v1/cart/get' @{customerId=$customerId}
$lines += CallStatus 'AddressAdd' '/api/v1/address/add' @{customerId=$customerId;type='HOME';addressLine='Test St';city='Hyd';state='TS';pincode='500001';isDefault=$false}
$lines += CallStatus 'CouponValidate' '/api/v1/coupons/validate' @{customerId=$customerId;couponCode='SAVE10';orderAmount=1500.00}
try {
  $oc=Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/orders/create' -ContentType 'application/json' -Body (@{token=$token;requestId='req-'+[guid]::NewGuid();data=@{customerId=$customerId;addressId=$addressId;couponCode='SAVE10';paymentMethod='UPI';items=@(@{variantId=$variantId;quantity=1;price=999.00})}}|ConvertTo-Json -Depth 10)
  $lines += "OrdersCreate: $($oc.status) - $($oc.message)"
  $orderId=$oc.data.orderId
  $lines += CallStatus 'OrdersList' '/api/v1/orders/list' @{customerId=$customerId}
  $lines += CallStatus 'OrdersDetail' '/api/v1/orders/detail' @{orderId=$orderId}
  $lines += CallStatus 'PaymentsCreate' '/api/v1/payments/create' @{orderId=$orderId;method='UPI';gatewayTxnId=('TXN-'+$ts);amount=999.00}
} catch {
  if($_.Exception.Response -ne $null){
    $sr=New-Object IO.StreamReader($_.Exception.Response.GetResponseStream())
    $lines += "OrdersCreate: FAILED - " + $sr.ReadToEnd()
  } else {
    $lines += "OrdersCreate: FAILED - $($_.Exception.Message)"
  }
}
$lines += CallStatus 'UserProfile' '/api/v1/user/profile' @{customerId=$customerId}
$lines += CallStatus 'UserUpdate' '/api/v1/user/update' @{customerId=$customerId;name='Ravi Kumar';email='ravi@test.com';mobile='9876543222'}

$lines | Set-Content -Path .\api_test_results.txt
