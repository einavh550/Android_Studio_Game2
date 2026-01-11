package com.example.android_studio_game.game

import kotlin.random.Random

class GameManager {

    // ============================================================
    //  COMPANION OBJECT - Constants
    // ============================================================

    companion object {
        const val ROWS = 12
        const val COLS = 5

        private const val PLAYER_ROW_OFFSET = 1
        const val PLAYER_ROW = ROWS - 1 - PLAYER_ROW_OFFSET
        private const val MAX_BOMBS = 3
    }

    // ============================================================
    //  DATA CLASSES
    // ============================================================

    data class Bomb(var r: Int, var c: Int)
    data class Coin(var r: Int, var c: Int)

    // ============================================================
    //  PROPERTIES - Game State
    // ============================================================

    var playerCol = 2
        private set

    var lives = 3
        private set

    var score = 0
        private set

    var coin: Coin? = null

    val bombs = mutableListOf<Bomb>()

    private var fastMode = false
    private var slowMode = false
    private var playerRowOffset = 0

    // ============================================================
    //  PLAYER MOVEMENT
    // ============================================================

    fun movePlayer(delta: Int) {
        val next = playerCol + delta
        if (next in 0 until COLS) {
            playerCol = next
        }
    }

    fun setPlayerRowOffset(offset: Int) {
        playerRowOffset = offset.coerceAtLeast(0)
    }

    private fun getPlayerRow(): Int {
        return ROWS - 1 - playerRowOffset
    }

    // ============================================================
    //  BOMBS - Movement & Spawning
    // ============================================================

    fun stepBombs() {
        bombs.forEach { it.r++ }
        bombs.removeAll { it.r > PLAYER_ROW }

        if (bombs.size < MAX_BOMBS) {
            bombs.add(Bomb(0, Random.Default.nextInt(COLS)))
        }
    }

    fun checkCollision(): Boolean {
        val playerRow = getPlayerRow()
        return bombs.any { it.r == PLAYER_ROW && it.c == playerCol }
    }

    // ============================================================
    //  COINS - Spawning & Collection
    // ============================================================

    fun spawnCoinIfNeeded() {
        if (coin != null) return

        val occupiedColumns = bombs.map { it.c }.toSet()
        val freeColumns = (0 until COLS).filter { it !in occupiedColumns }

        if (freeColumns.isNotEmpty()) {
            coin = Coin(
                r = 0,
                c = freeColumns.random()
            )
        }
    }

    fun stepCoin() {
        coin?.let {
            it.r++
            if (it.r > PLAYER_ROW) {
                coin = null
            }
        }
    }

    fun checkCoinCollected(): Boolean {
        val currentCoin = coin ?: return false

        val collected = (currentCoin.r == PLAYER_ROW && currentCoin.c == playerCol)

        if (collected) {
            score += 10
            coin = null
        }

        return collected
    }

    // ============================================================
    //  SCORE MANAGEMENT
    // ============================================================

    fun advanceScoreByDistance() {
        score++
    }

    // ============================================================
    //  CRASH & GAME OVER
    // ============================================================

    fun onCrash(): Boolean {
        lives--
        if (lives < 0) lives = 0
        bombs.removeAll { it.r == PLAYER_ROW && it.c == playerCol }
        return lives == 0
    }

    fun isGameOver(): Boolean = lives == 0

    // ============================================================
    //  GAME RESET
    // ============================================================

    fun resetGame() {
        lives = 3
        coin = null
        score = 0
        resetRound()
    }

    fun resetRound() {
        playerCol = 2
        bombs.clear()
    }

    // ============================================================
    //  SPEED MODES
    // ============================================================

    fun setFastMode(enabled: Boolean) {
        fastMode = enabled
        if (enabled) slowMode = false
    }

    fun isFastMode(): Boolean = fastMode

    fun setSlowMode(enabled: Boolean) {
        slowMode = enabled
        if (enabled) fastMode = false
    }

    fun getTickDelay(): Long {
        return when {
            fastMode -> 150L
            slowMode -> 750L
            else -> 350L
        }
    }
}