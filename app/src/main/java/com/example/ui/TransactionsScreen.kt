package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Expense
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    expenses: List<Expense>,
    viewModel: ExpenseViewModel
) {
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }

    // Filtered transaction list
    val filteredExpenses = remember(expenses, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") {
            expenses
        } else {
            expenses.filter { it.category == selectedCategoryFilter }
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = DeepSpaceDark,
                shape = CircleShape,
                modifier = Modifier
                    .testTag("add_expense_fab")
                    .padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Expense Item",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Header
            Text(
                text = "RUPEE LEDGER",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "All Transactions",
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Category filters horizontally scrollable row
            LazyRow(
                modifier = Modifier.fillMaxWidth().testTag("category_filters_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                item {
                    val isSelected = selectedCategoryFilter == "All"
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = "All" },
                        label = { Text("All 🌍") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SlateSurface,
                            labelColor = TextGray,
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.White
                        ),
                        border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else SlateBorder)
                    )
                }

                items(ExpenseUtils.categories) { cat ->
                    val isSelected = selectedCategoryFilter == cat
                    val emoji = ExpenseUtils.getEmoji(cat)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryFilter = cat },
                        label = { Text("$cat $emoji") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SlateSurface,
                            labelColor = TextGray,
                            selectedContainerColor = ExpenseUtils.getColor(cat),
                            selectedLabelColor = Color.White
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) ExpenseUtils.getColor(cat) else SlateBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable expense list
            if (filteredExpenses.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.weight(1f).testTag("expenses_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredExpenses, key = { it.id }) { item ->
                        ExpenseItemCard(
                            expense = item,
                            onDelete = { viewModel.deleteExpense(item) }
                        )
                    }
                }
            } else {
                // Empty state for filters or ledger
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "🔍",
                            fontSize = 64.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No items for '$selectedCategoryFilter'",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You can tap the '+' button in the bottom right corner to record a transaction in this segment.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Add Expense Dialog Component
    if (showAddDialog) {
        AddExpenseDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, category, desc, date ->
                viewModel.addExpense(
                    title = title,
                    amount = amount,
                    category = category,
                    dateMillis = date,
                    description = desc
                )
                showAddDialog = false
            }
        )
    }
}

@Composable
fun ExpenseItemCard(
    expense: Expense,
    onDelete: () -> Unit
) {
    val dateString = remember(expense.dateMillis) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val today = Calendar.getInstance()
        val expCalendar = Calendar.getInstance().apply { timeInMillis = expense.dateMillis }
        
        if (today.get(Calendar.YEAR) == expCalendar.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == expCalendar.get(Calendar.DAY_OF_YEAR)) {
            "Today"
        } else {
            today.add(Calendar.DAY_OF_YEAR, -1)
            if (today.get(Calendar.YEAR) == expCalendar.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == expCalendar.get(Calendar.DAY_OF_YEAR)) {
                "Yesterday"
            } else {
                sdf.format(Date(expense.dateMillis))
            }
        }
    }

    val catColor = ExpenseUtils.getColor(expense.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
            .testTag("expense_card_${expense.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SlateSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circle Badge with Category Emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.15f))
                    .border(1.dp, catColor.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = ExpenseUtils.getEmoji(expense.category),
                    fontSize = 22.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                    Text(text = "•", color = TextGray, fontSize = 10.sp)
                    Text(
                        text = expense.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = catColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (expense.description.trim().isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = expense.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount + Delete icon Column
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = ExpenseUtils.formatRupee(expense.amount),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.End
                )

                // Delete Icon button - 48x48 touches
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("delete_expense_${expense.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete expense",
                        tint = CoralAlert.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AddExpenseDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double, String, String, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountStr by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food & Groceries") }
    var description by remember { mutableStateOf("") }
    var isAmountError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, SlateBorder, RoundedCornerShape(24.dp))
                .testTag("add_expense_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Track New Spend 💸",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What did you spend on?") },
                    placeholder = { Text("e.g., Cold Coffee, Metro Fare") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedLabelColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_expense_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Amount Input In INR
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        isAmountError = it.toDoubleOrNull() == null && it.isNotEmpty()
                    },
                    label = { Text("Amount in Rupees (₹)") },
                    prefix = { Text("₹ ", color = EmeraldPrimary, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = isAmountError,
                    placeholder = { Text("0.00") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedLabelColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        errorBorderColor = CoralAlert
                    ),
                    supportingText = {
                        if (isAmountError) {
                            Text("Please enter a valid numeric amount", color = CoralAlert)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("add_expense_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category Chips Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Category Segment",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Simple wrap list of interactive Category Chips
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 3
                    ) {
                        ExpenseUtils.categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            val catColor = ExpenseUtils.getColor(cat)
                            val emoji = ExpenseUtils.getEmoji(cat)
                            
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) catColor else SlateBorder.copy(alpha = 0.5f))
                                    .border(1.dp, if (isSelected) catColor else SlateBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("category_chip_$cat"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$emoji $cat",
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color.White else TextWhite,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Optional Description Input
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details & Description (Optional)") },
                    placeholder = { Text("e.g., Shared with Sameer, weekly tapri log") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = SlateBorder,
                        focusedLabelColor = EmeraldPrimary,
                        unfocusedLabelColor = TextGray,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_expense_description"),
                    shape = RoundedCornerShape(12.dp)
                )

                // CTA Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, SlateBorder),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextGray)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val amount = amountStr.toDoubleOrNull()
                            if (amount != null && amount > 0 && !isAmountError) {
                                onSave(title, amount, selectedCategory, description, System.currentTimeMillis())
                            } else {
                                isAmountError = true
                            }
                        },
                        modifier = Modifier.weight(1f).testTag("dialog_save_expense_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
