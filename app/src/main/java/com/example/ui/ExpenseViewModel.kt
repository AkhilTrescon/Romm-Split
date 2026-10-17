package com.example.ui

import android.app.Application
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Expense
import com.example.data.model.Member
import com.example.data.model.MemberBalance
import com.example.data.model.MonthlyReportData
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.FirestoreExpenseRepository
import com.example.data.util.ExpenseCalculator
import com.example.data.util.ImageStorageHelper
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val localRepository: ExpenseRepository
    private val firestoreRepository: FirestoreExpenseRepository

    val activeMembers: StateFlow<List<Member>>
    val allExpenses: StateFlow<List<Expense>>

    // Selected period: "ALL" or "yyyy-MM" (e.g., "2026-10")
    private val _selectedPeriod = MutableStateFlow<String>("ALL")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow<String>("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Dialog & Inspection states
    private val _expenseToEdit = MutableStateFlow<Expense?>(null)
    val expenseToEdit: StateFlow<Expense?> = _expenseToEdit.asStateFlow()

    private val _isAddExpenseDialogOpen = MutableStateFlow(false)
    val isAddExpenseDialogOpen: StateFlow<Boolean> = _isAddExpenseDialogOpen.asStateFlow()

    private val _viewingReceiptExpense = MutableStateFlow<Expense?>(null)
    val viewingReceiptExpense: StateFlow<Expense?> = _viewingReceiptExpense.asStateFlow()

    private val _memberToEdit = MutableStateFlow<Member?>(null)
    val memberToEdit: StateFlow<Member?> = _memberToEdit.asStateFlow()

    private val _isAddMemberDialogOpen = MutableStateFlow(false)
    val isAddMemberDialogOpen: StateFlow<Boolean> = _isAddMemberDialogOpen.asStateFlow()

    private val _selectedMemberBalanceDetail = MutableStateFlow<MemberBalance?>(null)
    val selectedMemberBalanceDetail: StateFlow<MemberBalance?> = _selectedMemberBalanceDetail.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        localRepository = ExpenseRepository(database.expenseDao())
        firestoreRepository = FirestoreExpenseRepository(application, database.expenseDao(), viewModelScope)

        viewModelScope.launch {
            localRepository.seedIfEmpty()
        }

        activeMembers = localRepository.activeMembers.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allExpenses = localRepository.allExpenses.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    private val monthYearFormatter = SimpleDateFormat("yyyy-MM", Locale.US)
    private val monthDisplayFormatter = SimpleDateFormat("MMMM yyyy", Locale.US)

    // Dynamic list of available months formatted nicely
    val availablePeriods: StateFlow<List<Pair<String, String>>> = allExpenses.combine(_selectedPeriod) { expenses, _ ->
        val periods = mutableSetOf<String>()
        val cal = Calendar.getInstance()
        periods.add(monthYearFormatter.format(cal.time)) // current month

        for (exp in expenses) {
            periods.add(monthYearFormatter.format(exp.dateMillis))
        }

        val sorted = periods.toList().sortedDescending()
        val list = mutableListOf<Pair<String, String>>()
        list.add("ALL" to "All Time")
        for (p in sorted) {
            try {
                val date = monthYearFormatter.parse(p)
                val label = if (date != null) monthDisplayFormatter.format(date) else p
                list.add(p to label)
            } catch (_: Exception) {
                list.add(p to p)
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("ALL" to "All Time")
    )

    // Filtered expenses based on period, category, and search query
    val filteredExpenses: StateFlow<List<Expense>> = combine(
        allExpenses,
        _selectedPeriod,
        _selectedCategoryFilter,
        _searchQuery
    ) { expenses, period, category, query ->
        expenses.filter { exp ->
            val matchesPeriod = if (period == "ALL") {
                true
            } else {
                monthYearFormatter.format(exp.dateMillis) == period
            }

            val matchesCategory = category == null || exp.category.equals(category, ignoreCase = true)

            val matchesQuery = query.isBlank() ||
                exp.title.contains(query, ignoreCase = true) ||
                exp.notes.contains(query, ignoreCase = true) ||
                exp.category.contains(query, ignoreCase = true)

            matchesPeriod && matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current Monthly Report based on the chosen period
    val monthlyReport: StateFlow<MonthlyReportData> = combine(
        activeMembers,
        allExpenses,
        _selectedPeriod
    ) { members, expenses, period ->
        val periodExpenses = if (period == "ALL") {
            expenses
        } else {
            expenses.filter { monthYearFormatter.format(it.dateMillis) == period }
        }

        val label = if (period == "ALL") {
            "All Time"
        } else {
            try {
                val d = monthYearFormatter.parse(period)
                if (d != null) monthDisplayFormatter.format(d) else period
            } catch (_: Exception) {
                period
            }
        }

        ExpenseCalculator.generateMonthlyReport(label, members, periodExpenses)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MonthlyReportData(
            periodLabel = "All Time",
            totalExpensesCents = 0L,
            expenseCount = 0,
            averagePerMemberCents = 0L,
            categoryBreakdowns = emptyList(),
            memberBalances = emptyList(),
            settlementPlan = emptyList()
        )
    )

    fun setSelectedPeriod(period: String) {
        _selectedPeriod.value = period
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategoryFilter.value = if (_selectedCategoryFilter.value == category) null else category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // --- Expense Actions ---

    fun openAddExpenseDialog() {
        _expenseToEdit.value = null
        _isAddExpenseDialogOpen.value = true
    }

    fun openEditExpenseDialog(expense: Expense) {
        _expenseToEdit.value = expense
        _isAddExpenseDialogOpen.value = true
    }

    fun closeExpenseDialog() {
        _expenseToEdit.value = null
        _isAddExpenseDialogOpen.value = false
    }

    fun saveExpense(
        title: String,
        amountCents: Long,
        category: String,
        payerMemberId: Long,
        splitMemberIds: List<Long>,
        notes: String,
        receiptImagePath: String?,
        dateMillis: Long
    ) {
        viewModelScope.launch {
            val commaIds = splitMemberIds.joinToString(",")
            val current = _expenseToEdit.value
            val expenseToSave = if (current != null) {
                current.copy(
                    title = title,
                    amountCents = amountCents,
                    category = category,
                    payerMemberId = payerMemberId,
                    splitMemberIds = commaIds,
                    notes = notes,
                    receiptImagePath = receiptImagePath ?: current.receiptImagePath,
                    dateMillis = dateMillis
                )
            } else {
                Expense(
                    id = System.currentTimeMillis(),
                    title = title,
                    amountCents = amountCents,
                    category = category,
                    payerMemberId = payerMemberId,
                    splitMemberIds = commaIds,
                    notes = notes,
                    receiptImagePath = receiptImagePath,
                    dateMillis = dateMillis
                )
            }
            firestoreRepository.saveExpense(expenseToSave)
            closeExpenseDialog()
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            ImageStorageHelper.deleteReceiptImage(expense.receiptImagePath)
            firestoreRepository.deleteExpense(expense)
            if (_viewingReceiptExpense.value?.id == expense.id) {
                _viewingReceiptExpense.value = null
            }
        }
    }

    fun openReceiptViewer(expense: Expense) {
        _viewingReceiptExpense.value = expense
    }

    fun closeReceiptViewer() {
        _viewingReceiptExpense.value = null
    }

    // --- Member Actions ---

    fun openAddMemberDialog() {
        _memberToEdit.value = null
        _isAddMemberDialogOpen.value = true
    }

    fun openEditMemberDialog(member: Member) {
        _memberToEdit.value = member
        _isAddMemberDialogOpen.value = true
    }

    fun closeMemberDialog() {
        _memberToEdit.value = null
        _isAddMemberDialogOpen.value = false
    }

    fun saveMember(name: String, phone: String, colorHex: String, notes: String) {
        viewModelScope.launch {
            val current = _memberToEdit.value
            val memberToSave = if (current != null) {
                current.copy(
                    name = name,
                    phoneNumber = phone,
                    colorHex = colorHex,
                    notes = notes
                )
            } else {
                Member(
                    id = System.currentTimeMillis(),
                    name = name,
                    phoneNumber = phone,
                    colorHex = colorHex,
                    notes = notes
                )
            }
            firestoreRepository.saveMember(memberToSave)
            closeMemberDialog()
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            firestoreRepository.deleteMember(member)
        }
    }

    fun showMemberBalanceDetail(balance: MemberBalance?) {
        _selectedMemberBalanceDetail.value = balance
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        Firebase.auth.signOut()
        viewModelScope.launch {
            try {
                val credManager = CredentialManager.create(context)
                credManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (_: Exception) {}
            onComplete()
        }
    }
}
