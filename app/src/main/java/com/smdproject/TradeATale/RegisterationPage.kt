package com.smdproject.TradeATale

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class RegisterationPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_registeration_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth and Realtime Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(FirebaseConfig.REALTIME_DB_URL)

        // Get references to views
        val rootLayout = findViewById<RelativeLayout>(R.id.main)
        val headerLayout = findViewById<RelativeLayout>(R.id.header)
        val logoImageView = findViewById<ImageView>(R.id.logoImageView)
        val alreadyRegisteredTextView = findViewById<TextView>(R.id.alreadyRegisteredTextView)
        val usernameEditText = findViewById<EditText>(R.id.usernameEditText)
        val nameEditText = findViewById<EditText>(R.id.nameEditText)
        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val contactEditText = findViewById<EditText>(R.id.contactEditText)
        val registerButton = findViewById<TextView>(R.id.registerButton) // Assuming Button is styled as TextView

        if (rootLayout == null) Log.e("RegisterationPage", "rootLayout is null")
        if (headerLayout == null) Log.e("RegisterationPage", "headerLayout is null")
        if (logoImageView == null) Log.e("RegisterationPage", "logoImageView is null")
        if (alreadyRegisteredTextView == null) Log.e("RegisterationPage", "alreadyRegisteredTextView is null")

        // Set initial state for animation (white header and orange logo)
        headerLayout?.setBackgroundColor(android.graphics.Color.WHITE)
        logoImageView?.setImageResource(R.drawable.app_logo_orange_right)

        // Create a TranslateAnimation to slide in from the left
        val slideInFromLeft = TranslateAnimation(
            -1000f,  // Start from 1000px to the left
            0f,      // End at its normal position
            0f,      // No vertical movement
            0f
        )
        slideInFromLeft.duration = 1000 // 1 second

        // Create an AlphaAnimation for fading in
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 1000 // 1 second

        // Combine both animations into an AnimationSet
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(slideInFromLeft)
        animationSet.addAnimation(fadeIn)

        // Apply the animation to the root layout
        rootLayout?.startAnimation(animationSet)

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
                    headerLayout?.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()

                // Change logo to white_right after animation
                logoImageView?.setImageResource(R.drawable.app_logo_orange_right)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        // Set click listener for Already Registered text
        alreadyRegisteredTextView?.setOnClickListener {
            // Fade out and slide out to the left
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 2000
            val slideOutToLeft = TranslateAnimation(
                0f, -1000f,  // Slide to the left by 1000px
                0f, 0f
            )
            slideOutToLeft.duration = 2000
            val outAnimationSet = AnimationSet(true)
            outAnimationSet.addAnimation(fadeOut)
            outAnimationSet.addAnimation(slideOutToLeft)

            outAnimationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {
                    // Disable the view during animation to prevent interaction
                    rootLayout.isEnabled = false
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    // Hide the layout to prevent reappearance
                    rootLayout.visibility = View.GONE
                    val intent = Intent(this@RegisterationPage, LogInPage::class.java)
                    startActivity(intent)
                    finish() // Finish after starting the new activity
                }
            })

            rootLayout.startAnimation(outAnimationSet)
        }

        // Set click listener for Register button
        registerButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val contact = contactEditText.text.toString().trim()

            // Validate fields
            if (username.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty() || contact.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate password length (minimum 8 characters)
            if (password.length < 8) {
                Toast.makeText(this, "Password must be at least 8 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Register user with Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        saveUserToRealtimeDatabase(user, username, name, contact)
                        user?.sendEmailVerification()
                        Toast.makeText(
                            this,
                            "Registration successful. Please verify your email before logging in.",
                            Toast.LENGTH_LONG
                        ).show()
                        // Fade out and slide out to the left after successful registration
                        val fadeOut = AlphaAnimation(1f, 0f)
                        fadeOut.duration = 2000
                        val slideOutToLeft = TranslateAnimation(
                            0f, -1000f,  // Slide to the left by 1000px
                            0f, 0f
                        )
                        slideOutToLeft.duration = 2000
                        val outAnimationSet = AnimationSet(true)
                        outAnimationSet.addAnimation(fadeOut)
                        outAnimationSet.addAnimation(slideOutToLeft)

                        outAnimationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                            override fun onAnimationStart(animation: android.view.animation.Animation?) {
                                rootLayout.isEnabled = false
                            }

                            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}

                            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                                rootLayout.visibility = View.GONE
                                val intent = Intent(this@RegisterationPage, EmailVerificationPage::class.java)
                                startActivity(intent)
                                finish()
                            }
                        })

                        rootLayout.startAnimation(outAnimationSet)
                    } else {
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun saveUserToRealtimeDatabase(user: FirebaseUser?, username: String, name: String, contact: String) {
        if (user != null) {
            val userData = hashMapOf(
                "username" to username,
                "name" to name,
                "contact" to contact
            )
            database.reference
                .child("users")
                .child(user.uid)
                .setValue(userData)
                .addOnSuccessListener {
                    Log.d("RegisterationPage", "User data saved to Realtime Database")
                }
                .addOnFailureListener { e ->
                    Log.w("RegisterationPage", "Error saving user data", e)
                }
        }
    }
}