package com.example.android_studio_game.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.android_studio_game.game.GameManager
import com.example.android_studio_game.game.GameMenuController
import com.example.android_studio_game.data.HighScoreStorage
import com.example.android_studio_game.utilities.LocationHelper
import com.example.android_studio_game.R
import com.example.android_studio_game.utilities.SignalManager
import com.example.android_studio_game.utilities.SoundManager
import com.example.android_studio_game.interfaces.TiltCallback
import com.example.android_studio_game.utilities.TiltSensorManager
import kotlin.math.abs

class MainActivity : AppCompatActivity() {


    // ============================================================
    //  COMPANION OBJECT - Constants
    // ============================================================

    companion object {
        private const val VISIBLE_ROWS = 7
        private const val PLAYER_ROW_OFFSET = 1

        // for bonus
        private const val TILT_Y_FAST_THRESHOLD = 2.5f   // forth
        private const val TILT_Y_SLOW_THRESHOLD = -2.5f  // back
        private const val TILT_Y_DEADZONE = 1.0f // back to normal
        private const val NEUTRAL_SAMPLE_COUNT = 6

    }


    // ============================================================
    //  PROPERTIES - Views & Managers
    // ============================================================

    // Buttons
    private lateinit var btnLeft: ImageButton
    private lateinit var btnRight: ImageButton
    private lateinit var btnMenu: ImageButton

    // Hearts (Lives)
    private lateinit var heart1: ImageView
    private lateinit var heart2: ImageView
    private lateinit var heart3: ImageView
    private lateinit var carView: ImageView

    // Score & Grid
    private lateinit var scoreText: TextView
    private lateinit var grid: Array<Array<View>>

    // Managers
    private lateinit var gameManager: GameManager
    private lateinit var tiltSensorManager: TiltSensorManager
    private lateinit var menuController: GameMenuController

    // Mini Scores Overlay
    private lateinit var miniScoresContainer: View
    private lateinit var miniScoresFragment: MiniScoresFragment

    // Game Over Overlay
    private lateinit var gameOverContainer: View
    private lateinit var gameOverScore: TextView
    private lateinit var gameOverRestartBtn: View

    // State flags
    private var isGameOverShown = false
    private var isLoopPaused = false
    private var pendingScoreToSave: Int? = null

    // ============================================================
    //  GAME LOOP - Handler & Runnable
    // ============================================================

    private val handler = Handler(Looper.getMainLooper())

    private val loop = object : Runnable {
        override fun run() {
            tick()
            if (isLoopPaused || isGameOverShown) return
            handler.postDelayed(this, gameManager.getTickDelay())
        }
    }

    // ============================================================
    //  PERMISSION LAUNCHER - Location
    // ============================================================

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val granted = (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                    (perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true)

            val score = pendingScoreToSave ?: return@registerForActivityResult
            pendingScoreToSave = null

            if (granted) {
                LocationHelper.getLastLocation { lat, lng ->
                    HighScoreStorage.tryInsert(score, lat, lng)
                }
            } else {
                HighScoreStorage.tryInsert(score, null, null)
            }
        }

    // ============================================================
    //  LIFECYCLE - onCreate
    // ============================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupGameOverOverlay()
        setupManagers()
        setupSensors()
        tiltSensorManager.setEnabled(false)
        setupMenu()
        setupGrid()
        setupMiniScores()
        setupControls()
        updateControlsUi(tiltSensorManager.isEnabled())

        refreshAllUi()
        render()
        startGameLoop()
    }

    override fun onResume() {
        super.onResume()
        if (!isGameOverShown) resumeGameLoop()
        if (tiltSensorManager.isEnabled()) tiltSensorManager.start()
    }

    override fun onPause() {
        super.onPause()
        pauseGameLoop()
        tiltSensorManager.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(loop)
        SoundManager.release()
    }

    // ============================================================
    //  INITIALIZATION - Setup Functions
    // ============================================================

    private fun bindViews() {
        btnMenu = findViewById(R.id.btnMenu)
        btnLeft = findViewById(R.id.btnLeft)
        btnRight = findViewById(R.id.btnRight)

        heart1 = findViewById(R.id.heart1)
        heart2 = findViewById(R.id.heart2)
        heart3 = findViewById(R.id.heart3)

        scoreText = findViewById(R.id.scoreText)
        miniScoresContainer = findViewById(R.id.main_FRAME_mini_scores)

        carView = findViewById(R.id.carView)
        carView.visibility = View.GONE
    }

    private fun setupManagers() {
        SignalManager.init(this)
        SoundManager.init(this)
        HighScoreStorage.init(this)
        LocationHelper.init(this)

        gameManager = GameManager()
        gameManager.setPlayerRowOffset(PLAYER_ROW_OFFSET)
    }

    // ----------------------------
//  Tilt speed bonus (Y axis)
// ----------------------------
    private enum class TiltSpeedMode { NORMAL, FAST, SLOW }
    private var neutralTiltY: Float? = null
    private var neutralSumY = 0f
    private var neutralSamples = 0
    private var tiltSpeedMode: TiltSpeedMode = TiltSpeedMode.NORMAL

    private fun applyTiltSpeedMode(newMode: TiltSpeedMode) {
        if (tiltSpeedMode == newMode) return
        tiltSpeedMode = newMode

        when (tiltSpeedMode) {
            TiltSpeedMode.FAST -> {
                gameManager.setFastMode(true)
                gameManager.setSlowMode(false)
            }
            TiltSpeedMode.SLOW -> {
                gameManager.setSlowMode(true)
                gameManager.setFastMode(false)
            }
            TiltSpeedMode.NORMAL -> {
                gameManager.setFastMode(false)
                gameManager.setSlowMode(false)
            }
        }
        restartGameLoopImmediately()
    }



    private fun setupSensors() {
        tiltSensorManager = TiltSensorManager(this, object : TiltCallback {
            override fun tiltX(x: Float) {
                if (isGameOverShown) return
                if (!tiltSensorManager.isEnabled()) return
                if (x > 0) gameManager.movePlayer(-1) else gameManager.movePlayer(1)
            }

            override fun tiltY(y: Float) {
                // BONUS: Tilt back and forth for speed (Y axis)
                if (isGameOverShown) return
                if (!tiltSensorManager.isEnabled()) return

                // 1) Calibrate neutral position on first reading after enabling sensor mode
                if (neutralTiltY == null) {
                    neutralSumY += y
                    neutralSamples++

                    if (neutralSamples < NEUTRAL_SAMPLE_COUNT) {
                        return
                    }

                    neutralTiltY = neutralSumY / neutralSamples
                    applyTiltSpeedMode(TiltSpeedMode.NORMAL)
                    return
                }


                // 2) dy relative to neutral.
                val dy = -(y - neutralTiltY!!)


                // 3) Decide speed mode with a deadzone so it returns to NORMAL when you level the phone
                val mode = when {
                    abs(dy) < TILT_Y_DEADZONE -> TiltSpeedMode.NORMAL
                    dy > TILT_Y_FAST_THRESHOLD -> TiltSpeedMode.FAST
                    dy < TILT_Y_SLOW_THRESHOLD -> TiltSpeedMode.SLOW
                    else -> TiltSpeedMode.NORMAL
                }


                applyTiltSpeedMode(mode)
            }

        })
    }

    private fun setupMenu() {
        menuController = GameMenuController(
            activity = this,
            menuButton = btnMenu,
            gameManager = gameManager,
            tiltSensorManager = tiltSensorManager,
            onSpeedChanged = { restartGameLoopImmediately() },
            onHighScoresClicked = { openHighScoresScreen() },
            onMenuOpened = { pauseGameLoop() },
            onMenuClosed = { resumeGameLoop() },
            onControlModeChanged = { isSensorMode ->
                updateControlsUi(isSensorMode)
                if (isSensorMode) {
                    neutralTiltY = null
                    neutralSumY = 0f
                    neutralSamples = 0
                    applyTiltSpeedMode(TiltSpeedMode.NORMAL)
                }

            }

        )
        menuController.setup()
    }

    private fun setupGrid() {
        grid = Array(VISIBLE_ROWS) { r ->
            Array(GameManager.Companion.COLS) { c ->
                val id = resources.getIdentifier("mat_${r}_${c}", "id", packageName)
                findViewById<View>(id)
                    ?: error("Missing View id: mat_${r}_${c} in activity_main.xml")
            }
        }
    }

    private fun setupControls() {
        btnLeft.setOnClickListener { gameManager.movePlayer(-1) }
        btnRight.setOnClickListener { gameManager.movePlayer(1) }
    }

    private fun setupMiniScores() {
        miniScoresFragment = MiniScoresFragment()
        miniScoresFragment.onCloseClicked = { hideMiniScores() }

        supportFragmentManager.beginTransaction()
            .add(R.id.main_FRAME_mini_scores, miniScoresFragment)
            .commit()
    }

    private fun setupGameOverOverlay() {
        gameOverContainer = findViewById(R.id.main_FRAME_game_over)

        val gameOverView = layoutInflater.inflate(
            R.layout.view_game_over,
            gameOverContainer as ViewGroup,
            false
        )

        (gameOverContainer as ViewGroup).addView(gameOverView)
        gameOverContainer.visibility = View.GONE

        gameOverScore = gameOverView.findViewById(R.id.gameOver_LBL_score)
        gameOverRestartBtn = gameOverView.findViewById(R.id.gameOver_BTN_restart)

        gameOverRestartBtn.setOnClickListener {
            restartGame()
        }
    }

    // ============================================================
    //  GAME LOOP CONTROL
    // ============================================================

    private fun startGameLoop() {
        isLoopPaused = false
        handler.removeCallbacks(loop)
        handler.post(loop)
    }

    private fun pauseGameLoop() {
        isLoopPaused = true
        handler.removeCallbacks(loop)
    }

    private fun resumeGameLoop() {
        if (isGameOverShown) return
        isLoopPaused = false
        handler.removeCallbacks(loop)
        handler.post(loop)
    }

    private fun restartGameLoopImmediately() {
        if (isLoopPaused || isGameOverShown) return
        handler.removeCallbacks(loop)
        handler.post(loop)
    }

    // ============================================================
    //  GAME TICK & LOGIC
    // ============================================================

    private fun tick() {
        gameManager.stepBombs()
        gameManager.spawnCoinIfNeeded()
        gameManager.stepCoin()

        gameManager.advanceScoreByDistance()
        updateScoreUI()

        if (gameManager.checkCoinCollected()) {
            SignalManager.showToast("Collected Coin!\n+10")
            updateScoreUI()
        }

        if (gameManager.checkCollision()) {
            onCrash()
        }

        render()
    }

    private fun onCrash() {
        SignalManager.vibrate()
        SoundManager.playCrash()
        SignalManager.showToast("Crash")

        val gameOver = gameManager.onCrash()
        updateLivesUI()

        if (gameOver) {
            val finalScore = gameManager.score
            saveHighScoreWithMaybeLocation(finalScore)
            showGameOver(finalScore)
            return
        }
    }

    private fun restartGame() {
        hideGameOver()
        gameManager.resetGame()
        refreshAllUi()
        render()
        resumeGameLoop()
    }

    // ============================================================
    //  HIGH SCORE & LOCATION
    // ============================================================

    private fun saveHighScoreWithMaybeLocation(score: Int) {
        val hasFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            LocationHelper.getLastLocation { lat, lng ->
                HighScoreStorage.tryInsert(score, lat, lng)
            }
        } else {
            pendingScoreToSave = score
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // ============================================================
    //  RENDERING - Drawing the Game Board
    // ============================================================

    private fun render() {
        clearBoard()

        val offset = GameManager.Companion.ROWS - VISIBLE_ROWS
        drawBombs(offset)
        drawCoin(offset)
        drawPlayer()
    }

    private fun clearBoard() {
        for (r in 0 until VISIBLE_ROWS) {
            for (c in 0 until GameManager.Companion.COLS) {
                grid[r][c].background = null
            }
        }
    }

    private fun drawBombs(offset: Int) {
        gameManager.bombs.forEach { b ->
            val sr = b.r - offset
            if (sr in 0 until VISIBLE_ROWS) {
                grid[sr][b.c].setBackgroundResource(R.drawable.small_bomb)
            }
        }
    }

    private fun drawCoin(offset: Int) {
        gameManager.coin?.let { coin ->
            val sr = coin.r - offset
            if (sr in 0 until VISIBLE_ROWS) {
                grid[sr][coin.c].setBackgroundResource(R.drawable.small_coin)
            }
        }
    }

    private fun drawPlayer() {
        val playerScreenRow =
            (VISIBLE_ROWS - 1 - PLAYER_ROW_OFFSET).coerceIn(0, VISIBLE_ROWS - 1)

        grid[playerScreenRow][gameManager.playerCol].setBackgroundResource(R.drawable.car)
    }

    // ============================================================
    //  UI UPDATES - Lives, Score, Controls
    // ============================================================

    private fun refreshAllUi() {
        updateLivesUI()
        updateScoreUI()
    }

    private fun updateLivesUI() {
        heart1.visibility = if (gameManager.lives >= 1) View.VISIBLE else View.INVISIBLE
        heart2.visibility = if (gameManager.lives >= 2) View.VISIBLE else View.INVISIBLE
        heart3.visibility = if (gameManager.lives >= 3) View.VISIBLE else View.INVISIBLE
    }

    private fun updateScoreUI() {
        scoreText.text = "SCORE: ${gameManager.score}"
    }

    private fun updateControlsUi(isSensorMode: Boolean) {
        if (isSensorMode) {
            // Sensors mode: hide buttons
            btnLeft.visibility = View.INVISIBLE
            btnRight.visibility = View.INVISIBLE
            btnLeft.isEnabled = false
            btnRight.isEnabled = false
        } else {
            // Buttons mode: show buttons
            btnLeft.visibility = View.VISIBLE
            btnRight.visibility = View.VISIBLE
            btnLeft.isEnabled = true
            btnRight.isEnabled = true
        }
    }

    // ============================================================
    //  OVERLAYS - Mini Scores & Game Over
    // ============================================================



    private fun hideMiniScores() {
        miniScoresContainer.visibility = View.GONE
        resumeGameLoop()
    }

    private fun showGameOver(finalScore: Int) {
        isGameOverShown = true
        pauseGameLoop()

        // Stop input so nothing moves
        btnLeft.isEnabled = false
        btnRight.isEnabled = false
        btnMenu.isEnabled = false

        gameOverScore.text = "SCORE: $finalScore"

        gameOverContainer.visibility = View.VISIBLE
        gameOverContainer.bringToFront()
        gameOverContainer.elevation = 200f
    }

    private fun hideGameOver() {
        isGameOverShown = false
        gameOverContainer.visibility = View.GONE

        // Restore input based on current mode
        btnMenu.isEnabled = true
        updateControlsUi(tiltSensorManager.isEnabled())
    }

    // ============================================================
    //  NAVIGATION
    // ============================================================

    private fun openHighScoresScreen() {
        startActivity(Intent(this, LeaderboardActivity::class.java))
    }
}