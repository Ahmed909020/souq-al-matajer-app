package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.JoinRequest
import com.example.model.JoinRequestStatus
import com.example.model.MarketplaceCatalog
import com.example.ui.MarketplaceViewModel
import com.example.ui.components.MarketplaceImage
import com.example.util.MarketplaceIntents

@Composable
fun AddStoreScreen(
    viewModel: MarketplaceViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val ownerRequests by viewModel.ownerJoinRequests.collectAsStateWithLifecycle()
    val categories by viewModel.categoriesState.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var storeName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf(viewModel.userName) }
    var phone by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf(MarketplaceCatalog.cities.first()) }
    var selectedCategory by remember(categories) {
        mutableStateOf(categories.firstOrNull() ?: "مطاعم")
    }
    var address by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var logoUrl by remember { mutableStateOf("") }
    var cityDropdownExpanded by remember { mutableStateOf(false) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var lastSubmittedRequest by remember { mutableStateOf<JoinRequest?>(null) }

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            MarketplaceIntents.encodeImageUriToCompactDataUri(context, uri)?.let { encoded ->
                logoUrl = encoded
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "أضف متجرك إلى سوق المتاجر",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "أرسل بيانات متجرك للمراجعة، وعند القبول يتم تفعيله فوراً بباقة تجريبية مجانية لمدة 30 يوماً.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Existing Join Requests Status
        if (ownerRequests.isNotEmpty()) {
            Text(
                text = "حالة طلباتك السابقة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            ownerRequests.forEach { req ->
                JoinRequestStatusCard(
                    request = req,
                    onNotifyAdminWhatsApp = {
                        val msg = "مرحباً إدارة سوق المتاجر، أرسلت طلب انضمام لمتجر (${req.storeName}) في مدينة ${req.city} ضمن تصنيف ${req.category}."
                        MarketplaceIntents.openWhatsApp(context, MarketplaceCatalog.ADMIN_WHATSAPP_NUMBER, msg)
                    },
                )
            }
        }

        if (lastSubmittedRequest != null) {
            val req = lastSubmittedRequest!!
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "تم إرسال طلبك بنجاح وهو الآن قيد المراجعة",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedButton(
                        onClick = {
                            val msg = "مرحباً إدارة سوق المتاجر، قمت بتسجيل طلب متجر جديد: ${req.storeName} (${req.city} - ${req.category}). رقم التواصل: ${req.phone}"
                            MarketplaceIntents.openWhatsApp(context, MarketplaceCatalog.ADMIN_WHATSAPP_NUMBER, msg)
                        },
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إشعار الإدارة عبر واتساب", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Registration Form Card
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
                    text = "نموذج تسجيل المتجر",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                // Logo preview + Photo Picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MarketplaceImage(
                        imageUrl = logoUrl,
                        contentDescription = "معاينة شعار المتجر",
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(14.dp)),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedButton(
                            onClick = {
                                logoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pick_store_logo_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("اختيار شعار من المعرض")
                        }
                    }
                }

                OutlinedTextField(
                    value = if (logoUrl.startsWith("data:")) "" else logoUrl,
                    onValueChange = { logoUrl = it },
                    label = { Text("أو أدخل رابط الشعار (اختياري)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("اسم المتجر *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_store_name_input"),
                )

                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("اسم المالك *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_store_owner_input"),
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الهاتف / واتساب *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_store_phone_input"),
                )

                // City dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCity,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المدينة *") },
                        trailingIcon = {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { cityDropdownExpanded = true },
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { cityDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = cityDropdownExpanded,
                        onDismissRequest = { cityDropdownExpanded = false },
                    ) {
                        MarketplaceCatalog.cities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city) },
                                onClick = {
                                    selectedCity = city
                                    cityDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                // Category dropdown (50 categories)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("التصنيف *") },
                        trailingIcon = {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { categoryDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان التفصيلي *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_store_address_input"),
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("وصف المتجر والخدمات *") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_store_description_input"),
                )

                val formValid = storeName.trim().length >= 2 &&
                    ownerName.trim().length >= 2 &&
                    phone.trim().length >= 6 &&
                    address.trim().isNotEmpty() &&
                    description.trim().isNotEmpty()

                Button(
                    onClick = {
                        val req = JoinRequest(
                            storeName = storeName.trim(),
                            ownerName = ownerName.trim(),
                            phone = phone.trim(),
                            city = selectedCity,
                            category = selectedCategory,
                            address = address.trim(),
                            description = description.trim(),
                            logoUrl = logoUrl.trim(),
                        )
                        viewModel.submitJoinRequest(req) {
                            lastSubmittedRequest = req
                            storeName = ""
                            phone = ""
                            address = ""
                            description = ""
                            logoUrl = ""
                        }
                    },
                    enabled = formValid && !isBusy,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_join_request_button"),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال طلب الانضمام للمراجعة")
                }
            }
        }
    }
}

@Composable
private fun JoinRequestStatusCard(
    request: JoinRequest,
    onNotifyAdminWhatsApp: () -> Unit,
) {
    val statusEnum = JoinRequestStatus.fromId(request.status)
    val (bgColor, fgColor, icon) = when (statusEnum) {
        JoinRequestStatus.PENDING -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Filled.HourglassTop,
        )
        JoinRequestStatus.APPROVED -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Filled.CheckCircle,
        )
        JoinRequestStatus.REJECTED -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Filled.Error,
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, fgColor.copy(alpha = 0.4f)),
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
                Text(
                    text = request.storeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Surface(
                    color = bgColor,
                    contentColor = fgColor,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text(
                            text = statusEnum.arabicLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Text(
                text = "${request.city} • ${request.category}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (statusEnum == JoinRequestStatus.REJECTED && !request.rejectionReason.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "سبب الرفض: ${request.rejectionReason}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(10.dp),
                    )
                }
            }
            if (statusEnum == JoinRequestStatus.PENDING) {
                OutlinedButton(
                    onClick = onNotifyAdminWhatsApp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("متابعة الطلب عبر واتساب الإدارة")
                }
            }
        }
    }
}
