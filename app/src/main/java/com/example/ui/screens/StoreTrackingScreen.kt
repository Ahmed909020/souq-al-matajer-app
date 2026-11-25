package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MarketplaceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreTrackingScreen(
    storeId: String,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
) {
    val ownerStores by viewModel.ownerStores.collectAsStateWithLifecycle()
    val ownerOrders by viewModel.ownerOrders.collectAsStateWithLifecycle()
    val store = remember(ownerStores, storeId) {
        ownerStores.firstOrNull { it.id == storeId }
    }
    val storeOrdersCount = remember(ownerOrders, storeId) {
        ownerOrders.count { it.storeId == storeId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تحليلات وتتبع المتجر") },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("track_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = store?.name ?: "تتبع المتجر",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "رمز التتبع: /track/$storeId",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TrackingMetricCard(
                    title = "إجمالي المشاهدات",
                    value = "${store?.viewsCount ?: 0}",
                    icon = Icons.Filled.Visibility,
                    accent = MaterialTheme.colorScheme.primary,
                    bg = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.weight(1f),
                )
                TrackingMetricCard(
                    title = "طلبات واتساب",
                    value = "$storeOrdersCount",
                    icon = Icons.Filled.ShoppingBag,
                    accent = MaterialTheme.colorScheme.secondary,
                    bg = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.weight(1f),
                )
            }

            TrackingMetricCard(
                title = "متوسط التقييم (${store?.ratingCount ?: 0} تقييم)",
                value = String.format(java.util.Locale.US, "%.1f / 5.0", store?.ratingAvg ?: 0.0),
                icon = Icons.Filled.Star,
                accent = MaterialTheme.colorScheme.secondary,
                bg = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun TrackingMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    bg: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(color = bg, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(38.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(8.dp),
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
