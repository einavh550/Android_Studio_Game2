# Android Racing Game – Avoid the Bombs

A simple Android racing-style game developed as part of the course project.  
The goal is to control the car, avoid falling bombs, collect coins, and survive as long as possible while the game speed dynamically changes.

---

## Game Description
The player controls a racing car positioned at the bottom of the screen.  
Bombs fall from the top of the screen in multiple lanes, and the player must avoid collisions.  
Coins may appear during gameplay and increase the score when collected.

The game supports both button-based controls and sensor-based controls using device tilt.

---

## Controls

### Button Mode
- **Left Button** – Move the car left
- **Right Button** – Move the car right

### Sensor Mode
- **Tilt Left / Right** – Move the car accordingly
- **Tilt Forward (away from the body)** – Increase game speed (FAST mode)
- **Tilt Backward (towards the body)** – Decrease game speed (SLOW mode)
- **Hold device straight** – Return to normal speed

---

## Features
- Multiple falling bomb lanes
- Player lives system (heart-based)
- Dynamic score system (distance + coin bonus)
- Coin collection (+10 score)
- Game Over screen with restart option
- Popup menu with:
  - Control mode switching (Buttons / Sensors)
  - Speed mode toggle
  - Access to High Scores
- Sensor-based movement using accelerometer
- Tilt-based speed control (Bonus feature)
- Sound effects and vibration on collision
- Mini scores overlay (Top 10 scores)
- High Scores screen with optional location saving
- Google Maps integration for score location display

---

## Bonus Implementation
The game includes a bonus feature using the Y-axis of the accelerometer:
- Tilting the device forward increases the game speed
- Tilting the device backward slows the game down
- Returning the device to a neutral position restores normal speed

This feature is implemented with calibration, dead zone handling, and smooth speed transitions.

---

## Setup Instructions

### Google Maps API
To enable map functionality for high scores:

1. Create a file named `local.properties` in the root project directory
2. Add the following line:
