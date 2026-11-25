package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MarketplaceLoadingState(
    message: String = "جاري جلب بيانات المتاجر والمنتجات...",
    subtitle: String = "يرجى الانتظار لحظات أثناء الاتصال بخوادم Firebase",
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("marketplace_loading_indicator"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                shape = CircleShape,
                modifier = Modifier.size(68.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface,
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun FirebaseConnectionErrorState(
    title: String = "تعذر الاتصال بخوادم Firebase",
    message: String = "يرجى التحقق من اتصالك بالإنترنت ثم الضغط على إعادة المحاولة.",
    isRetrying: Boolean = false,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("firebase_connection_error_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = "فشل الاتصال بالخادم",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onRetry,
                enabled = !isRetrying,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("retry_firebase_button"),
            ) {
                if (isRetrying) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("جاري إعادة المحاولة...")
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعادة المحاولة")
                }
            }
        }
    }
}

@Composable
fun OfflineStatusBanner(
    message: String,
    isRetrying: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        modifier = modifier
            .fillMaxWidth()
            .testTag("offline_status_banner"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.WifiOff,
                contentDescription = "تنبيه عدم الاتصال بالإنترنت",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onRetry,
                enabled = !isRetrying,
                modifier = Modifier.testTag("offline_banner_retry_button"),
            ) {
                if (isRetrying) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Text(
                        text = "إعادة المحاولة",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
