package com.example.flux.ui.transaction

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.R
import com.example.flux.ui.theme.AppFont
import com.example.flux.ui.theme.UIBackground
import com.example.flux.ui.theme.UIGray
import com.example.flux.ui.theme.UISurface
import com.example.flux.ui.theme.UITeal
import com.example.flux.ui.theme.UIWhite
import com.example.flux.viewmodel.DashboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: DashboardViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Food and Beverages") }
    var isIncome by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = UIBackground,
        topBar = {
            TopAppBar(
                title = { Text("New Transaction", style = AppFont.Bold.copy(fontSize = 20.sp, color = UIWhite)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(UISurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painter = painterResource(R.drawable.ic_close), contentDescription = null, tint = UIWhite)
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
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("How much?", style = AppFont.Medium.copy(fontSize = 14.sp, color = UIGray))
            Spacer(modifier = Modifier.height(12.dp))

            AmountInput(value = amount, onValueChange = { amount = it })

            Spacer(modifier = Modifier.height(30.dp))

            IncomeExpenseToggle(isIncome = isIncome, onToggle = { isIncome = it })

            Spacer(modifier = Modifier.height(30.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Note", style = AppFont.Bold.copy(fontSize = 16.sp, color = UIWhite))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Buy pizza for dinner...", color = UIGray.copy(0.5f)) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = UISurface, unfocusedContainerColor = UISurface,
                        focusedBorderColor = UITeal, unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = UIWhite, unfocusedTextColor = UIWhite
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text("Category", style = AppFont.Bold.copy(fontSize = 16.sp, color = UIWhite))
            Spacer(modifier = Modifier.height(16.dp))

            CategoryGrid(selectedCategory = selectedCategory, onCategorySelected = { selectedCategory = it })

            Spacer(modifier = Modifier.weight(1f))

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
                    viewModel.addTransaction(amount = amountDouble, note = note, category = selectedCategory, isIncome = isIncome)
                    onBack()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UITeal),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Transaction", style = AppFont.Bold.copy(fontSize = 16.sp, color = UIBackground))
            }
        }
    }
}
