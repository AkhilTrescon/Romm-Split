package com.example.data.repository

import com.example.data.db.ExpenseDao
import com.example.data.model.Expense
import com.example.data.model.Member
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    val activeMembers: Flow<List<Member>> = expenseDao.getActiveMembers()
    val allMembers: Flow<List<Member>> = expenseDao.getAllMembers()
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesInRange(startMillis: Long, endMillis: Long): Flow<List<Expense>> {
        return expenseDao.getExpensesInRange(startMillis, endMillis)
    }

    suspend fun getMemberById(id: Long): Member? = expenseDao.getMemberById(id)

    suspend fun insertMember(member: Member): Long = expenseDao.insertMember(member)

    suspend fun updateMember(member: Member) = expenseDao.updateMember(member)

    suspend fun deleteMember(member: Member) = expenseDao.deleteMember(member)

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun deleteExpenseById(id: Long) = expenseDao.deleteExpenseById(id)

    suspend fun seedIfEmpty() {
        if (expenseDao.getMemberCount() == 0) {
            com.example.data.db.AppDatabase.populateInitialData(expenseDao)
        }
    }
}
