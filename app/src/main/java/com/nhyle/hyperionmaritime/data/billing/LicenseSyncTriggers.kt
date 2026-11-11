package com.nhyle.hyperionmaritime.data.billing

/**
 * Network account reconciliation is event driven. Android still asks Google
 * Play for up-to-date ownership, but a simple app resume or BillingClient
 * reconnection must not trigger Hyperion Supabase entitlement queries.
 */
internal fun shouldSyncLinkedAccountAfterPlayRefresh(
    checkoutPending: Boolean,
    previousTokens: Set<String>,
    currentTokens: Set<String>
): Boolean = checkoutPending || previousTokens != currentTokens
