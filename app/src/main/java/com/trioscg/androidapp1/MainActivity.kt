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
    private lateinit var roundTextView: TextView

    private val colors = listOf("red", "green", "blue", "yellow")
    private val sequence = mutableListOf<String>() // Stores the generated color sequence
    private val topScores = mutableListOf<Pair<Int, Int>>() // List to store Pair of (Round, Score)
    private val PREFS_NAME = "HighScores" // SharedPreferences name
    private val SCORES_KEY = "TopScores" // Key for storing scores

    private var playerIndex = 0 // Tracks player's progress through the sequence
    private var isPlayerTurn = false // Indicates if it's the player's turn
    private var score = 0 // Player's current score
    private var buttonClickSound: MediaPlayer? = null // Handles button sound playback
    private var round = 1 // Tracks current round number

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        redButton = findViewById(R.id.redButton)
        greenButton = findViewById(R.id.greenButton)
        blueButton = findViewById(R.id.blueButton)
        yellowButton = findViewById(R.id.yellowButton)
        startButton = findViewById(R.id.startButton)
        scoreTextView = findViewById(R.id.scoreTextView)
        roundTextView = findViewById(R.id.roundTextView)
        topScoresText = findViewById(R.id.topScoresText)
        resetHighScoresButton = findViewById(R.id.resetHighScoresButton)

        // Set up button listeners
        setButtonListeners()

        // Load and display top scores from SharedPreferences
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

        playButtonClickSound() // Play the sound when the button is pressed
        flashButton(color) // Flash the button visually

        if (color == sequence[playerIndex]) {
            score++ // Increment score for correct input
            updateScore() // Update score display
            playerIndex++

            if (playerIndex == sequence.size) {
                isPlayerTurn = false // Player finished the sequence
                // Wait a moment before showing next round
                Handler(Looper.getMainLooper()).postDelayed({ nextRound() }, 1000)
            }
        } else {
            playWrongSound() // Play wrong answer sound
            Toast.makeText(this, "Wrong! Game Over!", Toast.LENGTH_SHORT).show()
            updateTopScores() // Update top scores list
            displayTopScores() // Display top scores
            isPlayerTurn = false  // Prevent further input after game ends
        }
    } // onColorPressed()

    // Starts a new game by resetting score and sequence
    private fun startNewGame() {
        score = 0
        updateScore()
        sequence.clear()
        playerIndex = 0
        nextRound() // Start the first round
        round = 1
        updateRoundDisplay() // Display the current round
    } // startNewGame()

    // Adds a new color to the sequence and starts the next round
    private fun nextRound() {
        val newColor = colors.random() // Randomly pick a new color
        sequence.add(newColor) // Add the new color to the sequence
        playerIndex = 0 // Reset the player's index
        playSequence() // Play the sequence
        round++ // Increment the round
        updateRoundDisplay() // Update the round display
    } // nextRound()

    // Plays the current sequence visually and audibly
    private fun playSequence() {
        isPlayerTurn = false // Disable player input during sequence playback
        var delay = 0L

        // Schedule each color in the sequence to flash with delay
        for (color in sequence) {
            Handler(Looper.getMainLooper()).postDelayed({
                flashButton(color) // Flash each button in the sequence
            }, delay)
            delay += 700 // Add delay between flashes
        }

        // Enable user input after sequence finishes
        Handler(Looper.getMainLooper()).postDelayed({
            isPlayerTurn = true // Enable player input after sequence
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
        playButtonClickSound() // Play button click sound

        Handler(Looper.getMainLooper()).postDelayed({
            button.alpha = 1.0f // Restore brightness after flash
        }, 300)
    } // flashButton()

    // Updates the round display text view
    private fun updateRoundDisplay() {
        roundTextView.text = buildString {
            append("Round: ")
            append(round)
        }
    } // updateRoundDisplay()

    // Plays the button press sound
    private fun playButtonClickSound() {
        buttonClickSound?.release()
        buttonClickSound = MediaPlayer.create(this, R.raw.button_sound)
        buttonClickSound?.start()
        buttonClickSound?.setOnCompletionListener {
            it.release()
            buttonClickSound = null
        }
    } // playButtonClickSound()

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

    // Adds the score to the top scores list and saves it
    private fun updateTopScores() {
        topScores.add(Pair(round - 1, score)) // Store round and score as a pair
        topScores.sortByDescending { it.second } // Sort by score, descending
        if (topScores.size > 5) {
            topScores.removeAt(topScores.lastIndex) // Keep only the top 5 scores
        }
        saveTopScores() // Save the updated top scores
    } // updateTopScores()

    // Displays the top 5 scores on the screen
    private fun displayTopScores() {
        val scoreText = StringBuilder(getString(R.string.top_5_scores) + "\n")
        topScores.forEachIndexed { index, scorePair ->
            scoreText.append("${index + 1}: Round ${scorePair.first} - Score: ${scorePair.second}\n")
        }
        topScoresText.text = scoreText.toString()
    } // displayTopScores()

    // Saves the top scores to SharedPreferences
    private fun saveTopScores() {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val topScoresString = topScores.joinToString(",") { "${it.first}:${it.second}" } // Convert Pair to string "round:score"
        sharedPref.edit {
            putString(SCORES_KEY, topScoresString) // Store the scores as a string
        }
    } // saveTopScores()

    // Loads the top scores from SharedPreferences
    private fun loadTopScores() {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedScores = sharedPref.getString(SCORES_KEY, "")
        if (!savedScores.isNullOrBlank()) {
            topScores.clear()
            topScores.addAll(
                savedScores.split(",")
                    .filter { it.isNotBlank() }
                    .mapNotNull {
                        val parts = it.split(":")
                        if (parts.size == 2) {
                            val round = parts[0].toIntOrNull()
                            val score = parts[1].toIntOrNull()
                            if (round != null && score != null) Pair(round, score) else null
                        } else null
                    }
            )
        }
    } // loadTopScores()

    // Clears the high scores from both memory and storage
    private fun clearHighScores() {
        val sharedPref = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        sharedPref.edit {
            remove(SCORES_KEY) // Remove the saved scores from SharedPreferences
        }
        topScores.clear() // Clear the top scores list
    } // clearHighScores()
} // MainActivity class end