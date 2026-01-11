package com.example.android_studio_game

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import com.google.android.material.textview.MaterialTextView

class MiniScoresFragment : Fragment() {

    var onCloseClicked: (() -> Unit)? = null

    private lateinit var closeBtn: ImageButton
    private lateinit var scoreViews: Array<MaterialTextView>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_mini_scores, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViews(view)
        initViews()
    }

    private fun findViews(view: View) {
        closeBtn = view.findViewById(R.id.scores_BTN_close)

        scoreViews = arrayOf(
            view.findViewById(R.id.score_1),
            view.findViewById(R.id.score_2),
            view.findViewById(R.id.score_3),
            view.findViewById(R.id.score_4),
            view.findViewById(R.id.score_5),
            view.findViewById(R.id.score_6),
            view.findViewById(R.id.score_7),
            view.findViewById(R.id.score_8),
            view.findViewById(R.id.score_9),
            view.findViewById(R.id.score_10)
        )
    }

    private fun initViews() {
        closeBtn.setOnClickListener {
            onCloseClicked?.invoke()
        }
    }

    fun setScores(scores: List<Int>) {
        val top = scores.take(10)

        for (i in 0 until 10) {
            val value = top.getOrNull(i)
            scoreViews[i].text = value?.toString() ?: "—"
        }
    }
}
