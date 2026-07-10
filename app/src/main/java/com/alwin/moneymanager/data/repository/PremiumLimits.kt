package com.alwin.moneymanager.data.repository

/** Free-tier caps enforced when [com.alwin.moneymanager.data.billing.BillingRepository.isPremium] is false. */
object PremiumLimits {
    const val FREE_CATEGORY_LIMIT = 5
    const val FREE_EMI_LIMIT = 2
}
