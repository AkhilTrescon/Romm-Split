package com.example.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

@IgnoreExtraProperties
data class FirestoreMember(
    @DocumentId val docId: String = "",
    val id: Long = 0L,
    val name: String = "",
    val phoneNumber: String = "",
    val colorHex: String = "#1E88E5",
    val notes: String = "",
    val isArchived: Boolean = false,
    @ServerTimestamp val createdAt: Date? = null
) {
    fun toMember(): Member {
        return Member(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            colorHex = colorHex,
            notes = notes,
            isArchived = isArchived,
            createdAt = createdAt?.time ?: System.currentTimeMillis()
        )
    }

    companion object {
        fun fromMember(member: Member): FirestoreMember {
            return FirestoreMember(
                docId = member.id.toString(),
                id = member.id,
                name = member.name,
                phoneNumber = member.phoneNumber,
                colorHex = member.colorHex,
                notes = member.notes,
                isArchived = member.isArchived
            )
        }
    }
}

@IgnoreExtraProperties
data class FirestoreExpense(
    @DocumentId val docId: String = "",
    val id: Long = 0L,
    val title: String = "",
    val amountCents: Long = 0L,
    val category: String = "Grocery",
    val dateMillis: Long = 0L,
    val payerMemberId: Long = 0L,
    val notes: String = "",
    val receiptImagePath: String? = null,
    val splitMemberIds: String = "",
    @ServerTimestamp val createdAt: Date? = null
) {
    fun toExpense(): Expense {
        return Expense(
            id = id,
            title = title,
            amountCents = amountCents,
            category = category,
            dateMillis = if (dateMillis > 0L) dateMillis else (createdAt?.time ?: System.currentTimeMillis()),
            payerMemberId = payerMemberId,
            notes = notes,
            receiptImagePath = receiptImagePath,
            splitMemberIds = splitMemberIds
        )
    }

    companion object {
        fun fromExpense(expense: Expense): FirestoreExpense {
            return FirestoreExpense(
                docId = if (expense.id > 0) expense.id.toString() else System.currentTimeMillis().toString(),
                id = if (expense.id > 0) expense.id else System.currentTimeMillis(),
                title = expense.title,
                amountCents = expense.amountCents,
                category = expense.category,
                dateMillis = expense.dateMillis,
                payerMemberId = expense.payerMemberId,
                notes = expense.notes,
                receiptImagePath = expense.receiptImagePath,
                splitMemberIds = expense.splitMemberIds
            )
        }
    }
}
