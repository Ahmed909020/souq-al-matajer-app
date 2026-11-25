package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGroceryStore
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StoreCategoryChipsBar(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onShowAllCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allChips = remember(categories) { listOf("الكل") + categories }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .testTag("store_category_chips_bar"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "التصنيفات (${categories.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(
                    onClick = onShowAllCategories,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("all_categories_button"),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Apps,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("عرض الـ ${categories.size} تصنيفاً")
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(allChips, key = { it }) { cat ->
                    val selected = selectedCategory == cat
                    val chipIcon = remember(cat) { categoryIconFor(cat) }
                    FilterChip(
                        selected = selected,
                        onClick = { onCategorySelected(cat) },
                        label = {
                            Text(
                                text = cat,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            )
                        },
                        leadingIcon = if (chipIcon != null) {
                            {
                                Icon(
                                    imageVector = chipIcon,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            labelColor = MaterialTheme.colorScheme.onSurface,
                        ),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("category_chip_$cat"),
                    )
                }
            }
        }
    }
}

private fun categoryIconFor(category: String): ImageVector? = when (category) {
    "الكل" -> Icons.Filled.Storefront
    "مطاعم" -> Icons.Filled.Restaurant
    "بقالة" -> Icons.Filled.LocalGroceryStore
    "ملابس" -> Icons.Filled.Checkroom
    "إلكترونيات" -> Icons.Filled.Devices
    "عطور" -> Icons.Filled.Spa
    "صيدليات" -> Icons.Filled.LocalPharmacy
    "مخابز", "حلويات" -> Icons.Filled.BakeryDining
    "أثاث" -> Icons.Filled.Weekend
    "مستلزمات أطفال" -> Icons.Filled.ChildCare
    "رياضة" -> Icons.Filled.FitnessCenter
    "سيارات" -> Icons.Filled.DirectionsCar
    "خدمات" -> Icons.Filled.HomeRepairService
    "مقاهي" -> Icons.Filled.LocalCafe
    else -> null
}
