package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.example.data.Expense
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object GeminiManager {
    private const val TAG = "GeminiManager"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Calls Gemini API to analyze expenses and generate cost-minimization advice.
     */
    suspend fun analyzeExpensesInRupees(expenses: List<Expense>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is blank or placeholder!")
            return@withContext "API Key Not Configured.\n\nPlease configure your GEMINI_API_KEY securely in the AI Studio Secrets panel. This will inject the real key at runtime."
        }

        if (expenses.isEmpty()) {
            return@withContext "You don't have any expenses to analyze yet! Please add some daily expenses (e.g. food, transport, bills) first so that I can analyze your spending trends."
        }

        val df = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val expenseListString = StringBuilder()
        expenses.forEachIndexed { index, exp ->
            val dateStr = df.format(Date(exp.dateMillis))
            expenseListString.append("${index + 1}. Title: ${exp.title}, Amount: ₹${exp.amount}, Category: ${exp.category}, Date: $dateStr, Info: ${exp.description}\n")
        }

        val totalSpend = expenses.sumOf { it.amount }
        val categorySpends = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }
            .joinToString("\n") { "   • ${it.key}: ₹${it.value} (${String.format(Locale.US, "%.1f", (it.value / totalSpend) * 100)}%)" }

        val systemPrompt = """
            You are 'RupeeWise', an elite personal finance and budget optimization AI expert for Indian consumers. 
            Your goal is to help users minimize their daily expenses and savings dynamically while retaining style and high-quality living.
            Always address the user with a helpful, friendly, and objective tone. Refer to Indian-specific contexts e.g. local transport (auto-rickshaws, metro prepay card vs Uber/Ola), eating (Zomato/Swiggy vs tiffin service/cooking home), bills (Jio/Airtel recharge, state electricity boards), groceries (Zepto/Blinkit/Instamart vs local sabzi mandi/Kirana store).
            Format the response in neat, readable markup, using bold highlights for crucial recommendations and visual separators. Keep it highly action-oriented and positive!
        """.trimIndent()

        val userPrompt = """
            Please analyze my recent expenses and suggest dynamic, realistic cost-saving actions.
            
            SUMMARY OF CURRENT BUDGET:
            • Total Recent Spend: ₹$totalSpend
            • Category Breakdown:
            $categorySpends
            
            DETAILED LIST OF RECENT TRANSACTIONS:
            $expenseListString
            
            Please provide a structured response with:
            1. 🔍 **Brief Spend Trend Analysis** (highlighting potential leaky buckets, e.g., spending too much on Swiggy or auto-rides).
            2. 💡 **Top 3 High-Impact Cost Minimization Actions** (tailored to these actual items, providing alternatives in Indian Rupees - e.g. switching to metro cards, cooking, Zepto bulk, or disabling automatic renewals).
            3. 🎯 **Personalized Rupee Target Allocation** (suggesting a target list of spend of these categories based on classical 50-30-20 rules adjusted to Indian middle-class realities).
        """.trimIndent()

        try {
            val mediaType = "application/json; charset=utf-8".toMediaType()

            // JSON request structure:
            // {
            //   "contents": [ { "parts": [ { "text": "userPrompt" } ] } ],
            //   "systemInstruction": { "parts": [ { "text": "systemPrompt" } ] }
            // }
            val jsonRequest = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userPrompt)
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemPrompt)
                        })
                    })
                })
            }

            val requestBodyString = jsonRequest.toString()
            val body = requestBodyString.toRequestBody(mediaType)

            val urlWithKey = "$BASE_URL?key=$apiKey"
            val request = Request.Builder()
                .url(urlWithKey)
                .post(body)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e(TAG, "API call failed code ${response.code}: $errBody")
                return@withContext "Failed to analyze trends: HTTP Error ${response.code}. Please ensure your API key in AI Studio Secrets panel is valid and has Gemini API enabled."
            }

            val responseBodyString = response.body?.string() ?: ""
            val jsonResponse = JSONObject(responseBodyString)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext "Could not analyze. No suggestions returned from Gemini API."
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            if (content == null) {
                return@withContext "Error: response payload structure missing 'content'."
            }

            val parts = content.optJSONArray("parts")
            if (parts == null || parts.length() == 0) {
                return@withContext "Error: response payload structure missing 'parts'."
            }

            val text = parts.getJSONObject(0).optString("text")
            return@withContext text.ifBlank { "Could not parse review output." }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during analysis API call", e)
            return@withContext "Network Error: Could not connect to analyze trends. Please check your internet connectivity or API key setup. Details: ${e.localizedMessage}"
        }
    }
}
