package com.zeka.data.local.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ConfiguredModel(
    val id: String,
    val provider: String, // "Anthropic", "OpenAI", "Google", "OpenRouter", "Mistral"
    val name: String,
    val modelCode: String,
    val apiKey: String,
    val baseUrl: String,
    val temperature: Float,
    val maxTokens: Int,
    val topP: Float,
    val frequencyPenalty: Float,
    val presencePenalty: Float,
    val systemPrompt: String
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("provider", provider)
            put("name", name)
            put("modelCode", modelCode)
            put("apiKey", apiKey)
            put("baseUrl", baseUrl)
            put("temperature", temperature.toDouble())
            put("maxTokens", maxTokens)
            put("topP", topP.toDouble())
            put("frequencyPenalty", frequencyPenalty.toDouble())
            put("presencePenalty", presencePenalty.toDouble())
            put("systemPrompt", systemPrompt)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ConfiguredModel {
            return ConfiguredModel(
                id = json.getString("id"),
                provider = json.getString("provider"),
                name = json.getString("name"),
                modelCode = json.getString("modelCode"),
                apiKey = json.getString("apiKey"),
                baseUrl = json.optString("baseUrl", ""),
                temperature = json.optDouble("temperature", 0.7).toFloat(),
                maxTokens = json.optInt("maxTokens", 4096),
                topP = json.optDouble("topP", 1.0).toFloat(),
                frequencyPenalty = json.optDouble("frequencyPenalty", 0.0).toFloat(),
                presencePenalty = json.optDouble("presencePenalty", 0.0).toFloat(),
                systemPrompt = json.optString("systemPrompt", "")
            )
        }
    }
}

object ConfiguredModelStore {
    private const val PREFS_NAME = "zeka_configured_models"
    private const val KEY_MODELS = "models_list"

    fun saveModels(context: Context, models: List<ConfiguredModel>) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        models.forEach { jsonArray.put(it.toJsonObject()) }
        sharedPrefs.edit().putString(KEY_MODELS, jsonArray.toString()).apply()
    }

    fun loadModels(context: Context): List<ConfiguredModel> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString(KEY_MODELS, null) ?: return emptyList()
        val list = mutableListOf<ConfiguredModel>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(ConfiguredModel.fromJsonObject(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
