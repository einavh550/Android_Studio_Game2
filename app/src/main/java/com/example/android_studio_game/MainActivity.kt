package com.example.android_studio_game

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

class MainActivity : AppCompatActivity() {

    companion object {
        private const val VISIBLE_ROWS = 7
        private const val PLAYER_ROW_OFFSET = 1
    }

    private lateinit var btnLeft: ImageButton
    private lateinit var btnRight: ImageButton
    private lateinit var btnMenu: ImageButton

    private lateinit var heart1: ImageView
    private lateinit var heart2: ImageView
    private lateinit var heart3: ImageView
    private lateinit var carView: ImageView

    private lateinit var scoreText: TextView
    private lateinit var grid: Array<Array<View>>

    private lateinit var gameManager: GameManager
    private lateinit var tiltSensorManager: TiltSensorManager
    private lateinit var menuController: GameMenuController

    private lateinit var miniScoresContainer: View
    private lateinit var miniScoresFragment: MiniScoresFragment
    private lateinit var gameOverContainer: View
    private lateinit var gameOverScore: TextView
    private lateinit var gameOverRestartBtn: View
    private var isGameOverShown = false
    private var isLoopPaused = false





    private val handler = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            tick()

            if (isLoopPaused || isGameOverShown) return

            handler.postDelayed(this, gameManager.getTickDelay())
        }
    }


    private var pendingScoreToSave: Int? = null

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

    // ----------------------------
    //  Setup / Init
    // ----------------------------

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



    private fun setupSensors() {
        tiltSensorManager = TiltSensorManager(this, object : TiltCallback {
            override fun tiltX(x: Float) {
                if (isGameOverShown) return
                if (!tiltSensorManager.isEnabled()) return
                if (x > 0) gameManager.movePlayer(-1) else gameManager.movePlayer(1)
            }


            override fun tiltY(y: Float) {
                // BONUS later
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
            }
        )
        menuController.setup()
    }



    private fun setupGrid() {
        grid = Array(VISIBLE_ROWS) { r ->
            Array(GameManager.COLS) { c ->
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

    private fun startGameLoop() {
        isLoopPaused = false
        handler.removeCallbacks(loop)
        handler.post(loop)
    }

    private fun restartGameLoopImmediately() {
        if (isLoopPaused || isGameOverShown) return
        handler.removeCallbacks(loop)
        handler.post(loop)
    }

    private fun hideGameOver() {
        isGameOverShown = false
        gameOverContainer.visibility = View.GONE

        // restore input based on current mode
        btnMenu.isEnabled = true
        updateControlsUi(tiltSensorManager.isEnabled())
    }

    private fun restartGame() {
        hideGameOver()
        gameManager.resetGame()
        refreshAllUi()
        render()
        resumeGameLoop()
    }




    // ----------------------------
    //  Game Tick
    // ----------------------------

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

    // ----------------------------
    //  Rendering / UI
    // ----------------------------

    private fun render() {
        clearBoard()

        val offset = GameManager.ROWS - VISIBLE_ROWS
        drawBombs(offset)
        drawCoin(offset)
        drawPlayer()
    }

    private fun clearBoard() {
        for (r in 0 until VISIBLE_ROWS) {
            for (c in 0 until GameManager.COLS) {
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

            // optional: prevent clicks completely
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


    // ----------------------------
    // Mini Scores (legacy overlay)
    // ----------------------------

    private fun setupMiniScores() {
        miniScoresFragment = MiniScoresFragment()
        miniScoresFragment.onCloseClicked = { hideMiniScores() }

        supportFragmentManager.beginTransaction()
            .add(R.id.main_FRAME_mini_scores, miniScoresFragment)
            .commit()
    }

    private fun toggleMiniScores() {
        if (miniScoresContainer.visibility == View.VISIBLE) {
            hideMiniScores()
        } else {
            showMiniScores()
        }
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




    private fun showMiniScores() {
        pauseGameLoop()
        miniScoresContainer.visibility = View.VISIBLE
        miniScoresContainer.bringToFront()
        miniScoresContainer.elevation = 50f

        val top = HighScoreStorage.getTop10().map { it.score }
        miniScoresFragment.setScores(top)
    }

    private fun hideMiniScores() {
        miniScoresContainer.visibility = View.GONE
        resumeGameLoop()
    }

    private fun showGameOver(finalScore: Int) {
        isGameOverShown = true
        pauseGameLoop()

        // stop input so nothing moves
        btnLeft.isEnabled = false
        btnRight.isEnabled = false
        btnMenu.isEnabled = false

        gameOverScore.text = "SCORE: $finalScore"

        gameOverContainer.visibility = View.VISIBLE
        gameOverContainer.bringToFront()
        gameOverContainer.elevation = 200f
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

    private fun openHighScoresScreen() {
        startActivity(Intent(this, LeaderboardActivity::class.java))
    }

    // ----------------------------
    //  Lifecycle
    // ----------------------------

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
}
