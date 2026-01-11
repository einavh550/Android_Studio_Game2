package com.example.android_studio_game.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object HighScoreStorage {

    private const val SP_NAME = "sp_high_scores"
    private const val KEY_LIST = "key_list"

    private lateinit var sp: SharedPreferences

    fun init(context: Context) {
        sp = context.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    // -------- Public API --------

    fun getTop10(): List<HighScoreRecord> {
        return loadList()
            .sortedWith(compareByDescending<HighScoreRecord> { it.score }
                .thenByDescending { it.time })
            .take(10)
    }


    fun tryInsert(score: Int, lat: Double?, lng: Double?) {
        val record = HighScoreRecord(
            score = score,
            lat = lat,
            lng = lng,
            time = System.currentTimeMillis()
        )
        insert(record)
    }


    fun add(record: HighScoreRecord) {
        insert(record)
    }

    // -------- Internal --------

    private fun ensureInit() {
        check(::sp.isInitialized) {
            "HighScoreStorage.init(context) must be called before using HighScoreStorage"
        }
    }

    private fun loadList(): MutableList<HighScoreRecord> {
        ensureInit()

        val json = sp.getString(KEY_LIST, "[]") ?: "[]"
        val arr = JSONArray(json)

        val list = mutableListOf<HighScoreRecord>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)

            list.add(
                HighScoreRecord(
                    score = obj.getInt("score"),
                    lat = if (obj.isNull("lat")) null else obj.getDouble("lat"),
                    lng = if (obj.isNull("lng")) null else obj.getDouble("lng"),
                    time = obj.getLong("time")
                )
            )
        }
        return list
    }

    private fun insert(record: HighScoreRecord) {
        val currentTop10 = getTop10()
        val updatedTop10 = (currentTop10 + record)
            .sortedWith(compareByDescending<HighScoreRecord> { it.score }
                .thenByDescending { it.time })
            .take(10)

        if (updatedTop10 == currentTop10) return

        persist(updatedTop10)
    }

    private fun persist(list: List<HighScoreRecord>) {
        ensureInit()

        val arr = JSONArray()
        list.forEach { r ->
            val obj = JSONObject()
            obj.put("score", r.score)
            obj.put("lat", r.lat)
            obj.put("lng", r.lng)
            obj.put("time", r.time)
            arr.put(obj)
        }

        sp.edit().putString(KEY_LIST, arr.toString()).apply()
    }
}