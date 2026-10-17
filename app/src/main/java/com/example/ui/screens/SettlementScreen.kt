package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Expense
import com.example.data.model.MemberBalance
import com.example.data.model.MonthlyReportData
import com.example.data.model.SettlementTransaction
import com.example.data.util.WhatsAppShareHelper
import com.example.ui.components.MemberAvatar

@Composable
fun SettlementScreen(
    report: MonthlyReportData,
    allExpenses: List<Expense>,
    selectedPeriod: String,
    availablePeriods: List<Pair<String, String>>,
    onPeriodSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var periodMenuExpanded by remember { mutableStateOf(false) }
    var selectedBalanceForDetail by remember { mutableStateOf<MemberBalance?>(null) }
    var balanceSearchQuery by remember { mutableStateOf("") }
    var balanceFilterStatus by remember { mutableStateOf("ALL") }

    val currentPeriodLabel = availablePeriods.firstOrNull { it.first == selectedPeriod }?.second ?: "All Time"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settlement_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period & Action Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    ElevatedAssistChip(
                        onClick = { periodMenuExpanded = true },
                        label = {
                            Text(
                                text = "Period: $currentPeriodLabel",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.testTag("settlement_period_chip")
                    )

                    DropdownMenu(
                        expanded = periodMenuExpanded,
                        onDismissRequest = { periodMenuExpanded = false }
                    ) {
                        availablePeriods.forEach { (periodKey, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (periodKey == selectedPeriod) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    periodMenuExpanded = false
                                    onPeriodSelected(periodKey)
                                }
                            )
                        }
                    }
                }

                // WhatsApp Share Button
                Button(
                    onClick = {
                        val message = WhatsAppShareHelper.buildGroupWhatsAppSummary(report)
                        WhatsAppShareHelper.shareText(context, message)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF25D366), // Official WhatsApp Green
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("share_settlement_whatsapp_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp Report", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Settlement Banner Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Fair Monthly Settlement",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Total period expenses: ${report.totalFormatted} across ${report.expenseCount} bills. Debts are minimized so each flatmate pays or receives the exact fair balance.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Settlement Transactions Section (Who pays whom)
        item {
            Text(
                text = "Fair Settlement Plan (Who Pays Whom)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (report.settlementPlan.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "All balances are settled! No payments required between housemates.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(items = report.settlementPlan) { transaction ->
                SettlementTransactionCard(
                    transaction = transaction,
                    periodLabel = currentPeriodLabel
                )
            }
        }

        // Member Balances Header
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Housemate Balances",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${report.memberBalances.size} members",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (report.memberBalances.size > 5) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = balanceSearchQuery,
                    onValueChange = { balanceSearchQuery = it },
                    placeholder = { Text("Search flatmate balance...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    androidx.compose.material3.FilterChip(
                        selected = balanceFilterStatus == "ALL",
                        onClick = { balanceFilterStatus = "ALL" },
                        label = { Text("All (${report.memberBalances.size})", fontSize = 11.sp) }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = balanceFilterStatus == "OWES",
                        onClick = { balanceFilterStatus = "OWES" },
                        label = { Text("Owes (${report.memberBalances.count { it.owesMoney }})", fontSize = 11.sp) }
                    )
                    androidx.compose.material3.FilterChip(
                        selected = balanceFilterStatus == "RECEIVES",
                        onClick = { balanceFilterStatus = "RECEIVES" },
                        label = { Text("Receives (${report.memberBalances.count { it.isOwed }})", fontSize = 11.sp) }
                    )
                }
            }
        }

        // Member Balances List
        val filteredBalances = report.memberBalances.filter { b ->
            val matchesQuery = balanceSearchQuery.isBlank() || b.member.name.contains(balanceSearchQuery, ignoreCase = true)
            val matchesStatus = when (balanceFilterStatus) {
                "OWES" -> b.owesMoney
                "RECEIVES" -> b.isOwed
                else -> true
            }
            matchesQuery && matchesStatus
        }

        items(items = filteredBalances, key = { it.member.id }) { balance ->
            MemberBalanceCard(
                balance = balance,
                periodLabel = currentPeriodLabel,
                settlements = report.settlementPlan,
                onClick = { selectedBalanceForDetail = balance }
            )
        }

        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // Detail dialog for clicked member
    selectedBalanceForDetail?.let { detailBalance ->
        MemberDetailDialog(
            balance = detailBalance,
            allExpenses = allExpenses,
            onDismiss = { selectedBalanceForDetail = null }
        )
    }
}

@Composable
fun SettlementTransactionCard(
    transaction: SettlementTransaction,
    periodLabel: String
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // From Avatar & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                MemberAvatar(
                    name = transaction.fromMember.name,
                    colorHex = transaction.fromMember.colorHex,
                    size = 36.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = transaction.fromMember.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "pays",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Arrow & Amount in EUR
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = transaction.amountFormatted,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "pays to",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            // To Avatar & Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.weight(1f)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = transaction.toMember.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "receives",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                MemberAvatar(
                    name = transaction.toMember.name,
                    colorHex = transaction.toMember.colorHex,
                    size = 36.dp
                )

                Spacer(modifier = Modifier.width(6.dp))

                // WhatsApp Send Icon for this exact settlement payment
                IconButton(
                    onClick = {
                        val message = "👋 Hi *${transaction.fromMember.name}*, for our *RoomieSplit* expenses ($periodLabel), please transfer *${transaction.amountFormatted}* to *${transaction.toMember.name}*. Thanks! 🏠🇸🇮"
                        WhatsAppShareHelper.shareText(
                            context = context,
                            text = message,
                            phoneNumber = transaction.fromMember.phoneNumber
                        )
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF25D366).copy(alpha = 0.15f))
                        .testTag("send_whatsapp_transfer_${transaction.fromMember.id}_to_${transaction.toMember.id}")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send reminder on WhatsApp",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MemberBalanceCard(
    balance: MemberBalance,
    periodLabel: String,
    settlements: List<SettlementTransaction>,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    val statusColor = when {
        balance.isOwed -> Color(0xFF2E7D32) // Green: receives
        balance.owesMoney -> MaterialTheme.colorScheme.error // Red: owes
        else -> MaterialTheme.colorScheme.onSurfaceVariant // Settled
    }

    val statusText = when {
        balance.isOwed -> "Receives ${balance.netBalanceFormatted}"
        balance.owesMoney -> "Owes ${balance.netBalanceFormatted}"
        else -> "Settled €0.00"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("member_balance_card_${balance.member.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(
                name = balance.member.name,
                colorHex = balance.member.colorHex,
                size = 44.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = balance.member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (balance.member.notes.isNotBlank()) {
                    Text(
                        text = balance.member.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text(
                        text = "Paid: ${balance.totalPaidFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = " • ",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Share: ${balance.totalShareFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status chip & WhatsApp button
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val personalMsg = WhatsAppShareHelper.buildIndividualWhatsAppSummary(
                                balance = balance,
                                periodLabel = periodLabel,
                                settlements = settlements
                            )
                            WhatsAppShareHelper.shareText(
                                context = context,
                                text = personalMsg,
                                phoneNumber = balance.member.phoneNumber
                            )
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send WhatsApp summary",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "View details",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MemberDetailDialog(
    balance: MemberBalance,
    allExpenses: List<Expense>,
    onDismiss: () -> Unit
) {
    val paidExpenses = allExpenses.filter { it.payerMemberId == balance.member.id }
    val sharedExpenses = allExpenses.filter { it.getSplitMemberIdList().contains(balance.member.id) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MemberAvatar(
                        name = balance.member.name,
                        colorHex = balance.member.colorHex,
                        size = 40.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = balance.member.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (balance.member.phoneNumber.isNotBlank()) balance.member.phoneNumber else "Housemate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Paid for Flat:", style = MaterialTheme.typography.bodyMedium)
                            Text(balance.totalPaidFormatted, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fair Share Owed:", style = MaterialTheme.typography.bodyMedium)
                            Text(balance.totalShareFormatted, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Net Balance:", fontWeight = FontWeight.Bold)
                            val color = if (balance.isOwed) Color(0xFF2E7D32) else if (balance.owesMoney) MaterialTheme.colorScheme.error else Color.Gray
                            Text(
                                text = if (balance.isOwed) "+${balance.netBalanceFormatted}" else if (balance.owesMoney) "-${balance.netBalanceFormatted}" else "€0.00",
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bills Paid (${paidExpenses.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                paidExpenses.take(3).forEach {
                    Text(
                        text = "• ${it.title} (${it.amountFormatted})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Included in ${sharedExpenses.size} shared bills",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
