package com.smdproject.TradeATale

import android.content.Intent
import android.os.Bundle
import android.view.animation.BounceInterpolator
import android.view.animation.TranslateAnimation
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.graphics.Color
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.widget.RelativeLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class WelcomePage : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            startActivity(Intent(this, HomePage::class.java))
            finish()
            return
        }

        // Bounce animation for logo from top to its position
        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        val bounceAnimation = TranslateAnimation(0f, 0f, -1000f, 0f) // Start 1000px above
        bounceAnimation.duration = 2000 // 2 seconds
        bounceAnimation.interpolator = BounceInterpolator() // Adds bounce effect
        bounceAnimation.fillAfter = true

        // Start the bounce animation
        logoImageView.startAnimation(bounceAnimation)

        // Fade-out animation and background transition after bounce completes
        bounceAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                // Change logo source to app_logo_orange_right
                logoImageView.setImageResource(R.drawable.app_logo_orange_right)

                // Smooth background transition to white
                val colorFrom = Color.parseColor("#EA3E23")
                val colorTo = Color.WHITE
                val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                colorAnimation.duration = 1000 // 1 second
                colorAnimation.addUpdateListener { animator ->
                    findViewById<RelativeLayout>(R.id.main).setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()

                // Fade-out animation
                val fadeOutAnimation = AlphaAnimation(1f, 0f)
                fadeOutAnimation.duration = 2500 // 2.5 seconds
                fadeOutAnimation.fillAfter = true
                logoImageView.startAnimation(fadeOutAnimation)

                // Navigate to LogInPage after fade-out completes
                fadeOutAnimation.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                    override fun onAnimationStart(animation: android.view.animation.Animation?) {}

                    override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                        val intent = Intent(this@WelcomePage, LogInPage::class.java)
                        startActivity(intent)
                        finish() // Close WelcomePage
                    }

                    override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                })
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
    }
}