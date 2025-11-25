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
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class ViewBookPage : AppCompatActivity() {
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoImageView: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var searchIcon: ImageView
    private lateinit var bookImage: ImageView
    private lateinit var bookName: TextView
    private lateinit var bookAuthor: TextView
    private lateinit var bookDescription: TextView
    private lateinit var bookCategories: TextView
    private lateinit var barterButton: Button
    private lateinit var contactButton: Button
    private lateinit var saveButton: Button
    private lateinit var bookOwnerPic: ImageView
    private lateinit var bookOwnerName: TextView
    private lateinit var database: DatabaseReference
    private lateinit var dbHelper: DatabaseHelper
    private val client = OkHttpClient()
    private val GOOGLE_API_KEY = "AIzaSyDW-Hweo3zlykmB-PGYtywJOdDVMTjijlk" // Your Google Books API key
    private val TAG = "ViewBookPage"
    private var ownerId: String? = null
    private var ownerDisplayName: String? = null
    private var bookId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_book_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = FirebaseDatabase
            .getInstance(FirebaseConfig.REALTIME_DB_URL)
            .reference
        dbHelper = DatabaseHelper(this)

        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoImageView = findViewById(R.id.logoImageView)
        menuIcon = findViewById(R.id.menu_icon)
        searchIcon = findViewById(R.id.search_icon)
        bookImage = findViewById(R.id.bookImage)
        bookName = findViewById(R.id.bookName)
        bookAuthor = findViewById(R.id.bookAuthor)
        bookDescription = findViewById(R.id.bookDescription)
        bookCategories = findViewById(R.id.bookCategories)
        barterButton = findViewById(R.id.barterButton)
        contactButton = findViewById(R.id.contactButton)
        saveButton = findViewById(R.id.saveButton)
        bookOwnerPic = findViewById(R.id.bookOwnerPic)
        bookOwnerName = findViewById(R.id.bookOwnerName)

        bookId = intent.getStringExtra("book_id") ?: run {
            Toast.makeText(this, "Book ID not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch book details including ownerId
        database.child("book").child(bookId!!).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@ViewBookPage, "Book not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return
                }

                val name = snapshot.child("name").getValue(String::class.java) ?: "Unknown Title"
                val author = snapshot.child("author").getValue(String::class.java) ?: "Unknown Author"
                val description = snapshot.child("description").getValue(String::class.java) ?: "No description available"
                val categories = snapshot.child("categories").children.mapNotNull { it.getValue(String::class.java) }.joinToString(", ")
                ownerId = snapshot.child("ownerId").getValue(String::class.java)
                val image = snapshot.child("image").getValue(String::class.java)

                // Log the book being viewed
                Log.d(TAG, "=== Book Currently in View ===")
                Log.d(TAG, "Book ID: $bookId")
                Log.d(TAG, "Name: $name")
                Log.d(TAG, "Author: $author")
                Log.d(TAG, "Description: $description")
                Log.d(TAG, "Categories: ${if (categories.isNotEmpty()) categories else "No categories"}")
                Log.d(TAG, "Image URL: ${image ?: "Not provided"}")
                Log.d(TAG, "Owner ID: ${ownerId ?: "Not specified"}")
                Log.d(TAG, "=============================")

                // Populate book details
                bookName.text = name
                bookAuthor.text = author
                bookDescription.text = description
                bookCategories.text = if (categories.isNotEmpty()) categories else "No categories"

                loadBookCover(image, name)

                // Fetch owner details if ownerId exists
                if (ownerId != null) {
                    Log.d(TAG, "Fetching owner details for ownerId: $ownerId")
                    database.child("users").child(ownerId!!).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            if (userSnapshot.exists()) {
                                val ownerName = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown User"
                                ownerDisplayName = ownerName
                                val ownerPicUrl = userSnapshot.child("profilePublicId").getValue(String::class.java)
                                bookOwnerName.text = ownerName
                                val profileImageUrl = if (ownerPicUrl != null) "https://res.cloudinary.com/ddpt74pga/image/upload/$ownerPicUrl" else null
                                if (profileImageUrl != null) {
                                    Glide.with(this@ViewBookPage)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.default_profile_pic)
                                        .error(R.drawable.default_profile_pic)
                                        .into(bookOwnerPic)
                                } else {
                                    bookOwnerPic.setImageResource(R.drawable.default_profile_pic)
                                }
                            } else {
                                Log.w(TAG, "User data not found for ownerId: $ownerId")
                                bookOwnerName.text = "Unknown User"
                                bookOwnerPic.setImageResource(R.drawable.default_profile_pic)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(TAG, "Permission denied or error loading owner data for ownerId: $ownerId - ${error.message}")
                            bookOwnerName.text = "Unknown User"
                            bookOwnerPic.setImageResource(R.drawable.default_profile_pic)
                        }
                    })
                } else {
                    bookOwnerName.text = "Unknown User"
                    bookOwnerPic.setImageResource(R.drawable.default_profile_pic)
                }

                // Save button functionality using SavedBook
                saveButton.setOnClickListener {
                    if (dbHelper.bookExists(bookId!!)) {
                        Toast.makeText(this@ViewBookPage, "Book already saved", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Save aborted: Book with ID $bookId already exists in local DB")
                    } else {
                        val savedBook = SavedBook(
                            bookId = bookId!!,
                            name = name,
                            author = author,
                            description = description,
                            categories = categories,
                            image = image,
                            ownerId = ownerId ?: ""
                        )

                        // Log the book about to be saved
                        Log.d(TAG, "=== Preparing to Save Book to Local DB ===")
                        Log.d(TAG, "Book ID: ${savedBook.bookId}")
                        Log.d(TAG, "Name: ${savedBook.name}")
                        Log.d(TAG, "Author: ${savedBook.author}")
                        Log.d(TAG, "Description: ${savedBook.description}")
                        Log.d(TAG, "Categories: ${savedBook.categories}")
                        Log.d(TAG, "Image URL: ${savedBook.image ?: "Not provided"}")
                        Log.d(TAG, "Owner ID: ${savedBook.ownerId}")
                        Log.d(TAG, "=========================================")

                        // Save the book
                        dbHelper.saveBook(savedBook)

                        // Log confirmation of what was saved
                        Log.d(TAG, "=== Book Saved to Local DB ===")
                        Log.d(TAG, "Book ID: ${savedBook.bookId}")
                        Log.d(TAG, "Name: ${savedBook.name}")
                        Log.d(TAG, "Author: ${savedBook.author}")
                        Log.d(TAG, "Description: ${savedBook.description}")
                        Log.d(TAG, "Categories: ${savedBook.categories}")
                        Log.d(TAG, "Image (Title): ${savedBook.image}")
                        Log.d(TAG, "Owner ID: ${savedBook.ownerId}")
                        Log.d(TAG, "=============================")

                        Toast.makeText(this@ViewBookPage, "Book saved locally", Toast.LENGTH_SHORT).show()

                        // Query and log all books in the local DB
                        val allBooks = dbHelper.getAllSavedBooks()
                        Log.d(TAG, "=== All Books in Local DB ===")
                        if (allBooks.isEmpty()) {
                            Log.d(TAG, "No books found in local DB")
                        } else {
                            allBooks.forEachIndexed { index, book ->
                                Log.d(TAG, "Book ${index + 1}:")
                                Log.d(TAG, "  Book ID: ${book.bookId}")
                                Log.d(TAG, "  Name: ${book.name}")
                                Log.d(TAG, "  Author: ${book.author}")
                                Log.d(TAG, "  Description: ${book.description}")
                                Log.d(TAG, "  Categories: ${book.categories}")
                                Log.d(TAG, "  Image URL: ${book.image}")
                                Log.d(TAG, "  Owner ID: ${book.ownerId}")
                                Log.d(TAG, "-----------------------------")
                            }
                        }
                        Log.d(TAG, "============================")
                    }
                }

                // Barter button functionality
                barterButton.setOnClickListener {
                    showInventoryDialog()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ViewBookPage, "Failed to load book: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Firebase error: ${error.message}")
            }
        })

        // Set up UI animations
        headerLayout.setBackgroundColor(android.graphics.Color.WHITE)
        logoImageView.setImageResource(R.drawable.app_logo_orange_right)
        menuIcon.setImageResource(R.drawable.menu_logo)
        searchIcon.setImageResource(R.drawable.search_logo)

        val slideInFromRight = TranslateAnimation(1000f, 0f, 0f, 0f)
        slideInFromRight.duration = 1000
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 1000
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(slideInFromRight)
        animationSet.addAnimation(fadeIn)
        rootLayout.startAnimation(animationSet)

        animationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                val colorFrom = android.graphics.Color.WHITE
                val colorTo = resources.getColor(R.color.AppPrimary, theme)
                val colorAnimation = ValueAnimator.ofObject(android.animation.ArgbEvaluator(), colorFrom, colorTo)
                colorAnimation.duration = 1000
                colorAnimation.addUpdateListener { animator ->
                    headerLayout.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()
                logoImageView.setImageResource(R.drawable.app_logo_orange_right)
                menuIcon.setImageResource(R.drawable.menu_logo)
                searchIcon.setImageResource(R.drawable.search_logo)
            }
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        // Navigation setup
        fun applyExitAnimationAndNavigate(
            targetActivity: Class<*>,
            toastMessage: String,
            extras: Bundle? = null
        ) {
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 2000
            val slideOutToRight = TranslateAnimation(0f, 1000f, 0f, 0f)
            slideOutToRight.duration = 2000
            val outAnimationSet = AnimationSet(true)
            outAnimationSet.addAnimation(fadeOut)
            outAnimationSet.addAnimation(slideOutToRight)
            outAnimationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) { rootLayout.isEnabled = false }
                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    rootLayout.visibility = View.GONE
                    val intent = Intent(this@ViewBookPage, targetActivity)
                    extras?.let { intent.putExtras(it) }
                    startActivity(intent)
                    finish()
                }
            })
            rootLayout.startAnimation(outAnimationSet)
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        menuIcon.setOnClickListener { applyExitAnimationAndNavigate(MenuPage::class.java, "Navigating to MenuPage") }
        searchIcon.setOnClickListener { applyExitAnimationAndNavigate(SearchPage::class.java, "Navigating to SearchPage") }
        barterButton.setOnClickListener { applyExitAnimationAndNavigate(InventoryPage::class.java, "Navigating to InventoryPage") }
        contactButton.setOnClickListener {
            val targetId = ownerId
            if (targetId.isNullOrEmpty()) {
                Toast.makeText(this, "Owner information unavailable", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val extras = Bundle().apply {
                putString("chat_user_id", targetId)
                putString("chat_user_name", ownerDisplayName ?: "Book owner")
            }
            applyExitAnimationAndNavigate(ChatPage::class.java, "Opening chat", extras)
        }
    }

    private fun showInventoryDialog() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Create dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Select a Book to Barter")
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .create()

        // Create RecyclerView for the dialog
        val recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ViewBookPage)
        }

        // Fetch user's inventory (book IDs)
        database.child("Inventory").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val inventoryBooks = mutableListOf<InventoryBook>()
                val bookIds = snapshot.children.mapNotNull { it.key }

                if (bookIds.isEmpty()) {
                    Toast.makeText(this@ViewBookPage, "Your inventory is empty", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    return
                }

                // Fetch full book details for each book ID from the book table
                var booksFetched = 0
                for (bookId in bookIds) {
                    database.child("book").child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(bookSnapshot: DataSnapshot) {
                            if (bookSnapshot.exists()) {
                                val image = bookSnapshot.child("image").getValue(String::class.java)
                                val name = bookSnapshot.child("name").getValue(String::class.java)
                                val author = bookSnapshot.child("author").getValue(String::class.java)
                                val description = bookSnapshot.child("description").getValue(String::class.java)
                                val categories = bookSnapshot.child("categories").children.mapNotNull { it.getValue(String::class.java) }
                                inventoryBooks.add(InventoryBook(bookId, image, name, author, description, categories))
                            }

                            booksFetched++
                            // When all books are fetched, set up the adapter and show the dialog
                            if (booksFetched == bookIds.size) {
                                val adapter = InventoryBookAdapter(inventoryBooks) {
                                    // Do nothing on delete in this context
                                }
                                adapter.setOnItemClickListener { selectedBook ->
                                    Log.d(TAG, "Book clicked: ${selectedBook.name}, ID: ${selectedBook.bookId}")
                                    createBarterRequest(selectedBook.bookId)
                                    dialog.dismiss()
                                }
                                recyclerView.adapter = adapter
                                dialog.setView(recyclerView)
                                dialog.show()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            booksFetched++
                            Toast.makeText(this@ViewBookPage, "Failed to load book details: ${error.message}", Toast.LENGTH_SHORT).show()
                            if (booksFetched == bookIds.size && inventoryBooks.isEmpty()) {
                                dialog.dismiss()
                            }
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ViewBookPage, "Failed to load inventory: ${error.message}", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        })
    }

    private fun createBarterRequest(selectedBookId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }
        if (ownerId == null || bookId == null) {
            Toast.makeText(this, "Cannot create barter request: Missing owner or book ID", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d(TAG, "Creating barter request - yourID: $userId, BookID: $selectedBookId, OwnersBookID: $bookId, OwnerID: $ownerId")
        val barterRequest = mapOf(
            "yourID" to userId,
            "BookID" to selectedBookId,
            "OwnersBookID" to bookId
        )

        database.child("BarterRequest").child(ownerId!!).push()
            .setValue(barterRequest)
            .addOnSuccessListener {
                Toast.makeText(this, "Barter request sent successfully", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Barter request sent successfully for ownerId: $ownerId")
            }
            .addOnFailureListener { error ->
                Toast.makeText(this, "Failed to send barter request: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Failed to send barter request: ${error.message}")
            }
    }

    private fun loadBookCover(storedImage: String?, fallbackTitle: String) {
        val imageUrl = storedImage?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        if (imageUrl != null) {
            Log.d(TAG, "Loading stored cover image: $imageUrl")
            Glide.with(this@ViewBookPage)
                .asBitmap()
                .load(imageUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.default_book_cover)
                        .error(R.drawable.default_book_cover)
                )
                .transition(BitmapTransitionOptions.withCrossFade())
                .into(bookImage)
        } else {
            fetchBookCoverFromGoogleBooks(fallbackTitle) { remoteUrl ->
                runOnUiThread {
                    if (remoteUrl != null) {
                        Log.d(TAG, "Loading fallback Google cover: $remoteUrl")
                        Glide.with(this@ViewBookPage)
                            .asBitmap()
                            .load(remoteUrl)
                            .apply(
                                RequestOptions()
                                    .placeholder(R.drawable.default_book_cover)
                                    .error(R.drawable.default_book_cover)
                            )
                            .transition(BitmapTransitionOptions.withCrossFade())
                            .into(bookImage)
                    } else {
                        Log.w(TAG, "No image URL found for: $fallbackTitle")
                        bookImage.setImageResource(R.drawable.default_book_cover)
                    }
                }
            }
        }
    }

    private fun fetchBookCoverFromGoogleBooks(title: String, callback: (String?) -> Unit) {
        val encodedTitle = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
        val url = "https://www.googleapis.com/books/v1/volumes?q=intitle:$encodedTitle&key=$GOOGLE_API_KEY"
        Log.d(TAG, "Fetching book cover from: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "API call failed: ${e.message}")
                runOnUiThread { callback(null) }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        Log.d(TAG, "API response: $responseBody")
                        if (responseBody.isNullOrEmpty()) {
                            Log.w(TAG, "Empty response body")
                            runOnUiThread { callback(null) }
                            return
                        }

                        val json = JSONObject(responseBody)
                        val items = json.optJSONArray("items")
                        if (items == null || items.length() == 0) {
                            Log.w(TAG, "No items found for title: $title")
                            runOnUiThread { callback(null) }
                            return
                        }

                        val firstItem = items.getJSONObject(0)
                        val volumeInfo = firstItem.optJSONObject("volumeInfo")
                        val imageLinks = volumeInfo?.optJSONObject("imageLinks")
                        val thumbnail = imageLinks?.optString("thumbnail")?.replace("http://", "https://")
                        Log.d(TAG, "Thumbnail URL: $thumbnail")
                        runOnUiThread { callback(thumbnail) }
                    } else {
                        Log.e(TAG, "Unsuccessful response: ${response.code} - ${response.body?.string()}")
                        runOnUiThread { callback(null) }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing response: ${e.message}")
                    runOnUiThread { callback(null) }
                } finally {
                    response.close()
                }
            }
        })
    }
}