package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.JoinRequest
import com.example.model.JoinRequestStatus
import com.example.model.Order
import com.example.model.OrderStatus

class MarketplaceNotificationManager(private val context: Context) {

    private val appContext: Context = context.applicationContext
    private val systemNotificationManager: NotificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        ensureChannelsCreated()
    }

    fun ensureChannelsCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val offersChannel = NotificationChannel(
                CHANNEL_OFFERS,
                "العروض والكوبونات الجديدة",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "تذكير بالعروض الترويجية وكوبونات الخصم المتاحة في المتاجر"
            }

            val ordersChannel = NotificationChannel(
                CHANNEL_ORDERS,
                "تحديثات حالة الطلبات",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "إشعارات فورية عند تحديث حالة طلباتك أو مراجعة طلب انضمام متجرك"
            }

            systemNotificationManager.createNotificationChannel(offersChannel)
            systemNotificationManager.createNotificationChannel(ordersChannel)
        }
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun notifyNewOffer(
        title: String,
        message: String,
        notificationId: Int = (System.currentTimeMillis() and 0x7FFFFFFF).toInt(),
    ): Boolean {
        return dispatchNotification(
            channelId = CHANNEL_OFFERS,
            notificationId = notificationId,
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_DEFAULT,
        )
    }

    fun notifyOrderStatusUpdate(order: Order): Boolean {
        val statusLabel = OrderStatus.fromId(order.status).arabicLabel
        val title = "تحديث حالة طلبك من ${order.storeName}"
        val message = "طلبك (${order.productName} × ${order.quantity}) أصبح الآن: $statusLabel"
        return dispatchNotification(
            channelId = CHANNEL_ORDERS,
            notificationId = ("order_${order.id}").hashCode(),
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_HIGH,
        )
    }

    fun notifyJoinRequestStatusUpdate(request: JoinRequest): Boolean {
        val statusLabel = JoinRequestStatus.fromId(request.status).arabicLabel
        val title = "تحديث طلب انضمام ${request.storeName}"
        val message = if (request.status == JoinRequestStatus.REJECTED.id && !request.rejectionReason.isNullOrBlank()) {
            "الحالة: $statusLabel — السبب: ${request.rejectionReason}"
        } else {
            "حالة طلب انضمام متجرك أصبحت الآن: $statusLabel"
        }
        return dispatchNotification(
            channelId = CHANNEL_ORDERS,
            notificationId = ("join_${request.id}").hashCode(),
            title = title,
            message = message,
            priority = NotificationCompat.PRIORITY_HIGH,
        )
    }

    private fun dispatchNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        priority: Int,
    ): Boolean {
        if (!hasNotificationPermission()) return false
        ensureChannelsCreated()

        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_notification_store)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            systemNotificationManager.notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        const val CHANNEL_OFFERS = "souq_offers_channel"
        const val CHANNEL_ORDERS = "souq_orders_channel"
    }
}
