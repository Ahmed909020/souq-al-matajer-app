package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.Product
import com.example.model.Store
import com.example.model.UiState
import com.example.ui.MarketplaceViewModel
import com.example.ui.components.FeaturedBadge
import com.example.ui.components.FirebaseConnectionErrorState
import com.example.ui.components.MarketplaceImage
import com.example.ui.components.MarketplaceLoadingState
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.OrderNowDialog
import com.example.ui.components.ProductDetailsBottomSheet
import com.example.ui.components.RatingStarsBar
import com.example.ui.components.StoreProductCard
import com.example.ui.theme.FavoriteHeartRed
import com.example.ui.theme.StarGold
import com.example.util.MarketplaceIntents

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailScreen(
    storeId: String,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val storesUi by viewModel.storesState.collectAsStateWithLifecycle()
    val productsUi by viewModel.activeProductsState.collectAsStateWithLifecycle()
    val reviews by viewModel.selectedStoreReviews.collectAsStateWithLifecycle()
    val coupons by viewModel.selectedStoreCoupons.collectAsStateWithLifecycle()
    val favoriteStoreIds by viewModel.favoriteStoreIds.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val connectionError by viewModel.connectionError.collectAsStateWithLifecycle()
    val isRetryingConnection by viewModel.isRetryingConnection.collectAsStateWithLifecycle()

    LaunchedEffect(storeId) {
        viewModel.openStoreDetail(storeId)
    }

    val store: Store? = remember(storesUi, storeId) {
        (storesUi as? UiState.Success)?.data?.firstOrNull { it.id == storeId }
    }
    val products: List<Product> = remember(productsUi, storeId) {
        ((productsUi as? UiState.Success)?.data ?: emptyList()).filter { it.storeId == storeId }
    }

    var selectedProductForDetails by remember { mutableStateOf<Product?>(null) }
    var orderingProduct by remember { mutableStateOf<Pair<Product, Int>?>(null) }
    var reviewRating by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }

    val isFavorite = store?.id?.let { it in favoriteStoreIds } ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = store?.name ?: "تفاصيل المتجر",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("store_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                        )
                    }
                },
                actions = {
                    if (store != null) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite(store) },
                            modifier = Modifier.testTag("store_detail_favorite_button"),
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorite) "إزالة من المفضلة" else "حفظ في المفضلة",
                                tint = if (isFavorite) FavoriteHeartRed else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        if (store == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    storesUi is UiState.Loading -> {
                        MarketplaceLoadingState(
                            message = "جاري تحميل بيانات المتجر...",
                            subtitle = "يرجى الانتظار أثناء جلب المنتجات والمعلومات من Firebase",
                        )
                    }
                    storesUi is UiState.Error || !isOnline || connectionError != null -> {
                        FirebaseConnectionErrorState(
                            message = (storesUi as? UiState.Error)?.message
                                ?: connectionError
                                ?: "تعذر الاتصال بخوادم Firebase. يرجى التحقق من اتصال الإنترنت.",
                            isRetrying = isRetryingConnection,
                            onRetry = viewModel::retryFirebaseConnection,
                        )
                    }
                    else -> {
                        Text("المتجر غير متوفر حالياً")
                    }
                }
            }
            return@Scaffold
        }

        val liveRatingAvg = remember(reviews, store.ratingAvg) {
            if (reviews.isNotEmpty()) reviews.map { it.rating }.average() else store.ratingAvg
        }
        val liveRatingCount = remember(reviews, store.ratingCount) {
            maxOf(store.ratingCount, reviews.size.toLong())
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 175.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (!isOnline || connectionError != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    OfflineStatusBanner(
                        message = connectionError ?: "لا يوجد اتصال بالإنترنت — تعذر الوصول إلى خوادم Firebase",
                        isRetrying = isRetryingConnection,
                        onRetry = viewModel::retryFirebaseConnection,
                    )
                }
            }
            // Store Hero Info Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(20.dp),
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
                                imageUrl = store.logoUrl,
                                contentDescription = "شعار ${store.name}",
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = store.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f, fill = false),
                                    )
                                    if (store.featured) {
                                        FeaturedBadge()
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                        shape = RoundedCornerShape(8.dp),
                                    ) {
                                        Text(
                                            text = store.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Text(
                                            text = "${store.city} - ${store.address}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    RatingStarsBar(rating = liveRatingAvg, ratingCount = liveRatingCount)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Visibility,
                                            contentDescription = "المشاهدات",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = "${store.viewsCount} مشاهدة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = store.description,
                            style = MaterialTheme.typography.bodyLarge,
                        )

                        // Active coupons banner
                        val activeCoupons = coupons.filter {
                            it.active && it.expiryMillis > System.currentTimeMillis()
                        }
                        if (activeCoupons.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.LocalOffer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = "كوبون خصم متاح: ${activeCoupons.first().code} (${activeCoupons.first().discountPercent}%)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        // WhatsApp & Phone call action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    MarketplaceIntents.openWhatsApp(
                                        context,
                                        store.whatsappPhone,
                                        "مرحباً ${store.name}، أتواصل معكم عبر تطبيق سوق المتاجر."
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary,
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("store_whatsapp_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تواصل واتساب")
                            }

                            OutlinedButton(
                                onClick = { MarketplaceIntents.dialPhone(context, store.whatsappPhone) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("store_call_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اتصال هاتفي", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // Products Section Header
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "المنتجات (${products.size})",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            when {
                productsUi is UiState.Loading -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        MarketplaceLoadingState(
                            message = "جاري جلب منتجات المتجر...",
                            subtitle = "يرجى الانتظار لحظات أثناء التحميل من Firebase",
                        )
                    }
                }
                productsUi is UiState.Error -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        FirebaseConnectionErrorState(
                            message = (productsUi as UiState.Error).message,
                            isRetrying = isRetryingConnection,
                            onRetry = viewModel::retryFirebaseConnection,
                        )
                    }
                }
                products.isEmpty() -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = "لم يضف صاحب المتجر منتجات بعد.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp),
                            )
                        }
                    }
                }
                else -> {
                    items(products, key = { it.id }) { product ->
                        StoreProductCard(
                            product = product,
                            onProductClick = { selectedProductForDetails = product },
                            onOrderClick = { orderingProduct = product to 1 },
                        )
                    }
                }
            }

            // Visitor Reviews Section
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "تقييمات الزوار ($liveRatingCount)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        // Interactive Rating Picker
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text("تقييمك:", style = MaterialTheme.typography.bodyMedium)
                            for (star in 1..5) {
                                IconButton(
                                    onClick = { reviewRating = star },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("review_star_$star"),
                                ) {
                                    Icon(
                                        imageVector = if (star <= reviewRating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                        contentDescription = "تقييم $star نجوم",
                                        tint = if (star <= reviewRating) StarGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = reviewComment,
                            onValueChange = { reviewComment = it },
                            label = { Text("اكتب تعليقك عن المتجر") },
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("review_comment_input"),
                        )

                        Button(
                            onClick = {
                                if (reviewComment.isNotBlank()) {
                                    viewModel.submitReview(store, reviewRating, reviewComment) {
                                        reviewComment = ""
                                    }
                                }
                            },
                            enabled = reviewComment.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("submit_review_button"),
                        ) {
                            Text("إرسال التقييم")
                        }

                        reviews.forEach { review ->
                            Surface(
                                color = MaterialTheme.colorScheme.background,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = review.userName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        RatingStarsBar(rating = review.rating.toDouble(), starSize = 14.dp)
                                    }
                                    Text(
                                        text = review.comment,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Product Details & Quick WhatsApp Order Bottom Sheet
    val detailProduct = selectedProductForDetails
    if (detailProduct != null && store != null) {
        ProductDetailsBottomSheet(
            store = store,
            product = detailProduct,
            onDismiss = { selectedProductForDetails = null },
            onQuickWhatsAppOrder = { qty ->
                selectedProductForDetails = null
                MarketplaceIntents.sendProductOrderViaWhatsApp(context, store, detailProduct, qty)
            },
            onOpenOrderDialog = { qty ->
                selectedProductForDetails = null
                orderingProduct = detailProduct to qty
            },
        )
    }

    // Order Now Dialog (Name, Phone, Quantity, Address -> WhatsApp Link)
    val activeOrderPair = orderingProduct
    if (activeOrderPair != null && store != null) {
        val (activeProduct, initialQty) = activeOrderPair
        OrderNowDialog(
            store = store,
            product = activeProduct,
            initialQuantity = initialQty,
            defaultName = viewModel.userName,
            availableCoupons = coupons.filter { it.active && it.expiryMillis > System.currentTimeMillis() },
            onDismiss = { orderingProduct = null },
            onSubmitOrder = { order, whatsappMsg ->
                viewModel.placeOrder(order) {
                    orderingProduct = null
                    MarketplaceIntents.openWhatsApp(context, store.whatsappPhone, whatsappMsg)
                }
            },
        )
    }
}
