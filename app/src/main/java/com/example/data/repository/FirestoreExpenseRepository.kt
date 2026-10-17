package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.db.ExpenseDao
import com.example.data.model.Expense
import com.example.data.model.FirestoreExpense
import com.example.data.model.FirestoreMember
import com.example.data.model.Member
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class FirestoreExpenseRepository(
    private val context: Context,
    private val localDao: ExpenseDao,
    private val scope: CoroutineScope
) {
    private val TAG = "FirestoreExpenseRepo"

    private val db: FirebaseFirestore by lazy {
        val databaseId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }

    private val flatId = "slovenia_roomies"

    private val membersRef by lazy {
        db.collection("flats").document(flatId).collection("members")
    }

    private val expensesRef by lazy {
        db.collection("flats").document(flatId).collection("expenses")
    }

    init {
        // Start background sync from Firestore to local Room database whenever user is authenticated
        scope.launch(Dispatchers.IO) {
            observeRemoteMembers()
        }
        scope.launch(Dispatchers.IO) {
            observeRemoteExpenses()
        }
    }

    private suspend fun observeRemoteMembers() {
        try {
            membersRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FirestoreMember::class.java).map { it.toMember() }
                }
                .catch { e ->
                    Log.e(TAG, "Error observing remote members", e)
                }
                .collect { remoteMembers ->
                    if (remoteMembers.isNotEmpty()) {
                        localDao.insertMembers(remoteMembers)
                    } else if (Firebase.auth.currentUser != null) {
                        seedRemoteInitialDataIfEmpty()
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start observing remote members", e)
        }
    }

    private suspend fun observeRemoteExpenses() {
        try {
            expensesRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(FirestoreExpense::class.java).map { it.toExpense() }
                }
                .catch { e ->
                    Log.e(TAG, "Error observing remote expenses", e)
                }
                .collect { remoteExpenses ->
                    if (remoteExpenses.isNotEmpty()) {
                        localDao.insertExpenses(remoteExpenses)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start observing remote expenses", e)
        }
    }

    suspend fun seedRemoteInitialDataIfEmpty() {
        try {
            val countSnapshot = membersRef.limit(1).get().await()
            if (countSnapshot.isEmpty) {
                // Seed 5 housemates living together in Slovenia
                val member1 = Member(id = 1L, name = "Ajaz", phoneNumber = "+386 40 123 456", colorHex = "#1E88E5", notes = "Room 1")
                val member2 = Member(id = 2L, name = "Luka", phoneNumber = "+386 41 234 567", colorHex = "#43A047", notes = "Room 2")
                val member3 = Member(id = 3L, name = "Matej", phoneNumber = "+386 51 345 678", colorHex = "#FB8C00", notes = "Room 3")
                val member4 = Member(id = 4L, name = "Ana", phoneNumber = "+386 70 456 789", colorHex = "#E53935", notes = "Room 4")
                val member5 = Member(id = 5L, name = "Jan", phoneNumber = "+386 31 567 890", colorHex = "#8E24AA", notes = "Living Room")

                val seedMembers = listOf(member1, member2, member3, member4, member5)
                for (m in seedMembers) {
                    membersRef.document(m.id.toString()).set(FirestoreMember.fromMember(m)).await()
                }

                val now = System.currentTimeMillis()
                val dayMillis = 24 * 60 * 60 * 1000L
                val allIds = "1,2,3,4,5"

                val seedExpenses = listOf(
                    Expense(
                        id = 1L,
                        title = "Apartment Rent (Ljubljana)",
                        amountCents = 125000L,
                        category = "Room Rent",
                        dateMillis = now - (5 * dayMillis),
                        payerMemberId = 1L,
                        notes = "Monthly flat rent transfer via NLB bank",
                        splitMemberIds = allIds
                    ),
                    Expense(
                        id = 2L,
                        title = "Spar Weekly Groceries",
                        amountCents = 14850L,
                        category = "Grocery",
                        dateMillis = now - (3 * dayMillis),
                        payerMemberId = 2L,
                        notes = "Vegetables, milk, eggs, bread, pasta and olive oil",
                        splitMemberIds = allIds
                    ),
                    Expense(
                        id = 3L,
                        title = "Telemach High-Speed Internet & TV",
                        amountCents = 4290L,
                        category = "Bills & Utilities",
                        dateMillis = now - (2 * dayMillis),
                        payerMemberId = 3L,
                        notes = "Monthly fiber wifi invoice",
                        splitMemberIds = allIds
                    )
                )

                for (exp in seedExpenses) {
                    expensesRef.document(exp.id.toString()).set(FirestoreExpense.fromExpense(exp)).await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error seeding remote data", e)
        }
    }

    // --- Write methods that push to Cloud Firestore and update local cache ---

    suspend fun saveMember(member: Member) {
        val memberToSave = if (member.id <= 0) {
            member.copy(id = System.currentTimeMillis())
        } else {
            member
        }

        // Local instant update
        localDao.insertMember(memberToSave)

        // Cloud sync
        try {
            if (Firebase.auth.currentUser != null) {
                membersRef.document(memberToSave.id.toString())
                    .set(FirestoreMember.fromMember(memberToSave))
                    .await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloud write failed for member, saved locally", e)
        }
    }

    suspend fun deleteMember(member: Member) {
        localDao.deleteMember(member)
        try {
            if (Firebase.auth.currentUser != null) {
                membersRef.document(member.id.toString()).delete().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloud delete failed for member", e)
        }
    }

    suspend fun saveExpense(expense: Expense) {
        val expenseToSave = if (expense.id <= 0) {
            expense.copy(id = System.currentTimeMillis())
        } else {
            expense
        }

        // Local instant update
        localDao.insertExpense(expenseToSave)

        // Cloud sync to all roommates
        try {
            if (Firebase.auth.currentUser != null) {
                expensesRef.document(expenseToSave.id.toString())
                    .set(FirestoreExpense.fromExpense(expenseToSave))
                    .await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloud write failed for expense, saved locally", e)
        }
    }

    suspend fun deleteExpense(expense: Expense) {
        localDao.deleteExpense(expense)
        try {
            if (Firebase.auth.currentUser != null) {
                expensesRef.document(expense.id.toString()).delete().await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Cloud delete failed for expense", e)
        }
    }
}
