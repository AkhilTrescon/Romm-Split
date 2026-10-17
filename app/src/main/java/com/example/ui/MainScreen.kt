package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ReceiptViewerDialog
import com.example.ui.screens.AddEditExpenseDialog
import com.example.ui.screens.AddEditMemberDialog
import com.example.ui.screens.ExpensesScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.MonthlyReportScreen
import com.example.ui.screens.SettlementScreen

enum class AppTab(val title: String, val icon: ImageVector) {
    BILLS("Bills", Icons.Default.ReceiptLong),
    SETTLEMENT("Settlement", Icons.Default.AccountBalanceWallet),
    REPORT("Report", Icons.Default.BarChart),
    HOUSEMATES("Flatmates", Icons.Default.Group)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: ExpenseViewModel = viewModel()
) {
    var selectedTabOrdinal by rememberSaveable { mutableIntStateOf(AppTab.BILLS.ordinal) }
    val currentTab = AppTab.entries[selectedTabOrdinal]

    val members by viewModel.activeMembers.collectAsStateWithLifecycle()
    val allExpenses by viewModel.allExpenses.collectAsStateWithLifecycle()
    val filteredExpenses by viewModel.filteredExpenses.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val availablePeriods by viewModel.availablePeriods.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val monthlyReport by viewModel.monthlyReport.collectAsStateWithLifecycle()

    val isAddExpenseDialogOpen by viewModel.isAddExpenseDialogOpen.collectAsStateWithLifecycle()
    val expenseToEdit by viewModel.expenseToEdit.collectAsStateWithLifecycle()
    val viewingReceiptExpense by viewModel.viewingReceiptExpense.collectAsStateWithLifecycle()
    val isAddMemberDialogOpen by viewModel.isAddMemberDialogOpen.collectAsStateWithLifecycle()
    val memberToEdit by viewModel.memberToEdit.collectAsStateWithLifecycle()

    // Handle back button when on sub-screens
    BackHandler(enabled = currentTab != AppTab.BILLS) {
        selectedTabOrdinal = AppTab.BILLS.ordinal
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🇸🇮 RoomieSplit",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "EUR €",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    val context = LocalContext.current
                    var showAccountMenu by remember { mutableStateOf(false) }
                    val user = Firebase.auth.currentUser

                    IconButton(
                        onClick = { showAccountMenu = true },
                        modifier = Modifier.testTag("account_sync_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Cloud Sync & Account",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showAccountMenu,
                        onDismissRequest = { showAccountMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "Cloud Sync Active",
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                    Text(
                                        text = user?.email ?: "Signed in with Google",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = { showAccountMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sign Out", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showAccountMenu = false
                                viewModel.signOut(context) {}
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("main_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { selectedTabOrdinal = tab.ordinal },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = { Text(tab.title) },
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.BILLS -> {
                    ExpensesScreen(
                        expenses = filteredExpenses,
                        allMembers = members,
                        selectedPeriod = selectedPeriod,
                        availablePeriods = availablePeriods,
                        selectedCategory = selectedCategory,
                        searchQuery = searchQuery,
                        onPeriodSelected = { viewModel.setSelectedPeriod(it) },
                        onCategorySelected = { viewModel.setSelectedCategory(it) },
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                        onAddExpense = { viewModel.openAddExpenseDialog() },
                        onEditExpense = { viewModel.openEditExpenseDialog(it) },
                        onDeleteExpense = { viewModel.deleteExpense(it) },
                        onViewReceipt = { viewModel.openReceiptViewer(it) }
                    )
                }
                AppTab.SETTLEMENT -> {
                    SettlementScreen(
                        report = monthlyReport,
                        allExpenses = allExpenses,
                        selectedPeriod = selectedPeriod,
                        availablePeriods = availablePeriods,
                        onPeriodSelected = { viewModel.setSelectedPeriod(it) }
                    )
                }
                AppTab.REPORT -> {
                    MonthlyReportScreen(
                        report = monthlyReport,
                        selectedPeriod = selectedPeriod,
                        availablePeriods = availablePeriods,
                        onPeriodSelected = { viewModel.setSelectedPeriod(it) }
                    )
                }
                AppTab.HOUSEMATES -> {
                    MembersScreen(
                        members = members,
                        allExpenses = allExpenses,
                        monthlyReport = monthlyReport,
                        onAddMember = { viewModel.openAddMemberDialog() },
                        onEditMember = { viewModel.openEditMemberDialog(it) },
                        onDeleteMember = { viewModel.deleteMember(it) }
                    )
                }
            }
        }
    }

    // Add / Edit Expense Dialog
    if (isAddExpenseDialogOpen) {
        AddEditExpenseDialog(
            expenseToEdit = expenseToEdit,
            members = members,
            onDismiss = { viewModel.closeExpenseDialog() },
            onSave = { title, amountCents, category, payerId, splitMemberIds, notes, receiptImagePath, dateMillis ->
                viewModel.saveExpense(
                    title = title,
                    amountCents = amountCents,
                    category = category,
                    payerMemberId = payerId,
                    splitMemberIds = splitMemberIds,
                    notes = notes,
                    receiptImagePath = receiptImagePath,
                    dateMillis = dateMillis
                )
            }
        )
    }

    // Bill Print / Receipt Viewer Dialog
    viewingReceiptExpense?.let { expense ->
        val payer = members.firstOrNull { it.id == expense.payerMemberId }
        ReceiptViewerDialog(
            expense = expense,
            payer = payer,
            onDismiss = { viewModel.closeReceiptViewer() }
        )
    }

    // Add / Edit Member Dialog
    if (isAddMemberDialogOpen) {
        AddEditMemberDialog(
            memberToEdit = memberToEdit,
            onDismiss = { viewModel.closeMemberDialog() },
            onSave = { name, phone, colorHex, notes ->
                viewModel.saveMember(name, phone, colorHex, notes)
            }
        )
    }
}
