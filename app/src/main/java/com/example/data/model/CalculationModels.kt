package com.example.data.model

import java.util.Locale

data class MemberBalance(
    val member: Member,
    val totalPaidCents: Long = 0L,
    val totalShareCents: Long = 0L,
    val netBalanceCents: Long = 0L // totalPaidCents - totalShareCents (+ means receives, - means owes)
) {
    val totalPaidFormatted: String
        get() = String.format(Locale.US, "€%.2f", totalPaidCents / 100.0)

    val totalShareFormatted: String
        get() = String.format(Locale.US, "€%.2f", totalShareCents / 100.0)

    val netBalanceFormatted: String
        get() = String.format(Locale.US, "€%.2f", kotlin.math.abs(netBalanceCents) / 100.0)

    val isSettled: Boolean
        get() = netBalanceCents == 0L

    val isOwed: Boolean
        get() = netBalanceCents > 0L // Needs to receive money

    val owesMoney: Boolean
        get() = netBalanceCents < 0L // Needs to pay money
}

data class SettlementTransaction(
    val fromMember: Member,
    val toMember: Member,
    val amountCents: Long
) {
    val amountFormatted: String
        get() = String.format(Locale.US, "€%.2f", amountCents / 100.0)
}

data class CategoryBreakdown(
    val category: String,
    val totalCents: Long,
    val count: Int,
    val percentage: Float
) {
    val totalFormatted: String
        get() = String.format(Locale.US, "€%.2f", totalCents / 100.0)
}

data class MonthlyReportData(
    val periodLabel: String,
    val totalExpensesCents: Long,
    val expenseCount: Int,
    val averagePerMemberCents: Long,
    val categoryBreakdowns: List<CategoryBreakdown>,
    val memberBalances: List<MemberBalance>,
    val settlementPlan: List<SettlementTransaction>
) {
    val totalFormatted: String
        get() = String.format(Locale.US, "€%.2f", totalExpensesCents / 100.0)

    val averageFormatted: String
        get() = String.format(Locale.US, "€%.2f", averagePerMemberCents / 100.0)
}
