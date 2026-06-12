package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import java.util.Locale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Expense
import com.example.ui.theme.*
import kotlin.math.atan2

@Composable
fun DashboardScreen(
    expenses: List<Expense>,
    viewModel: ExpenseViewModel,
    onNavigateToTransactions: () -> Unit
) {
    val totalExpense = expenses.sumOf { it.amount }
    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }.mapValues { entry ->
            entry.value.sumOf { it.amount }
        }
    }

    val aiState by viewModel.aiAnalysisState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // App slogan / status header
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = "RUPEEWISE DASHBOARD",
                style = MaterialTheme.typography.labelLarge,
                color = EmeraldPrimary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Your Spend Analytics",
                style = MaterialTheme.typography.headlineMedium,
                color = TextWhite,
                fontWeight = FontWeight.ExtraBold
            )
        }

        // Summary Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total spent card
            Card(
                modifier = Modifier
                    .weight(1.2f)
                    .border(1.dp, SlateBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = PastelTealHighlight),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Spending",
                            style = MaterialTheme.typography.labelMedium,
                            color = VelvetDeepTeal.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Live",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = ExpenseUtils.formatRupee(totalExpense),
                        style = MaterialTheme.typography.headlineSmall,
                        color = VelvetDeepTeal,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Real-time tracking",
                        style = MaterialTheme.typography.bodySmall,
                        color = VelvetDeepTeal.copy(alpha = 0.6f)
                    )
                }
            }

            // Top expense category card
            val topCategory = remember(categoryTotals) {
                if (categoryTotals.isEmpty()) "None"
                else categoryTotals.maxByOrNull { it.value }?.key ?: "None"
            }
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, SlateBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Leaky Bucket 🪣",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextGray,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = topCategory,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (topCategory != "None") ExpenseUtils.getColor(topCategory) else TextWhite,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Highest category",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
        }

        // Interactive Donut Chart Section
        if (expenses.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Category Allocation Map",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Hover or tap on segments to view spend amount",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    CategoryDonutChart(
                        categoryTotals = categoryTotals,
                        totalExpense = totalExpense
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Progress Bars for categories
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExpenseUtils.categories.forEach { category ->
                            val amount = categoryTotals[category] ?: 0.0
                            if (amount > 0) {
                                val ratio = (amount / totalExpense).toFloat()
                                CategoryProgressRow(
                                    category = category,
                                    amount = amount,
                                    ratio = ratio
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Empty state placeholder
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📭 No Expenses Tracked Yet",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please add transactions to generate spending reports, visual allocation map, and trend graphs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextGray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onNavigateToTransactions,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                    ) {
                        Text("Add Expense Item", fontWeight = FontWeight.Bold, color = DeepSpaceDark)
                    }
                }
            }
        }

        // AI Advice minimizing suggestions Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = VelvetDeepTeal),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(FluorescentCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🤖", fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Cost Minimization Copilot",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Analyze leakages & suggest actions in Rupees",
                            style = MaterialTheme.typography.bodySmall,
                            color = FluorescentCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                when (aiState) {
                    is AiAnalysisState.Idle -> {
                        Text(
                            text = "Let Gemini analyze your transaction history to formulate highly actionable strategies to trim your food deliveries, transportation, recharges, and shopping budgets dynamically.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = { viewModel.runAiAnalysis() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FluorescentCyan,
                                contentColor = VelvetDeepTeal
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Analyze Spending & Suggest Actions",
                                color = VelvetDeepTeal,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    is AiAnalysisState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = FluorescentCyan)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "RupeeWise engine processing...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FluorescentCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Evaluating Indian local alternatives & targets",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }

                    is AiAnalysisState.Success -> {
                        val recommendations = (aiState as AiAnalysisState.Success).recommendations
                        
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(VelvetDeepTeal.copy(alpha = 0.6f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    MarkdownTextRenderer(
                                        text = recommendations,
                                        textColor = Color.White,
                                        boldColor = FluorescentCyan
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.runAiAnalysis() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = FluorescentCyan,
                                    contentColor = VelvetDeepTeal
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    "Re-Analyze Database",
                                    color = VelvetDeepTeal,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    is AiAnalysisState.Error -> {
                        val errorMessage = (aiState as AiAnalysisState.Error).errorMessage
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Error", tint = CoralAlert)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Copilot Connection Error",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CoralAlert,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = { viewModel.runAiAnalysis() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                            ) {
                                Text("Retry Analysis", fontWeight = FontWeight.Bold, color = DeepSpaceDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryDonutChart(
    categoryTotals: Map<String, Double>,
    totalExpense: Double
) {
    var activeCategory by remember { mutableStateOf<String?>(null) }
    val displayCategory = activeCategory ?: categoryTotals.maxByOrNull { it.value }?.key ?: ""
    val displayAmount = categoryTotals[displayCategory] ?: 0.0

    Box(
        modifier = Modifier
            .size(240.dp)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .pointerInput(categoryTotals) {
                    detectTapGestures { offset ->
                        // Calculate tap angle
                        val center = size.width / 2f
                        val x = offset.x - center
                        val y = offset.y - center
                        var angleDeg = Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
                        if (angleDeg < 0) {
                            angleDeg += 360f
                        }

                        // Match angle to category slice
                        var accumulatedAngle = 0f
                        categoryTotals.keys.forEach { key ->
                            val amount = categoryTotals[key] ?: 0.0
                            if (amount > 0) {
                                val sweep = ((amount / totalExpense) * 360f).toFloat()
                                if (angleDeg >= accumulatedAngle && angleDeg < (accumulatedAngle + sweep)) {
                                    activeCategory = key
                                    return@detectTapGestures
                                }
                                accumulatedAngle += sweep
                            }
                        }
                    }
                }
        ) {
            val strokeWidth = 32.dp.toPx()
            var startAngle = 0f

            categoryTotals.keys.forEach { key ->
                val amount = categoryTotals[key] ?: 0.0
                if (amount > 0) {
                    val sweepAngle = ((amount / totalExpense) * 360f).toFloat()
                    val color = ExpenseUtils.getColor(key)

                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = Size(size.width, size.height),
                        topLeft = Offset(0f, 0f)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        // Inner circular summary
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(40.dp)
        ) {
            Text(
                text = ExpenseUtils.getEmoji(displayCategory) + " " + displayCategory,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = ExpenseUtils.formatRupee(displayAmount),
                style = MaterialTheme.typography.titleLarge,
                color = TextWhite,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            val percent = if (totalExpense > 0) (displayAmount / totalExpense) * 100 else 0.0
            Text(
                text = String.format(Locale.US, "%.1f%% of total", percent),
                style = MaterialTheme.typography.labelSmall,
                color = ExpenseUtils.getColor(displayCategory),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CategoryProgressRow(
    category: String,
    amount: Double,
    ratio: Float
) {
    val categoryColor = ExpenseUtils.getColor(category)
    val emoji = ExpenseUtils.getEmoji(category)
    
    // Smooth width animation
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(ratio) {
        animatedProgress.animateTo(
            targetValue = ratio,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 16.sp)
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = ExpenseUtils.formatRupee(amount),
                style = MaterialTheme.typography.bodyMedium,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rounded linear bar representing allocation
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(SlateBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress.value)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(categoryColor, categoryColor.copy(alpha = 0.7f))
                            )
                        )
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "${(ratio * 100).toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = TextGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

/**
 * A beautiful, customized text renderer that renders standard markdown lists,
 * bold highlights, and headers into beautiful styled text blocks matching the
 * personal finance dashboard theme, avoiding unformatted block texts.
 */
@Composable
fun MarkdownTextRenderer(
    text: String,
    textColor: Color = TextWhite,
    boldColor: Color = EmeraldPrimary
) {
    val lines = text.split("\n")
    
    lines.forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@forEach

        when {
            trimmed.startsWith("###") -> {
                val headerText = trimmed.removePrefix("###").trim()
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleMedium,
                    color = boldColor,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            trimmed.startsWith("##") -> {
                val headerText = trimmed.removePrefix("##").trim()
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.titleLarge,
                    color = boldColor,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            trimmed.startsWith("#") -> {
                val headerText = trimmed.removePrefix("#").trim()
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = headerText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = GoldenAmber,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            trimmed.startsWith("*") || trimmed.startsWith("-") || trimmed.startsWith("•") -> {
                val bulletContent = trimmed.removePrefix("*")
                    .removePrefix("-")
                    .removePrefix("•")
                    .trim()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "•",
                        fontSize = 18.sp,
                        color = boldColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = parseBoldMarkdown(bulletContent, boldColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            else -> {
                // Check if it's numbered list
                val numberingRegex = "^[0-9]+\\.".toRegex()
                val isNumbered = numberingRegex.find(trimmed)
                if (isNumbered != null) {
                    val number = isNumbered.value
                    val content = trimmed.removePrefix(number).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = number,
                            style = MaterialTheme.typography.bodyMedium,
                            color = boldColor,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseBoldMarkdown(content, boldColor),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Text(
                        text = parseBoldMarkdown(trimmed, boldColor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        lineHeight = 20.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Parses markdown **double stars** to create an AnnotatedString containing styled bold spans.
 */
@Composable
fun parseBoldMarkdown(text: String, boldColor: Color = EmeraldPrimary): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val nextBoldStart = text.indexOf("**", cursor)
            if (nextBoldStart == -1) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, nextBoldStart))
            val nextBoldEnd = text.indexOf("**", nextBoldStart + 2)
            if (nextBoldEnd == -1) {
                append(text.substring(nextBoldStart))
                break
            }
            withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, color = boldColor)) {
                append(text.substring(nextBoldStart + 2, nextBoldEnd))
            }
            cursor = nextBoldEnd + 2
        }
    }
}
