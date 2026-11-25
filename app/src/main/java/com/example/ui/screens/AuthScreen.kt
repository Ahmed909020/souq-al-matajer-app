package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.onGoogleSignInClicked

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.background,
                    )
                )
            )
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_marketplace_hero_1795621525560),
                contentDescription = "واجهة سوق المتاجر",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop,
            )

            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                shape = RoundedCornerShape(999.dp),
            ) {
                Text(
                    text = "المنصة العربية الأولى للمتاجر المحلية",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                )
            }

            Text(
                text = "سوق المتاجر",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "اكتشف عشرات المتاجر في مدينتك، اطلب مباشرة عبر واتساب، أو أضف متجرك وابدأ البيع مع تجربة مجانية 30 يوماً.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    FeatureRow(
                        icon = Icons.Filled.Storefront,
                        title = "50 تصنيفاً وتغطية شاملة للمدن",
                        subtitle = "مطاعم، عطور، إلكترونيات، ملابس، وأكثر",
                    )
                    FeatureRow(
                        icon = Icons.Filled.LocalOffer,
                        title = "طلب فوري عبر واتساب وكوبونات خصم",
                        subtitle = "أرسل تفاصيل طلبك مباشرة للتاجر دون عناء",
                    )
                    FeatureRow(
                        icon = Icons.Filled.Verified,
                        title = "لوحة متجر متكاملة للتجار",
                        subtitle = "إدارة المنتجات، المشاهدات، والتقييمات",
                    )
                }
            }

            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isLoading = false
                        },
                    )
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Login,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = "تسجيل الدخول عبر Google",
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
