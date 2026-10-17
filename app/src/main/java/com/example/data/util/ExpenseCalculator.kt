package com.example.data.util

import com.example.data.model.CategoryBreakdown
import com.example.data.model.Expense
import com.example.data.model.Member
import com.example.data.model.MemberBalance
import com.example.data.model.MonthlyReportData
import com.example.data.model.SettlementTransaction
import java.util.PriorityQueue
import kotlin.math.abs
import kotlin.math.min

object ExpenseCalculator {

    /**
     * Calculates the exact cent balance for each active member.
     * Guaranteed that sum of all net balances equals 0.
     */
    fun calculateBalances(
        members: List<Member>,
        expenses: List<Expense>
    ): List<MemberBalance> {
        val memberMap = members.associateBy { it.id }
        val paidMap = members.associate { it.id to 0L }.toMutableMap()
        val shareMap = members.associate { it.id to 0L }.toMutableMap()

        for (expense in expenses) {
            // Add to payer's total paid
            paidMap[expense.payerMemberId] = (paidMap[expense.payerMemberId] ?: 0L) + expense.amountCents

            // Calculate exact split shares
            val splitIds = expense.getSplitMemberIdList().filter { memberMap.containsKey(it) }
            val count = splitIds.size
            if (count > 0) {
                val baseShare = expense.amountCents / count
                val remainder = (expense.amountCents % count).toInt()

                splitIds.forEachIndexed { index, memberId ->
                    val memberShare = if (index < remainder) baseShare + 1L else baseShare
                    shareMap[memberId] = (shareMap[memberId] ?: 0L) + memberShare
                }
            }
        }

        return members.map { member ->
            val totalPaid = paidMap[member.id] ?: 0L
            val totalShare = shareMap[member.id] ?: 0L
            MemberBalance(
                member = member,
                totalPaidCents = totalPaid,
                totalShareCents = totalShare,
                netBalanceCents = totalPaid - totalShare
            )
        }
    }

    /**
     * Computes the minimum number of peer-to-peer settlement transactions
     * to completely settle all debts fairly.
     */
    fun calculateSettlementPlan(
        balances: List<MemberBalance>
    ): List<SettlementTransaction> {
        // Debtor has negative net balance (owes money)
        // Creditor has positive net balance (is owed money)
        class DebtNode(val member: Member, var amount: Long)

        val debtors = PriorityQueue<DebtNode> { a, b -> b.amount.compareTo(a.amount) }
        val creditors = PriorityQueue<DebtNode> { a, b -> b.amount.compareTo(a.amount) }

        for (b in balances) {
            if (b.netBalanceCents < 0) {
                debtors.add(DebtNode(b.member, abs(b.netBalanceCents)))
            } else if (b.netBalanceCents > 0) {
                creditors.add(DebtNode(b.member, b.netBalanceCents))
            }
        }

        val transactions = mutableListOf<SettlementTransaction>()

        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val debtor = debtors.poll()!!
            val creditor = creditors.poll()!!

            val settleAmount = min(debtor.amount, creditor.amount)
            if (settleAmount > 0) {
                transactions.add(
                    SettlementTransaction(
                        fromMember = debtor.member,
                        toMember = creditor.member,
                        amountCents = settleAmount
                    )
                )
            }

            debtor.amount -= settleAmount
            creditor.amount -= settleAmount

            if (debtor.amount > 0) {
                debtors.add(debtor)
            }
            if (creditor.amount > 0) {
                creditors.add(creditor)
            }
        }

        return transactions
    }

    /**
     * Calculates category spending breakdown and statistics.
     */
    fun calculateCategoryBreakdowns(expenses: List<Expense>): List<CategoryBreakdown> {
        if (expenses.isEmpty()) return emptyList()

        val totalAllCents = expenses.sumOf { it.amountCents }
        if (totalAllCents == 0L) return emptyList()

        return expenses.groupBy { it.category }
            .map { (category, categoryExpenses) ->
                val catTotal = categoryExpenses.sumOf { it.amountCents }
                val percentage = (catTotal.toFloat() / totalAllCents.toFloat()) * 100f
                CategoryBreakdown(
                    category = category,
                    totalCents = catTotal,
                    count = categoryExpenses.size,
                    percentage = percentage
                )
            }
            .sortedByDescending { it.totalCents }
    }

    /**
     * Generates a complete monthly report object.
     */
    fun generateMonthlyReport(
        periodLabel: String,
        members: List<Member>,
        expenses: List<Expense>
    ): MonthlyReportData {
        val totalExpensesCents = expenses.sumOf { it.amountCents }
        val activeMemberCount = members.count { !it.isArchived }
        val averagePerMember = if (activeMemberCount > 0) totalExpensesCents / activeMemberCount else 0L

        val balances = calculateBalances(members, expenses)
        val settlements = calculateSettlementPlan(balances)
        val categories = calculateCategoryBreakdowns(expenses)

        return MonthlyReportData(
            periodLabel = periodLabel,
            totalExpensesCents = totalExpensesCents,
            expenseCount = expenses.size,
            averagePerMemberCents = averagePerMember,
            categoryBreakdowns = categories,
            memberBalances = balances,
            settlementPlan = settlements
        )
    }
}
