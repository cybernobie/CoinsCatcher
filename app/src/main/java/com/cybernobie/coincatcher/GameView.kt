package com.cybernobie.coincatcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.util.Random

class GameView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val paint = Paint()
    private val textPaint = Paint()
    
    // Player (Collector) properties
    private var playerRect = RectF()
    private var playerWidth = 200f
    private var playerHeight = 50f
    private var initialPlayerWidth = 200f
    private val heightFromBottom = 200f
    
    // Game Items
    private val coinRadius = 40f
    data class GameItem(var x: Float, var y: Float, val speed: Float, val type: Int) // 0: Coin, 1: Life
    private val items = ArrayList<GameItem>()
    
    // Game State
    private var score = 0
    private var lives = 3
    private val maxLives = 5
    var isGameOver = false
    var isGameStarted = false
    var isPaused = false
    private val random = Random()
    private var screenWidth = 0
    private var screenHeight = 0
    
    // Audio
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    
    var gameOverListener: (() -> Unit)? = null

    init {
        paint.isAntiAlias = true
        textPaint.isAntiAlias = true
        textPaint.textAlign = Paint.Align.CENTER
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
        
        // Initialize player size based on screen
        initialPlayerWidth = w * 0.15f // Start at 15%
        playerWidth = initialPlayerWidth
        playerHeight = 60f // Fixed height
        
        updatePlayerRect(w / 2f)
    }
    
    fun startGame() {
        isGameStarted = true
        isPaused = false
        resetGame()
    }
    
    fun togglePause() {
        if (isGameStarted && !isGameOver) {
            isPaused = !isPaused
            invalidate()
        }
    }

    private fun updatePlayerRect(centerX: Float) {
        var left = centerX - playerWidth / 2
        var right = centerX + playerWidth / 2
        
        // Clamp to screen
        if (left < 0) {
            left = 0f
            right = playerWidth
        }
        if (right > screenWidth) {
            right = screenWidth.toFloat()
            left = screenWidth - playerWidth
        }
        
        val top = screenHeight - heightFromBottom - playerHeight
        val bottom = screenHeight - heightFromBottom
        playerRect.set(left, top, right, bottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Background is now handled by Lottie in the layout

        if (!isGameStarted) {
            return
        }

        if (isPaused) {
            drawPaused(canvas)
            return
        }

        if (isGameOver) {
            drawGameOver(canvas)
            return
        }

        drawPlayer(canvas)
        drawGameItems(canvas)
        drawScore(canvas)
        drawLives(canvas)

        invalidate()
    }

    private fun drawPlayer(canvas: Canvas) {
        paint.color = Color.DKGRAY
        paint.style = Paint.Style.FILL
        canvas.drawRect(playerRect, paint)
        
        paint.color = Color.BLACK
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        canvas.drawRect(playerRect, paint)
        paint.style = Paint.Style.FILL 
    }

    private fun drawGameItems(canvas: Canvas) {
        val iterator = items.iterator()
        while (iterator.hasNext()) {
            val item = iterator.next()
            
            if (item.type == 0) { // Coin
                // Outer Gold Circle
                paint.color = Color.parseColor("#FFD700") 
                paint.style = Paint.Style.FILL
                canvas.drawCircle(item.x, item.y, coinRadius, paint)
                
                // Inner Ring
                paint.color = Color.parseColor("#DAA520") 
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 5f
                canvas.drawCircle(item.x, item.y, coinRadius - 5, paint)
                
                // Dollar Sign
                textPaint.color = Color.parseColor("#B8860B") 
                textPaint.textSize = coinRadius * 1.2f
                textPaint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                val fontMetrics = textPaint.fontMetrics
                val offset = (fontMetrics.descent + fontMetrics.ascent) / 2
                canvas.drawText("$", item.x, item.y - offset, textPaint)
            } else { // Life (Heart)
                textPaint.color = Color.RED
                textPaint.textSize = coinRadius * 2f
                textPaint.typeface = android.graphics.Typeface.DEFAULT
                val fontMetrics = textPaint.fontMetrics
                val offset = (fontMetrics.descent + fontMetrics.ascent) / 2
                canvas.drawText("❤", item.x, item.y - offset, textPaint)
            }

            // Move
            item.y += item.speed
            
            // Collision Check
            if (RectF.intersects(playerRect, RectF(item.x - coinRadius, item.y - coinRadius, item.x + coinRadius, item.y + coinRadius))) {
                if (item.type == 0) {
                    collectCoin()
                } else {
                    collectLife()
                }
                iterator.remove()
            } else if (item.y > screenHeight) {
                if (item.type == 0) {
                    // Missed coin - Lose Life
                    lives--
                    if (lives <= 0) {
                        isGameOver = true
                        gameOverListener?.invoke()
                    }
                }
                // Missed life - nothing happens, just remove
                if (items.contains(item)) iterator.remove() // Check contains to avoid concurrent mod if logic changes
            }
        }
        
        // Spawn Items
        if (random.nextInt(100) < 2) { 
            val x = random.nextFloat() * (screenWidth - 2 * coinRadius) + coinRadius
            val speed = 10f + random.nextFloat() * 10f
            // 10% chance for Life, 90% Coin
            val type = if (random.nextInt(10) == 0) 1 else 0
            items.add(GameItem(x, -coinRadius, speed, type))
        }
    }

    private fun collectCoin() {
        score++
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        
        val maxPlayerWidth = screenWidth * 0.30f
        if (playerWidth < maxPlayerWidth) {
            playerWidth *= 1.05f
            if (playerWidth > maxPlayerWidth) playerWidth = maxPlayerWidth
        }
        updatePlayerRect(playerRect.centerX())
    }
    
    private fun collectLife() {
        if (lives < maxLives) {
            lives++
            // Different sound for life?
            toneGenerator.startTone(ToneGenerator.TONE_DTMF_0, 150) 
        }
    }

    private fun drawScore(canvas: Canvas) {
        textPaint.color = Color.BLACK
        textPaint.textSize = 60f
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("Score: $score", 50f, 200f, textPaint)
    }
    
    private fun drawLives(canvas: Canvas) {
        textPaint.color = Color.RED
        textPaint.textSize = 60f
        textPaint.textAlign = Paint.Align.RIGHT
        var hearts = ""
        for (i in 1..lives) {
            hearts += "❤ "
        }
        canvas.drawText(hearts, screenWidth - 50f, 100f, textPaint)
    }

    private fun drawGameOver(canvas: Canvas) {
        textPaint.color = Color.BLACK
        textPaint.textSize = 100f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Game Over", screenWidth / 2f, screenHeight / 2f, textPaint)
        textPaint.textSize = 60f
        canvas.drawText("Score: $score", screenWidth / 2f, screenHeight / 2f + 150f, textPaint)
        // Removed "Tap to Restart" text
    }
    
    private fun drawPaused(canvas: Canvas) {
        textPaint.color = Color.BLACK
        textPaint.textSize = 100f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("PAUSED", screenWidth / 2f, screenHeight / 2f, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isGameStarted || isPaused) return false
        
        if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
            if (!isGameOver) {
                updatePlayerRect(event.x)
            } 
            // Removed resetGame() on touch
        }
        return true
    }

    private fun resetGame() {
        score = 0
        lives = 3
        items.clear()
        isGameOver = false
        playerWidth = initialPlayerWidth
        updatePlayerRect(screenWidth / 2f)
        invalidate()
    }
}
