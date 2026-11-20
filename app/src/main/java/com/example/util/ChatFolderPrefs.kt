package com.example.util

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistent store for ALL top chat filter tabs / groups (both system groups like
 * UNREAD, ARCHIVED, GOLD_TRADER, BUSINESSMAN and custom folders) in one unified order
 * so every top group can be created, edited/renamed, deleted, or reordered.
 */
data class TopChatGroupConfig(
    val id: String,
    val label: String,
    val filterKey: String,
    val isBuiltIn: Boolean,
    val isHidden: Boolean = false
)

object ChatFolderPrefs {
    private const val PREFS_NAME = "oozox_chat_top_groups_prefs"
    private const val KEY_GROUPS_JSON = "top_groups_json"

    const val KEY_UNREAD = "BUILTIN_UNREAD"
    const val KEY_ARCHIVED = "BUILTIN_ARCHIVED"
    const val KEY_GOLD_TRADER = "BUILTIN_GOLD_TRADER"
    const val KEY_BUSINESSMAN = "BUILTIN_BUSINESSMAN"
    const val CUSTOM_ID_PREFIX = "CUSTOM_"

    private fun defaultGroups(): List<TopChatGroupConfig> = listOf(
        TopChatGroupConfig(id = KEY_UNREAD, label = "غير مقروءة", filterKey = KEY_UNREAD, isBuiltIn = true),
        TopChatGroupConfig(id = KEY_ARCHIVED, label = "المؤرشفة", filterKey = KEY_ARCHIVED, isBuiltIn = true),
        TopChatGroupConfig(id = KEY_GOLD_TRADER, label = "تجار ذهب", filterKey = KEY_GOLD_TRADER, isBuiltIn = true),
        TopChatGroupConfig(id = KEY_BUSINESSMAN, label = "حسابات مالية", filterKey = KEY_BUSINESSMAN, isBuiltIn = true)
    )

    fun customIdForName(name: String): String = "$CUSTOM_ID_PREFIX${name.trim()}"

    fun loadTopGroups(context: Context): List<TopChatGroupConfig> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_GROUPS_JSON, null)
        if (raw.isNullOrBlank()) {
            return defaultGroups()
        }
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<TopChatGroupConfig>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    TopChatGroupConfig(
                        id = obj.getString("id"),
                        label = obj.getString("label"),
                        filterKey = obj.optString("filterKey", obj.getString("id")),
                        isBuiltIn = obj.optBoolean("isBuiltIn", false),
                        isHidden = obj.optBoolean("isHidden", false)
                    )
                )
            }
            if (result.isEmpty()) defaultGroups() else result
        } catch (_: Exception) {
            defaultGroups()
        }
    }

    private fun saveTopGroups(context: Context, groups: List<TopChatGroupConfig>) {
        val arr = JSONArray()
        groups.forEach { g ->
            arr.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("label", g.label)
                    put("filterKey", g.filterKey)
                    put("isBuiltIn", g.isBuiltIn)
                    put("isHidden", g.isHidden)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GROUPS_JSON, arr.toString())
            .apply()
    }

    /**
     * Synchronizes persisted group order with the active list of custom chat folder names
     * without disturbing the user's custom ordering of built-in and custom groups.
     */
    fun syncWithCustomFolders(context: Context, customFolderNames: List<String>): List<TopChatGroupConfig> {
        val cleanCustom = customFolderNames.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val current = loadTopGroups(context)
        val retained = current.filter { cfg ->
            cfg.isBuiltIn || cleanCustom.contains(cfg.filterKey)
        }.toMutableList()

        val existingKeys = retained.filter { !it.isBuiltIn }.map { it.filterKey }.toSet()
        cleanCustom.forEach { name ->
            if (name !in existingKeys) {
                retained.add(
                    TopChatGroupConfig(
                        id = customIdForName(name),
                        label = name,
                        filterKey = name,
                        isBuiltIn = false,
                        isHidden = false
                    )
                )
            }
        }
        if (retained != current) {
            saveTopGroups(context, retained)
        }
        return retained
    }

    fun renameTopGroup(context: Context, groupId: String, newLabel: String): List<TopChatGroupConfig> {
        val trimmed = newLabel.trim()
        if (trimmed.isEmpty()) return loadTopGroups(context)
        val current = loadTopGroups(context)
        val updated = current.map { g ->
            if (g.id == groupId) {
                if (g.isBuiltIn) g.copy(label = trimmed, isHidden = false)
                else g.copy(id = customIdForName(trimmed), label = trimmed, filterKey = trimmed, isHidden = false)
            } else g
        }
        saveTopGroups(context, updated)
        return updated
    }

    fun renameCustomFolderPreservingOrder(context: Context, oldName: String, newName: String): List<TopChatGroupConfig> {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return loadTopGroups(context)
        val oldId = customIdForName(oldName)
        val current = loadTopGroups(context)
        val updated = current.map { g ->
            if (!g.isBuiltIn && (g.id == oldId || g.filterKey == oldName.trim())) {
                g.copy(id = customIdForName(trimmed), label = trimmed, filterKey = trimmed, isHidden = false)
            } else g
        }
        saveTopGroups(context, updated)
        return updated
    }

    fun deleteTopGroup(context: Context, groupId: String): List<TopChatGroupConfig> {
        val current = loadTopGroups(context)
        val updated = current.mapNotNull { g ->
            when {
                g.id != groupId -> g
                g.isBuiltIn -> g.copy(isHidden = true)
                else -> null
            }
        }
        saveTopGroups(context, updated)
        return updated
    }

    fun moveTopGroup(context: Context, groupId: String, delta: Int): List<TopChatGroupConfig> {
        if (delta == 0) return loadTopGroups(context)
        val current = loadTopGroups(context).toMutableList()
        val visibleIds = current.filter { !it.isHidden }.map { it.id }
        val visibleIdx = visibleIds.indexOf(groupId)
        if (visibleIdx == -1) return current
        val targetVisibleIdx = (visibleIdx + delta).coerceIn(0, visibleIds.lastIndex)
        if (targetVisibleIdx == visibleIdx) return current

        val targetId = visibleIds[targetVisibleIdx]
        val realIdxA = current.indexOfFirst { it.id == groupId }
        val realIdxB = current.indexOfFirst { it.id == targetId }
        if (realIdxA == -1 || realIdxB == -1) return current

        val item = current.removeAt(realIdxA)
        val insertIdx = if (realIdxA < realIdxB) realIdxB else realIdxB
        current.add(insertIdx.coerceIn(0, current.size), item)
        saveTopGroups(context, current)
        return current
    }

    fun restoreBuiltInDefaults(context: Context): List<TopChatGroupConfig> {
        val current = loadTopGroups(context)
        val byId = current.associateBy { it.id }
        val defaults = defaultGroups()
        val rebuiltBuiltIn = defaults.map { def ->
            val prev = byId[def.id]
            prev?.copy(label = def.label, filterKey = def.filterKey, isHidden = false) ?: def
        }
        val customOnly = current.filter { !it.isBuiltIn }
        val updated = rebuiltBuiltIn + customOnly
        saveTopGroups(context, updated)
        return updated
    }
}
