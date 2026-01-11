package com.example.android_studio_game

import kotlin.random.Random

class GameManager {

    companion object {
        const val ROWS = 12
        const val COLS = 5

        private const val PLAYER_ROW_OFFSET = 1
        const val PLAYER_ROW = ROWS - 1 - PLAYER_ROW_OFFSET
        private const val MAX_BOMBS = 3
    }

    var playerCol = 2
        private set

    var lives = 3
        private set

    var score = 0
        private set



    var coin: Coin? = null
    private var fastMode = false
    private var playerRowOffset = 0




    data class Bomb(var r: Int, var c: Int)
    data class Coin(var r: Int, var c: Int)

    val bombs = mutableListOf<Bomb>()

    fun movePlayer(delta: Int) {
        val next = playerCol + delta
        if (next in 0 until COLS) {
            playerCol = next
        }
    }

    fun stepBombs() {
        bombs.forEach { it.r++ }
        bombs.removeAll { it.r > PLAYER_ROW }

        if (bombs.size < MAX_BOMBS) {
            bombs.add(Bomb(0, Random.nextInt(COLS)))
        }
    }


    fun checkCollision(): Boolean {
        val playerRow = getPlayerRow()
        return bombs.any { it.r == PLAYER_ROW && it.c == playerCol }
    }

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


    fun advanceScoreByDistance() {
        score++
    }


    fun onCrash(): Boolean {
        lives--
        if (lives < 0) lives = 0
        bombs.removeAll { it.r == PLAYER_ROW && it.c == playerCol }
        return lives == 0
    }


    fun isGameOver(): Boolean = lives == 0

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

    fun setFastMode(enabled: Boolean) {
        fastMode = enabled
    }
    fun isFastMode(): Boolean = fastMode


    fun getTickDelay(): Long {
        return if (fastMode) 200L else 350L
    }

    fun setPlayerRowOffset(offset: Int) {
        playerRowOffset = offset.coerceAtLeast(0)
    }

    private fun getPlayerRow(): Int {
        return ROWS - 1 - playerRowOffset
    }


}
