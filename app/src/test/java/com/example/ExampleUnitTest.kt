package com.example

import com.example.data.model.Expense
import com.example.data.model.Member
import com.example.data.util.ExpenseCalculator
import com.example.data.util.WhatsAppShareHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ExampleUnitTest {

    private val member1 = Member(id = 1L, name = "Ajaz", phoneNumber = "+386 40 123 456")
    private val member2 = Member(id = 2L, name = "Luka", phoneNumber = "+386 41 234 567")
    private val member3 = Member(id = 3L, name = "Matej", phoneNumber = "+386 51 345 678")
    private val member4 = Member(id = 4L, name = "Ana", phoneNumber = "+386 70 456 789")
    private val member5 = Member(id = 5L, name = "Jan", phoneNumber = "+386 31 567 890")

    private val members = listOf(member1, member2, member3, member4, member5)

    @Test
    fun testExactCentSplittingAndZeroSum() {
        // Expense of €100.01 split among 3 members: 33.34, 33.34, 33.33 -> sum = 100.01
        val expense = Expense(
            id = 1L,
            title = "Groceries",
            amountCents = 10001L,
            category = "Grocery",
            payerMemberId = 1L,
            splitMemberIds = "1,2,3"
        )

        val balances = ExpenseCalculator.calculateBalances(members, listOf(expense))

        val ajaz = balances.first { it.member.id == 1L }
        val luka = balances.first { it.member.id == 2L }
        val matej = balances.first { it.member.id == 3L }
        val ana = balances.first { it.member.id == 4L }

        assertEquals(10001L, ajaz.totalPaidCents)
        assertEquals(3334L, ajaz.totalShareCents)
        assertEquals(6667L, ajaz.netBalanceCents) // 10001 - 3334 = 6667 (positive)

        assertEquals(0L, luka.totalPaidCents)
        assertEquals(3334L, luka.totalShareCents)
        assertEquals(-3334L, luka.netBalanceCents) // owes 33.34

        assertEquals(0L, matej.totalPaidCents)
        assertEquals(3333L, matej.totalShareCents)
        assertEquals(-3333L, matej.netBalanceCents) // owes 33.33

        assertEquals(0L, ana.netBalanceCents) // not in split

        // Total net balance across all participants MUST be zero
        val totalNet = balances.sumOf { it.netBalanceCents }
        assertEquals(0L, totalNet)
    }

    @Test
    fun testSettlementPlanSimplifiesDebts() {
        val expense1 = Expense(
            id = 1L,
            title = "Rent",
            amountCents = 100000L, // €1000.00
            category = "Room Rent",
            payerMemberId = 1L, // Ajaz
            splitMemberIds = "1,2,3,4,5" // €200.00 each
        )
        val expense2 = Expense(
            id = 2L,
            title = "Utilities",
            amountCents = 25000L, // €250.00
            category = "Bills & Utilities",
            payerMemberId = 2L, // Luka
            splitMemberIds = "1,2,3,4,5" // €50.00 each
        )

        val balances = ExpenseCalculator.calculateBalances(members, listOf(expense1, expense2))
        val settlements = ExpenseCalculator.calculateSettlementPlan(balances)

        // Ajaz paid 1000, owes 250 -> net +750
        // Luka paid 250, owes 250 -> net 0
        // Matej paid 0, owes 250 -> net -250
        // Ana paid 0, owes 250 -> net -250
        // Jan paid 0, owes 250 -> net -250
        // Matej, Ana, Jan each pay Ajaz €250 -> 3 transactions!
        assertEquals(3, settlements.size)
        settlements.forEach {
            assertEquals(1L, it.toMember.id) // All pay Ajaz
            assertEquals(25000L, it.amountCents) // Each pays €250.00
        }
    }

    @Test
    fun testWhatsAppGroupSummaryContainsRequiredDetails() {
        val report = ExpenseCalculator.generateMonthlyReport(
            periodLabel = "October 2026",
            members = members,
            expenses = listOf(
                Expense(
                    id = 1L,
                    title = "Spar Market",
                    amountCents = 5000L,
                    category = "Grocery",
                    payerMemberId = 1L,
                    splitMemberIds = "1,2,3,4,5"
                )
            )
        )

        val text = WhatsAppShareHelper.buildGroupWhatsAppSummary(report)

        assertTrue(text.contains("RoomieSplit"))
        assertTrue(text.contains("October 2026"))
        assertTrue(text.contains("€50.00"))
        assertTrue(text.contains("Ajaz"))
        assertTrue(text.contains("Fair Settlement Plan"))
    }

    @Test
    fun testTenPlusMembersSplit() {
        val tenMembers = (1..10).map { i ->
            Member(id = i.toLong(), name = "Roommate $i", notes = "Room $i")
        }

        // €150.07 split among 10 roommates -> 15.01 for first 7, 15.00 for remaining 3 -> sum = 150.07
        val expense = Expense(
            id = 99L,
            title = "Large Group Groceries",
            amountCents = 15007L,
            category = "Grocery",
            payerMemberId = 1L,
            splitMemberIds = (1..10).joinToString(",")
        )

        val balances = ExpenseCalculator.calculateBalances(tenMembers, listOf(expense))
        val totalNet = balances.sumOf { it.netBalanceCents }
        assertEquals(0L, totalNet)

        val settlements = ExpenseCalculator.calculateSettlementPlan(balances)
        // Debtor sum must equal creditor sum
        val totalSettled = settlements.sumOf { it.amountCents }
        assertEquals(15007L - 1501L, totalSettled) // Payer's net receivable = 15007 - 1501 = 13506
    }

    @Test
    fun testDetailedPersonalStatement() {
        val expense = Expense(
            id = 1L,
            title = "Ljubljana Rent",
            amountCents = 100000L,
            category = "Room Rent",
            payerMemberId = 1L,
            splitMemberIds = "1,2,3,4,5"
        )
        val balances = ExpenseCalculator.calculateBalances(members, listOf(expense))
        val settlements = ExpenseCalculator.calculateSettlementPlan(balances)
        val ajazBalance = balances.first { it.member.id == 1L }

        val statement = WhatsAppShareHelper.buildDetailedMemberStatement(
            member = member1,
            periodLabel = "October 2026",
            allExpenses = listOf(expense),
            balance = ajazBalance,
            settlements = settlements
        )

        assertTrue(statement.contains("Personal Statement"))
        assertTrue(statement.contains("Ajaz"))
        assertTrue(statement.contains("Ljubljana Rent"))
    }
}
