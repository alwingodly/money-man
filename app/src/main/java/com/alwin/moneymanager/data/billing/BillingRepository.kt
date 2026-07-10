package com.alwin.moneymanager.data.billing

import android.app.Activity
import android.content.Context
import com.alwin.moneymanager.BuildConfig
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton

const val PREMIUM_PRODUCT_ID = "premium_unlock"

/**
 * One-time "Buy me a coffee" unlock (₹9) gating: unlimited categories/EMIs, all theme colors, and
 * the Retro LCD style. Debug builds are always unlocked so development never has to fight the
 * paywall; release builds check the real Play Billing entitlement, tied to whichever Google
 * account is signed into the Play Store on the device — no separate sign-in needed.
 */
@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _isPremium = MutableStateFlow(BuildConfig.DEBUG)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { purchases.forEach { handlePurchase(it) } }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { refreshPurchases() }
                }
            }

            override fun onBillingServiceDisconnected() {
                // Retried lazily — the next queryPurchases()/purchasePremium() call reconnects.
            }
        })
    }

    private suspend fun queryPurchases(): List<Purchase> = suspendCancellableCoroutine { cont ->
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                cont.resumeWith(Result.success(purchases))
            } else {
                cont.resumeWith(Result.success(emptyList()))
            }
        }
    }

    private suspend fun queryPremiumProductDetails(): ProductDetails? = suspendCancellableCoroutine { cont ->
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PREMIUM_PRODUCT_ID)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder().setProductList(productList).build()
        billingClient.queryProductDetailsAsync(params) { _, result ->
            cont.resumeWith(Result.success(result.productDetailsList.firstOrNull()))
        }
    }

    private suspend fun refreshPurchases() {
        queryPurchases().forEach { handlePurchase(it) }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.products.contains(PREMIUM_PRODUCT_ID)) return

        _isPremium.value = true
        if (!purchase.isAcknowledged) {
            val ackParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(ackParams) { }
        }
    }

    /** Looks up the product and launches Play's own purchase UI. Result arrives via [purchasesUpdatedListener]. */
    fun purchasePremium(activity: Activity) {
        scope.launch {
            val productDetails = queryPremiumProductDetails() ?: return@launch
            val offerToken = productDetails.oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
                ?: return@launch

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                )
                .build()
            billingClient.launchBillingFlow(activity, flowParams)
        }
    }
}
