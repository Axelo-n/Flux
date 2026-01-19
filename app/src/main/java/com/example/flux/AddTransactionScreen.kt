package com.example.flux

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.ui.theme.*

@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    // Update parameter: tambah isIncome (Boolean)
    onSave: (Double, String, String, Boolean) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food") }

    // State buat nentuin Income/Expense (Default: Expense/False)
    var isIncome by remember { mutableStateOf(false) }

    // Warna tema berubah sesuai tipe transaksi
    val themeColor by animateColorAsState(
        targetValue = if (isIncome) UIGreen else UIRed, // Hijau atau Merah
        animationSpec = tween(500), label = "themeColor"
    )

    val expenseCategories = listOf("Food and Beverages", "Transportation", "Groceries and Shopping", "Clothes and Shoes", "Entertainment", "Account Transfer", "Other")
    val incomeCategories = listOf("Salary", "Account Transfer", "Investment", "Gift", "Other")

    // Pilih list kategori berdasarkan tipe
    val currentCategories = if (isIncome) incomeCategories else expenseCategories

    Scaffold(
        containerColor = UIBackground,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 20.dp, end = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(UISurface)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home_outline), // Ganti icon back
                        contentDescription = "Back",
                        tint = UIWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isIncome) "New Income" else "New Expense",
                    style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 16.sp)
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(40.dp))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // 1. TYPE TOGGLE (Income / Expense)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(UISurface),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tombol Expense
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (!isIncome) UIRed else Color.Transparent) // Merah kalo aktif
                        .clickable { isIncome = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Expense", style = AppFont.SemiBold.copy(color = if (!isIncome) UIWhite else UIGray, fontSize = 14.sp))
                }

                // Tombol Income
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (isIncome) UIGreen else Color.Transparent) // Hijau kalo aktif
                        .clickable { isIncome = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text("Income", style = AppFont.SemiBold.copy(color = if (isIncome) UIWhite else UIGray, fontSize = 14.sp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 2. INPUT AMOUNT
            Text("Enter Amount", style = AppFont.Medium.copy(color = UIGray, fontSize = 14.sp))
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isIncome) "+ Rp" else "- Rp", // Tanda +/- visual aja
                    style = AppFont.Bold.copy(color = themeColor, fontSize = 32.sp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = amountText,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amountText = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontFamily = AppFont.Bold.fontFamily,
                        fontSize = 48.sp,
                        color = UIWhite
                    ),
                    cursorBrush = SolidColor(themeColor), // Kursor ngikut warna tema
                    decorationBox = { innerTextField ->
                        if (amountText.isEmpty()) {
                            Text("0", style = AppFont.Bold.copy(color = UIGray.copy(alpha = 0.5f), fontSize = 48.sp))
                        }
                        innerTextField()
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // 3. INPUT NOTE
            FluxTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = "Add a note...",
                cursorColor = themeColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 4. CATEGORY SELECTOR
            Text("Category", style = AppFont.SemiBold.copy(color = UIWhite, fontSize = 16.sp), modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(currentCategories) { cat ->
                    val isSelected = cat == selectedCategory
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) themeColor else UISurface)
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = cat,
                            style = AppFont.Medium.copy(
                                color = if (isSelected) UIBackground else UIWhite,
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 5. SAVE BUTTON
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    onSave(amount, noteText, selectedCategory, isIncome)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Transaction", style = AppFont.Bold.copy(color = UIBackground, fontSize = 16.sp))
            }
        }
    }
}

@Composable
fun FluxTextField(value: String, onValueChange: (String) -> Unit, placeholder: String, cursorColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(UISurface)
            .padding(20.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, style = AppFont.Medium.copy(color = UIGray, fontSize = 16.sp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(fontFamily = AppFont.Medium.fontFamily, fontSize = 16.sp, color = UIWhite),
            cursorBrush = SolidColor(cursorColor),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ... Preview Code (Tambahin FluxTheme di preview lo yang sebelumnya) ...

@Preview(
    showBackground = true,
    backgroundColor = 0xFF0B0E14,
    showSystemUi = true,
    device = "id:pixel_5"
)
@Composable
fun AddTransactionScreenPreview() {
    FluxTheme {
        AddTransactionScreen(
            onBack = {},
            // Update disini: Tambahin parameter ke-4 (isIncome)
            onSave = { amount, note, category, isIncome ->
                println("Preview Save: $amount | $note | $category | Income? $isIncome")
            }
        )
    }
}