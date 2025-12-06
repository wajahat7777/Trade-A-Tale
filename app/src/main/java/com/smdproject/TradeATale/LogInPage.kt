package com.smdproject.TradeATale

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class LogInPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_log_in_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { current ->
            if (current.isEmailVerified) {
                startActivity(Intent(this, HomePage::class.java))
            } else {
                startActivity(Intent(this, EmailVerificationPage::class.java))
            }
            finish()
            return
        }

        // Get references to views
        val rootLayout = findViewById<RelativeLayout>(R.id.main)
        val headerLayout = findViewById<RelativeLayout>(R.id.header)
        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        val registerTextView = findViewById<TextView>(R.id.registerTextView)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<TextView>(R.id.loginButton)

        // Set initial state for animation (white header and orange logo)
        headerLayout.setBackgroundColor(android.graphics.Color.WHITE)
        logoImageView.setImageResource(R.drawable.app_logo_orange_right)

        // Create a TranslateAnimation to slide in from the right
        val slideInFromRight = TranslateAnimation(
            1000f,  // Start from 1000px to the right
            0f,     // End at its normal position
            0f,     // No vertical movement
            0f
        )
        slideInFromRight.duration = 1000 // 1 second

        // Create an AlphaAnimation for fading in
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 1000 // 1 second

        // Combine both animations into an AnimationSet
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(slideInFromRight)
        animationSet.addAnimation(fadeIn)

        // Apply the animation to the root layout
        rootLayout.startAnimation(animationSet)

        // Change header background to AppPrimary and logo to white_right after animation ends with fade effect
        animationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                // Fade header background from white to AppPrimary
                val colorFrom = android.graphics.Color.WHITE
                val colorTo = resources.getColor(R.color.AppPrimary, theme)
                val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)
                colorAnimation.duration = 1000 // 1 second
                colorAnimation.addUpdateListener { animator ->
                    headerLayout.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()

                // Change logo to white_right after animation
                logoImageView.setImageResource(R.drawable.app_logo_orange_right)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        // Set click listener for Register Account text
        registerTextView.setOnClickListener {
            // Fade out and slide out to the right
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 2000
            val slideOutToRight = TranslateAnimation(
                0f, 1000f,  // Slide to the right by 1000px
                0f, 0f
            )
            slideOutToRight.duration = 2000
            val outAnimationSet = AnimationSet(true)
            outAnimationSet.addAnimation(fadeOut)
            outAnimationSet.addAnimation(slideOutToRight)

            outAnimationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {
                    // Disable the view during animation to prevent interaction
                    rootLayout.isEnabled = false
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    // Hide the layout to prevent reappearance
                    rootLayout.visibility = View.GONE
                    val intent = Intent(this@LogInPage, RegisterationPage::class.java)
                    startActivity(intent)
                    finish() // Finish after starting the new activity
                }
            })

            rootLayout.startAnimation(outAnimationSet)
        }

        // Set click listener for Login button
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate password length (minimum 8 characters)
            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Sign in with Firebase Authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user == null || !user.isEmailVerified) {
                            Toast.makeText(
                                this,
                                "Please verify your email before logging in.",
                                Toast.LENGTH_LONG
                            ).show()
                            startActivity(Intent(this, EmailVerificationPage::class.java))
                            finish()
                            return@addOnCompleteListener
                        }
                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                        
                        // Initialize OneSignal after successful login
                        OneSignalManager.initialize(this)
                        // Save Player ID after login
                        OneSignalManager.savePlayerIdAfterLogin()
                        
                        // Fade out and slide out to the right after successful login
                        val fadeOut = AlphaAnimation(1f, 0f)
                        fadeOut.duration = 2000
                        val slideOutToRight = TranslateAnimation(
                            0f, 1000f,  // Slide to the right by 1000px
                            0f, 0f
                        )
                        slideOutToRight.duration = 2000
                        val outAnimationSet = AnimationSet(true)
                        outAnimationSet.addAnimation(fadeOut)
                        outAnimationSet.addAnimation(slideOutToRight)

                        outAnimationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                            override fun onAnimationStart(animation: android.view.animation.Animation?) {
                                rootLayout.isEnabled = false
                            }

                            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}

                            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                                rootLayout.visibility = View.GONE
                                // Navigate to a home page or stay logged in (modify intent as needed)
                                val intent = Intent(this@LogInPage, HomePage::class.java)
                                startActivity(intent)
                                finish()
                            }
                        })

                        rootLayout.startAnimation(outAnimationSet)
                    } else {
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}