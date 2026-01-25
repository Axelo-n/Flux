package com.example.flux

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    viewModel: DashboardViewModel,
    transactionId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // 1. Cari Data Lama
    val transaction = viewModel.getTransactionById(transactionId)

    if (transaction == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // 2. State Form
    var amount by remember { mutableStateOf(transaction.amount.toInt().toString()) }
    var note by remember { mutableStateOf(transaction.title) }
    var selectedCategory by remember { mutableStateOf(transaction.category) }
    var isIncome by remember { mutableStateOf(transaction.isIncome) }

    // State Dialog Delete
    var showDeleteDialog by remember { mutableStateOf(false) }

    // List Kategori
    val categories = listOf(
        Triple("Food and Beverages", R.drawable.ic_food_outline, CatOrange),
        Triple("Transportation", R.drawable.ic_car_outline, CatGreen),
        Triple("Groceries and Shopping", R.drawable.ic_cart_outline, CatPurple),
        Triple("Entertainment", R.drawable.ic_ticket_outline, CatYellow),
        Triple("Account Transfer", R.drawable.ic_card_outline, CatBlue),
        Triple("Other", R.drawable.ic_other_outline, CatGrey)
    )

    Scaffold(
        containerColor = UIBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edit Transaction",
                        style = AppFont.Bold.copy(fontSize = 20.sp, color = UIWhite)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(UISurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, tint = UIWhite)
                        }
                    }
                },
                actions = {
                    // TOMBOL DELETE KHUSUS EDIT SCREEN
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(UIRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = UIRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = UIBackground)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. HERO INPUT (NOMINAL) ---
            Text("How much?", style = AppFont.Medium.copy(fontSize = 14.sp, color = UIGray))
            Spacer(modifier = Modifier.height(12.dp))

            TextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                textStyle = AppFont.Bold.copy(fontSize = 48.sp, color = UIWhite, textAlign = TextAlign.Center),
                placeholder = { Text("0", style = AppFont.Bold.copy(fontSize = 48.sp, color = UIGray.copy(0.3f)), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                prefix = { Text("Rp ", style = AppFont.Bold.copy(fontSize = 48.sp, color = UITeal), modifier = Modifier.padding(end = 4.dp)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = UITeal
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(30.dp))

            // --- 2. SEGMENTED CONTROL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(UISurface)
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Expense Option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (!isIncome) UIRed.copy(alpha = 0.2f) else Color.Transparent)
                            .border(width = if (!isIncome) 1.dp else 0.dp, color = if (!isIncome) UIRed else Color.Transparent, shape = RoundedCornerShape(12.dp))
                            .clickable { isIncome = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Expense", style = AppFont.Bold.copy(color = if (!isIncome) UIRed else UIGray))
                    }
                    // Income Option
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isIncome) UIGreen.copy(alpha = 0.2f) else Color.Transparent)
                            .border(width = if (isIncome) 1.dp else 0.dp, color = if (isIncome) UIGreen else Color.Transparent, shape = RoundedCornerShape(12.dp))
                            .clickable { isIncome = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Income", style = AppFont.Bold.copy(color = if (isIncome) UIGreen else UIGray))
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- 3. NOTE INPUT ---
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Note", style = AppFont.Bold.copy(fontSize = 16.sp, color = UIWhite))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Update note...", color = UIGray.copy(0.5f)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = UISurface,
                        unfocusedContainerColor = UISurface,
                        focusedBorderColor = UITeal,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = UIWhite,
                        unfocusedTextColor = UIWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            // --- 4. CATEGORY GRID (Pre-selected) ---
            Text("Category", style = AppFont.Bold.copy(fontSize = 16.sp, color = UIWhite))
            Spacer(modifier = Modifier.height(16.dp))

            categories.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    rowItems.forEach { (catName, catIconRes, catColor) ->
                        val isSelected = selectedCategory == catName

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedCategory = catName },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) catColor else UISurface)
                                    .border(width = 2.dp, color = if (isSelected) UIWhite.copy(0.2f) else Color.Transparent, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = catIconRes),
                                    contentDescription = null,
                                    tint = if (isSelected) UIWhite else catColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = catName, style = AppFont.Medium.copy(fontSize = 12.sp, color = if (isSelected) UIWhite else UIGray), maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- 5. TOMBOL SAVE CHANGES ---
            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: 0.0
                    if (amountDouble <= 0) {
                        Toast.makeText(context, "Isi nominal uangnya dulu ya! 💸", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (note.isBlank()) {
                        Toast.makeText(context, "Catatannya jangan kosong dong! 📝", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // UPDATE TRANSACTION
                    viewModel.updateTransaction(
                        id = transactionId,
                        amount = amountDouble,
                        note = note,
                        category = selectedCategory,
                        isIncome = isIncome,
                        date = transaction.date
                    )
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UITeal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Changes", style = AppFont.Bold.copy(fontSize = 16.sp, color = UIBackground))
            }
        }

        // --- KONFIRMASI DELETE ---
        if (showDeleteDialog) {
            FluxAlertDialog(
                title = "Delete Transaction?",
                message = "Are you sure? This action cannot be undone.",
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    viewModel.deleteTransaction(transactionId)
                    showDeleteDialog = false
                    onBack()
                }
            )
        }
    }
}