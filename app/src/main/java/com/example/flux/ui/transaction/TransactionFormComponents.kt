package com.example.flux.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.flux.ui.theme.AppFont
import com.example.flux.ui.theme.CatBlue
import com.example.flux.ui.theme.CatGreen
import com.example.flux.ui.theme.CatGrey
import com.example.flux.ui.theme.CatOrange
import com.example.flux.ui.theme.CatPurple
import com.example.flux.ui.theme.CatYellow
import com.example.flux.ui.theme.UIGray
import com.example.flux.ui.theme.UIGreen
import com.example.flux.ui.theme.UIRed
import com.example.flux.ui.theme.UISurface
import com.example.flux.ui.theme.UITeal
import com.example.flux.ui.theme.UIWhite
import com.example.flux.R

data class CategoryItem(
    val name: String,
    val iconRes: Int,
    val color: Color
)

val transactionCategories = listOf(
    CategoryItem("Food and Beverages", R.drawable.ic_food_outline, CatOrange),
    CategoryItem("Transportation", R.drawable.ic_car_outline, CatGreen),
    CategoryItem("Groceries and Shopping", R.drawable.ic_cart_outline, CatPurple),
    CategoryItem("Entertainment", R.drawable.ic_ticket_outline, CatYellow),
    CategoryItem("Account Transfer", R.drawable.ic_card_outline, CatBlue),
    CategoryItem("Other", R.drawable.ic_other_outline, CatGrey)
)

@Composable
fun AmountInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = { if (it.all { char -> char.isDigit() }) onValueChange(it) },
        textStyle = AppFont.Bold.copy(fontSize = 48.sp, color = UIWhite, textAlign = TextAlign.Center),
        placeholder = {
            Text(
                "0",
                style = AppFont.Bold.copy(fontSize = 48.sp, color = UIGray.copy(0.3f)),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
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
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun IncomeExpenseToggle(
    isIncome: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(UISurface)
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (!isIncome) UIRed.copy(alpha = 0.2f) else Color.Transparent)
                    .border(width = if (!isIncome) 1.dp else 0.dp, color = if (!isIncome) UIRed else Color.Transparent, shape = RoundedCornerShape(12.dp))
                    .clickable { onToggle(false) },
                contentAlignment = Alignment.Center
            ) {
                Text("Expense", style = AppFont.Bold.copy(color = if (!isIncome) UIRed else UIGray))
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isIncome) UIGreen.copy(alpha = 0.2f) else Color.Transparent)
                    .border(width = if (isIncome) 1.dp else 0.dp, color = if (isIncome) UIGreen else Color.Transparent, shape = RoundedCornerShape(12.dp))
                    .clickable { onToggle(true) },
                contentAlignment = Alignment.Center
            ) {
                Text("Income", style = AppFont.Bold.copy(color = if (isIncome) UIGreen else UIGray))
            }
        }
    }
}

@Composable
fun CategoryGrid(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        transactionCategories.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                rowItems.forEach { item ->
                    val isSelected = selectedCategory == item.name
                    Column(
                        modifier = Modifier.weight(1f).clickable { onCategorySelected(item.name) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) item.color else UISurface)
                                .border(width = 2.dp, color = if (isSelected) UIWhite.copy(0.2f) else Color.Transparent, shape = CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = null,
                                tint = if (isSelected) UIWhite else item.color,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.name,
                            style = AppFont.Medium.copy(fontSize = 12.sp, color = if (isSelected) UIWhite else UIGray),
                            maxLines = 2,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
