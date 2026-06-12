package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double, // Amount in Indian Rupees (INR)
    val category: String, // e.g., "Food", "Transport", "Bills", "Shopping", "Entertainment", "Others"
    val dateMillis: Long, // Date when expense was incurred
    val description: String = ""
)
