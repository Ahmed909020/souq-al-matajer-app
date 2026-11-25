package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.model.Store
import com.example.model.UiState
import com.example.ui.MarketplaceViewModel
import com.example.ui.components.StoreMarketplaceCard
import com.example.ui.theme.FavoriteHeartRed

@Composable
fun FavoritesScreen(
    viewModel: MarketplaceViewModel,
    onStoreClick: (String) -> Unit,
    onBrowseMarketplace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteStores by viewModel.favoriteStores.collectAsStateWithLifecycle()
    val storesUi by viewModel.storesState.collectAsStateWithLifecycle()
    val activeProductsUi by viewModel.activeProductsState.collectAsStateWithLifecycle()

    val liveStoresMap = remember(storesUi) {
        val list = (storesUi as? UiState.Success)?.data ?: emptyList()
        list.associateBy { it.id }
    }
    val productsByStore = remember(activeProductsUi) {
        val list = (activeProductsUi as? UiState.Success)?.data ?: emptyList()
        list.groupBy { it.storeId }
    }

    val resolvedFavoriteStores: List<Store> = remember(favoriteStores, liveStoresMap) {
        favoriteStores.map { fav ->
            liveStoresMap[fav.storeId] ?: Store(
                id = fav.storeId,
                name = fav.storeName,
                category = fav.storeCategory,
                city = fav.storeCity,
                logoUrl = fav.storeLogoUrl,
                description = "متجر محفوظ في قائمتك الخاصة",
                active = true,
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = FavoriteHeartRed.copy(alpha = 0.14f),
                    shape = CircleShape,
                    modifier = Modifier.size(44.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Favorite,
                            contentDescription = null,
                            tint = FavoriteHeartRed,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "المتاجر المفضلة",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "قائمتك الخاصة المحفوظة في Firestore (${resolvedFavoriteStores.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (resolvedFavoriteStores.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("favorites_empty_state"),
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(68.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.FavoriteBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Text(
                            text = "قائمة المفضلة فارغة حالياً",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "اضغط على أيقونة القلب في بطاقة أي متجر لحفظه هنا والوصول إليه بسرعة لاحقاً.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onBrowseMarketplace,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("favorites_browse_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("تصفح متاجر السوق")
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 270.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("favorites_stores_grid"),
            ) {
                items(resolvedFavoriteStores, key = { it.id }) { store ->
                    val count = productsByStore[store.id]?.size ?: store.productsCount.toInt()
                    StoreMarketplaceCard(
                        store = store,
                        productsCount = count,
                        isFavorite = true,
                        onFavoriteClick = { viewModel.toggleFavorite(store) },
                        onClick = { onStoreClick(store.id) },
                    )
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
