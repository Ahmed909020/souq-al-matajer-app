package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.AlertCategory
import com.example.model.MarketplaceCatalog
import com.example.model.OrderStatus
import com.example.model.UiState
import com.example.ui.MarketplaceViewModel
import com.example.ui.components.FirebaseConnectionErrorState
import com.example.ui.components.MarketplaceImage
import com.example.ui.components.MarketplaceLoadingState
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.StoreCategoryChipsBar
import com.example.ui.components.StoreMarketplaceCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.OrangeAccent
import com.example.util.MarketplaceIntents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MarketplaceViewModel,
    onStoreClick: (String) -> Unit,
    onOpenAdmin: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val categories by viewModel.categoriesState.collectAsStateWithLifecycle()
    val filteredStores by viewModel.filteredStores.collectAsStateWithLifecycle()
    val filteredSearchProducts by viewModel.filteredSearchProducts.collectAsStateWithLifecycle()
    val activeProductsUi by viewModel.activeProductsState.collectAsStateWithLifecycle()
    val storesUi by viewModel.storesState.collectAsStateWithLifecycle()
    val favoriteStoreIds by viewModel.favoriteStoreIds.collectAsStateWithLifecycle()
    val inAppAlerts by viewModel.inAppAlerts.collectAsStateWithLifecycle()
    val unreadAlertCount by viewModel.unreadAlertCount.collectAsStateWithLifecycle()
    val customerOrders by viewModel.customerOrders.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val connectionError by viewModel.connectionError.collectAsStateWithLifecycle()
    val isRetryingConnection by viewModel.isRetryingConnection.collectAsStateWithLifecycle()

    val productsByStore = remember(activeProductsUi) {
        val list = (activeProductsUi as? UiState.Success)?.data ?: emptyList()
        list.groupBy { it.storeId }
    }
    val storeNamesById = remember(storesUi) {
        val list = (storesUi as? UiState.Success)?.data ?: emptyList()
        list.associate { it.id to it.name }
    }

    var showAllCategoriesSheet by remember { mutableStateOf(false) }
    var showNotificationsSheet by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.sendOfferReminderNotification()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Marketplace Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(42.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = "شعار سوق المتاجر",
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "سوق المتاجر",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "أهلاً، ${viewModel.userName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.markAlertsRead()
                                showNotificationsSheet = true
                            },
                            modifier = Modifier.testTag("notifications_bell_button"),
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadAlertCount > 0) {
                                        Badge {
                                            Text(unreadAlertCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (unreadAlertCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                    contentDescription = "الإشعارات والتنبيهات",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        if (viewModel.isAdmin) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .testTag("header_admin_button")
                                    .clickable(onClick = onOpenAdmin),
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AdminPanelSettings,
                                        contentDescription = "لوحة الإدارة",
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "الإدارة",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "تسجيل الخروج",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                // Search bar (store name or product name)
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = {
                        Text(
                            text = "ابحث باسم المتجر أو اسم المنتج...",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(
                                    imageVector = Icons.Filled.Clear,
                                    contentDescription = "مسح البحث",
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("marketplace_search_input"),
                )
            }
        }

        // Category Filter Chips right at the top of the Home Screen
        StoreCategoryChipsBar(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = viewModel::setSelectedCategory,
            onShowAllCategories = { showAllCategoriesSheet = true },
        )

        if (!isOnline || connectionError != null) {
            OfflineStatusBanner(
                message = connectionError ?: "لا يوجد اتصال بالإنترنت — تعذر الوصول إلى خوادم Firebase",
                isRetrying = isRetryingConnection,
                onRetry = viewModel::retryFirebaseConnection,
            )
        }

        // Scrollable Marketplace Content
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 270.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // Hero Banner
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_marketplace_hero_1795621525560),
                        contentDescription = "سوق المتاجر المحلي",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        EmeraldPrimary.copy(alpha = 0.88f),
                                        EmeraldPrimary.copy(alpha = 0.35f),
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            color = OrangeAccent,
                            contentColor = Color.White,
                            shape = RoundedCornerShape(999.dp),
                        ) {
                            Text(
                                text = "متاجر مميزة وعروض حصرية",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                        Text(
                            text = "تسوّق من أفضل المتاجر المحلية",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "اطلب مباشرة عبر واتساب بدون عمولات",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.92f),
                        )
                    }
                }
            }

            // Matching products quick strip when user searches by product name
            if (searchQuery.isNotBlank() && filteredSearchProducts.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "المنتجات المطابقة (${filteredSearchProducts.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.testTag("matched_products_search_row"),
                        ) {
                            items(filteredSearchProducts, key = { it.id }) { product ->
                                val storeName = storeNamesById[product.storeId] ?: "متجر"
                                Card(
                                    onClick = { onStoreClick(product.storeId) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier
                                        .width(170.dp)
                                        .testTag("search_product_card_${product.id}"),
                                ) {
                                    Column {
                                        MarketplaceImage(
                                            imageUrl = product.imageUrl,
                                            contentDescription = product.name,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(92.dp),
                                        )
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(3.dp),
                                        ) {
                                            Text(
                                                text = product.name,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = storeName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                            Text(
                                                text = MarketplaceIntents.formatPrice(product.price),
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // City Filter Chips
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationCity,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "تصفية حسب المدينة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    val cityChips = remember { listOf("الكل") + MarketplaceCatalog.cities }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(cityChips) { city ->
                            val selected = selectedCity == city
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.setSelectedCity(city) },
                                label = { Text(city) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    labelColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                modifier = Modifier.testTag("city_chip_$city"),
                            )
                        }
                    }
                }
            }

            // Section Title
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "المتاجر المتاحة (${filteredStores.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (selectedCategory != "الكل" || selectedCity != "الكل" || searchQuery.isNotEmpty()) {
                        TextButton(onClick = {
                            viewModel.setSearchQuery("")
                            viewModel.setSelectedCategory("الكل")
                            viewModel.setSelectedCity("الكل")
                        }) {
                            Text("إعادة ضبط الفلاتر", color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            val isLoadingData = storesUi is UiState.Loading || activeProductsUi is UiState.Loading
            val errorMessage = (storesUi as? UiState.Error)?.message
                ?: (activeProductsUi as? UiState.Error)?.message

            if (isLoadingData) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    MarketplaceLoadingState(
                        message = "جاري جلب المتاجر والمنتجات...",
                        subtitle = "يرجى الانتظار أثناء تحميل أحدث المتاجر من Firebase",
                    )
                }
            } else if (errorMessage != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    FirebaseConnectionErrorState(
                        message = errorMessage,
                        isRetrying = isRetryingConnection,
                        onRetry = viewModel::retryFirebaseConnection,
                    )
                }
            } else if (filteredStores.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Storefront,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                            Text(
                                text = "لا توجد متاجر مطابقة لبحثك",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "جرّب تغيير التصنيف أو المدينة أو البحث باسم منتج آخر",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else {
                items(filteredStores, key = { it.id }) { store ->
                    val count = productsByStore[store.id]?.size ?: store.productsCount.toInt()
                    StoreMarketplaceCard(
                        store = store,
                        productsCount = count,
                        isFavorite = store.id in favoriteStoreIds,
                        onFavoriteClick = { viewModel.toggleFavorite(store) },
                        onClick = { onStoreClick(store.id) },
                    )
                }
            }
        }
    }

    if (showAllCategoriesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAllCategoriesSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "جميع التصنيفات (${categories.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 130.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(380.dp),
                ) {
                    val allCats = listOf("الكل") + categories
                    items(allCats) { cat ->
                        val selected = selectedCategory == cat
                        Surface(
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            },
                            contentColor = if (selected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable {
                                viewModel.setSelectedCategory(cat)
                                showAllCategoriesSheet = false
                            },
                        ) {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showNotificationsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNotificationsSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "مركز الإشعارات والعروض",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(999.dp),
                    ) {
                        Text(
                            text = "${inAppAlerts.size} تنبيه",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                viewModel.sendOfferReminderNotification()
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else {
                            viewModel.sendOfferReminderNotification()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("trigger_offer_notification_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال تذكير محلي بالعروض الحالية")
                }

                if (customerOrders.isNotEmpty()) {
                    Text(
                        text = "حالة طلباتك الأخيرة (${customerOrders.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    customerOrders.take(3).forEach { order ->
                        val statusLabel = OrderStatus.fromId(order.status).arabicLabel
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ShoppingBag,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp),
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${order.productName} • ${order.storeName}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "الحالة: $statusLabel",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(260.dp),
                ) {
                    if (inAppAlerts.isEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = "ستظهر هنا تنبيهات العروض الجديدة وتحديثات حالة طلباتك فور وصولها.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                    } else {
                        items(inAppAlerts, key = { it.id }) { alert ->
                            val isOffer = alert.category == AlertCategory.OFFER
                            Surface(
                                color = if (isOffer) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Icon(
                                        imageVector = if (isOffer) Icons.Filled.LocalOffer else Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = if (isOffer) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp),
                                    ) {
                                        Text(
                                            text = alert.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = alert.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
