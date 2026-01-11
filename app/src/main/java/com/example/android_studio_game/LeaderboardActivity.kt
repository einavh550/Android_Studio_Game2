package com.example.android_studio_game

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.android_studio_game.interfaces.Callback_HighScoreClicked
import com.example.android_studio_game.ui.HighScoreFragment
import com.example.android_studio_game.ui.MapFragment
import com.google.android.material.button.MaterialButton


class LeaderboardActivity : AppCompatActivity() {

    private lateinit var main_FRAME_list: FrameLayout
    private lateinit var main_FRAME_map: FrameLayout

    private lateinit var mapFragment: MapFragment
    private lateinit var highScoreFragment: HighScoreFragment
    private lateinit var leaderboard_BTN_back: MaterialButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_leaderboard)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // might need to delete this viewcompact lines


        HighScoreStorage.init(this)
        SignalManager.init(this)

        findViews()
        initViews()
    }

    private fun findViews() {
        main_FRAME_list = findViewById(R.id.main_FRAME_list)
        main_FRAME_map = findViewById(R.id.main_FRAME_map)
        leaderboard_BTN_back = findViewById(R.id.leaderboard_BTN_back)
    }

    private fun initViews() {
        leaderboard_BTN_back.setOnClickListener {
            finish()
        }
        mapFragment = MapFragment()
        supportFragmentManager
            .beginTransaction()
            .add(R.id.main_FRAME_map, mapFragment)
            .commit()

        highScoreFragment = HighScoreFragment()

        HighScoreFragment.highScoreItemClicked =
            object : Callback_HighScoreClicked {
                override fun highScoreItemClicked(lat: Double, lon: Double) {
                    mapFragment.zoom(lat, lon)
                }
            }

        supportFragmentManager
            .beginTransaction()
            .add(R.id.main_FRAME_list, highScoreFragment)
            .commit()
    }
}
