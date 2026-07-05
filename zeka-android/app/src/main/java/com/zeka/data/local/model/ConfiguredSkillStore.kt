package com.zeka.data.local.model

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ConfiguredSkill(
    val id: String,
    val name: String,
    val triggerKeyword: String,
    val promptInstruction: String
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("triggerKeyword", triggerKeyword)
            put("promptInstruction", promptInstruction)
        }
    }

    companion object {
        fun fromJsonObject(json: JSONObject): ConfiguredSkill {
            return ConfiguredSkill(
                id = json.getString("id"),
                name = json.getString("name"),
                triggerKeyword = json.getString("triggerKeyword"),
                promptInstruction = json.getString("promptInstruction")
            )
        }
    }
}

object ConfiguredSkillStore {
    private const val PREFS_NAME = "zeka_configured_skills"
    private const val KEY_SKILLS = "skills_list"

    fun saveSkills(context: Context, skills: List<ConfiguredSkill>) {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = JSONArray()
        skills.forEach { jsonArray.put(it.toJsonObject()) }
        sharedPrefs.edit().putString(KEY_SKILLS, jsonArray.toString()).apply()
    }

    fun loadSkills(context: Context): List<ConfiguredSkill> {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = sharedPrefs.getString(KEY_SKILLS, null) ?: return emptyList()
        val list = mutableListOf<ConfiguredSkill>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(ConfiguredSkill.fromJsonObject(obj))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
