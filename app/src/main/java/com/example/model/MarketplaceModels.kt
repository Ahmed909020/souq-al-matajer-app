package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SubscriptionPlan(
    val id: String,
    val arabicLabel: String,
    val durationDays: Long,
    val defaultPrice: Double,
) {
    TRIAL_30("trial_30", "تجريبية 30 يوم", 30L, 0.0),
    MONTHLY("monthly", "شهري", 30L, 99.0),
    YEARLY("yearly", "سنوي", 365L, 990.0);

    companion object {
        fun fromId(raw: String): SubscriptionPlan =
            entries.firstOrNull { it.id == raw } ?: TRIAL_30
    }
}

enum class SubscriptionStatus(val id: String, val arabicLabel: String) {
    ACTIVE("active", "نشط"),
    PAUSED("paused", "متوقف"),
    EXPIRED("expired", "منتهي");

    companion object {
        fun fromId(raw: String): SubscriptionStatus =
            entries.firstOrNull { it.id == raw } ?: ACTIVE
    }
}

enum class JoinRequestStatus(val id: String, val arabicLabel: String) {
    PENDING("pending", "قيد المراجعة"),
    APPROVED("approved", "مقبول"),
    REJECTED("rejected", "مرفوض");

    companion object {
        fun fromId(raw: String): JoinRequestStatus =
            entries.firstOrNull { it.id == raw } ?: PENDING
    }
}

enum class OrderStatus(val id: String, val arabicLabel: String) {
    SENT_WHATSAPP("sent_whatsapp", "مرسل عبر واتساب"),
    CONFIRMED("confirmed", "تم تأكيد الطلب"),
    COMPLETED("completed", "مكتمل");

    companion object {
        fun fromId(raw: String): OrderStatus =
            entries.firstOrNull { it.id == raw } ?: SENT_WHATSAPP
    }
}

@Keep
@IgnoreExtraProperties
data class Store(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val name: String = "",
    val description: String = "",
    val address: String = "",
    val city: String = "",
    val category: String = "",
    val logoUrl: String = "",
    val whatsappPhone: String = "",
    val active: Boolean = true,
    val featured: Boolean = false,
    val spotlightFee: Double = 0.0,
    val ratingAvg: Double = 0.0,
    val ratingCount: Long = 0L,
    val viewsCount: Long = 0L,
    val productsCount: Long = 0L,
    val planName: String = "تجريبية 30 يوم",
    val subscriptionStatus: String = "active",
    val subscriptionEndMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    fun isSubscriptionValid(nowMillis: Long = System.currentTimeMillis()): Boolean =
        active && subscriptionStatus == "active" && (subscriptionEndMillis == 0L || subscriptionEndMillis > nowMillis)
}

@Keep
@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val storeOwnerId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val productId: String = "",
    val productName: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Long = 1L,
    val couponCode: String? = null,
    val discountPercent: Long = 0L,
    val totalPrice: Double = 0.0,
    val status: String = "sent_whatsapp",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Review(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Long = 5L,
    val comment: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Subscription(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val ownerId: String = "",
    val plan: String = "trial_30",
    val price: Double = 0.0,
    val status: String = "active",
    val startDateMillis: Long = 0L,
    val endDateMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class Coupon(
    @DocumentId val id: String = "",
    val storeId: String = "",
    val ownerId: String = "",
    val code: String = "",
    val discountPercent: Long = 10L,
    val expiryMillis: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class JoinRequest(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerEmail: String = "",
    val storeName: String = "",
    val ownerName: String = "",
    val phone: String = "",
    val city: String = "",
    val category: String = "",
    val address: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val status: String = "pending",
    val rejectionReason: String? = null,
    val approvedStoreId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class MarketplaceCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val sortOrder: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class FavoriteStore(
    @DocumentId val id: String = "",
    val userId: String = "",
    val storeId: String = "",
    val storeName: String = "",
    val storeCategory: String = "",
    val storeCity: String = "",
    val storeLogoUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
)

enum class AlertCategory {
    OFFER,
    ORDER_UPDATE,
}

data class MarketplaceAlert(
    val id: String,
    val title: String,
    val message: String,
    val category: AlertCategory,
    val timestampMillis: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
)
