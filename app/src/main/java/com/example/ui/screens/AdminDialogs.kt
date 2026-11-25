package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.Store
import com.example.model.Subscription
import com.example.model.SubscriptionPlan
import com.example.model.SubscriptionStatus
import com.example.util.MarketplaceIntents

@Composable
fun RejectRequestDialog(
    storeName: String,
    onDismiss: () -> Unit,
    onConfirmReject: (String) -> Unit,
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("رفض طلب متجر $storeName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("سيظهر سبب الرفض لصاحب المتجر في صفحته.")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب الرفض *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmReject(reason.trim()) },
                enabled = reason.trim().isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("تأكيد الرفض")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

@Composable
fun SubscriptionManageDialog(
    store: Store,
    existingSubscription: Subscription?,
    onDismiss: () -> Unit,
    onSave: (SubscriptionPlan, Double, String, Long, Long) -> Unit,
) {
    var selectedPlan by remember {
        mutableStateOf(
            SubscriptionPlan.fromId(existingSubscription?.plan ?: SubscriptionPlan.TRIAL_30.id)
        )
    }
    var priceText by remember {
        mutableStateOf((existingSubscription?.price ?: selectedPlan.defaultPrice).toString())
    }
    var selectedStatus by remember {
        mutableStateOf(existingSubscription?.status ?: store.subscriptionStatus)
    }
    var durationDaysText by remember {
        mutableStateOf(selectedPlan.durationDays.toString())
    }
    val startMillis = remember(existingSubscription) {
        existingSubscription?.startDateMillis?.takeIf { it > 0L } ?: System.currentTimeMillis()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدارة اشتراك ${store.name}") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("الخطة:", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SubscriptionPlan.entries.forEach { plan ->
                        FilterChip(
                            selected = selectedPlan == plan,
                            onClick = {
                                selectedPlan = plan
                                priceText = plan.defaultPrice.toString()
                                durationDaysText = plan.durationDays.toString()
                            },
                            label = { Text(plan.arabicLabel) },
                        )
                    }
                }

                Text("الحالة:", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SubscriptionStatus.entries.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st.id,
                            onClick = { selectedStatus = st.id },
                            label = { Text(st.arabicLabel) },
                        )
                    }
                }

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("السعر (ر.س)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = durationDaysText,
                    onValueChange = { durationDaysText = it },
                    label = { Text("مدة الاشتراك بالأيام من اليوم") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )

                Text(
                    text = "تاريخ البداية: ${MarketplaceIntents.formatArabicDate(startMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                val previewEnd = System.currentTimeMillis() +
                    (durationDaysText.toLongOrNull() ?: 30L).coerceAtLeast(1L) * 24L * 60 * 60 * 1000
                Text(
                    text = "تاريخ الانتهاء الجديد: ${MarketplaceIntents.formatArabicDate(previewEnd)}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            val price = priceText.toDoubleOrNull() ?: 0.0
            val days = (durationDaysText.toLongOrNull() ?: 30L).coerceAtLeast(1L)
            val endMillis = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000
            Button(
                onClick = { onSave(selectedPlan, price, selectedStatus, startMillis, endMillis) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("حفظ الاشتراك")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}

@Composable
fun SpotlightManageDialog(
    store: Store,
    onDismiss: () -> Unit,
    onSave: (Boolean, Double) -> Unit,
) {
    var featured by remember { mutableStateOf(store.featured) }
    var feeText by remember {
        mutableStateOf(if (store.spotlightFee > 0) store.spotlightFee.toString() else "150")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إدارة Spotlight — ${store.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("تفعيل متجر مميز Spotlight")
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }
                OutlinedTextField(
                    value = feeText,
                    onValueChange = { feeText = it },
                    label = { Text("رسوم التمييز الإضافية (ر.س)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            val fee = if (featured) (feeText.toDoubleOrNull() ?: 0.0) else 0.0
            Button(
                onClick = { onSave(featured, fee) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text("تطبيق")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}
