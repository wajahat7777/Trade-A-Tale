package com.smdproject.TradeATale

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.FirebaseDatabase

class SearchPage : AppCompatActivity() {
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoImageView: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var booksRecyclerView: RecyclerView
    private lateinit var bookAdapter: BookAdapter
    private lateinit var database: com.google.firebase.database.DatabaseReference
    private val TAG = "SearchPage"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search_page)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        database = FirebaseDatabase
            .getInstance(FirebaseConfig.REALTIME_DB_URL)
            .reference

        // Initialize views
        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoImageView = findViewById(R.id.logoImageView)
        menuIcon = findViewById(R.id.menu_icon)
        searchInput = findViewById(R.id.Search)
        searchButton = findViewById(R.id.SearchLogo)
        booksRecyclerView = findViewById(R.id.booksRecyclerView)

        // Set up RecyclerView
        bookAdapter = BookAdapter()
        booksRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        booksRecyclerView.adapter = bookAdapter

        // Set initial state for animation (white header and orange logo)
        headerLayout.setBackgroundColor(android.graphics.Color.WHITE)
        logoImageView.setImageResource(R.drawable.app_logo_orange_right)
        menuIcon.setImageResource(R.drawable.menu_logo)

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
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        // Helper function to apply exit animation and navigate
        fun applyExitAnimationAndNavigate(targetActivity: Class<*>, toastMessage: String, book: Book? = null) {
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
                        Intent(this@SearchPage, ViewBookPage::class.java).apply {
                            putExtra("book_title", book.name)
                            putExtra("book_author", book.author)
                            putExtra("book_id", book.bookId)
                        }
                    } else {
                        Intent(this@SearchPage, targetActivity)
                    }
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

        // Set click listener for search button
        searchButton.setOnClickListener {
            val title = searchInput.text.toString().trim()
            if (title.isNotEmpty()) {
                searchBooksByName(title)
            } else {
                Toast.makeText(this, "Please enter a book title", Toast.LENGTH_SHORT).show()
            }
        }

        // Add click listener to book items
        bookAdapter.setOnItemClickListener { book ->
            applyExitAnimationAndNavigate(ViewBookPage::class.java, "Navigating to ViewBookPage", book)
        }
    }

    private fun searchBooksByName(title: String) {
        val bookList = mutableListOf<Book>()
        database.child("book").orderByChild("name").startAt(title).endAt(title + "\uf8ff")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(this@SearchPage, "No books found", Toast.LENGTH_SHORT).show()
                        bookAdapter.submitBooks(emptyList())
                        return
                    }

                    snapshot.children.forEach { bookSnapshot ->
                        val bookId = bookSnapshot.key ?: return@forEach
                        val image = bookSnapshot.child("image").getValue(String::class.java)
                        val name = bookSnapshot.child("name").getValue(String::class.java)
                        val author = bookSnapshot.child("author").getValue(String::class.java)
                        bookList.add(Book(bookId, image, name, author))
                    }

                    bookAdapter.submitBooks(bookList)
                    if (bookList.isEmpty()) {
                        Toast.makeText(this@SearchPage, "No books found matching '$title'", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Search failed: ${error.message}")
                    Toast.makeText(this@SearchPage, "Search failed: ${error.message}", Toast.LENGTH_SHORT).show()
                    bookAdapter.submitBooks(emptyList())
                }
            })
    }
}