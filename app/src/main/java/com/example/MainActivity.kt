package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                
                // Initialize Room database & repository
                val database = remember { AppDatabase.getDatabase(context) }
                val repository = remember { ExpenseRepository(database.expenseDao()) }
                
                // Inject viewmodel with Factory
                val expenseViewModel: ExpenseViewModel = viewModel(
                    factory = ExpenseViewModelFactory(repository)
                )

                AppMainLayout(viewModel = expenseViewModel)
            }
        }
    }
}

@Composable
fun AppMainLayout(viewModel: ExpenseViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    val expenses by viewModel.expensesState.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepSpaceDark),
        containerColor = DeepSpaceDark,
        bottomBar = {
            // High fidelity dark themed NavigationBar with safe drawn space classes
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_navigation_bar")
                    .border(1.dp, SlateBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                containerColor = SlateSurface,
                tonalElevation = 8.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                // Dashboard Tab
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Dashboard summary",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Dashboard", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepSpaceDark,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray,
                        indicatorColor = EmeraldPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                // Ledger Tab
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Transactions ledger",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Ledger", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepSpaceDark,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray,
                        indicatorColor = EmeraldPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_ledger")
                )

                // Copilot Tab
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Face,
                            contentDescription = "AI Copilot Terminal",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Copilot", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = DeepSpaceDark,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = TextGray,
                        unselectedTextColor = TextGray,
                        indicatorColor = EmeraldPrimary
                    ),
                    modifier = Modifier.testTag("nav_tab_copilot")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            when (selectedTab) {
                0 -> DashboardScreen(
                    expenses = expenses,
                    viewModel = viewModel,
                    onNavigateToTransactions = { selectedTab = 1 }
                )
                1 -> TransactionsScreen(
                    expenses = expenses,
                    viewModel = viewModel
                )
                2 -> AiInsightsScreen(
                    expenses = expenses
                )
            }
        }
    }
}
