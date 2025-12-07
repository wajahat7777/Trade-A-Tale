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
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class HomePage : AppCompatActivity() {
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoImageView: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var searchIcon: ImageView
    private lateinit var categoriesRecyclerView: RecyclerView
    private lateinit var sectionedAdapter: SectionedHomeAdapter
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home_page)

        // Initialize OneSignal (in case user was already logged in and skipped WelcomePage)
        OneSignalManager.initialize(this)
        // Also try to save Player ID after a delay (in case user is logged in)
        OneSignalManager.savePlayerIdAfterLogin()

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase with explicit RTDB instance to ensure we hit the TradeATale backend
        database = FirebaseDatabase
            .getInstance(FirebaseConfig.REALTIME_DB_URL)
            .reference

        // Initialize views
        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoImageView = findViewById(R.id.logoImageView)
        menuIcon = findViewById(R.id.menu_icon)
        searchIcon = findViewById(R.id.search_icon)
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView)

        // Set up RecyclerView for categories
        categoriesRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        sectionedAdapter = SectionedHomeAdapter(emptyMap()) { book ->
            applyExitAnimationAndNavigate(ViewBookPage::class.java, "Navigating to ViewBookPage", book)
        }
        categoriesRecyclerView.adapter = sectionedAdapter

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

                // Change logo to white_right after animation
                logoImageView.setImageResource(R.drawable.app_logo_orange_right)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        // Load all books grouped by categories
        loadAllBooksByCategories()

        // Set click listeners for navigation
        menuIcon.setOnClickListener {
            applyExitAnimationAndNavigate(MenuPage::class.java, "Navigating to MenuPage")
        }

        searchIcon.setOnClickListener {
            applyExitAnimationAndNavigate(SearchPage::class.java, "Navigating to SearchPage")
        }

        // Logo click to navigate to HomePage (refresh)
        logoImageView.setOnClickListener {
            // Already on HomePage, just refresh
            loadAllBooksByCategories()
        }
    }

    private fun applyExitAnimationAndNavigate(targetActivity: Class<*>, toastMessage: String, book: Book? = null) {
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
                val intent = if (book != null) {
                    Intent(this@HomePage, ViewBookPage::class.java).apply {
                        putExtra("book_title", book.name)
                        putExtra("book_author", book.author)
                        putExtra("book_id", book.bookId)
                    }
                } else {
                    Intent(this@HomePage, targetActivity)
                }
                startActivity(intent)
                finish()
            }
        })

        rootLayout.startAnimation(outAnimationSet)
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
    }

    private fun loadAllBooksByCategories() {
        database.child("book").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    sectionedAdapter = SectionedHomeAdapter(emptyMap()) { book ->
                        applyExitAnimationAndNavigate(ViewBookPage::class.java, "Navigating to ViewBookPage", book)
                    }
                    categoriesRecyclerView.adapter = sectionedAdapter
                    return
                }

                val categoryBooksMap = mutableMapOf<String, MutableList<Book>>()

                snapshot.children.forEach { bookSnapshot ->
                    val bookId = bookSnapshot.key ?: return@forEach
                    val image = bookSnapshot.child("image").getValue(String::class.java)
                    val name = bookSnapshot.child("name").getValue(String::class.java)
                    val author = bookSnapshot.child("author").getValue(String::class.java)
                    val categories = bookSnapshot.child("categories").children.mapNotNull { it.getValue(String::class.java) }

                    val book = Book(bookId, image, name, author)
                    
                    // Add book to each of its categories
                    categories.forEach { category ->
                        categoryBooksMap.getOrPut(category) { mutableListOf() }.add(book)
                    }
                }

                // Update adapter with grouped books
                sectionedAdapter = SectionedHomeAdapter(categoryBooksMap) { book ->
                    applyExitAnimationAndNavigate(ViewBookPage::class.java, "Navigating to ViewBookPage", book)
                }
                categoriesRecyclerView.adapter = sectionedAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomePage, "Failed to load books: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}