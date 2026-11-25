package com.example

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.example.model.FavoriteStore
import com.example.model.OrderStatus
import com.example.ui.MarketplaceViewModel
import com.example.ui.theme.SouqDarkColorScheme
import com.example.ui.theme.SouqLightColorScheme
import com.example.util.MarketplaceNotificationManager
import com.google.firebase.firestore.FirebaseFirestoreException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("سوق المتاجر", appName)
  }

  @Test
  fun `firebase unavailable and io exceptions map to retryable Arabic message`() {
    val unavailable = FirebaseFirestoreException(
      "Unavailable",
      FirebaseFirestoreException.Code.UNAVAILABLE
    )
    val msg = MarketplaceViewModel.formatFirebaseErrorMessage(unavailable)
    val ioMsg = MarketplaceViewModel.formatFirebaseErrorMessage(IOException("Offline"))
    assertTrue(msg.contains("Firebase"))
    assertTrue(msg.contains("إعادة المحاولة"))
    assertTrue(ioMsg.contains("Firebase"))
    assertTrue(ioMsg.contains("إعادة المحاولة"))
  }

  @Test
  fun `notification manager registers offers and orders channels`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    MarketplaceNotificationManager(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      assertNotNull(manager.getNotificationChannel(MarketplaceNotificationManager.CHANNEL_OFFERS))
      assertNotNull(manager.getNotificationChannel(MarketplaceNotificationManager.CHANNEL_ORDERS))
    }
  }

  @Test
  fun `dark and light color schemes have distinct adaptive surfaces`() {
    assertNotEquals(SouqLightColorScheme.background, SouqDarkColorScheme.background)
    assertNotEquals(SouqLightColorScheme.surface, SouqDarkColorScheme.surface)
    assertNotEquals(SouqLightColorScheme.onSurface, SouqDarkColorScheme.onSurface)
  }

  @Test
  fun `order status and favorite store defaults are safe`() {
    assertEquals(OrderStatus.CONFIRMED, OrderStatus.fromId("confirmed"))
    assertEquals(OrderStatus.SENT_WHATSAPP, OrderStatus.fromId("unknown_status"))
    val fav = FavoriteStore(userId = "u1", storeId = "s1", storeName = "متجر النور")
    assertEquals("s1", fav.storeId)
    assertEquals("متجر النور", fav.storeName)
  }
}
