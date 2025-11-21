package com.cybernobie.coincatcher

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MainActivity : AppCompatActivity() {

    private var mInterstitialAd: InterstitialAd? = null
    private lateinit var gameView: GameView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this) {}

        // Load Banner Ad
        val adView = findViewById<AdView>(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        // Load Interstitial Ad
        loadInterstitialAd()

        gameView = findViewById(R.id.gameView)
        val startButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.startButton)
        val pauseButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.pauseButton)
        val devInfoButton = findViewById<com.google.android.material.button.MaterialButton>(R.id.devInfoButton)
        
        pauseButton.visibility = android.view.View.GONE // Hide initially

        startButton.setOnClickListener {
            startButton.visibility = android.view.View.GONE
            devInfoButton.visibility = android.view.View.GONE
            pauseButton.visibility = android.view.View.VISIBLE
            gameView.startGame()
        }
        
        pauseButton.setOnClickListener {
            gameView.togglePause()
            pauseButton.text = if (gameView.isPaused) "Resume" else "Pause"
        }
        
        devInfoButton.setOnClickListener {
            showDevInfoDialog()
        }

        gameView.gameOverListener = {
            runOnUiThread {
                showInterstitialAd()
                pauseButton.visibility = android.view.View.GONE
                startButton.visibility = android.view.View.VISIBLE
                devInfoButton.visibility = android.view.View.VISIBLE
                startButton.text = "Play Again"
            }
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (gameView.isGameStarted && !gameView.isPaused && !gameView.isGameOver) {
                    gameView.togglePause()
                    pauseButton.text = "Resume"
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    
    private fun showDevInfoDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Developer Info")
            .setMessage("Coin Catcher\nVersion 1.0\n\nCreated by: Aryan Singh Negi\nPowered by: Team CyberNobie")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(this, getString(R.string.interstitial_ad_unit_id), adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d("TAG", adError.toString())
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d("TAG", "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                    
                    mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            Log.d("TAG", "Ad dismissed fullscreen content.")
                            mInterstitialAd = null
                            loadInterstitialAd() // Reload for next time
                        }

                        override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                            Log.d("TAG", "Ad failed to show fullscreen content.")
                            mInterstitialAd = null
                        }

                        override fun onAdShowedFullScreenContent() {
                            Log.d("TAG", "Ad showed fullscreen content.")
                        }
                    }
                }
            })
    }

    private fun showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(this)
        } else {
            Log.d("TAG", "The interstitial ad wasn't ready yet.")
            loadInterstitialAd()
        }
    }
}
