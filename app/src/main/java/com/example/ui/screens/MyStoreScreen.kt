package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.MarketplaceCatalog
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.Store
import com.example.ui.MarketplaceViewModel
import com.example.ui.components.MarketplaceImage
import com.example.util.MarketplaceIntents
import java.util.concurrent.TimeUnit

@Composable
fun MyStoreScreen(
    viewModel: MarketplaceViewModel,
    onNavigateToAddStore: () -> Unit,
    onOpenTrackingPage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val ownerStores by viewModel.ownerStores.collectAsStateWithLifecycle()
    val ownerProducts by viewModel.ownerProducts.collectAsStateWithLifecycle()
    val ownerCoupons by viewModel.ownerCoupons.collectAsStateWithLifecycle()
    val ownerOrders by viewModel.ownerOrders.collectAsStateWithLifecycle()

    val myStore: Store? = ownerStores.firstOrNull()

    var showEditStoreDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showAddCouponDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "متجري وإدارة المنتجات",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )

        if (myStore == null) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = "لا يوجد لديك متجر مفعّل حالياً",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "أضف بيانات متجرك من صفحة \"أضف متجرك\" وبعد موافقة الإدارة ستتمكن من إدارة منتجاتك وكوبوناتك هنا.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = onNavigateToAddStore,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.testTag("go_to_add_store_button"),
                    ) {
                        Text("أضف متجرك الآن")
                    }
                }
            }
            return@Column
        }

        // Store Summary & Info Editor Trigger
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MarketplaceImage(
                        imageUrl = myStore.logoUrl,
                        contentDescription = "شعار ${myStore.name}",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = myStore.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${myStore.city} • ${myStore.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    OutlinedButton(
                        onClick = { showEditStoreDialog = true },
                        modifier = Modifier.testTag("edit_my_store_button"),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل")
                    }
                }

                // Subscription Card
                val remainingDays = remember(myStore.subscriptionEndMillis) {
                    val diff = myStore.subscriptionEndMillis - System.currentTimeMillis()
                    TimeUnit.MILLISECONDS.toDays(diff).coerceAtLeast(0L)
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "الباقة: ${myStore.planName}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = "المتبقي: $remainingDays يوم",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = "تاريخ الانتهاء: ${MarketplaceIntents.formatArabicDate(myStore.subscriptionEndMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "للتجديد تواصل مع الإدارة",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                        )
                        OutlinedButton(
                            onClick = {
                                MarketplaceIntents.openWhatsApp(
                                    context,
                                    MarketplaceCatalog.ADMIN_WHATSAPP_NUMBER,
                                    "مرحباً إدارة سوق المتاجر، أرغب بتجديد اشتراك متجري (${myStore.name})."
                                )
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تواصل مع الإدارة للتجديد", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Views & Tracking /track/[storeId]
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Column {
                                Text(
                                    text = "${myStore.viewsCount} مشاهدة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "/track/${myStore.id}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                )
                            }
                        }
                        Button(
                            onClick = { onOpenTrackingPage(myStore.id) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                            ),
                            modifier = Modifier.testTag("open_tracking_page_button"),
                        ) {
                            Icon(Icons.Filled.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("صفحة التتبع")
                        }
                    }
                }
            }
        }

        // Products Management
        val storeProducts = remember(ownerProducts, myStore.id) {
            ownerProducts.filter { it.storeId == myStore.id }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "منتجاتي (${storeProducts.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Button(
                onClick = { showAddProductDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.testTag("add_product_button"),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إضافة منتج")
            }
        }

        storeProducts.forEach { product ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MarketplaceImage(
                        imageUrl = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = MarketplaceIntents.formatPrice(product.price),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Switch(
                        checked = product.active,
                        onCheckedChange = { active ->
                            viewModel.toggleProductActive(product.id, active)
                        },
                    )
                    IconButton(onClick = { editingProduct = product }) {
                        Icon(Icons.Filled.Edit, contentDescription = "تعديل المنتج", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { viewModel.deleteProduct(product.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "حذف المنتج", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        // Coupons Management
        val storeCoupons = remember(ownerCoupons, myStore.id) {
            ownerCoupons.filter { it.storeId == myStore.id }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "كوبونات الخصم (${storeCoupons.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            OutlinedButton(
                onClick = { showAddCouponDialog = true },
                modifier = Modifier.testTag("owner_add_coupon_button"),
            ) {
                Icon(Icons.Filled.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("إنشاء كوبون")
            }
        }

        storeCoupons.forEach { coupon ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "${coupon.code} — خصم ${coupon.discountPercent}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = "ينتهي في ${MarketplaceIntents.formatArabicDate(coupon.expiryMillis)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = coupon.active,
                            onCheckedChange = { viewModel.toggleCouponActive(coupon.id, it) },
                        )
                        IconButton(onClick = { viewModel.deleteCoupon(coupon.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "حذف الكوبون", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }

        // Recent Orders with Order Status Updating
        if (ownerOrders.isNotEmpty()) {
            Text(
                text = "الطلبات الواردة (${ownerOrders.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            ownerOrders.take(10).forEach { order ->
                val currentStatus = OrderStatus.fromId(order.status)
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "${order.productName} × ${order.quantity} — ${MarketplaceIntents.formatPrice(order.totalPrice)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "العميل: ${order.customerName} | ${order.customerPhone}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            text = "العنوان: ${order.customerAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(OrderStatus.entries) { status ->
                                val selected = currentStatus == status
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (!selected) {
                                            viewModel.updateOrderStatus(order.id, status)
                                        }
                                    },
                                    label = { Text(status.arabicLabel) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        labelColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                    modifier = Modifier.testTag("order_status_${order.id}_${status.id}"),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditStoreDialog && myStore != null) {
        EditStoreDialog(
            store = myStore,
            onDismiss = { showEditStoreDialog = false },
            onSave = { name, ownerName, desc, address, city, cat, phone, logo ->
                viewModel.updateStoreInfoOwner(
                    myStore.id, name, ownerName, desc, address, city, cat, phone, logo
                ) {
                    showEditStoreDialog = false
                }
            },
        )
    }

    if ((showAddProductDialog || editingProduct != null) && myStore != null) {
        ProductEditorDialog(
            initial = editingProduct,
            storeId = myStore.id,
            onDismiss = {
                showAddProductDialog = false
                editingProduct = null
            },
            onSave = { product ->
                viewModel.saveProduct(product) {
                    showAddProductDialog = false
                    editingProduct = null
                }
            },
        )
    }

    if (showAddCouponDialog && myStore != null) {
        CreateCouponDialog(
            storeName = myStore.name,
            onDismiss = { showAddCouponDialog = false },
            onCreate = { code, discount, days ->
                viewModel.createCoupon(myStore, code, discount, days) {
                    showAddCouponDialog = false
                }
            },
        )
    }
}

@Composable
private fun EditStoreDialog(
    store: Store,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(store.name) }
    var ownerName by remember { mutableStateOf(store.ownerName) }
    var description by remember { mutableStateOf(store.description) }
    var address by remember { mutableStateOf(store.address) }
    var phone by remember { mutableStateOf(store.whatsappPhone) }
    var logoUrl by remember { mutableStateOf(store.logoUrl) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            MarketplaceIntents.encodeImageUriToCompactDataUri(context, uri)?.let { logoUrl = it }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل بيانات المتجر") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تغيير الشعار من المعرض")
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("اسم المتجر") })
                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("اسم المالك") })
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("رقم الواتساب") })
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("العنوان") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("الوصف") }, maxLines = 3)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, ownerName, description, address, store.city, store.category, phone, logoUrl)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

@Composable
fun ProductEditorDialog(
    initial: Product?,
    storeId: String,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit,
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var priceText by remember { mutableStateOf(initial?.price?.toString() ?: "") }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            MarketplaceIntents.encodeImageUriToCompactDataUri(context, uri)?.let { imageUrl = it }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "إضافة منتج جديد" else "تعديل المنتج") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختيار صورة المنتج")
                }
                OutlinedTextField(
                    value = if (imageUrl.startsWith("data:")) "" else imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("أو رابط صورة المنتج") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المنتج *") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("السعر (ر.س) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف المنتج") },
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            val price = priceText.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    val model = (initial ?: Product(storeId = storeId)).copy(
                        storeId = storeId,
                        name = name.trim(),
                        description = description.trim(),
                        price = price,
                        imageUrl = imageUrl.trim(),
                    )
                    onSave(model)
                },
                enabled = name.trim().length >= 2 && price > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("حفظ المنتج")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

@Composable
fun CreateCouponDialog(
    storeName: String,
    onDismiss: () -> Unit,
    onCreate: (String, Int, Int) -> Unit,
) {
    var code by remember { mutableStateOf("") }
    var percentText by remember { mutableStateOf("15") }
    var daysText by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إنشاء كوبون خصم لمتجر $storeName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("كود الخصم (مثال: SAVE20)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = percentText,
                    onValueChange = { percentText = it },
                    label = { Text("نسبة الخصم % (1 - 90)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it },
                    label = { Text("صلاحية الكوبون بالأيام") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            val pct = percentText.toIntOrNull() ?: 0
            val days = daysText.toIntOrNull() ?: 30
            Button(
                onClick = { onCreate(code, pct, days) },
                enabled = code.trim().length >= 2 && pct in 1..90,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("إنشاء")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}
