package com.example

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ScanItem(
    val id: String = UUID.randomUUID().toString(),
    val value: String,
    val typeName: String,
    val timestamp: Long = System.currentTimeMillis()
)

object ScannerHistory {
    private const val PREFS_NAME = "pindai_barcode_prefs"
    private const val KEY_HISTORY = "scan_history"

    fun getHistory(context: Context): List<ScanItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val list = mutableListOf<ScanItem>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    ScanItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        value = obj.optString("value", ""),
                        typeName = obj.optString("typeName", "Barcode"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Return reverse sorted so newest scanned items appear at the top
        return list.sortedByDescending { it.timestamp }
    }

    fun saveItem(context: Context, value: String, typeName: String): ScanItem {
        val currentList = getHistory(context).toMutableList()
        
        // Avoid duplicate scans in a short span if they have the exact same value
        val existingIndex = currentList.indexOfFirst { it.value == value }
        if (existingIndex != -1) {
            // Remove the old one and we'll insert a fresh one at the top
            currentList.removeAt(existingIndex)
        }

        val newItem = ScanItem(value = value, typeName = typeName)
        currentList.add(0, newItem) // add to top

        saveList(context, currentList)
        return newItem
    }

    fun deleteItem(context: Context, id: String) {
        val currentList = getHistory(context).filter { it.id != id }
        saveList(context, currentList)
    }

    fun clearHistory(context: Context) {
        saveList(context, emptyList())
    }

    private fun saveList(context: Context, list: List<ScanItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("value", item.value)
                put("typeName", item.typeName)
                put("timestamp", item.timestamp)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_HISTORY, jsonArray.toString()).apply()
    }

    fun getBarcodeFormatName(formatValue: Int): String {
        return when (formatValue) {
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_CODE_93 -> "Code 93"
            Barcode.FORMAT_CODABAR -> "Codabar"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "Aztec"
            else -> "Barcode/QR"
        }
    }
}
