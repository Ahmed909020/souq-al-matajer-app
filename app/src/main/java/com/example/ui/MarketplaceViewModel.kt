package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MarketplaceRepository
import com.example.data.NetworkConnectivityObserver
import com.example.model.AlertCategory
import com.example.model.Coupon
import com.example.model.FavoriteStore
import com.example.model.JoinRequest
import com.example.model.JoinRequestStatus
import com.example.model.MarketplaceAlert
import com.example.model.MarketplaceCatalog
import com.example.model.MarketplaceCategory
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.Review
import com.example.model.Store
import com.example.model.Subscription
import com.example.model.SubscriptionPlan
import com.example.model.UiState
import com.example.util.MarketplaceNotificationManager
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class MarketplaceViewModel(
    private val repository: MarketplaceRepository,
    val currentUserId: String,
    val userEmail: String,
    val userName: String,
    private val networkObserver: NetworkConnectivityObserver? = null,
    private val notificationManager: MarketplaceNotificationManager? = null,
) : ViewModel() {

    val isAdmin: Boolean = userEmail.equals(MarketplaceCatalog.ADMIN_EMAIL, ignoreCase = true)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("الكل")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedCity = MutableStateFlow("الكل")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    private val _selectedStoreId = MutableStateFlow<String?>(null)

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events = _events.asSharedFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _retryTrigger = MutableStateFlow(0)

    val isOnline: StateFlow<Boolean> = networkObserver?.isOnline ?: MutableStateFlow(true)

    private val _connectionError = MutableStateFlow<String?>(null)
    val connectionError: StateFlow<String?> = _connectionError.asStateFlow()

    private val _isRetryingConnection = MutableStateFlow(false)
    val isRetryingConnection: StateFlow<Boolean> = _isRetryingConnection.asStateFlow()

    private val _inAppAlerts = MutableStateFlow<List<MarketplaceAlert>>(emptyList())
    val inAppAlerts: StateFlow<List<MarketplaceAlert>> = _inAppAlerts.asStateFlow()

    val unreadAlertCount: StateFlow<Int> = _inAppAlerts
        .map { alerts -> alerts.count { !it.isRead } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), 0)

    // Public active stores (with 6 sample stores shown when Firestore is not yet seeded)
    val storesState: StateFlow<UiState<List<Store>>> = _retryTrigger
        .flatMapLatest {
            repository.observeActiveStores()
                .map<List<Store>, UiState<List<Store>>> { liveStores ->
                    val now = System.currentTimeMillis()
                    val validLive = liveStores.filter { it.isSubscriptionValid(now) }
                    val result = if (liveStores.isEmpty()) {
                        MarketplaceCatalog.sampleStores(currentUserId)
                    } else {
                        validLive
                    }
                    UiState.Success(
                        result.sortedWith(
                            compareByDescending<Store> { it.featured }
                                .thenByDescending { it.ratingAvg }
                        )
                    )
                }
                .onStart { emit(UiState.Loading) }
                .catchFirestoreError("stores")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val activeProductsState: StateFlow<UiState<List<Product>>> = _retryTrigger
        .flatMapLatest {
            repository.observeActiveProducts()
                .map<List<Product>, UiState<List<Product>>> { liveProducts ->
                    val result = liveProducts.ifEmpty { MarketplaceCatalog.sampleProducts(currentUserId) }
                    UiState.Success(result)
                }
                .onStart { emit(UiState.Loading) }
                .catchFirestoreError("products")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), UiState.Loading)

    val categoriesState: StateFlow<List<String>> = repository.observeActiveCategories()
        .map { live ->
            if (live.isEmpty()) {
                MarketplaceCatalog.defaultCategories
            } else {
                live.sortedBy { it.sortOrder }.map { it.name }
            }
        }
        .catch { error ->
            if (error is CancellationException) throw error
            emit(MarketplaceCatalog.defaultCategories)
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT),
            MarketplaceCatalog.defaultCategories
        )

    // Filtered marketplace stores (search by store name OR product name, category chip, city)
    val filteredStores: StateFlow<List<Store>> = combine(
        storesState,
        activeProductsState,
        _searchQuery,
        _selectedCategory,
        _selectedCity,
    ) { storesUi, productsUi, query, category, city ->
        val stores = (storesUi as? UiState.Success)?.data ?: emptyList()
        val products = (productsUi as? UiState.Success)?.data ?: emptyList()
        val q = query.trim()

        val matchingStoreIdsFromProducts = if (q.isEmpty()) {
            emptySet()
        } else {
            products.filter {
                matchesQuery(it.name, q) || matchesQuery(it.description, q)
            }.map { it.storeId }.toSet()
        }

        stores.filter { store ->
            val matchesCategory = category == "الكل" || store.category == category
            val matchesCity = city == "الكل" || store.city == city
            val matchesSearch = q.isEmpty() ||
                matchesQuery(store.name, q) ||
                matchesQuery(store.description, q) ||
                matchesQuery(store.category, q) ||
                store.id in matchingStoreIdsFromProducts
            matchesCategory && matchesCity && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    // Matching products for direct product discovery from the home search bar
    val filteredSearchProducts: StateFlow<List<Product>> = combine(
        storesState,
        activeProductsState,
        _searchQuery,
        _selectedCategory,
        _selectedCity,
    ) { storesUi, productsUi, query, category, city ->
        val q = query.trim()
        if (q.isEmpty()) return@combine emptyList()
        val stores = (storesUi as? UiState.Success)?.data ?: emptyList()
        val visibleStoreIds = stores.filter { store ->
            (category == "الكل" || store.category == category) &&
                (city == "الكل" || store.city == city)
        }.map { it.id }.toSet()

        val products = (productsUi as? UiState.Success)?.data ?: emptyList()
        products.filter { product ->
            product.storeId in visibleStoreIds &&
                (matchesQuery(product.name, q) || matchesQuery(product.description, q))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    // Selected store reviews & coupons
    val selectedStoreReviews: StateFlow<List<Review>> = _selectedStoreId
        .flatMapLatest { storeId ->
            if (storeId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeStoreReviews(storeId).map { live ->
                    live.ifEmpty { MarketplaceCatalog.sampleReviews().filter { it.storeId == storeId } }
                }.catch { error ->
                    if (error is CancellationException) throw error
                    emit(MarketplaceCatalog.sampleReviews().filter { it.storeId == storeId })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val selectedStoreCoupons: StateFlow<List<Coupon>> = _selectedStoreId
        .flatMapLatest { storeId ->
            if (storeId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                repository.observeStoreCoupons(storeId).map { live ->
                    live.ifEmpty { MarketplaceCatalog.sampleCoupons(currentUserId).filter { it.storeId == storeId } }
                }.catch { error ->
                    if (error is CancellationException) throw error
                    emit(MarketplaceCatalog.sampleCoupons(currentUserId).filter { it.storeId == storeId })
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    // Owner streams
    val ownerStores: StateFlow<List<Store>> = repository.observeOwnerStores(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val ownerProducts: StateFlow<List<Product>> = repository.observeOwnerProducts(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val ownerJoinRequests: StateFlow<List<JoinRequest>> = repository.observeOwnerJoinRequests(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val ownerSubscriptions: StateFlow<List<Subscription>> = repository.observeOwnerSubscriptions(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val ownerCoupons: StateFlow<List<Coupon>> = repository.observeOwnerCoupons(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val ownerOrders: StateFlow<List<Order>> = repository.observeOwnerOrders(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            emit(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    // User Favorites, Customer Orders & Active Offers/Coupons
    val favoriteStores: StateFlow<List<FavoriteStore>> = _retryTrigger
        .flatMapLatest {
            repository.observeUserFavorites(currentUserId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val favoriteStoreIds: StateFlow<Set<String>> = favoriteStores
        .map { list -> list.map { it.storeId }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptySet())

    val customerOrders: StateFlow<List<Order>> = _retryTrigger
        .flatMapLatest {
            repository.observeCustomerOrders(currentUserId)
                .catch { error ->
                    if (error is CancellationException) throw error
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val activeCoupons: StateFlow<List<Coupon>> = _retryTrigger
        .flatMapLatest {
            repository.observeActiveCoupons()
                .map { live ->
                    live.ifEmpty { MarketplaceCatalog.sampleCoupons(currentUserId) }
                }
                .catch { error ->
                    if (error is CancellationException) throw error
                    emit(MarketplaceCatalog.sampleCoupons(currentUserId))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    // Admin streams (active only when isAdmin == true)
    val adminAllStores: StateFlow<List<Store>> = if (isAdmin) {
        repository.observeAllStoresAdmin()
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val adminAllJoinRequests: StateFlow<List<JoinRequest>> = if (isAdmin) {
        repository.observeAllJoinRequestsAdmin()
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val adminAllSubscriptions: StateFlow<List<Subscription>> = if (isAdmin) {
        repository.observeAllSubscriptionsAdmin()
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val adminAllCoupons: StateFlow<List<Coupon>> = if (isAdmin) {
        repository.observeAllCouponsAdmin()
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val adminAllCategories: StateFlow<List<MarketplaceCategory>> = if (isAdmin) {
        repository.observeAllCategoriesAdmin()
            .catch { error ->
                if (error is CancellationException) throw error
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    init {
        viewModelScope.launch {
            isOnline.collect { online ->
                if (!online) {
                    _connectionError.value = "لا يوجد اتصال بالإنترنت. تعذر الوصول إلى خوادم Firebase."
                } else if (_connectionError.value != null) {
                    checkServerConnectivity(showFeedbackToast = false)
                }
            }
        }
        viewModelScope.launch {
            checkServerConnectivity(showFeedbackToast = false)
        }
        if (isAdmin) {
            viewModelScope.launch {
                repository.seedSampleMarketplaceIfEmpty()
            }
        }
        startLocalNotificationMonitors()
    }

    private fun startLocalNotificationMonitors() {
        // 1. Monitor customer order status transitions
        viewModelScope.launch {
            var knownOrderStatuses: Map<String, String>? = null
            customerOrders.collect { orders ->
                if (knownOrderStatuses == null) {
                    if (orders.isNotEmpty()) {
                        knownOrderStatuses = orders.associate { it.id to it.status }
                        val latest = orders.first()
                        prependAlert(
                            MarketplaceAlert(
                                id = "order_init_${latest.id}",
                                title = "متابعة طلبك: ${latest.productName}",
                                message = "الحالة الحالية لدى ${latest.storeName}: ${OrderStatus.fromId(latest.status).arabicLabel}",
                                category = AlertCategory.ORDER_UPDATE,
                                isRead = true,
                            )
                        )
                    }
                    return@collect
                }
                val prev = knownOrderStatuses!!
                orders.forEach { order ->
                    val oldStatus = prev[order.id]
                    if (oldStatus != null && oldStatus != order.status) {
                        notificationManager?.notifyOrderStatusUpdate(order)
                        val label = OrderStatus.fromId(order.status).arabicLabel
                        prependAlert(
                            MarketplaceAlert(
                                id = "order_${order.id}_${order.status}",
                                title = "تحديث حالة طلبك من ${order.storeName}",
                                message = "طلبك (${order.productName}) أصبح الآن: $label",
                                category = AlertCategory.ORDER_UPDATE,
                            )
                        )
                        _events.tryEmit("تحديث طلبك (${order.productName}): $label")
                    }
                }
                knownOrderStatuses = orders.associate { it.id to it.status }
            }
        }

        // 2. Monitor store owner's join request status transitions
        viewModelScope.launch {
            var knownRequestStatuses: Map<String, String>? = null
            ownerJoinRequests.collect { requests ->
                if (knownRequestStatuses == null) {
                    if (requests.isNotEmpty()) {
                        knownRequestStatuses = requests.associate { it.id to it.status }
                    }
                    return@collect
                }
                val prev = knownRequestStatuses!!
                requests.forEach { req ->
                    val oldStatus = prev[req.id]
                    if (oldStatus != null && oldStatus != req.status) {
                        notificationManager?.notifyJoinRequestStatusUpdate(req)
                        val label = JoinRequestStatus.fromId(req.status).arabicLabel
                        prependAlert(
                            MarketplaceAlert(
                                id = "join_${req.id}_${req.status}",
                                title = "تحديث طلب متجر ${req.storeName}",
                                message = "حالة طلب الانضمام أصبحت: $label",
                                category = AlertCategory.ORDER_UPDATE,
                            )
                        )
                    }
                }
                knownRequestStatuses = requests.associate { it.id to it.status }
            }
        }

        // 3. Monitor new active offers/coupons
        viewModelScope.launch {
            var knownCouponIds: Set<String>? = null
            activeCoupons.collect { coupons ->
                val validCoupons = coupons.filter {
                    it.active && it.expiryMillis > System.currentTimeMillis()
                }
                if (knownCouponIds == null) {
                    if (validCoupons.isNotEmpty()) {
                        knownCouponIds = validCoupons.map { it.id }.toSet()
                        val featuredCoupon = validCoupons.first()
                        prependAlert(
                            MarketplaceAlert(
                                id = "offer_seed_${featuredCoupon.id}",
                                title = "عرض خصم متاح الآن (${featuredCoupon.discountPercent}%)",
                                message = "استخدم الكود ${featuredCoupon.code} عند الطلب عبر واتساب",
                                category = AlertCategory.OFFER,
                                isRead = true,
                            )
                        )
                    }
                    return@collect
                }
                val prev = knownCouponIds!!
                validCoupons.filter { it.id !in prev }.forEach { coupon ->
                    val title = "عرض جديد: خصم ${coupon.discountPercent}%"
                    val msg = "تم تفعيل كوبون ${coupon.code} — استفد منه قبل انتهاء العرض!"
                    notificationManager?.notifyNewOffer(title, msg, coupon.id.hashCode())
                    prependAlert(
                        MarketplaceAlert(
                            id = "offer_${coupon.id}",
                            title = title,
                            message = msg,
                            category = AlertCategory.OFFER,
                        )
                    )
                }
                knownCouponIds = validCoupons.map { it.id }.toSet()
            }
        }
    }

    private fun prependAlert(alert: MarketplaceAlert) {
        val current = _inAppAlerts.value.filterNot { it.id == alert.id }
        _inAppAlerts.value = (listOf(alert) + current).take(25)
    }

    fun markAlertsRead() {
        _inAppAlerts.value = _inAppAlerts.value.map { it.copy(isRead = true) }
    }

    fun sendOfferReminderNotification() {
        val now = System.currentTimeMillis()
        val coupon = activeCoupons.value.firstOrNull { it.active && it.expiryMillis > now }
        val latestOrder = customerOrders.value.firstOrNull()
        val title: String
        val body: String
        val category: AlertCategory
        if (coupon != null) {
            title = "تذكير بعرض خاص — خصم ${coupon.discountPercent}%"
            body = "استخدم كود الخصم (${coupon.code}) الآن واحصل على توفير فوري عند الطلب عبر واتساب!"
            category = AlertCategory.OFFER
        } else if (latestOrder != null) {
            val statusLabel = OrderStatus.fromId(latestOrder.status).arabicLabel
            title = "متابعة طلبك من ${latestOrder.storeName}"
            body = "حالة طلب (${latestOrder.productName}): $statusLabel"
            category = AlertCategory.ORDER_UPDATE
        } else {
            title = "عروض سوق المتاجر اليومية"
            body = "تصفح المتاجر المميزة واكتشف أحدث المنتجات والكوبونات الحصرية في مدينتك."
            category = AlertCategory.OFFER
        }

        val posted = if (category == AlertCategory.ORDER_UPDATE && latestOrder != null) {
            notificationManager?.notifyOrderStatusUpdate(latestOrder) ?: false
        } else {
            notificationManager?.notifyNewOffer(title, body) ?: false
        }
        prependAlert(
            MarketplaceAlert(
                id = "manual_${System.currentTimeMillis()}",
                title = title,
                message = body,
                category = category,
                isRead = true,
            )
        )
        _events.tryEmit(
            if (posted) "تم إرسال إشعار التذكير إلى شريط التنبيهات بنجاح"
            else "تم تحديث التنبيهات داخل التطبيق (فعّل إذن الإشعارات لتنبيهات النظام)"
        )
    }

    fun retryFirebaseConnection() {
        if (_isRetryingConnection.value) return
        viewModelScope.launch {
            _isRetryingConnection.value = true
            _retryTrigger.value += 1
            checkServerConnectivity(showFeedbackToast = true)
            _isRetryingConnection.value = false
        }
    }

    private suspend fun checkServerConnectivity(showFeedbackToast: Boolean) {
        val onlineNow = networkObserver?.checkIsOnlineNow() ?: true
        if (!onlineNow) {
            val offlineMsg = "لا يوجد اتصال بالإنترنت. تعذر الوصول إلى خوادم Firebase."
            _connectionError.value = offlineMsg
            if (showFeedbackToast) {
                _events.tryEmit(offlineMsg)
            }
            return
        }
        val pingResult = repository.verifyFirebaseConnection()
        pingResult.onSuccess {
            _connectionError.value = null
            if (showFeedbackToast) {
                _events.tryEmit("تم الاتصال بخوادم Firebase وتحديث البيانات بنجاح")
            }
        }.onFailure { err ->
            val friendly = formatFirebaseErrorMessage(err)
            _connectionError.value = friendly
            if (showFeedbackToast) {
                _events.tryEmit(friendly)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        networkObserver?.unregister()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSelectedCity(city: String) {
        _selectedCity.value = city
    }

    fun openStoreDetail(storeId: String) {
        _selectedStoreId.value = storeId
        viewModelScope.launch {
            repository.incrementStoreViews(storeId)
        }
    }

    fun seedSampleMarketplace() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.seedSampleMarketplaceIfEmpty()
            _isBusy.value = false
            res.onSuccess { seeded ->
                _events.tryEmit(
                    if (seeded) "تم نشر 6 متاجر تجريبية و50 تصنيفاً في قاعدة البيانات"
                    else "المتاجر موجودة بالفعل في قاعدة البيانات"
                )
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "تعذر تحميل البيانات التجريبية")
            }
        }
    }

    fun placeOrder(order: Order, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.placeOrder(order)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("تم تجهيز الطلب وإرساله عبر واتساب")
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "حدث خطأ أثناء تسجيل الطلب")
            }
        }
    }

    fun submitReview(store: Store, rating: Int, comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.submitReview(store, rating, comment)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("شكراً لتقييمك! تم نشر رأيك بنجاح")
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "تعذر إرسال التقييم")
            }
        }
    }

    fun submitJoinRequest(request: JoinRequest, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.submitJoinRequest(request)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("تم إرسال طلب انضمام متجرك للمراجعة بنجاح")
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "تعذر إرسال الطلب")
            }
        }
    }

    fun approveJoinRequest(request: JoinRequest) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.approveJoinRequest(request)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("تمت الموافقة على ${request.storeName} وتفعيل باقة التجربة 30 يوم")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "تعذر قبول الطلب")
            }
        }
    }

    fun rejectJoinRequest(requestId: String, reason: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.rejectJoinRequest(requestId, reason)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("تم رفض الطلب مع إرفاق السبب")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "تعذر رفض الطلب")
            }
        }
    }

    fun updateStoreSpotlight(storeId: String, featured: Boolean, fee: Double) {
        viewModelScope.launch {
            val res = repository.updateStoreSpotlight(storeId, featured, fee)
            res.onSuccess {
                _events.tryEmit(if (featured) "تم تمييز المتجر في Spotlight" else "تمت إزالة التمييز")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "تعذر تحديث Spotlight")
            }
        }
    }

    fun setStoreActive(storeId: String, active: Boolean) {
        viewModelScope.launch {
            val res = repository.setStoreActive(storeId, active)
            res.onSuccess {
                _events.tryEmit(if (active) "تم تفعيل المتجر وظهوره في السوق" else "تم إيقاف المتجر وإخفاؤه")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "تعذر تغيير حالة المتجر")
            }
        }
    }

    fun saveSubscriptionAdmin(
        existingSubId: String?,
        store: Store,
        plan: SubscriptionPlan,
        price: Double,
        status: String,
        startMillis: Long,
        endMillis: Long,
    ) {
        viewModelScope.launch {
            val res = repository.saveSubscriptionAdmin(
                existingSubId = existingSubId,
                store = store,
                plan = plan,
                price = price,
                status = status,
                startMillis = startMillis,
                endMillis = endMillis,
            )
            res.onSuccess {
                _events.tryEmit("تم تحديث اشتراك ${store.name}")
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "تعذر تحديث الاشتراك")
            }
        }
    }

    fun updateStoreInfoOwner(
        storeId: String,
        name: String,
        ownerName: String,
        description: String,
        address: String,
        city: String,
        category: String,
        whatsappPhone: String,
        logoUrl: String,
        onSuccess: () -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateStoreInfoOwner(
                storeId, name, ownerName, description, address, city, category, whatsappPhone, logoUrl
            )
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("تم حفظ بيانات المتجر بنجاح")
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "تعذر تحديث بيانات المتجر")
            }
        }
    }

    fun saveProduct(product: Product, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveProduct(product)
            _isBusy.value = false
            res.onSuccess {
                _events.tryEmit("تم حفظ المنتج بنجاح")
                onSuccess()
            }.onFailure { err ->
                _events.tryEmit(err.localizedMessage ?: "تعذر حفظ المنتج")
            }
        }
    }

    fun toggleProductActive(productId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleProductActive(productId, active)
                .onSuccess { _events.tryEmit(if (active) "تم تفعيل المنتج" else "تم إخفاء المنتج") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "تعذر تحديث المنتج") }
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId)
                .onSuccess { _events.tryEmit("تم حذف المنتج") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "تعذر حذف المنتج") }
        }
    }

    fun createCoupon(store: Store, code: String, discountPercent: Int, daysValid: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val expiry = System.currentTimeMillis() + daysValid.coerceAtLeast(1) * 24L * 60 * 60 * 1000
            repository.createCoupon(store, code, discountPercent, expiry)
                .onSuccess {
                    _events.tryEmit("تم إنشاء كوبون الخصم بنجاح")
                    onSuccess()
                }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "تعذر إنشاء الكوبون") }
        }
    }

    fun toggleCouponActive(couponId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleCouponActive(couponId, active)
        }
    }

    fun deleteCoupon(couponId: String) {
        viewModelScope.launch {
            repository.deleteCoupon(couponId)
                .onSuccess { _events.tryEmit("تم حذف الكوبون") }
        }
    }

    fun addCategory(name: String, sortOrder: Int) {
        viewModelScope.launch {
            repository.addCategory(name, sortOrder)
                .onSuccess { _events.tryEmit("تمت إضافة التصنيف: $name") }
                .onFailure { _events.tryEmit(it.localizedMessage ?: "تعذر إضافة التصنيف") }
        }
    }

    fun toggleCategoryActive(categoryId: String, active: Boolean) {
        viewModelScope.launch {
            repository.toggleCategoryActive(categoryId, active)
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
                .onSuccess {
                    _events.tryEmit("تم تحديث حالة الطلب إلى: ${newStatus.arabicLabel}")
                }
                .onFailure {
                    _events.tryEmit(it.localizedMessage ?: "تعذر تحديث حالة الطلب")
                }
        }
    }

    fun toggleFavorite(store: Store) {
        val currentlyFavorite = store.id in favoriteStoreIds.value
        viewModelScope.launch {
            repository.toggleFavoriteStore(store, currentlyFavorite)
                .onSuccess { added ->
                    _events.tryEmit(
                        if (added) "تم حفظ ${store.name} في المفضلة"
                        else "تمت إزالة ${store.name} من المفضلة"
                    )
                }
                .onFailure { err ->
                    _events.tryEmit(err.localizedMessage ?: "تعذر تحديث قائمة المفضلة")
                }
        }
    }

    private fun <T> Flow<UiState<T>>.catchFirestoreError(tagName: String): Flow<UiState<T>> =
        this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w("MarketplaceVM", "Error observing $tagName", rootError)
            val friendly = formatFirebaseErrorMessage(rootError)
            _connectionError.value = friendly
            emit(UiState.Error(friendly))
        }

    companion object {
        private const val STOP_TIMEOUT = 5000L

        fun formatFirebaseErrorMessage(error: Throwable): String {
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            return when {
                error is TimeoutCancellationException ->
                    "انتهت مهلة الاتصال بخوادم Firebase. يرجى التحقق من اتصال الإنترنت والمحاولة مجدداً."
                firestoreError?.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                    firestoreError?.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
                    generateSequence(error) { it.cause }.any { it is IOException } ->
                    "تعذر الاتصال بخوادم Firebase. يرجى التحقق من اتصال الإنترنت ثم الضغط على إعادة المحاولة."
                firestoreError?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "لا تملك صلاحية الوصول إلى هذه البيانات حالياً. يرجى تسجيل الدخول والمحاولة مجدداً."
                else ->
                    "حدث خطأ أثناء جلب البيانات من Firebase. تحقق من اتصالك بالإنترنت وأعد المحاولة."
            }
        }

        fun normalizeForSearch(text: String): String {
            return text.trim().lowercase()
                .replace('أ', 'ا')
                .replace('إ', 'ا')
                .replace('آ', 'ا')
                .replace('ة', 'ه')
                .replace('ى', 'ي')
        }

        fun matchesQuery(source: String, query: String): Boolean {
            val q = normalizeForSearch(query)
            if (q.isEmpty()) return true
            return normalizeForSearch(source).contains(q)
        }
    }
}
