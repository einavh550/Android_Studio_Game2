package com.example.android_studio_game

import android.widget.ImageButton
import android.widget.PopupMenu
import android.view.MenuItem


class GameMenuController(
    private val activity: MainActivity,
    private val menuButton: ImageButton,
    private val gameManager: GameManager,
    private val tiltSensorManager: TiltSensorManager,
    private val onSpeedChanged: () -> Unit,
    private val onHighScoresClicked: () -> Unit,
    private val onMenuOpened: () -> Unit,
    private val onMenuClosed: () -> Unit,
    private val onControlModeChanged: (Boolean) -> Unit
) {

    fun setup() {
        menuButton.setOnClickListener {
            onMenuOpened()
            showMenu()
        }
    }

    private fun showMenu() {
        val popup = PopupMenu(activity, menuButton)
        popup.menuInflater.inflate(R.menu.menu_game, popup.menu)

        popup.setOnDismissListener {
            onMenuClosed()
        }

        // sync Fast Mode check state
        popup.menu.findItem(R.id.menu_fast_mode).isChecked =
            gameManager.isFastMode()

        val controlItem = popup.menu.findItem(R.id.menu_control_mode)
        syncControlTitle(controlItem)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {

                R.id.menu_fast_mode -> {
                    toggleFastMode(item)
                    true
                }

                R.id.menu_control_mode -> {
                    toggleControlMode(item)
                    true
                }
                R.id.menu_high_scores -> {
                    onHighScoresClicked()
                    popup.dismiss()
                    true
                }

                else -> false
            }
        }

        popup.show()
    }

    private fun toggleFastMode(item: android.view.MenuItem) {
        val newState = !gameManager.isFastMode()
        gameManager.setFastMode(newState)
        item.isChecked = newState

        // notify Activity so loop speed updates immediately
        onSpeedChanged()
    }
    private fun toggleControlMode(item: MenuItem) {
        val newState = !tiltSensorManager.isEnabled() // true=sensors, false=buttons
        tiltSensorManager.setEnabled(newState)
        syncControlTitle(item)
        onControlModeChanged(newState)
    }

    private fun syncControlTitle(item: MenuItem) {
        val isSensorMode = tiltSensorManager.isEnabled()
        item.title = if (isSensorMode) "Control: Sensors" else "Control: Buttons"
    }


}
