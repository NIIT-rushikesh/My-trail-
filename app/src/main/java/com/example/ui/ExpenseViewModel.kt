package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiManager
import com.example.data.Expense
import com.example.data.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiAnalysisState {
    object Idle : AiAnalysisState
    object Loading : AiAnalysisState
    data class Success(val recommendations: String) : AiAnalysisState
    data class Error(val errorMessage: String) : AiAnalysisState
}

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    // Reactive StateFlow list of expenses ordered by date
    val expensesState: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _aiAnalysisState = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiAnalysisState: StateFlow<AiAnalysisState> = _aiAnalysisState.asStateFlow()

    init {
        // Auto-seed typical Indian household expenses if none exist
        viewModelScope.launch {
            expensesState.collect { list ->
                if (list.isEmpty()) {
                    seedDefaultExpenses()
                }
            }
        }
    }

    private suspend fun seedDefaultExpenses() {
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        val defaultExpenses = listOf(
            Expense(
                title = "Monthly PG Rent",
                amount = 8500.0,
                category = "Rent & Bills",
                dateMillis = now - (5 * oneDayMs),
                description = "Sector 62 Noida shared accommodation"
            ),
            Expense(
                title = "Swiggy - Biryani Order",
                amount = 450.0,
                category = "Food & Groceries",
                dateMillis = now - (4 * oneDayMs),
                description = "Weekend dinner family pack"
            ),
            Expense(
                title = "Auto Metro Station Fare",
                amount = 50.0,
                category = "Transport",
                dateMillis = now - (3 * oneDayMs),
                description = "Local shared auto to Huda City Centre metro"
            ),
            Expense(
                title = "Airtel Mobile Prepaid Recharge",
                amount = 299.0,
                category = "Rent & Bills",
                dateMillis = now - (2 * oneDayMs),
                description = "1.5GB daily data plan monthly"
            ),
            Expense(
                title = "Myntra Summer T-Shirt",
                amount = 699.0,
                category = "Shopping",
                dateMillis = now - (2 * oneDayMs),
                description = "Cotton printed casual t-shirt"
            ),
            Expense(
                title = "Cinema & Popcorn",
                amount = 480.0,
                category = "Entertainment",
                dateMillis = now - oneDayMs,
                description = "Watched latest sci-fi movie in multiplex"
            ),
            Expense(
                title = "Morning Cutting Chai (10 cups)",
                amount = 120.0,
                category = "Food & Groceries",
                dateMillis = now,
                description = "Office tapri tea gatherings weekly"
            ),
            Expense(
                title = "Monthly Gas Pipeline Bill",
                amount = 650.0,
                category = "Rent & Bills",
                dateMillis = now - (10 * oneDayMs),
                description = "Adani Gas cylinder replacement"
            )
        )

        for (expense in defaultExpenses) {
            repository.insertExpense(expense)
        }
    }

    fun addExpense(title: String, amount: Double, category: String, dateMillis: Long, description: String = "") {
        viewModelScope.launch {
            val expense = Expense(
                title = title.ifBlank { "Untitled Expense" },
                amount = if (amount > 0) amount else 0.0,
                category = category,
                dateMillis = dateMillis,
                description = description
            )
            repository.insertExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    fun deleteExpenseById(id: Int) {
        viewModelScope.launch {
            repository.deleteExpenseById(id)
        }
    }

    fun runAiAnalysis() {
        _aiAnalysisState.value = AiAnalysisState.Loading
        viewModelScope.launch {
            try {
                // Fetch the list from state
                val list = expensesState.value
                val result = GeminiManager.analyzeExpensesInRupees(list)
                _aiAnalysisState.value = AiAnalysisState.Success(result)
            } catch (e: Exception) {
                _aiAnalysisState.value = AiAnalysisState.Error(e.localizedMessage ?: "Unknown analysis error")
            }
        }
    }

    fun dismissAiDialog() {
        _aiAnalysisState.value = AiAnalysisState.Idle
    }
}

class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
