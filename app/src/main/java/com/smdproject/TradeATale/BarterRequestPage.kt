package com.smdproject.TradeATale

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BarterRequestPage : AppCompatActivity() {
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoImageView: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var searchIcon: ImageView
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var requestsRecyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private val requestList = mutableListOf<BarterRequest>()
    private val TAG = "BarterRequestPage"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_barter_request_page)

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
        
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "User not authenticated. Please log in.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LogInPage::class.java))
            finish()
            return
        }

        // Initialize views
        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoImageView = findViewById(R.id.logoImageView)
        menuIcon = findViewById(R.id.menu_icon)
        searchIcon = findViewById(R.id.search_icon)
        requestsRecyclerView = findViewById(R.id.requestsRecyclerView)
        emptyTextView = findViewById(R.id.emptyTextView)
        
        // Setup RecyclerView
        requestsRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = BarterRequestAdapter(requestList) { request ->
            // Handle accept/reject actions
            handleBarterRequest(request)
        }
        requestsRecyclerView.adapter = adapter

        // Set initial state for animation (white header and orange logo)
        headerLayout.setBackgroundColor(android.graphics.Color.WHITE)
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

        // Change header background to AppPrimary and logo to white_right after animation ends
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

                // Change logo and icons to white variants after animation
                logoImageView.setImageResource(R.drawable.app_logo_orange_right)
                menuIcon.setImageResource(R.drawable.menu_logo)
                searchIcon.setImageResource(R.drawable.search_logo)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
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
                    val intent = Intent(this@BarterRequestPage, targetActivity)
                    startActivity(intent)
                    finish()
                }
            })

            rootLayout.startAnimation(outAnimationSet)
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        // Set click listeners for navigation
        menuIcon.setOnClickListener {
            applyExitAnimationAndNavigate(MenuPage::class.java, "Navigating to MenuPage")
        }

        searchIcon.setOnClickListener {
            applyExitAnimationAndNavigate(SearchPage::class.java, "Navigating to SearchPage")
        }
        
        // Logo click to navigate to HomePage
        logoImageView.setOnClickListener {
            applyExitAnimationAndNavigate(HomePage::class.java, "Navigating to HomePage")
        }
        
        // Load barter requests
        loadBarterRequests(userId)
    }
    
    private fun loadBarterRequests(userId: String) {
        requestList.clear()
        Log.d(TAG, "Loading barter requests for user: $userId")
        
        database.child("BarterRequest").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                        emptyTextView.visibility = View.VISIBLE
                        requestsRecyclerView.visibility = View.GONE
                        Log.d(TAG, "No barter requests found")
                        return
                    }
                    
                    emptyTextView.visibility = View.GONE
                    requestsRecyclerView.visibility = View.VISIBLE
                    
                    val totalRequests = snapshot.childrenCount.toInt()
                    var requestsProcessed = 0
                    
                    snapshot.children.forEach { requestSnapshot ->
                        val requestId = requestSnapshot.key ?: return@forEach
                        val yourID = requestSnapshot.child("yourID").getValue(String::class.java)
                        val bookID = requestSnapshot.child("BookID").getValue(String::class.java)
                        val ownersBookID = requestSnapshot.child("OwnersBookID").getValue(String::class.java)
                        
                        if (yourID != null && bookID != null && ownersBookID != null) {
                            // Fetch requester info and both books
                            fetchRequestDetails(requestId, yourID, bookID, ownersBookID) {
                                requestsProcessed++
                                if (requestsProcessed == totalRequests) {
                                    (requestsRecyclerView.adapter as? BarterRequestAdapter)?.notifyDataSetChanged()
                                }
                            }
                        } else {
                            requestsProcessed++
                        }
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to load barter requests: ${error.message}")
                    Toast.makeText(this@BarterRequestPage, "Failed to load requests: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
    
    private fun fetchRequestDetails(
        requestId: String,
        requesterId: String,
        requesterBookId: String,
        yourBookId: String,
        onComplete: () -> Unit
    ) {
        // Fetch requester name
        database.child("users").child(requesterId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(userSnapshot: DataSnapshot) {
                    val requesterName = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown User"
                    
                    // Fetch requester's book
                    database.child("book").child(requesterBookId)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(requesterBookSnapshot: DataSnapshot) {
                                val requesterBookName = requesterBookSnapshot.child("name").getValue(String::class.java) ?: "Unknown Book"
                                val requesterBookImage = requesterBookSnapshot.child("image").getValue(String::class.java)
                                
                                // Fetch your book
                                database.child("book").child(yourBookId)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(yourBookSnapshot: DataSnapshot) {
                                            val yourBookName = yourBookSnapshot.child("name").getValue(String::class.java) ?: "Unknown Book"
                                            val yourBookImage = yourBookSnapshot.child("image").getValue(String::class.java)
                                            
                                            val request = BarterRequest(
                                                requestId = requestId,
                                                requesterId = requesterId,
                                                requesterName = requesterName,
                                                requesterBookId = requesterBookId,
                                                requesterBookName = requesterBookName,
                                                requesterBookImage = requesterBookImage,
                                                yourBookId = yourBookId,
                                                yourBookName = yourBookName,
                                                yourBookImage = yourBookImage
                                            )
                                            
                                            requestList.add(request)
                                            onComplete()
                                        }
                                        
                                        override fun onCancelled(error: DatabaseError) {
                                            Log.e(TAG, "Failed to fetch your book: ${error.message}")
                                            onComplete()
                                        }
                                    })
                            }
                            
                            override fun onCancelled(error: DatabaseError) {
                                Log.e(TAG, "Failed to fetch requester book: ${error.message}")
                                onComplete()
                            }
                        })
                }
                
                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Failed to fetch requester info: ${error.message}")
                    onComplete()
                }
            })
    }
    
    private fun handleBarterRequest(request: BarterRequest) {
        // This will be called when user accepts/rejects
        // For now, just show a toast
        Toast.makeText(this, "Barter request from ${request.requesterName}", Toast.LENGTH_SHORT).show()
    }
}

data class BarterRequest(
    val requestId: String,
    val requesterId: String,
    val requesterName: String,
    val requesterBookId: String,
    val requesterBookName: String,
    val requesterBookImage: String?,
    val yourBookId: String,
    val yourBookName: String,
    val yourBookImage: String?
)