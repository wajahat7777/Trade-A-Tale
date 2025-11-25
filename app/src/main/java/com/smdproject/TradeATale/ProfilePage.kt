package com.smdproject.TradeATale

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.bumptech.glide.Glide

class ProfilePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoImageView: ImageView
    private lateinit var backNavigation: ImageView
    private lateinit var editProfile: ImageView
    private lateinit var bookAdapter: BookAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_page)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase
            .getInstance(FirebaseConfig.REALTIME_DB_URL)
            .reference
        val userId = auth.currentUser?.uid ?: return

        // Initialize views
        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoImageView = findViewById(R.id.logoImageView)
        backNavigation = findViewById(R.id.back_navigation)
        editProfile = findViewById(R.id.editProfile)

        // Set up RecyclerView for books
        val recyclerView = findViewById<RecyclerView>(R.id.bookRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        bookAdapter = BookAdapter()
        recyclerView.adapter = bookAdapter

        // Add snapping to show one book at a time
        val snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(recyclerView)

        // Set initial state for animation (white header and orange logos)
        headerLayout.setBackgroundColor(android.graphics.Color.WHITE)
        logoImageView.setImageResource(R.drawable.app_logo_orange_right)
        backNavigation.setImageResource(R.drawable.back_navigation)
        editProfile.setImageResource(R.drawable.edit_profile_orange)

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

        // Change header background to AppPrimary and logos to white variants after animation ends
        animationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                // Fade header background from white to AppPrimary
                val colorFrom = android.graphics.Color.WHITE
                val colorTo = resources.getColor(R.color.AppPrimary, theme)
                val colorAnimation = ValueAnimator.ofObject(android.animation.ArgbEvaluator(), colorFrom, colorTo)
                colorAnimation.duration = 1000 // 1 second
                colorAnimation.addUpdateListener { animator ->
                    headerLayout.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()

                // Change logos to white variants after animation
                logoImageView.setImageResource(R.drawable.app_logo_orange_right)
                editProfile.setImageResource(R.drawable.edit_profile)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        // Load profile picture, name, and bio
        database.child("users").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profilePublicId = snapshot.child("profilePublicId").getValue(String::class.java)
                val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                val bio = snapshot.child("bio").getValue(String::class.java) ?: ""

                // Load profile picture using Cloudinary URL
                if (profilePublicId?.isNotEmpty() == true) {
                    val profileImageUrl = "https://res.cloudinary.com/ddpt74pga/image/upload/$profilePublicId"
                    Glide.with(this@ProfilePage)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.default_profile_pic)
                        .into(findViewById(R.id.ProfilePic))
                }

                // Load name
                findViewById<TextView>(R.id.Name).text = name

                // Load bio if it exists
                if (bio.isNotEmpty()) {
                    findViewById<TextView>(R.id.Bio).text = bio
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfilePage, "Failed to load profile data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Load user's inventory and fetch book details
        val bookList = mutableListOf<Book>()
        database.child("Inventory").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@ProfilePage, "No books in inventory", Toast.LENGTH_SHORT).show()
                    return
                }

                snapshot.children.forEach { bookSnapshot ->
                    val bookId = bookSnapshot.key ?: return@forEach
                    // Fetch book details from /book/{bookid}
                    database.child("book").child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(bookSnapshot: DataSnapshot) {
                            val image = bookSnapshot.child("image").getValue(String::class.java)
                            val name = bookSnapshot.child("name").getValue(String::class.java)
                            val author = bookSnapshot.child("author").getValue(String::class.java)
                            bookList.add(Book(bookId, image, name, author))
                            bookAdapter.submitBooks(bookList)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            if (error.code == DatabaseError.PERMISSION_DENIED) {
                                Toast.makeText(this@ProfilePage, "Permission denied. Please log in again.", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this@ProfilePage, LogInPage::class.java))
                                finish()
                            } else {
                                Toast.makeText(this@ProfilePage, "Failed to load inventory: ${error.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ProfilePage, "Failed to load inventory: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Helper function to apply exit animation and navigate
        fun applyExitAnimationAndNavigate(targetActivity: Class<*>, toastMessage: String) {
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 2000
            val slideOutToRight = TranslateAnimation(0f, 1000f, 0f, 0f)
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
                    val intent = Intent(this@ProfilePage, targetActivity)
                    startActivity(intent)
                    finish()
                }
            })

            rootLayout.startAnimation(outAnimationSet)
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        // Set click listener for back navigation
        backNavigation.setOnClickListener {
            applyExitAnimationAndNavigate(MenuPage::class.java, "Navigating to MenuPage")
        }

        // Set click listener for edit profile navigation
        editProfile.setOnClickListener {
            applyExitAnimationAndNavigate(EditProfilePage::class.java, "Navigating to EditProfilePage")
        }

        // Inventory button listener (placeholder for future functionality)
        findViewById<Button>(R.id.InventoryButton).setOnClickListener {
            applyExitAnimationAndNavigate(InventoryPage::class.java, "View your inventory")
        }
    }
}