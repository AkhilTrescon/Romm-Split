package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.data.model.Expense
import com.example.data.model.Member
import com.example.data.util.ImageStorageHelper
import com.example.data.util.ReceiptScannerHelper
import com.example.ui.components.MemberAvatar
import com.example.ui.components.getCategoryVisuals
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditExpenseDialog(
    expenseToEdit: Expense?,
    members: List<Member>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amountCents: Long,
        category: String,
        payerId: Long,
        splitMemberIds: List<Long>,
        notes: String,
        receiptImagePath: String?,
        dateMillis: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(expenseToEdit?.title ?: "") }
    var amountString by remember {
        mutableStateOf(
            expenseToEdit?.let {
                String.format(Locale.US, "%.2f", it.amountCents / 100.0)
            } ?: ""
        )
    }
    var category by remember { mutableStateOf(expenseToEdit?.category ?: "Grocery") }
    var payerId by remember {
        mutableLongStateOf(
            expenseToEdit?.payerMemberId ?: (members.firstOrNull()?.id ?: 1L)
        )
    }
    var notes by remember { mutableStateOf(expenseToEdit?.notes ?: "") }
    var receiptImagePath by remember { mutableStateOf<String?>(expenseToEdit?.receiptImagePath) }
    var dateMillis by remember { mutableLongStateOf(expenseToEdit?.dateMillis ?: System.currentTimeMillis()) }

    var isScanningReceipt by remember { mutableStateOf(false) }
    var scanResultMessage by remember { mutableStateOf<String?>(null) }
    var memberFilterQuery by remember { mutableStateOf("") }

    val splitMemberIds = remember {
        mutableStateListOf<Long>().apply {
            if (expenseToEdit != null) {
                addAll(expenseToEdit.getSplitMemberIdList())
            } else {
                addAll(members.map { it.id })
            }
        }
    }

    // Function to scan receipt file using ML Kit Text Recognition
    fun scanReceiptFile(imagePath: String) {
        val file = File(imagePath)
        if (!file.exists()) return

        coroutineScope.launch {
            isScanningReceipt = true
            scanResultMessage = null
            try {
                val scannedData = ReceiptScannerHelper.scanReceipt(context, file)

                var matchedDetails = mutableListOf<String>()

                if (!scannedData.detectedTitle.isNullOrBlank()) {
                    title = scannedData.detectedTitle
                    matchedDetails.add(scannedData.detectedTitle)
                }
                if (scannedData.detectedAmountCents != null && scannedData.detectedAmountCents > 0L) {
                    amountString = String.format(Locale.US, "%.2f", scannedData.detectedAmountCents / 100.0)
                    matchedDetails.add("€$amountString")
                }
                if (!scannedData.detectedCategory.isNullOrBlank()) {
                    category = scannedData.detectedCategory
                    matchedDetails.add(scannedData.detectedCategory)
                }
                if (scannedData.detectedDateMillis != null) {
                    dateMillis = scannedData.detectedDateMillis
                }

                if (matchedDetails.isNotEmpty()) {
                    scanResultMessage = "Read from bill: ${matchedDetails.joinToString(" • ")}"
                    Toast.makeText(context, "Bill details extracted automatically!", Toast.LENGTH_SHORT).show()
                } else {
                    scanResultMessage = "Bill print attached. Please verify or enter amount if not clearly visible."
                }
            } catch (e: Exception) {
                scanResultMessage = "Could not parse text automatically. You can enter details manually."
            } finally {
                isScanningReceipt = false
            }
        }
    }

    // Photo picker launcher for bill receipt
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageHelper.saveReceiptImage(context, uri)
            if (savedPath != null) {
                receiptImagePath = savedPath
                scanReceiptFile(savedPath)
            } else {
                Toast.makeText(context, "Failed to save bill receipt", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val categories = listOf(
        "Grocery",
        "Room Rent",
        "Household Items",
        "Bills & Utilities",
        "Dining & Takeout",
        "Transportation",
        "Other"
    )

    // Calculate approximate share per person for preview
    val parsedAmountCents = (amountString.replace(",", ".").toDoubleOrNull()?.times(100))?.toLong() ?: 0L
    val perPersonCents = if (splitMemberIds.isNotEmpty()) parsedAmountCents / splitMemberIds.size else 0L

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
                .testTag("add_edit_expense_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (expenseToEdit == null) "Add Bill / Expense" else "Edit Bill / Expense",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Upload bill print to auto-read details, or enter manually",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("dialog_close_button")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable content form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Bill Print / Receipt Photo Section (Highlighted at top for easy upload-first flow)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DocumentScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Bill Print (Auto-Reads Amount & Store)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (isScanningReceipt) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val imagePath = receiptImagePath
                            val file = if (!imagePath.isNullOrBlank()) File(imagePath) else null

                            if (file != null && file.exists()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.85f))
                                ) {
                                    SubcomposeAsyncImage(
                                        model = file,
                                        contentDescription = "Bill print preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                        loading = {
                                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                                            }
                                        }
                                    )

                                    // Action buttons overlay
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                    ) {
                                        IconButton(
                                            onClick = { scanReceiptFile(file.absolutePath) },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "Re-scan bill",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                photoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddAPhoto,
                                                contentDescription = "Change photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(
                                            onClick = {
                                                ImageStorageHelper.deleteReceiptImage(receiptImagePath)
                                                receiptImagePath = null
                                                scanResultMessage = null
                                            },
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.6f))
                                                .testTag("remove_receipt_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Remove receipt",
                                                tint = Color.Red,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .testTag("attach_bill_button"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload Bill Print / Photo")
                                }
                            }

                            // Scan status badge
                            if (scanResultMessage != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = scanResultMessage!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Title Input (Optional - automatically filled or defaults to category/receipt)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Expense Name (Optional)") },
                        placeholder = { Text("e.g. Spar, Apartment Rent, Telemach (or leave blank)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_title_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Amount Input (Optional if read from bill or manually entered)
                    OutlinedTextField(
                        value = amountString,
                        onValueChange = { amountString = it },
                        label = { Text("Amount in Slovenia (€ EUR) (Optional)") },
                        placeholder = { Text("0.00 (auto-read from bill if uploaded)") },
                        leadingIcon = {
                            Icon(Icons.Default.Euro, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_amount_input")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Category Selector
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = category.equals(cat, ignoreCase = true)
                            val (catIcon, catColor) = getCategoryVisuals(cat)
                            FilterChip(
                                selected = isSelected,
                                onClick = { category = cat },
                                label = { Text(cat) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = catIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else catColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Payer Selector: Who Paid?
                    Text(
                        text = "Who Paid the Bill?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        members.forEach { member ->
                            val isSelected = member.id == payerId
                            FilterChip(
                                selected = isSelected,
                                onClick = { payerId = member.id },
                                label = { Text(member.name) },
                                leadingIcon = {
                                    MemberAvatar(
                                        name = member.name,
                                        colorHex = member.colorHex,
                                        size = 20.dp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                modifier = Modifier.testTag("payer_chip_${member.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Split Members Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Split With (${splitMemberIds.size} of ${members.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (splitMemberIds.isNotEmpty() && parsedAmountCents > 0) {
                                Text(
                                    text = String.format(Locale.US, "≈ €%.2f each", perPersonCents / 100.0),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Row {
                            TextButton(
                                onClick = {
                                    splitMemberIds.clear()
                                    splitMemberIds.addAll(members.map { it.id })
                                }
                            ) {
                                Text("All", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = {
                                    splitMemberIds.clear()
                                }
                            ) {
                                Text("Clear", fontSize = 12.sp)
                            }
                        }
                    }

                    if (members.size > 6) {
                        OutlinedTextField(
                            value = memberFilterQuery,
                            onValueChange = { memberFilterQuery = it },
                            placeholder = { Text("Filter flatmates...", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val displayMembers = if (memberFilterQuery.isBlank()) members else members.filter { it.name.contains(memberFilterQuery, ignoreCase = true) }
                        displayMembers.forEach { member ->
                            val isIncluded = splitMemberIds.contains(member.id)
                            FilterChip(
                                selected = isIncluded,
                                onClick = {
                                    if (isIncluded) {
                                        splitMemberIds.remove(member.id)
                                    } else {
                                        splitMemberIds.add(member.id)
                                    }
                                },
                                label = { Text(member.name) },
                                leadingIcon = {
                                    if (isIncluded) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        MemberAvatar(
                                            name = member.name,
                                            colorHex = member.colorHex,
                                            size = 18.dp
                                        )
                                    }
                                },
                                modifier = Modifier.testTag("split_chip_${member.id}")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Optional Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes / Store Details (Optional)") },
                        placeholder = { Text("e.g. Ljubljana Spar Supermarket receipt") },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("expense_notes_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Actions: Cancel & Save (No mandatory block!)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_expense_button")
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            // Non-mandatory expense name: Fall back to category or "Bill Receipt"
                            val finalTitle = if (title.trim().isNotEmpty()) {
                                title.trim()
                            } else if (receiptImagePath != null) {
                                "$category Receipt"
                            } else {
                                "$category Expense"
                            }

                            // Non-mandatory amount: parse clean amount or default to 0.00
                            val cleanAmount = amountString.replace(",", ".").toDoubleOrNull() ?: 0.0
                            val cents = Math.round(cleanAmount * 100.0)

                            // Default split to all members if none selected
                            val finalSplitMembers = if (splitMemberIds.isNotEmpty()) {
                                splitMemberIds.toList()
                            } else {
                                members.map { it.id }
                            }

                            onSave(
                                finalTitle,
                                cents,
                                category,
                                payerId,
                                finalSplitMembers,
                                notes.trim(),
                                receiptImagePath,
                                dateMillis
                            )
                        },
                        modifier = Modifier.testTag("save_expense_button")
                    ) {
                        Text("Save Expense")
                    }
                }
            }
        }
    }
}
