package com.example

import com.example.model.MarketplaceCatalog
import com.example.ui.MarketplaceViewModel
import com.example.util.MarketplaceIntents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun marketplaceCatalog_has50CategoriesAnd6SampleStores() {
        assertEquals(50, MarketplaceCatalog.defaultCategories.size)
        assertEquals(6, MarketplaceCatalog.sampleStores("owner_1").size)
        assertTrue(MarketplaceCatalog.sampleProducts("owner_1").isNotEmpty())
    }

    @Test
    fun whatsappMessage_includesStoreAndProductDetails() {
        val store = MarketplaceCatalog.sampleStores("owner_1").first()
        val product = MarketplaceCatalog.sampleProducts("owner_1").first { it.storeId == store.id }
        val message = MarketplaceIntents.buildProductWhatsAppMessage(store, product, quantity = 2)
        assertTrue(message.contains(store.name))
        assertTrue(message.contains(product.name))
        assertTrue(message.contains("2"))
    }

    @Test
    fun whatsappOrderUrl_generatesValidWaMeLinkWithOrderDetails() {
        val message = MarketplaceIntents.buildFullOrderWhatsAppMessage(
            storeName = "مطعم البيت الدمشقي",
            productName = "وجبة شاورما",
            quantity = 3,
            totalPrice = 105.0,
            customerName = "أحمد",
            customerPhone = "0501234567",
            customerAddress = "الرياض - حي العليا",
        )
        val url = MarketplaceIntents.buildWhatsAppOrderUrl("0551112233", message)
        assertNotNull(url)
        assertTrue(url!!.startsWith("https://wa.me/966551112233?text="))
        assertTrue(message.contains("أحمد"))
        assertTrue(message.contains("0501234567"))
        assertTrue(message.contains("الرياض - حي العليا"))
        assertTrue(message.contains("3"))
    }

    @Test
    fun searchHelper_matchesArabicStoresAndProductsWithNormalization() {
        assertTrue(MarketplaceViewModel.matchesQuery("أجهزة إلكترونية حديثة", "اجهزه"))
        assertTrue(MarketplaceViewModel.matchesQuery("وجبة شاورما عربي", "شاورما"))
    }
}
