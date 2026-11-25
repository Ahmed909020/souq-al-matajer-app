package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Coupon
import com.example.model.FavoriteStore
import com.example.model.JoinRequest
import com.example.model.MarketplaceCatalog
import com.example.model.MarketplaceCategory
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.model.Product
import com.example.model.Review
import com.example.model.Store
import com.example.model.Subscription
import com.example.model.SubscriptionPlan
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class MarketplaceRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("يجب تسجيل الدخول عبر Google أولاً.")

    private fun isCurrentUserAdmin(): Boolean {
        val email = auth.currentUser?.email ?: return false
        return email.equals(MarketplaceCatalog.ADMIN_EMAIL, ignoreCase = true)
    }

    // --- Real-time Observation Streams ---

    fun observeActiveStores(): Flow<List<Store>> = safeCollectionFlow(
        path = "stores",
        query = db.collection("stores").whereEqualTo("active", true),
        mapper = { snap -> snap.toObjects(Store::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeAllStoresAdmin(): Flow<List<Store>> = safeCollectionFlow(
        path = "stores",
        query = db.collection("stores"),
        mapper = { snap -> snap.toObjects(Store::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeOwnerStores(ownerId: String): Flow<List<Store>> = safeCollectionFlow(
        path = "stores",
        query = db.collection("stores").whereEqualTo("ownerId", ownerId),
        mapper = { snap -> snap.toObjects(Store::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeActiveProducts(): Flow<List<Product>> = safeCollectionFlow(
        path = "products",
        query = db.collection("products").whereEqualTo("active", true),
        mapper = { snap -> snap.toObjects(Product::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeOwnerProducts(ownerId: String): Flow<List<Product>> = safeCollectionFlow(
        path = "products",
        query = db.collection("products").whereEqualTo("ownerId", ownerId),
        mapper = { snap -> snap.toObjects(Product::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeStoreReviews(storeId: String): Flow<List<Review>> = safeCollectionFlow(
        path = "reviews",
        query = db.collection("reviews").whereEqualTo("storeId", storeId),
        mapper = { snap -> snap.toObjects(Review::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeStoreCoupons(storeId: String): Flow<List<Coupon>> = safeCollectionFlow(
        path = "coupons",
        query = db.collection("coupons")
            .whereEqualTo("storeId", storeId)
            .whereEqualTo("active", true),
        mapper = { snap -> snap.toObjects(Coupon::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeOwnerCoupons(ownerId: String): Flow<List<Coupon>> = safeCollectionFlow(
        path = "coupons",
        query = db.collection("coupons").whereEqualTo("ownerId", ownerId),
        mapper = { snap -> snap.toObjects(Coupon::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeAllCouponsAdmin(): Flow<List<Coupon>> = safeCollectionFlow(
        path = "coupons",
        query = db.collection("coupons"),
        mapper = { snap -> snap.toObjects(Coupon::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeOwnerJoinRequests(ownerId: String): Flow<List<JoinRequest>> = safeCollectionFlow(
        path = "joinRequests",
        query = db.collection("joinRequests").whereEqualTo("ownerId", ownerId),
        mapper = { snap -> snap.toObjects(JoinRequest::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeAllJoinRequestsAdmin(): Flow<List<JoinRequest>> = safeCollectionFlow(
        path = "joinRequests",
        query = db.collection("joinRequests"),
        mapper = { snap -> snap.toObjects(JoinRequest::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeOwnerSubscriptions(ownerId: String): Flow<List<Subscription>> = safeCollectionFlow(
        path = "subscriptions",
        query = db.collection("subscriptions").whereEqualTo("ownerId", ownerId),
        mapper = { snap -> snap.toObjects(Subscription::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeAllSubscriptionsAdmin(): Flow<List<Subscription>> = safeCollectionFlow(
        path = "subscriptions",
        query = db.collection("subscriptions"),
        mapper = { snap -> snap.toObjects(Subscription::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeOwnerOrders(ownerId: String): Flow<List<Order>> = safeCollectionFlow(
        path = "orders",
        query = db.collection("orders").whereEqualTo("storeOwnerId", ownerId),
        mapper = { snap -> snap.toObjects(Order::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeCustomerOrders(customerId: String): Flow<List<Order>> = safeCollectionFlow(
        path = "orders",
        query = db.collection("orders").whereEqualTo("customerId", customerId),
        mapper = { snap -> snap.toObjects(Order::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeActiveCoupons(): Flow<List<Coupon>> = safeCollectionFlow(
        path = "coupons",
        query = db.collection("coupons").whereEqualTo("active", true),
        mapper = { snap -> snap.toObjects(Coupon::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeUserFavorites(userId: String): Flow<List<FavoriteStore>> = safeCollectionFlow(
        path = "users/$userId/favorites",
        query = db.collection("users").document(userId).collection("favorites"),
        mapper = { snap -> snap.toObjects(FavoriteStore::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeActiveCategories(): Flow<List<MarketplaceCategory>> = safeCollectionFlow(
        path = "categories",
        query = db.collection("categories").whereEqualTo("active", true),
        mapper = { snap -> snap.toObjects(MarketplaceCategory::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    fun observeAllCategoriesAdmin(): Flow<List<MarketplaceCategory>> = safeCollectionFlow(
        path = "categories",
        query = db.collection("categories"),
        mapper = { snap -> snap.toObjects(MarketplaceCategory::class.java, ServerTimestampBehavior.ESTIMATE) },
    )

    private fun <T> safeCollectionFlow(
        path: String,
        query: com.google.firebase.firestore.Query,
        mapper: (com.google.firebase.firestore.QuerySnapshot) -> List<T>,
    ): Flow<List<T>> = flow {
        val uid = requireUserId()
        emitAll(
            query.snapshots()
                .map { snapshot -> mapper(snapshot) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun verifyFirebaseConnection(): Result<Unit> {
        return try {
            requireUserId()
            withTimeout(6000L) {
                db.enableNetwork().await()
                db.collection("stores")
                    .whereEqualTo("active", true)
                    .limit(1)
                    .get(Source.SERVER)
                    .await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            if (e is kotlinx.coroutines.TimeoutCancellationException) {
                Result.failure(e)
            } else {
                throw e
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "stores")
            Result.failure(e)
        }
    }

    // --- Seeding Sample Data (Admin Only) ---

    suspend fun seedSampleMarketplaceIfEmpty(): Result<Boolean> {
        return try {
            val uid = requireUserId()
            if (!isCurrentUserAdmin()) {
                return Result.success(false)
            }
            val existingStores = db.collection("stores").limit(1).get().await()
            if (!existingStores.isEmpty) {
                return Result.success(false)
            }

            val batch = db.batch()
            val now = System.currentTimeMillis()

            MarketplaceCatalog.defaultCategories.forEachIndexed { idx, catName ->
                val catRef = db.collection("categories").document("cat_${idx + 1}")
                batch.set(
                    catRef,
                    mapOf(
                        "name" to catName,
                        "sortOrder" to (idx + 1),
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            val sampleStores = MarketplaceCatalog.sampleStores(uid)
            sampleStores.forEach { store ->
                val storeRef = db.collection("stores").document(store.id)
                batch.set(
                    storeRef,
                    mapOf(
                        "ownerId" to store.ownerId,
                        "ownerName" to store.ownerName,
                        "name" to store.name,
                        "description" to store.description,
                        "address" to store.address,
                        "city" to store.city,
                        "category" to store.category,
                        "logoUrl" to store.logoUrl,
                        "whatsappPhone" to store.whatsappPhone,
                        "active" to store.active,
                        "featured" to store.featured,
                        "spotlightFee" to store.spotlightFee,
                        "ratingAvg" to store.ratingAvg,
                        "ratingCount" to store.ratingCount,
                        "viewsCount" to store.viewsCount,
                        "productsCount" to store.productsCount,
                        "planName" to store.planName,
                        "subscriptionStatus" to store.subscriptionStatus,
                        "subscriptionEndMillis" to store.subscriptionEndMillis,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )

                val planEnum = when (store.planName) {
                    "سنوي" -> SubscriptionPlan.YEARLY
                    "شهري" -> SubscriptionPlan.MONTHLY
                    else -> SubscriptionPlan.TRIAL_30
                }
                val subRef = db.collection("subscriptions").document("sub_${store.id}")
                batch.set(
                    subRef,
                    mapOf(
                        "storeId" to store.id,
                        "storeName" to store.name,
                        "ownerId" to store.ownerId,
                        "plan" to planEnum.id,
                        "price" to planEnum.defaultPrice,
                        "status" to store.subscriptionStatus,
                        "startDateMillis" to now,
                        "endDateMillis" to store.subscriptionEndMillis,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            MarketplaceCatalog.sampleProducts(uid).forEach { product ->
                val prodRef = db.collection("products").document(product.id)
                batch.set(
                    prodRef,
                    mapOf(
                        "storeId" to product.storeId,
                        "ownerId" to product.ownerId,
                        "name" to product.name,
                        "description" to product.description,
                        "price" to product.price,
                        "imageUrl" to product.imageUrl,
                        "active" to product.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            MarketplaceCatalog.sampleReviews().forEach { review ->
                val revRef = db.collection("reviews").document(review.id)
                batch.set(
                    revRef,
                    mapOf(
                        "storeId" to review.storeId,
                        "userId" to uid,
                        "userName" to review.userName,
                        "rating" to review.rating,
                        "comment" to review.comment,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            MarketplaceCatalog.sampleCoupons(uid).forEach { coupon ->
                val coupRef = db.collection("coupons").document(coupon.id)
                batch.set(
                    coupRef,
                    mapOf(
                        "storeId" to coupon.storeId,
                        "ownerId" to coupon.ownerId,
                        "code" to coupon.code,
                        "discountPercent" to coupon.discountPercent,
                        "expiryMillis" to coupon.expiryMillis,
                        "active" to coupon.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            batch.commit().await()
            Result.success(true)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "stores")
            Result.failure(e)
        }
    }

    // --- Visitor Actions ---

    suspend fun incrementStoreViews(storeId: String): Result<Unit> {
        val docPath = "stores/$storeId"
        return try {
            requireUserId()
            val storeRef = db.collection("stores").document(storeId)
            val snap = storeRef.get().await()
            if (!snap.exists()) return Result.success(Unit)
            val currentViews = snap.getLong("viewsCount") ?: 0L
            storeRef.update(
                mapOf(
                    "viewsCount" to (currentViews + 1L),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun placeOrder(order: Order): Result<Unit> {
        val colPath = "orders"
        return try {
            val uid = requireUserId()
            require(order.storeId.isNotBlank()) { "معرف المتجر مطلوب" }
            require(order.customerName.trim().length >= 2) { "الاسم يجب أن يكون حرفين على الأقل" }
            require(order.customerPhone.trim().length >= 6) { "رقم الهاتف غير صالح" }
            require(order.customerAddress.trim().length >= 3) { "يرجى إدخال العنوان بالتفصيل" }
            require(order.quantity >= 1L) { "الكمية يجب أن تكون 1 على الأقل" }

            val storeSnap = db.collection("stores").document(order.storeId).get().await()
            if (!storeSnap.exists()) {
                return Result.success(Unit) // Sample showcase store not yet synced to Firestore
            }

            val docRef = db.collection("orders").document()
            val payload = mapOf(
                "storeId" to order.storeId,
                "storeName" to order.storeName.take(120),
                "storeOwnerId" to order.storeOwnerId,
                "customerId" to uid,
                "customerName" to order.customerName.trim().take(100),
                "customerPhone" to order.customerPhone.trim().take(30),
                "customerAddress" to order.customerAddress.trim().take(300),
                "productId" to order.productId,
                "productName" to order.productName.take(140),
                "unitPrice" to order.unitPrice,
                "quantity" to order.quantity,
                "couponCode" to order.couponCode?.trim()?.takeIf { it.isNotEmpty() }?.take(40),
                "discountPercent" to order.discountPercent.coerceIn(0L, 90L),
                "totalPrice" to order.totalPrice.coerceAtLeast(0.0),
                "status" to "sent_whatsapp",
                "createdAt" to FieldValue.serverTimestamp(),
            ).filterValues { it != null } as Map<String, Any>

            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colPath)
            Result.failure(e)
        }
    }

    suspend fun submitReview(store: Store, rating: Int, comment: String): Result<Unit> {
        val docPath = "reviews"
        return try {
            val uid = requireUserId()
            val userName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "زائر"
            val trimmedComment = comment.trim()
            require(trimmedComment.isNotEmpty()) { "يرجى كتابة تعليق التقييم" }
            val boundedRating = rating.coerceIn(1, 5)

            val storeRef = db.collection("stores").document(store.id)
            val storeSnap = storeRef.get().await()
            if (!storeSnap.exists()) {
                return Result.failure(IllegalStateException("المتجر التجريبي غير منشور في قاعدة البيانات بعد"))
            }

            val currentAvg = storeSnap.getDouble("ratingAvg") ?: store.ratingAvg
            val currentCount = storeSnap.getLong("ratingCount") ?: store.ratingCount
            val newCount = currentCount + 1L
            val newAvg = ((currentAvg * currentCount) + boundedRating) / newCount.toDouble()

            val revRef = db.collection("reviews").document()
            val batch = db.batch()
            batch.set(
                revRef,
                mapOf(
                    "storeId" to store.id,
                    "userId" to uid,
                    "userName" to userName.take(100),
                    "rating" to boundedRating.toLong(),
                    "comment" to trimmedComment.take(600),
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(
                storeRef,
                mapOf(
                    "ratingAvg" to ((newAvg * 10.0).toInt() / 10.0).coerceIn(1.0, 5.0),
                    "ratingCount" to newCount,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    // --- Join Request / Add Store Flow ---

    suspend fun submitJoinRequest(request: JoinRequest): Result<String> {
        val colPath = "joinRequests"
        return try {
            val uid = requireUserId()
            val email = auth.currentUser?.email ?: "owner@souq.app"
            require(request.storeName.trim().length >= 2) { "اسم المتجر مطلوب (حرفان على الأقل)" }
            require(request.ownerName.trim().length >= 2) { "اسم المالك مطلوب" }
            require(request.phone.trim().length >= 6) { "رقم الهاتف مطلوب" }
            require(request.city.trim().length >= 2) { "المدينة مطلوبة" }
            require(request.category.trim().length >= 2) { "التصنيف مطلوب" }
            require(request.address.trim().isNotEmpty()) { "العنوان مطلوب" }
            require(request.description.trim().isNotEmpty()) { "وصف المتجر مطلوب" }

            val docRef = db.collection("joinRequests").document()
            val payload = mapOf(
                "ownerId" to uid,
                "ownerEmail" to email.take(160),
                "storeName" to request.storeName.trim().take(120),
                "ownerName" to request.ownerName.trim().take(100),
                "phone" to request.phone.trim().take(30),
                "city" to request.city.trim().take(80),
                "category" to request.category.trim().take(80),
                "address" to request.address.trim().take(250),
                "description" to request.description.trim().take(1500),
                "logoUrl" to request.logoUrl.take(350000),
                "status" to "pending",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colPath)
            Result.failure(e)
        }
    }

    // --- Admin Approval & Management ---

    suspend fun approveJoinRequest(request: JoinRequest): Result<String> {
        val path = "joinRequests/${request.id}"
        return try {
            requireUserId()
            val storeRef = db.collection("stores").document()
            val subRef = db.collection("subscriptions").document()
            val reqRef = db.collection("joinRequests").document(request.id)

            val now = System.currentTimeMillis()
            val trialEnd = now + SubscriptionPlan.TRIAL_30.durationDays * 24L * 60 * 60 * 1000

            val batch = db.batch()
            batch.set(
                storeRef,
                mapOf(
                    "ownerId" to request.ownerId,
                    "ownerName" to request.ownerName,
                    "name" to request.storeName,
                    "description" to request.description,
                    "address" to request.address,
                    "city" to request.city,
                    "category" to request.category,
                    "logoUrl" to request.logoUrl,
                    "whatsappPhone" to request.phone,
                    "active" to true,
                    "featured" to false,
                    "spotlightFee" to 0.0,
                    "ratingAvg" to 0.0,
                    "ratingCount" to 0L,
                    "viewsCount" to 0L,
                    "productsCount" to 0L,
                    "planName" to SubscriptionPlan.TRIAL_30.arabicLabel,
                    "subscriptionStatus" to "active",
                    "subscriptionEndMillis" to trialEnd,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.set(
                subRef,
                mapOf(
                    "storeId" to storeRef.id,
                    "storeName" to request.storeName,
                    "ownerId" to request.ownerId,
                    "plan" to SubscriptionPlan.TRIAL_30.id,
                    "price" to 0.0,
                    "status" to "active",
                    "startDateMillis" to now,
                    "endDateMillis" to trialEnd,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(
                reqRef,
                mapOf(
                    "status" to "approved",
                    "approvedStoreId" to storeRef.id,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(storeRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun rejectJoinRequest(requestId: String, reason: String): Result<Unit> {
        val path = "joinRequests/$requestId"
        return try {
            requireUserId()
            val trimmed = reason.trim()
            require(trimmed.isNotEmpty()) { "يرجى كتابة سبب الرفض" }
            db.collection("joinRequests").document(requestId)
                .update(
                    mapOf(
                        "status" to "rejected",
                        "rejectionReason" to trimmed.take(500),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStoreSpotlight(
        storeId: String,
        featured: Boolean,
        spotlightFee: Double,
    ): Result<Unit> {
        val path = "stores/$storeId"
        return try {
            requireUserId()
            db.collection("stores").document(storeId).update(
                mapOf(
                    "featured" to featured,
                    "spotlightFee" to spotlightFee.coerceAtLeast(0.0),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun setStoreActive(storeId: String, active: Boolean): Result<Unit> {
        val path = "stores/$storeId"
        return try {
            requireUserId()
            db.collection("stores").document(storeId).update(
                mapOf(
                    "active" to active,
                    "subscriptionStatus" to if (active) "active" else "paused",
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveSubscriptionAdmin(
        existingSubId: String?,
        store: Store,
        plan: SubscriptionPlan,
        price: Double,
        status: String,
        startMillis: Long,
        endMillis: Long,
    ): Result<Unit> {
        val path = "subscriptions"
        return try {
            requireUserId()
            val batch = db.batch()
            val subRef = if (!existingSubId.isNullOrBlank()) {
                db.collection("subscriptions").document(existingSubId)
            } else {
                db.collection("subscriptions").document()
            }

            if (existingSubId.isNullOrBlank()) {
                batch.set(
                    subRef,
                    mapOf(
                        "storeId" to store.id,
                        "storeName" to store.name,
                        "ownerId" to store.ownerId,
                        "plan" to plan.id,
                        "price" to price.coerceAtLeast(0.0),
                        "status" to status,
                        "startDateMillis" to startMillis,
                        "endDateMillis" to endMillis,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            } else {
                batch.update(
                    subRef,
                    mapOf(
                        "storeName" to store.name,
                        "plan" to plan.id,
                        "price" to price.coerceAtLeast(0.0),
                        "status" to status,
                        "startDateMillis" to startMillis,
                        "endDateMillis" to endMillis,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                )
            }

            val isStoreVisible = status == "active" && endMillis > System.currentTimeMillis()
            batch.update(
                db.collection("stores").document(store.id),
                mapOf(
                    "active" to isStoreVisible,
                    "planName" to plan.arabicLabel,
                    "subscriptionStatus" to status,
                    "subscriptionEndMillis" to endMillis,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Store Owner Store & Product Management ---

    suspend fun updateStoreInfoOwner(
        storeId: String,
        name: String,
        ownerName: String,
        description: String,
        address: String,
        city: String,
        category: String,
        whatsappPhone: String,
        logoUrl: String,
    ): Result<Unit> {
        val path = "stores/$storeId"
        return try {
            requireUserId()
            require(name.trim().length >= 2) { "اسم المتجر مطلوب" }
            db.collection("stores").document(storeId).update(
                mapOf(
                    "name" to name.trim().take(120),
                    "ownerName" to ownerName.trim().take(100),
                    "description" to description.trim().take(1500),
                    "address" to address.trim().take(250),
                    "city" to city.trim().take(80),
                    "category" to category.trim().take(80),
                    "whatsappPhone" to whatsappPhone.trim().take(30),
                    "logoUrl" to logoUrl.take(350000),
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveProduct(product: Product): Result<Unit> {
        val colPath = "products"
        return try {
            val uid = requireUserId()
            require(product.storeId.isNotBlank()) { "المتجر مطلوب" }
            require(product.name.trim().length >= 2) { "اسم المنتج مطلوب" }
            require(product.price > 0.0) { "السعر يجب أن يكون أكبر من صفر" }

            if (product.id.isBlank()) {
                val docRef = db.collection("products").document()
                docRef.set(
                    mapOf(
                        "storeId" to product.storeId,
                        "ownerId" to uid,
                        "name" to product.name.trim().take(140),
                        "description" to product.description.trim().take(1500),
                        "price" to product.price,
                        "imageUrl" to product.imageUrl.take(350000),
                        "active" to product.active,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            } else {
                db.collection("products").document(product.id).update(
                    mapOf(
                        "name" to product.name.trim().take(140),
                        "description" to product.description.trim().take(1500),
                        "price" to product.price,
                        "imageUrl" to product.imageUrl.take(350000),
                        "active" to product.active,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, colPath)
            Result.failure(e)
        }
    }

    suspend fun toggleProductActive(productId: String, active: Boolean): Result<Unit> {
        val path = "products/$productId"
        return try {
            requireUserId()
            db.collection("products").document(productId).update(
                mapOf(
                    "active" to active,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        val path = "products/$productId"
        return try {
            requireUserId()
            db.collection("products").document(productId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Coupons Management ---

    suspend fun createCoupon(
        store: Store,
        code: String,
        discountPercent: Int,
        expiryMillis: Long,
    ): Result<Unit> {
        val colPath = "coupons"
        return try {
            val uid = requireUserId()
            val cleanCode = code.trim().uppercase().take(40)
            require(cleanCode.length >= 2) { "رمز الكوبون قصير جداً" }
            val cleanPercent = discountPercent.coerceIn(1, 90)
            val ownerIdForCoupon = if (isCurrentUserAdmin()) store.ownerId else uid

            val docRef = db.collection("coupons").document()
            docRef.set(
                mapOf(
                    "storeId" to store.id,
                    "ownerId" to ownerIdForCoupon,
                    "code" to cleanCode,
                    "discountPercent" to cleanPercent.toLong(),
                    "expiryMillis" to expiryMillis,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colPath)
            Result.failure(e)
        }
    }

    suspend fun toggleCouponActive(couponId: String, active: Boolean): Result<Unit> {
        val path = "coupons/$couponId"
        return try {
            requireUserId()
            db.collection("coupons").document(couponId).update(mapOf("active" to active)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCoupon(couponId: String): Result<Unit> {
        val path = "coupons/$couponId"
        return try {
            requireUserId()
            db.collection("coupons").document(couponId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Categories Management (Admin) ---

    suspend fun addCategory(name: String, sortOrder: Int): Result<Unit> {
        val colPath = "categories"
        return try {
            requireUserId()
            val clean = name.trim()
            require(clean.length >= 2) { "اسم التصنيف مطلوب" }
            db.collection("categories").document().set(
                mapOf(
                    "name" to clean.take(80),
                    "sortOrder" to sortOrder,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, colPath)
            Result.failure(e)
        }
    }

    suspend fun toggleCategoryActive(categoryId: String, active: Boolean): Result<Unit> {
        val path = "categories/$categoryId"
        return try {
            requireUserId()
            db.collection("categories").document(categoryId).update(mapOf("active" to active)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Order Status Updates (Store Owner / Admin) ---

    suspend fun updateOrderStatus(orderId: String, status: OrderStatus): Result<Unit> {
        val path = "orders/$orderId"
        return try {
            requireUserId()
            db.collection("orders").document(orderId)
                .update(mapOf("status" to status.id))
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- User Favorites Management ---

    suspend fun toggleFavoriteStore(store: Store, isCurrentlyFavorite: Boolean): Result<Boolean> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val path = "users/$uid/favorites/${store.id}"
        return try {
            val favRef = db.collection("users").document(uid)
                .collection("favorites").document(store.id)
            if (isCurrentlyFavorite) {
                favRef.delete().await()
                Result.success(false)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "storeId" to store.id,
                    "storeName" to store.name.trim().ifBlank { "متجر" }.take(120),
                    "storeCategory" to store.category.trim().take(80),
                    "storeCity" to store.city.trim().take(80),
                    "storeLogoUrl" to store.logoUrl.take(350000),
                    "createdAt" to FieldValue.serverTimestamp(),
                )
                favRef.set(payload).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
