package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.model.Product
import com.example.model.Store
import com.example.model.Subscription
import com.example.model.SubscriptionPlan
import com.example.model.SubscriptionStatus
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MarketplaceIntents {

    fun buildProductWhatsAppMessage(
        store: Store,
        product: Product,
        quantity: Int = 1,
    ): String = buildString {
        append("مرحباً ${store.name}،\n")
        append("أرغب في شراء المنتج التالي من متجركم عبر تطبيق سوق المتاجر:\n")
        append("• المنتج: ${product.name}\n")
        append("• السعر: ${formatPrice(product.price)}\n")
        append("• الكمية: $quantity\n")
        append("• الإجمالي التقديري: ${formatPrice(product.price * quantity)}\n")
        if (product.description.isNotBlank()) {
            append("• التفاصيل: ${product.description}\n")
        }
        append("يرجى تأكيد التوفر وطريقة التوصيل.")
    }

    fun buildFullOrderWhatsAppMessage(
        storeName: String,
        productName: String,
        quantity: Int,
        totalPrice: Double,
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        couponCode: String? = null,
        discountPercent: Int = 0,
    ): String = buildString {
        append("مرحباً $storeName، أرغب بطلب جديد من تطبيق سوق المتاجر:\n")
        append("• المنتج: $productName\n")
        append("• الكمية: $quantity\n")
        if (!couponCode.isNullOrBlank() && discountPercent > 0) {
            append("• كوبون الخصم: $couponCode (-$discountPercent%)\n")
        }
        append("• الإجمالي النهائي: ${formatPrice(totalPrice)}\n")
        append("• الاسم: ${customerName.trim()}\n")
        append("• رقم الهاتف: ${customerPhone.trim()}\n")
        append("• العنوان: ${customerAddress.trim()}")
    }

    fun normalizePhoneForWhatsApp(rawPhone: String): String? {
        val digits = rawPhone.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        return if (digits.startsWith("05") && digits.length == 10) {
            "966" + digits.drop(1)
        } else {
            digits
        }
    }

    fun buildWhatsAppOrderUrl(rawPhone: String, message: String): String? {
        val normalized = normalizePhoneForWhatsApp(rawPhone) ?: return null
        val encoded = URLEncoder.encode(message, "UTF-8").replace("+", "%20")
        return "https://wa.me/$normalized?text=$encoded"
    }

    fun sendProductOrderViaWhatsApp(
        context: Context,
        store: Store,
        product: Product,
        quantity: Int = 1,
    ) {
        val message = buildProductWhatsAppMessage(store, product, quantity)
        openWhatsApp(context, store.whatsappPhone, message)
    }

    fun openWhatsApp(context: Context, rawPhone: String, message: String) {
        val url = buildWhatsAppOrderUrl(rawPhone, message)
        if (url == null) {
            Toast.makeText(context, "رقم الواتساب غير صالح", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح واتساب على هذا الجهاز", Toast.LENGTH_SHORT).show()
        }
    }

    fun dialPhone(context: Context, rawPhone: String) {
        val cleaned = rawPhone.filter { it.isDigit() || it == '+' }
        if (cleaned.isEmpty()) {
            Toast.makeText(context, "رقم الهاتف غير متوفر", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleaned")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
        }
    }

    fun encodeImageUriToCompactDataUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val original = BitmapFactory.decodeStream(stream) ?: return null
                val maxDim = 320
                val scale = minOf(
                    maxDim.toFloat() / original.width.coerceAtLeast(1),
                    maxDim.toFloat() / original.height.coerceAtLeast(1),
                    1f
                )
                val targetW = (original.width * scale).toInt().coerceAtLeast(1)
                val targetH = (original.height * scale).toInt().coerceAtLeast(1)
                val resized = if (scale < 1f) {
                    Bitmap.createScaledBitmap(original, targetW, targetH, true)
                } else {
                    original
                }
                val out = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 72, out)
                if (resized !== original) resized.recycle()
                original.recycle()
                val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                "data:image/jpeg;base64,$base64"
            }
        } catch (_: Exception) {
            null
        }
    }

    fun printMonthlyReport(
        context: Context,
        stores: List<Store>,
        subscriptions: List<Subscription>,
    ) {
        val now = System.currentTimeMillis()
        val sevenDaysLater = now + 7L * 24 * 60 * 60 * 1000
        val totalRevenue = subscriptions.sumOf { it.price } +
            stores.filter { it.featured }.sumOf { it.spotlightFee }
        val activeStoresCount = stores.count { it.active }
        val endingSoon = subscriptions.filter {
            it.status == "active" && it.endDateMillis in now..sevenDaysLater
        }

        val rowsHtml = subscriptions.joinToString("\n") { sub ->
            val planLabel = SubscriptionPlan.fromId(sub.plan).arabicLabel
            val statusLabel = SubscriptionStatus.fromId(sub.status).arabicLabel
            val endStr = formatArabicDate(sub.endDateMillis)
            """
            <tr>
              <td>${escapeHtml(sub.storeName)}</td>
              <td>${escapeHtml(planLabel)}</td>
              <td>${formatPrice(sub.price)}</td>
              <td>${escapeHtml(statusLabel)}</td>
              <td>${escapeHtml(endStr)}</td>
            </tr>
            """.trimIndent()
        }

        val endingSoonHtml = if (endingSoon.isEmpty()) {
            "<p>لا توجد اشتراكات تنتهي خلال الأيام السبعة القادمة.</p>"
        } else {
            "<ul>" + endingSoon.joinToString("") {
                "<li>${escapeHtml(it.storeName)} — ينتهي في ${escapeHtml(formatArabicDate(it.endDateMillis))}</li>"
            } + "</ul>"
        }

        val html = """
        <!DOCTYPE html>
        <html dir="rtl" lang="ar">
        <head>
          <meta charset="UTF-8" />
          <style>
            body { font-family: sans-serif; color: #141E1A; padding: 24px; direction: rtl; }
            h1 { color: #0D7B4E; border-bottom: 2px solid #0D7B4E; padding-bottom: 8px; }
            .kpi-row { display: flex; gap: 12px; margin: 16px 0; }
            .kpi { flex: 1; border: 1px solid #E0EAE4; border-radius: 10px; padding: 12px; background: #F7FAF8; }
            .kpi-title { font-size: 12px; color: #54635C; }
            .kpi-val { font-size: 20px; font-weight: bold; color: #0D7B4E; margin-top: 4px; }
            table { width: 100%; border-collapse: collapse; margin-top: 16px; }
            th, td { border: 1px solid #D9E4DE; padding: 8px 10px; text-align: right; font-size: 13px; }
            th { background-color: #0D7B4E; color: #ffffff; }
            tr:nth-child(even) { background-color: #F7FAF8; }
          </style>
        </head>
        <body>
          <h1>التقرير الشهري — سوق المتاجر</h1>
          <p>تاريخ التقرير: ${escapeHtml(formatArabicDate(now))}</p>
          <div class="kpi-row">
            <div class="kpi"><div class="kpi-title">إجمالي المتاجر</div><div class="kpi-val">${stores.size}</div></div>
            <div class="kpi"><div class="kpi-title">المتاجر النشطة</div><div class="kpi-val">$activeStoresCount</div></div>
            <div class="kpi"><div class="kpi-title">إجمالي الإيرادات</div><div class="kpi-val">${formatPrice(totalRevenue)}</div></div>
          </div>
          <h3>الاشتراكات القريبة من الانتهاء (خلال 7 أيام)</h3>
          $endingSoonHtml
          <h3>تفاصيل الاشتراكات</h3>
          <table>
            <thead>
              <tr>
                <th>المتجر</th>
                <th>الخطة</th>
                <th>السعر</th>
                <th>الحالة</th>
                <th>تاريخ الانتهاء</th>
              </tr>
            </thead>
            <tbody>
              $rowsHtml
            </tbody>
          </table>
        </body>
        </html>
        """.trimIndent()

        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                if (printManager != null) {
                    val adapter = view.createPrintDocumentAdapter("SouqAlMatager_Monthly_Report")
                    printManager.print(
                        "التقرير الشهري - سوق المتاجر",
                        adapter,
                        PrintAttributes.Builder().build()
                    )
                }
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    fun formatArabicDate(millis: Long): String {
        if (millis <= 0L) return "غير محدد"
        val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.forLanguageTag("ar"))
        return sdf.format(Date(millis))
    }

    fun formatPrice(amount: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 0
        }
        return "${formatter.format(amount)} ر.س"
    }

    private fun escapeHtml(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
