package com.trioscg.androidapp1

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {

    private lateinit var redButton: ImageButton
    private lateinit var greenButton: ImageButton
    private lateinit var blueButton: ImageButton
    private lateinit var yellowButton: ImageButton
    private lateinit var startButton: Button
    private lateinit var scoreTextView: TextView
    private lateinit var topScoresText: TextView
    private lateinit var resetHighScoresButton: Button

    private val colors = listOf("red", "green", "blue", "yellow")
    private val sequence = mutableListOf<String>() // Stores the generated color sequence
    private val topScores = mutableListOf<Int>() // Stores top 5 high scores

    private val PREFS_NAME = "HighScores" // SharedPreferences name
    private val SCORES_KEY = "TopScores" // Key for storing scores

    private var playerIndex = 0 // Tracks player's progress through the sequence
    private var isPlayerTurn = false // Indicates if it's the player's turn
    private var score = 0 // Player's current score
    private var buttonClickSound: MediaPlayer? = null // Handles button sound playback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        redButton = findViewById(R.id.redButton)
        greenButton = findViewById(R.id.greenButton)
        blueButton = findViewById(R.id.blueButton)
        yellowButton = findViewById(R.id.yellowButton)
        startButton = findViewById(R.id.startButton)
        scoreTextView = findViewById(R.id.scoreTextView)
        topScoresText = findViewById(R.id.topScoresText)
        resetHighScoresButton = findViewById(R.id.resetHighScoresButton)

        // Set up button listeners
        setButtonListeners()

        // Load and display top scores
        loadTopScores()
        displayTopScores()

        // Start a new game when the start button is clicked
        startButton.setOnClickListener {
            startNewGame()
        }

        // Clear high scores when reset button is clicked
        resetHighScoresButton.setOnClickListener {
            clearHighScores()
            topScoresText.text = getString(R.string.top_5_scores)
            Toast.makeText(this, getString(R.string.high_scores_cleared), Toast.LENGTH_SHORT).show()
        }
    } // onCreate()

    // Sets up the listeners for each color button
    private fun setButtonListeners() {
        redButton.setOnClickListener { onColorPressed("red") }
        greenButton.setOnClickListener { onColorPressed("green") }
        blueButton.setOnClickListener { onColorPressed("blue") }
        yellowButton.setOnClickListener { onColorPressed("yellow") }
    } // setButtonListeners()

    // Handles logic when a color button is pressed by the player
    private fun onColorPressed(color: String) {
        if (!isPlayerTurn) return  // Ignore input if not player's turn

        playButtonClickSound()
        flashButton(color)

        if (color == sequence[playerIndex]) {
            score++
            updateScore()
            playerIndex++

            if (playerIndex == sequence.size) {
                isPlayerTurn = false
                // Wait a moment before showing next round
                Handler(Looper.getMainLooper()).postDelayed({ nextRound() }, 1000)
            }
        } else {
            playWrongSound()
            Toast.makeText(this, "Wrong! Game Over!", Toast.LENGTH_SHORT).show()
            updateTopScores()
            displayTopScores()
            isPlayerTurn = false  // Prevent further input after game ends
        }
    } // onColorPressed()

    // Starts a new game by resetting score and sequence
    private fun startNewGame() {
        score = 0
        updateScore()
        sequence.clear()
        playerIndex = 0
        nextRound()
    } // startNewGame()

    // Adds a new color to the sequence and starts the next round
    private fun nextRound() {
        val newColor = colors.random()
        sequence.add(newColor)
        playerIndex = 0
        playSequence()
    } // nextRound()

    // Plays the current sequence visually and audibly
    private fun playSequence() {
        isPlayerTurn = false
        var delay = 0L

        // Schedule each color in the sequence to flash with delay
        for (color in sequence) {
            Handler(Looper.getMainLooper()).postDelayed({
                flashButton(color)
            }, delay)
            delay += 700
        }

        // Enable user input after sequence finishes
        Handler(Looper.getMainLooper()).postDelayed({
            isPlayerTurn = true
        }, delay)
    } // playSequence()

    // Flashes the button and plays the sound for the given color
    private fun flashButton(color: String) {
        val button = when (color) {
            "red" -> redButton
            "green" -> greenButton
            "blue" -> blueButton
            "yellow" -> yellowButton
            else -> return
        }

        button.alpha = 0.3f // Dim the button to indicate a flash
        playButtonClickSound()

        Handler(Looper.getMainLooper()).postDelayed({
            button.alpha = 1.0f // Restore brightness
        }, 300)
    } // flashButton()

    // Plays the button press sound
    private fun playButtonClickSound() {
        buttonClickSound?.release()
        buttonClickSound = MediaPlayer.create(this, R.raw.button_sound)
        buttonClickSound?.start()
        buttonClickSound?.setOnCompletionListener {
            it.release()
            buttonClickSound = null
        }
    } // playButtonClickedSound()

    // Plays the wrong answer sound effect
    private fun playWrongSound() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.wrong)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    } // playWrongSound()

    // Updates the score text view
    private fun updateScore() {
        scoreTextView.text = buildString {
        append("Score: ")
        append(score)
        }
    } // updateScore()

    // Adds the score to the top scores and saves it
    private fun updateTopScores() {
        topScores.add(score)
        topScores.sortDescending()
        if (topScores.size > 5) {
            topScores.removeAt(topScores.lastIndex)
        }
        saveTopScores()
    } // updateTopScores()

    // Displays the top 5 scores on the screen
    private fun displayTopScores() {
        val scoreText = StringBuilder(getString(R.string.top_5_scores) + "\n")
        topScores.forEachIndexed { index, score ->
            scoreText.append("${index + 1}: $score\n")
        }
        topScoresText.text = scoreText.toString()
    } // displayTopScores()

    // Saves the top scores to shared preferences
    private fun saveTopScores() {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sharedPref.edit {
            putString(SCORES_KEY, topScores.joinToString(","))
        }
    } // saveTopScores()

    // Loads the top scores from shared preferences
    private fun loadTopScores() {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedScores = sharedPref.getString(SCORES_KEY, "")
        if (!savedScores.isNullOrBlank()) {
            topScores.clear()
            topScores.addAll(
                savedScores.split(",")
                    .filter { it.isNotBlank() }
                    .mapNotNull { it.toIntOrNull() }
            )
        }
    } // loadTopScores()

    // Clears the high scores from memory and storage
    private fun clearHighScores() {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sharedPref.edit {
            remove(SCORES_KEY)
        }
        topScores.clear()
    } // clearHighScores()
} // AndroidApp1