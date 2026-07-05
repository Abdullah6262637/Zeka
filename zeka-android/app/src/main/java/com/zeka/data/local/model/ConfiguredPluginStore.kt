package com.zeka.data.local.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ConfiguredPlugin(
    val id: String,
    val name: String,
    val description: String,
    val isEnabled: Boolean
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("description", description)
            put("isEnabled", isEnabled)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ConfiguredPlugin {
            return ConfiguredPlugin(
                id = json.getString("id"),
                name = json.getString("name"),
                description = json.getString("description"),
                isEnabled = json.optBoolean("isEnabled", true)
            )
        }
    }
}

object ConfiguredPluginStore {
    private const val PREFS_NAME = "zeka_configured_plugins"
    private const val KEY_PLUGINS = "plugins_list"

    fun savePlugins(context: Context, plugins: List<ConfiguredPlugin>) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        plugins.forEach { jsonArray.put(it.toJsonObject()) }
        sharedPrefs.edit().putString(KEY_PLUGINS, jsonArray.toString()).apply()
    }

    fun loadPlugins(context: Context): List<ConfiguredPlugin> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString(KEY_PLUGINS, null) ?: return emptyList()
        val list = mutableListOf<ConfiguredPlugin>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(ConfiguredPlugin.fromJsonObject(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
