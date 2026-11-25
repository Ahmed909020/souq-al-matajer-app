package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.Coupon
import com.example.model.Order
import com.example.model.Product
import com.example.model.Store
import com.example.util.MarketplaceIntents

@Composable
fun StoreProductCard(
    product: Product,
    onProductClick: () -> Unit,
    onOrderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
            .clickable(onClick = onProductClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column {
            MarketplaceImage(
                imageUrl = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
            )
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = MarketplaceIntents.formatPrice(product.price),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                Button(
                    onClick = onOrderClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("order_product_${product.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اطلب الآن")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsBottomSheet(
    store: Store,
    product: Product,
    onDismiss: () -> Unit,
    onQuickWhatsAppOrder: (Int) -> Unit,
    onOpenOrderDialog: (Int) -> Unit,
) {
    var quantity by remember(product.id) { mutableIntStateOf(1) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MarketplaceImage(
                imageUrl = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Text(
                text = "${store.name} • ${MarketplaceIntents.formatPrice(product.price)}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )

            if (product.description.isNotBlank()) {
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("الكمية:", style = MaterialTheme.typography.bodyLarge)
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { if (quantity > 1) quantity-- },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = "تقليل الكمية",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Text(
                    text = quantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { quantity++ },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "زيادة الكمية",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "الإجمالي",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = MarketplaceIntents.formatPrice(product.price * quantity),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Button(
                onClick = { onQuickWhatsAppOrder(quantity) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("product_detail_whatsapp_order_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("الطلب عبر واتساب")
            }

            OutlinedButton(
                onClick = { onOpenOrderDialog(quantity) },
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("product_detail_full_checkout_button"),
            ) {
                Text("اطلب الآن (إدخال العنوان ورقم الهاتف)", color = MaterialTheme.colorScheme.secondary)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun OrderNowDialog(
    store: Store,
    product: Product,
    initialQuantity: Int = 1,
    defaultName: String,
    availableCoupons: List<Coupon>,
    onDismiss: () -> Unit,
    onSubmitOrder: (Order, String) -> Unit,
) {
    var customerName by remember(product.id) { mutableStateOf(defaultName) }
    var customerPhone by remember(product.id) { mutableStateOf("") }
    var quantityText by remember(product.id, initialQuantity) {
        mutableStateOf(initialQuantity.coerceAtLeast(1).toString())
    }
    var customerAddress by remember(product.id) { mutableStateOf("") }
    var couponCodeInput by remember(product.id) { mutableStateOf("") }

    val quantity = (quantityText.toIntOrNull() ?: 1).coerceAtLeast(1)

    val matchedCoupon = remember(couponCodeInput, availableCoupons) {
        val code = couponCodeInput.trim().uppercase()
        if (code.isEmpty()) null else availableCoupons.firstOrNull { it.code.uppercase() == code }
    }
    val discountPct = matchedCoupon?.discountPercent?.toInt() ?: 0
    val rawTotal = product.price * quantity
    val finalTotal = rawTotal * (1.0 - discountPct / 100.0)

    val whatsappMessage = remember(
        store.name,
        product.name,
        quantity,
        finalTotal,
        customerName,
        customerPhone,
        customerAddress,
        matchedCoupon,
        discountPct,
    ) {
        MarketplaceIntents.buildFullOrderWhatsAppMessage(
            storeName = store.name,
            productName = product.name,
            quantity = quantity,
            totalPrice = finalTotal,
            customerName = customerName,
            customerPhone = customerPhone,
            customerAddress = customerAddress,
            couponCode = matchedCoupon?.code,
            discountPercent = discountPct,
        )
    }

    val isValid = customerName.trim().length >= 2 &&
        customerPhone.trim().length >= 6 &&
        customerAddress.trim().length >= 3 &&
        (quantityText.toIntOrNull() ?: 0) >= 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .padding(horizontal = 16.dp, vertical = 24.dp)
                .testTag("order_now_dialog"),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "اطلب الآن عبر واتساب",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "${store.name} • ${product.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_order_dialog_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "إغلاق",
                        )
                    }
                }

                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("الاسم") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_name_input"),
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("رقم الهاتف") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_phone_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { input ->
                            val digitsOnly = input.filter { it.isDigit() }
                            quantityText = digitsOnly
                        },
                        label = { Text("الكمية") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("order_quantity_input"),
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                if (quantity > 1) {
                                    quantityText = (quantity - 1).toString()
                                }
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "تقليل الكمية",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(48.dp)
                            .clickable {
                                quantityText = (quantity + 1).toString()
                            },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "زيادة الكمية",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                OutlinedTextField(
                    value = customerAddress,
                    onValueChange = { customerAddress = it },
                    label = { Text("العنوان") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_address_input"),
                )

                OutlinedTextField(
                    value = couponCodeInput,
                    onValueChange = { couponCodeInput = it },
                    label = { Text("كود الخصم (اختياري)") },
                    singleLine = true,
                    supportingText = {
                        if (matchedCoupon != null) {
                            Text("تم تطبيق خصم ${matchedCoupon.discountPercent}%", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_coupon_input"),
                )

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "الإجمالي النهائي",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = MarketplaceIntents.formatPrice(finalTotal),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Button(
                    onClick = {
                        val order = Order(
                            storeId = store.id,
                            storeName = store.name,
                            storeOwnerId = store.ownerId,
                            productId = product.id,
                            productName = product.name,
                            unitPrice = product.price,
                            quantity = quantity.toLong(),
                            customerName = customerName.trim(),
                            customerPhone = customerPhone.trim(),
                            customerAddress = customerAddress.trim(),
                            couponCode = matchedCoupon?.code,
                            discountPercent = discountPct.toLong(),
                            totalPrice = finalTotal,
                        )
                        onSubmitOrder(order, whatsappMessage)
                    },
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_whatsapp_order_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال الطلب عبر واتساب")
                }
            }
        }
    }
}
