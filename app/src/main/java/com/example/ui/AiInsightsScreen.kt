package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.api.GeminiManager
import com.example.data.Expense
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

data class CopilotMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun AiInsightsScreen(
    expenses: List<Expense>
) {
    var userQuery by remember { mutableStateOf("") }
    val chatMessages = remember { mutableStateListOf<CopilotMessage>() }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Initialize with a welcome message from RupeeWise
    if (chatMessages.isEmpty()) {
        chatMessages.add(
            CopilotMessage(
                text = "Namaste! I am **RupeeWise**, your personal cost minimization assistant.\n\nI can check your active expense list and suggest custom strategies to cut down excess spend on **Zomato, Ola, Amazon shopping, and high bills**.\n\nTry sending a custom query, or tap one of the **Quick Indian Finance Questions** below!",
                isUser = false
            )
        )
    }

    val quickQuestions = listOf(
        "🍽️ Cut Swiggy/Zomato food expenditure",
        "🚇 Save on commute charges (Metro vs Cabs)",
        "🎯 Advise 50-30-20 budget target list",
        "📦 How to trim shopping delivery fees"
    )

    fun sendCopilotQuery(prompt: String) {
        if (prompt.trim().isEmpty() || isLoading) return
        
        chatMessages.add(CopilotMessage(text = prompt, isUser = true))
        userQuery = ""
        isLoading = true

        coroutineScope.launch {
            try {
                // Enrich question with database analysis
                val listSize = expenses.size
                val totalSpend = expenses.sumOf { it.amount }
                val databaseBrief = expenses.groupBy { it.category }
                    .mapValues { it.value.sumOf { exp -> exp.amount } }
                    .entries.joinToString("; ") { "${it.key}: ₹${it.value}" }

                val enrichedPrompt = """
                    The user has $listSize active expense transactions in their Indian Rupee tracker database with a recent total spend of ₹$totalSpend.
                    Here is the category spend breakdown: $databaseBrief.
                    
                    The user's specific query is: "$prompt".
                    
                    Please reply to them directly as 'RupeeWise' personal financial optimizer. Keep your advice focused on Indian market choices (e.g. state-run bus recharges, Zepto pass, Swiggy One, local grocery mandi, Airtel/Jio recharges instead of postpaids). Format the response in clear bold markdown.
                """.trimIndent()

                val apiResponse = GeminiManager.analyzeExpensesInRupees(
                    expenses = expenses
                ).let { fullAnalysis ->
                    // Make Gemini API call based on combined context
                    // To keep API latency low and response specific, we generate a content prompt for Gemini
                    val directPrompt = """
                        You are 'RupeeWise' personal financial analyst.
                        The user's expense status is: Recent spend of ₹$totalSpend. Category breakdown: $databaseBrief.
                        User's question: "$prompt"
                        
                        Provide a brief, helpful, highly focused advice tailored specifically to answer this question. Keep it under 200 words. Bold headings, highlight Indian optimization tricks.
                    """.trimIndent()
                    GeminiManager.analyzeExpensesInRupees(
                        expenses = expenses + Expense(id = -99, title = "Query Prompt Placeholder: $prompt", amount = 0.0, category = "Others", dateMillis = System.currentTimeMillis())
                    ) // Send full lists
                    
                    // Call the API with direct question response
                    GeminiManager.analyzeExpensesInRupees(
                        // Just run standard analysis or customized query
                        expenses = expenses
                    )
                    // Let's call standard analysis but prefix it or just get direct feedback by calling the API
                    // To do custom queries, let's call the actual model with the enriched prompt!
                    // Wait, our GeminiManager.analyzeExpensesInRupees expects the expense lists.
                    // Let's create an expense with title as the user prompt! That is incredibly smart
                    // because our GeminiManager lists transactions sequentially.
                    // So we can append a mock transaction carrying the custom question as description!
                    val queryMockExpense = Expense(
                        id = -1,
                        title = "USER_QUESTION: $prompt",
                        amount = 0.0,
                        category = "Others",
                        dateMillis = System.currentTimeMillis(),
                        description = "Please specifically answer: '$prompt' in your response."
                    )
                    GeminiManager.analyzeExpensesInRupees(expenses + queryMockExpense)
                }

                chatMessages.add(CopilotMessage(text = apiResponse, isUser = false))
            } catch (e: Exception) {
                chatMessages.add(CopilotMessage(text = "Error: Could not obtain AI advice. Details: ${e.localizedMessage}", isUser = false))
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Header
        Text(
            text = "AI FINANCIAL COPILOT",
            style = MaterialTheme.typography.labelLarge,
            color = EmeraldPrimary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        Text(
            text = "RupeeWise Terminal",
            style = MaterialTheme.typography.headlineMedium,
            color = TextWhite,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Chats lazy area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SlateSurface)
                .border(1.dp, SlateBorder, RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                        .testTag("chat_messages_column"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    items(chatMessages, key = { it.id }) { message ->
                        ChatBubbleRow(message = message)
                    }

                    if (isLoading) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = EmeraldPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "RupeeWise is formulating advice...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Quick Questions horizontal suggestions block
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DeepSpaceDark.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Quick Indian Finance Questions:",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextGray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickQuestions.forEach { question ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldPrimary.copy(alpha = 0.08f))
                                    .border(1.dp, EmeraldPrimary.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                                    .clickable(enabled = !isLoading) { sendCopilotQuery(question) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("quick_question_${question.take(5)}")
                            ) {
                                Text(
                                    text = question,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Chat text entry input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = userQuery,
                onValueChange = { userQuery = it },
                placeholder = { Text("Ask budget questions (e.g. Zomato tips)...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = SlateBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedLabelColor = EmeraldPrimary,
                    unfocusedLabelColor = TextGray
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_text_field"),
                maxLines = 2,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Text
                )
            )

            // Submit button
            Button(
                onClick = {
                    sendCopilotQuery(userQuery)
                },
                enabled = userQuery.trim().isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    disabledContainerColor = SlateBorder,
                    disabledContentColor = TextGray
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("send_chat_button"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Send Ask Questions",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ChatBubbleRow(message: CopilotMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgBrush = if (message.isUser) {
        Brush.linearGradient(listOf(EmeraldPrimary, EmeraldPrimary))
    } else {
        // Soft pale mint bubble background
        Brush.linearGradient(listOf(Color(0xFFE4F2F0), Color(0xFFE4F2F0)))
    }
    val contentColor = if (message.isUser) Color.White else TextWhite
    val textStyle = MaterialTheme.typography.bodyMedium

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_bubble_${if (message.isUser) "user" else "ai"}"),
        horizontalAlignment = alignment
    ) {
        Row(
            horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            if (!message.isUser) {
                // Circle Badge for RupeeWise AI
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.2f))
                        .border(1.dp, EmeraldPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (message.isUser) 16.dp else 2.dp,
                            bottomEnd = if (message.isUser) 2.dp else 16.dp
                        )
                    )
                    .background(bgBrush)
                    .border(
                        1.dp, 
                        if (message.isUser) Color.Transparent else SlateBorder, 
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (message.isUser) 16.dp else 2.dp,
                            bottomEnd = if (message.isUser) 2.dp else 16.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Column {
                    if (message.isUser) {
                        Text(
                            text = message.text,
                            style = textStyle,
                            color = contentColor
                        )
                    } else {
                        MarkdownTextRenderer(text = message.text, textColor = TextWhite)
                    }
                }
            }
        }
    }
}
