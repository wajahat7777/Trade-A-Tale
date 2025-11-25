package com.smdproject.TradeATale

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MenuPage : AppCompatActivity() {
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoImageView: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var searchIcon: ImageView
    private lateinit var messagesText: TextView
    private lateinit var savedBooksText: TextView
    private lateinit var barterRequestText: TextView
    private lateinit var inventoryText: TextView
    private lateinit var barterHistoryText: TextView
    private lateinit var viewProfileText: TextView
    private lateinit var logoutText: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu_page)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Initialize views
        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoImageView = findViewById(R.id.logoImageView)
        menuIcon = findViewById(R.id.menu_icon)
        searchIcon = findViewById(R.id.search_icon)
        messagesText = findViewById(R.id.messagesText)
        savedBooksText = findViewById(R.id.savedBooksText)
        barterRequestText = findViewById(R.id.barterRequestText)
        inventoryText = findViewById(R.id.inventoryText)
        barterHistoryText = findViewById(R.id.barterHistoryText)
        viewProfileText = findViewById(R.id.viewProfileText)
        logoutText = findViewById(R.id.logoutText)

        // Set initial state for animation (orange header and white logos)
        headerLayout.setBackgroundColor(android.graphics.Color.parseColor("#EA3E23"))
        logoImageView.setImageResource(R.drawable.app_logo_orange_right)
        menuIcon.setImageResource(R.drawable.menu_logo)
        searchIcon.setImageResource(R.drawable.search_logo)

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

        // Change header background to white and logos to orange after animation ends
        animationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                // Fade header background from #EA3E23 to white
                val colorFrom = android.graphics.Color.parseColor("#EA3E23")
                val colorTo = android.graphics.Color.WHITE
                val colorAnimation = ValueAnimator.ofObject(android.animation.ArgbEvaluator(), colorFrom, colorTo)
                colorAnimation.duration = 1000 // 1 second
                colorAnimation.addUpdateListener { animator ->
                    headerLayout.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()

                // Change logos to orange variants after animation
                logoImageView.setImageResource(R.drawable.app_logo_orange_right)
                menuIcon.setImageResource(R.drawable.menu_logo_orange)
                searchIcon.setImageResource(R.drawable.search_logo_orange)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        // Helper function to apply exit animation and navigate
        fun applyExitAnimationAndNavigate(targetActivity: Class<*>, toastMessage: String, clearStack: Boolean = false) {
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
                    val intent = Intent(this@MenuPage, targetActivity)
                    if (clearStack) {
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish() // Finish after starting the new activity
                }
            })

            rootLayout.startAnimation(outAnimationSet)
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        // Set click listeners for navigation with exit animation
        menuIcon.setOnClickListener {
            applyExitAnimationAndNavigate(HomePage::class.java, "Navigating to HomePage")
        }

        searchIcon.setOnClickListener {
            applyExitAnimationAndNavigate(SearchPage::class.java, "Navigating to SearchPage")
        }

        messagesText.setOnClickListener {
            applyExitAnimationAndNavigate(MessagesPage::class.java, "Navigating to MessagesPage")
        }

        savedBooksText.setOnClickListener {
            applyExitAnimationAndNavigate(SavedBooksPage::class.java, "Navigating to SavedBooksPage")
        }

        barterRequestText.setOnClickListener {
            applyExitAnimationAndNavigate(BarterRequestPage::class.java, "Navigating to BarterRequestPage")
        }

        inventoryText.setOnClickListener {
            applyExitAnimationAndNavigate(InventoryPage::class.java, "Navigating to InventoryPage")
        }

        barterHistoryText.setOnClickListener {
            applyExitAnimationAndNavigate(BarterHistoryPage::class.java, "Navigating to BarterHistoryPage")
        }

        viewProfileText.setOnClickListener {
            applyExitAnimationAndNavigate(ProfilePage::class.java, "Navigating to ProfilePage")
        }

        logoutText.setOnClickListener {
            auth.signOut() // Sign out from Firebase
            applyExitAnimationAndNavigate(LogInPage::class.java, "Logged out successfully", clearStack = true)
        }
    }
}