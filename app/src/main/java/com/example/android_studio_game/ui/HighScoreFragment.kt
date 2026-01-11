package com.example.android_studio_game.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.android_studio_game.data.HighScoreStorage
import com.example.android_studio_game.R
import com.example.android_studio_game.utilities.SignalManager
import com.example.android_studio_game.interfaces.Callback_HighScoreClicked
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class HighScoreFragment : Fragment() {

    companion object {
        var highScoreItemClicked: Callback_HighScoreClicked? = null
    }

    private lateinit var rows: Array<MaterialCardView>
    private lateinit var scoreLabels: Array<MaterialTextView>
    private lateinit var timeLabels: Array<MaterialTextView>
    private val timeFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())


    private var records = HighScoreStorage.getTop10()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v: View = inflater.inflate(R.layout.fragment_leaderboard, container, false)
        findViews(v)
        initViews()
        return v
    }

    override fun onResume() {
        super.onResume()
        refreshScores()
    }

    private fun findViews(v: View) {
        rows = arrayOf(
            v.findViewById(R.id.highScore_ROW_1),
            v.findViewById(R.id.highScore_ROW_2),
            v.findViewById(R.id.highScore_ROW_3),
            v.findViewById(R.id.highScore_ROW_4),
            v.findViewById(R.id.highScore_ROW_5),
            v.findViewById(R.id.highScore_ROW_6),
            v.findViewById(R.id.highScore_ROW_7),
            v.findViewById(R.id.highScore_ROW_8),
            v.findViewById(R.id.highScore_ROW_9),
            v.findViewById(R.id.highScore_ROW_10)
        )

        scoreLabels = arrayOf(
            v.findViewById(R.id.highScore_LBL_score_1),
            v.findViewById(R.id.highScore_LBL_score_2),
            v.findViewById(R.id.highScore_LBL_score_3),
            v.findViewById(R.id.highScore_LBL_score_4),
            v.findViewById(R.id.highScore_LBL_score_5),
            v.findViewById(R.id.highScore_LBL_score_6),
            v.findViewById(R.id.highScore_LBL_score_7),
            v.findViewById(R.id.highScore_LBL_score_8),
            v.findViewById(R.id.highScore_LBL_score_9),
            v.findViewById(R.id.highScore_LBL_score_10)
        )



        timeLabels = arrayOf(
            v.findViewById(R.id.highScore_LBL_time_1),
            v.findViewById(R.id.highScore_LBL_time_2),
            v.findViewById(R.id.highScore_LBL_time_3),
            v.findViewById(R.id.highScore_LBL_time_4),
            v.findViewById(R.id.highScore_LBL_time_5),
            v.findViewById(R.id.highScore_LBL_time_6),
            v.findViewById(R.id.highScore_LBL_time_7),
            v.findViewById(R.id.highScore_LBL_time_8),
            v.findViewById(R.id.highScore_LBL_time_9),
            v.findViewById(R.id.highScore_LBL_time_10)
        )

    }

    private fun initViews() {
        for (i in 0 until 10) {
            rows[i].setOnClickListener {
                val rec = records.getOrNull(i) ?: return@setOnClickListener

                val lat = rec.lat
                val lng = rec.lng

                if (lat != null && lng != null) {
                    highScoreItemClicked?.highScoreItemClicked(lat, lng)
                } else {
                    SignalManager.showToast("No location saved for this score")
                }
            }
        }

        refreshScores()
    }

    private fun refreshScores() {
        records = HighScoreStorage.getTop10()

        for (i in 0 until 10) {
            val rec = records.getOrNull(i)

            scoreLabels[i].text = if (rec != null) "Score: ${rec.score}" else "Score: —"

            timeLabels[i].text = if (rec != null) {
                timeFormat.format(Date(rec.time))
            } else {
                ""
            }
        }

    }
}
