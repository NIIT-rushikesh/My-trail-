package com.example.ui

import androidx.compose.ui.graphics.Color

object ExpenseUtils {

    // Standard categories with associated emojis and theme colors
    val categories = listOf(
        "Food & Groceries",
        "Transport",
        "Rent & Bills",
        "Shopping",
        "Entertainment",
        "Others"
    )

    fun getEmoji(category: String): String {
        return when (category) {
            "Food & Groceries" -> "🍔"
            "Transport" -> "🚗"
            "Rent & Bills" -> "🏠"
            "Shopping" -> "🛍️"
            "Entertainment" -> "🎬"
            else -> "📦"
        }
    }

    fun getColor(category: String): Color {
        return when (category) {
            "Food & Groceries" -> Color(0xFF10B981) // Emerald Green
            "Transport" -> Color(0xFF14B8A6)       // Soft Teal
            "Rent & Bills" -> Color(0xFFF59E0B)    // Golden Orange
            "Shopping" -> Color(0xFFEC4899)        // Bright Pink
            "Entertainment" -> Color(0xFFEF4444)   // Coral Red
            else -> Color(0xFF6B7280)              // Muted Gray
        }
    }

    /**
     * Formats an amount to Indian Rupees format (₹) with commas (lakhs/crores style),
     * or a clean localized system currency.
     */
    fun formatRupee(amount: Double): String {
        // Standard Indian formatting: split lakhs/crores or standard decimal formatting
        return try {
            val amountLong = amount.toLong()
            val fraction = ((amount - amountLong) * 100).toLong()
            val text = amountLong.toString()
            
            val formatted = when {
                text.length <= 3 -> text
                text.length <= 5 -> {
                    // e.g. 12,345
                    val pos = text.length - 3
                    text.substring(0, pos) + "," + text.substring(pos)
                }
                text.length <= 7 -> {
                    // e.g. 12,34,567
                    val pos1 = text.length - 3
                    val part1 = text.substring(0, pos1)
                    val part2 = text.substring(pos1)
                    val pos2 = part1.length - 2
                    part1.substring(0, pos2) + "," + part1.substring(pos2) + "," + part2
                }
                else -> {
                    // Default fallback
                    val reversed = text.reversed()
                    val builder = StringBuilder()
                    for (i in reversed.indices) {
                        if (i == 3) {
                            builder.append(",")
                        } else if (i > 3 && (i - 3) % 2 == 0) {
                            builder.append(",")
                        }
                        builder.append(reversed[i])
                    }
                    builder.reverse().toString()
                }
            }
            if (fraction > 0) {
                "₹$formatted.${String.format("%02d", fraction)}"
            } else {
                "₹$formatted"
            }
        } catch (e: Exception) {
            "₹${String.format("%.2f", amount)}"
        }
    }
}
