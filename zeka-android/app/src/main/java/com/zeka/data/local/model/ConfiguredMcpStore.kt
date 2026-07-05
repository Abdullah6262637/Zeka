package com.zeka.data.local.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ConfiguredMcpServer(
    val id: String,
    val name: String,
    val url: String,
    val isConnected: Boolean
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("url", url)
            put("isConnected", isConnected)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ConfiguredMcpServer {
            return ConfiguredMcpServer(
                id = json.getString("id"),
                name = json.getString("name"),
                url = json.getString("url"),
                isConnected = json.optBoolean("isConnected", false)
            )
        }
    }
}

object ConfiguredMcpStore {
    private const val PREFS_NAME = "zeka_configured_mcp"
    private const val KEY_SERVERS = "mcp_servers_list"

    fun saveServers(context: Context, servers: List<ConfiguredMcpServer>) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        servers.forEach { jsonArray.put(it.toJsonObject()) }
        sharedPrefs.edit().putString(KEY_SERVERS, jsonArray.toString()).apply()
    }

    fun loadServers(context: Context): List<ConfiguredMcpServer> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString(KEY_SERVERS, null) ?: return emptyList()
        val list = mutableListOf<ConfiguredMcpServer>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(ConfiguredMcpServer.fromJsonObject(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
