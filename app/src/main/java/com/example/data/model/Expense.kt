package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amountCents: Long, // Precise euro amount in cents (e.g. €24.50 = 2450L)
    val category: String, // e.g., "Grocery", "Room Rent", "Household Items", "Bills & Utilities", "Other"
    val dateMillis: Long = System.currentTimeMillis(),
    val payerMemberId: Long,
    val notes: String = "",
    val receiptImagePath: String? = null, // Local absolute path to saved receipt image
    val splitMemberIds: String = "" // Comma separated IDs of members sharing the cost, e.g. "1,2,3,4,5"
) {
    fun getSplitMemberIdList(): List<Long> {
        if (splitMemberIds.isBlank()) return emptyList()
        return splitMemberIds.split(",")
            .mapNotNull { it.trim().toLongOrNull() }
    }

    val amountFormatted: String
        get() {
            val euros = amountCents / 100.0
            return String.format(java.util.Locale.US, "€%.2f", euros)
        }
}
