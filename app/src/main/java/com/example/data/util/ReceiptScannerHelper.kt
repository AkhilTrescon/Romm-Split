package com.example.data.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern
import kotlin.coroutines.resume

data class ScannedBillData(
    val detectedTitle: String? = null,
    val detectedAmountCents: Long? = null,
    val detectedCategory: String? = null,
    val detectedDateMillis: Long? = null,
    val rawTextSummary: String = ""
)

object ReceiptScannerHelper {

    private const val TAG = "ReceiptScannerHelper"

    /**
     * Reads and parses a bill print image using ML Kit Text Recognition on-device.
     */
    suspend fun scanReceipt(context: Context, imageFile: File): ScannedBillData {
        return suspendCancellableCoroutine { continuation ->
            try {
                if (!imageFile.exists()) {
                    continuation.resume(ScannedBillData())
                    return@suspendCancellableCoroutine
                }

                val inputImage = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val result = parseVisionText(visionText.text)
                        continuation.resume(result)
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "ML Kit text recognition failed", e)
                        continuation.resume(ScannedBillData())
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error initiating receipt scanning", e)
                continuation.resume(ScannedBillData())
            }
        }
    }

    private fun parseVisionText(fullText: String): ScannedBillData {
        if (fullText.isBlank()) return ScannedBillData()

        val lines = fullText.lines().map { it.trim() }.filter { it.isNotBlank() }

        var detectedTitle: String? = null
        var detectedCategory: String? = null
        var detectedAmountCents: Long? = null
        var detectedDateMillis: Long? = null

        // 1. Detect Merchant / Store Name from lines or keywords
        val lowerText = fullText.lowercase()

        val knownStores = mapOf(
            "spar" to ("Spar" to "Grocery"),
            "interspar" to ("Interspar" to "Grocery"),
            "mercator" to ("Mercator" to "Grocery"),
            "hofer" to ("Hofer" to "Grocery"),
            "lidl" to ("Lidl" to "Grocery"),
            "tuš" to ("Tuš" to "Grocery"),
            "tus" to ("Tuš" to "Grocery"),
            "dm drogerie" to ("DM Drogerie" to "Household Items"),
            "dm" to ("DM Drogerie" to "Household Items"),
            "müller" to ("Müller" to "Household Items"),
            "muller" to ("Müller" to "Household Items"),
            "telemach" to ("Telemach" to "Bills & Utilities"),
            "telekom" to ("Telekom Slovenije" to "Bills & Utilities"),
            "a1 slovenija" to ("A1" to "Bills & Utilities"),
            "snaga" to ("Snaga Utilities" to "Bills & Utilities"),
            "elektro" to ("Elektro" to "Bills & Utilities"),
            "petrol" to ("Petrol" to "Transportation"),
            "omv" to ("OMV" to "Transportation"),
            "mol" to ("MOL" to "Transportation"),
            "ikea" to ("IKEA" to "Household Items"),
            "bauhaus" to ("Bauhaus" to "Household Items"),
            "najemnina" to ("Apartment Rent" to "Room Rent"),
            "stanarina" to ("Apartment Rent" to "Room Rent")
        )

        for ((key, pair) in knownStores) {
            if (lowerText.contains(key)) {
                detectedTitle = pair.first
                detectedCategory = pair.second
                break
            }
        }

        // If no known merchant, pick the cleanest first line that looks like a store header
        if (detectedTitle == null) {
            val potentialHeader = lines.firstOrNull { line ->
                line.length in 3..35 && !line.matches(Regex(".*[0-9]{5,}.*"))
            }
            if (potentialHeader != null) {
                detectedTitle = potentialHeader
            }
        }

        // 2. Detect Total Amount (Looking for keywords: SKUPAJ, ZA PLAČILO, ZNESEK, TOTAL, EUR, €)
        // In Slovenian receipts: "SKUPAJ", "ZA PLAČILO", "ZNESEK ZA PLAČILO", "EUR"
        val totalKeywords = listOf("skupaj", "za plačilo", "za placilo", "znesek", "total", "kartica", "gotovina", "eur")
        var bestAmountCents: Long? = null

        val numberPattern = Pattern.compile("(\\d{1,4}[.,]\\d{2})")

        // First pass: look on lines containing total keywords
        for (line in lines.reversed()) {
            val lineLower = line.lowercase()
            val containsKeyword = totalKeywords.any { lineLower.contains(it) }
            if (containsKeyword) {
                val matcher = numberPattern.matcher(line)
                var lastMatchedValue: Double? = null
                while (matcher.find()) {
                    val numStr = matcher.group(1).replace(",", ".")
                    numStr.toDoubleOrNull()?.let { lastMatchedValue = it }
                }
                if (lastMatchedValue != null && lastMatchedValue!! > 0.0) {
                    bestAmountCents = Math.round(lastMatchedValue!! * 100.0)
                    break
                }
            }
        }

        // Second pass: if keyword not found, search highest reasonable number in bottom half of receipt
        if (bestAmountCents == null) {
            var maxFound = 0.0
            for (line in lines.takeLast(lines.size / 2 + 1)) {
                val matcher = numberPattern.matcher(line)
                while (matcher.find()) {
                    val numStr = matcher.group(1).replace(",", ".")
                    numStr.toDoubleOrNull()?.let {
                        if (it in 0.50..5000.0 && it > maxFound) {
                            maxFound = it
                        }
                    }
                }
            }
            if (maxFound > 0.0) {
                bestAmountCents = Math.round(maxFound * 100.0)
            }
        }

        detectedAmountCents = bestAmountCents

        // 3. Detect Date (DD.MM.YYYY or DD/MM/YYYY)
        val datePattern = Pattern.compile("(\\d{1,2})[./-](\\d{1,2})[./-](\\d{2,4})")
        val dateMatcher = datePattern.matcher(fullText)
        if (dateMatcher.find()) {
            val day = dateMatcher.group(1).toIntOrNull() ?: 1
            val month = dateMatcher.group(2).toIntOrNull() ?: 1
            var year = dateMatcher.group(3).toIntOrNull() ?: 2026
            if (year < 100) year += 2000
            try {
                val cal = java.util.Calendar.getInstance()
                cal.set(year, month - 1, day)
                detectedDateMillis = cal.timeInMillis
            } catch (_: Exception) {}
        }

        return ScannedBillData(
            detectedTitle = detectedTitle,
            detectedAmountCents = detectedAmountCents,
            detectedCategory = detectedCategory,
            detectedDateMillis = detectedDateMillis,
            rawTextSummary = lines.take(6).joinToString(" | ")
        )
    }
}
