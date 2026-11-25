package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.model.Store
import com.example.ui.theme.FavoriteHeartRed
import com.example.ui.theme.OrangeAccent
import com.example.ui.theme.StarGold

@Composable
fun MarketplaceImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val decodedBitmap = remember(imageUrl) {
        if (imageUrl.startsWith("data:image") && imageUrl.contains(",")) {
            try {
                val base64Part = imageUrl.substringAfter(",")
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else {
            null
        }
    }

    if (decodedBitmap != null) {
        Image(
            bitmap = decodedBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
        )
    } else if (imageUrl.isNotBlank()) {
        SubcomposeAsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Storefront,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp),
                    )
                }
            },
            error = {
                PlaceholderImageBox(modifier = Modifier.fillMaxSize())
            },
        )
    } else {
        PlaceholderImageBox(modifier = modifier)
    }
}

@Composable
private fun PlaceholderImageBox(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                )
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Storefront,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
fun RatingStarsBar(
    rating: Double,
    ratingCount: Long? = null,
    starSize: Dp = 16.dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        val rounded = rating.toInt().coerceIn(0, 5)
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rounded) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (i <= rounded) StarGold else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(starSize),
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = String.format(java.util.Locale.US, "%.1f", rating),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        if (ratingCount != null) {
            Text(
                text = "($ratingCount)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun FeaturedBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = OrangeAccent,
        contentColor = Color.White,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = "متجر مميز",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun StoreMarketplaceCard(
    store: Store,
    productsCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onFavoriteClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("store_card_${store.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (store.featured) 1.5.dp else 1.dp,
            color = if (store.featured) MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (store.featured) 4.dp else 1.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
            ) {
                MarketplaceImage(
                    imageUrl = store.logoUrl,
                    contentDescription = "شعار ${store.name}",
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                            )
                        )
                )
                if (store.featured) {
                    FeaturedBadge(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                    )
                }
                if (onFavoriteClick != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                        shape = CircleShape,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(48.dp),
                    ) {
                        IconButton(
                            onClick = onFavoriteClick,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("favorite_toggle_${store.id}"),
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isFavorite) "إزالة ${store.name} من المفضلة" else "حفظ ${store.name} في المفضلة",
                                tint = if (isFavorite) FavoriteHeartRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = store.category,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MarketplaceImage(
                        imageUrl = store.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape),
                    )
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = store.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = store.city,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "$productsCount منتجات",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                }

                RatingStarsBar(
                    rating = store.ratingAvg,
                    ratingCount = store.ratingCount,
                )
            }
        }
    }
}
