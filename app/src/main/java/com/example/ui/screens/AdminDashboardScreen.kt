package com.example.ui.screens

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.JoinRequest
import com.example.model.MarketplaceCatalog
import com.example.model.Store
import com.example.model.SubscriptionStatus
import com.example.ui.MarketplaceViewModel
import com.example.ui.components.FeaturedBadge
import com.example.util.MarketplaceIntents

@Composable
fun AdminDashboardScreen(
    viewModel: MarketplaceViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val allStores by viewModel.adminAllStores.collectAsStateWithLifecycle()
    val allRequests by viewModel.adminAllJoinRequests.collectAsStateWithLifecycle()
    val allSubscriptions by viewModel.adminAllSubscriptions.collectAsStateWithLifecycle()
    val allCoupons by viewModel.adminAllCoupons.collectAsStateWithLifecycle()
    val allCategories by viewModel.adminAllCategories.collectAsStateWithLifecycle()

    if (!viewModel.isAdmin) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "هذه الصفحة مخصصة لإدارة سوق المتاجر فقط",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("الطلبات", "المتاجر والاشتراكات", "التصنيفات", "الكوبونات", "التقارير")

    var rejectingRequest by remember { mutableStateOf<JoinRequest?>(null) }
    var managingSubStore by remember { mutableStateOf<Store?>(null) }
    var managingSpotlightStore by remember { mutableStateOf<Store?>(null) }
    var couponForStore by remember { mutableStateOf<Store?>(null) }
    var categorySearch by remember { mutableStateOf("") }
    var newCategoryName by remember { mutableStateOf("") }

    val pendingRequests = remember(allRequests) { allRequests.filter { it.status == "pending" } }
    val activeStoresCount = remember(allStores) { allStores.count { it.active } }
    val monthlyRevenue = remember(allSubscriptions, allStores) {
        allSubscriptions.sumOf { it.price } + allStores.filter { it.featured }.sumOf { it.spotlightFee }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "لوحة تحكم الإدارة",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            if (allStores.isEmpty()) {
                OutlinedButton(
                    onClick = { viewModel.seedSampleMarketplace() },
                    modifier = Modifier.testTag("admin_seed_samples_button"),
                ) {
                    Text("نشر 6 متاجر تجريبية")
                }
            }
        }

        // KPI Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AdminStatCard("إجمالي المتاجر", "${allStores.size}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            AdminStatCard("المتاجر النشطة", "$activeStoresCount", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AdminStatCard("طلبات معلقة", "${pendingRequests.size}", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
            AdminStatCard("الاشتراكات", "${allSubscriptions.size}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        }
        AdminStatCard(
            title = "الإيرادات الشهرية المحصلة",
            value = MarketplaceIntents.formatPrice(monthlyRevenue),
            accent = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth(),
        )

        // Admin Section Chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tabs.indices.toList()) { idx ->
                FilterChip(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    label = { Text(tabs[idx]) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.testTag("admin_tab_$idx"),
                )
            }
        }

        when (selectedTab) {
            // 0: Pending Requests
            0 -> {
                Text(
                    text = "طلبات الانضمام المعلقة (${pendingRequests.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (pendingRequests.isEmpty()) {
                    EmptyAdminBox("لا توجد طلبات معلقة حالياً.")
                } else {
                    pendingRequests.forEach { req ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = req.storeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = "المالك: ${req.ownerName} • ${req.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = "${req.city} • ${req.category} • ${req.address}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(req.description, style = MaterialTheme.typography.bodyMedium)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = { viewModel.approveJoinRequest(req) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("approve_request_${req.id}"),
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("قبول وتفعيل 30 يوم")
                                    }
                                    OutlinedButton(
                                        onClick = { rejectingRequest = req },
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("reject_request_${req.id}"),
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("رفض", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        val msg = "إشعار طلب انضمام جديد في سوق المتاجر:\nالمتجر: ${req.storeName}\nالمالك: ${req.ownerName}\nالمدينة: ${req.city}\nالتصنيف: ${req.category}\nالهاتف: ${req.phone}"
                                        MarketplaceIntents.openWhatsApp(context, MarketplaceCatalog.ADMIN_WHATSAPP_NUMBER, msg)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("رابط إشعار واتساب للإدارة")
                                }
                            }
                        }
                    }
                }
            }

            // 1: Stores, Subscriptions & Spotlight
            1 -> {
                Text(
                    text = "إدارة المتاجر والاشتراكات (${allStores.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                allStores.forEach { store ->
                    val statusLabel = SubscriptionStatus.fromId(store.subscriptionStatus).arabicLabel
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = store.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        if (store.featured) FeaturedBadge()
                                    }
                                    Text(
                                        text = "${store.city} • ${store.category} • الخطة: ${store.planName} ($statusLabel)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "تاريخ الانتهاء: ${MarketplaceIntents.formatArabicDate(store.subscriptionEndMillis)}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                                Switch(
                                    checked = store.active,
                                    onCheckedChange = { viewModel.setStoreActive(store.id, it) },
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedButton(
                                    onClick = { managingSubStore = store },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Text("الاشتراك")
                                }
                                OutlinedButton(
                                    onClick = { managingSpotlightStore = store },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Spotlight")
                                }
                                OutlinedButton(
                                    onClick = { couponForStore = store },
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Filled.LocalOffer, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("كوبون")
                                }
                            }
                        }
                    }
                }
            }

            // 2: Categories Management
            2 -> {
                OutlinedTextField(
                    value = categorySearch,
                    onValueChange = { categorySearch = it },
                    label = { Text("بحث في التصنيفات") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("إضافة تصنيف جديد") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                viewModel.addCategory(newCategoryName, allCategories.size + 1)
                                newCategoryName = ""
                            }
                        },
                        enabled = newCategoryName.trim().length >= 2,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        modifier = Modifier.height(54.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "إضافة")
                    }
                }

                val filteredCats = remember(allCategories, categorySearch) {
                    val q = categorySearch.trim().lowercase()
                    allCategories
                        .sortedBy { it.sortOrder }
                        .filter { q.isEmpty() || it.name.lowercase().contains(q) }
                }

                filteredCats.forEach { cat ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(cat.name, style = MaterialTheme.typography.titleSmall)
                            Switch(
                                checked = cat.active,
                                onCheckedChange = { viewModel.toggleCategoryActive(cat.id, it) },
                            )
                        }
                    }
                }
            }

            // 3: Coupons Management
            3 -> {
                Text(
                    text = "جميع كوبونات الخصم (${allCoupons.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (allCoupons.isEmpty()) {
                    EmptyAdminBox("لا توجد كوبونات خصم بعد. يمكنك إنشاء كوبون من تبويب المتاجر.")
                } else {
                    allCoupons.forEach { coupon ->
                        val storeName = allStores.firstOrNull { it.id == coupon.storeId }?.name ?: coupon.storeId
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = "${coupon.code} (${coupon.discountPercent}%)",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.secondary,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "$storeName • ينتهي ${MarketplaceIntents.formatArabicDate(coupon.expiryMillis)}",
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
                }
            }

            // 4: Monthly Reports & Print
            4 -> {
                val now = System.currentTimeMillis()
                val sevenDaysLater = now + 7L * 24 * 60 * 60 * 1000
                val endingSoon = remember(allSubscriptions) {
                    allSubscriptions.filter { it.status == "active" && it.endDateMillis in now..sevenDaysLater }
                }

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "ملخص التقرير الشهري",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "إجمالي المبالغ المحصلة (الاشتراكات + Spotlight): ${MarketplaceIntents.formatPrice(monthlyRevenue)}",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "اشتراكات تنتهي خلال 7 أيام: ${endingSoon.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        endingSoon.forEach { sub ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "${sub.storeName} — ينتهي في ${MarketplaceIntents.formatArabicDate(sub.endDateMillis)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }

                        Button(
                            onClick = {
                                MarketplaceIntents.printMonthlyReport(context, allStores, allSubscriptions)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("print_monthly_report_button"),
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("طباعة التقرير الشهري")
                        }
                    }
                }
            }
        }
    }

    rejectingRequest?.let { req ->
        RejectRequestDialog(
            storeName = req.storeName,
            onDismiss = { rejectingRequest = null },
            onConfirmReject = { reason ->
                viewModel.rejectJoinRequest(req.id, reason)
                rejectingRequest = null
            },
        )
    }

    managingSubStore?.let { store ->
        val existingSub = allSubscriptions.firstOrNull { it.storeId == store.id }
        SubscriptionManageDialog(
            store = store,
            existingSubscription = existingSub,
            onDismiss = { managingSubStore = null },
            onSave = { plan, price, status, start, end ->
                viewModel.saveSubscriptionAdmin(existingSub?.id, store, plan, price, status, start, end)
                managingSubStore = null
            },
        )
    }

    managingSpotlightStore?.let { store ->
        SpotlightManageDialog(
            store = store,
            onDismiss = { managingSpotlightStore = null },
            onSave = { featured, fee ->
                viewModel.updateStoreSpotlight(store.id, featured, fee)
                managingSpotlightStore = null
            },
        )
    }

    couponForStore?.let { store ->
        CreateCouponDialog(
            storeName = store.name,
            onDismiss = { couponForStore = null },
            onCreate = { code, discount, days ->
                viewModel.createCoupon(store, code, discount, days) {
                    couponForStore = null
                }
            },
        )
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EmptyAdminBox(message: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
        )
    }
}
