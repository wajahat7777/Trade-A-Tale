package com.smdproject.TradeATale

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.*
import android.widget.AdapterView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID

class InventoryPage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoImageView: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var searchIcon: ImageView
    private lateinit var addBookButton: RelativeLayout
    private lateinit var bookAdapter: SectionedInventoryAdapter
    private val bookList = mutableListOf<InventoryBook>() // Ensure this is the list used everywhere
    private val httpClient = OkHttpClient()
    private val CLOUD_NAME = "ddpt74pga"
    private val BOOK_UPLOAD_PRESET = "ShelfShare"
    private val BOOK_IMAGE_FOLDER = "TradeATale/books"

    private var pendingImagePreview: ImageView? = null
    private var pendingImageStatus: TextView? = null
    private var pendingImageProgress: ProgressBar? = null
    private var pendingImageResult: ((String?) -> Unit)? = null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadImageToCloudinary(uri)
        } else {
            pendingImageStatus?.text = "Image selection cancelled"
            pendingImageResult?.invoke(null)
            clearImageTargets()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inventory_page)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoImageView = findViewById(R.id.logoImageView)
        menuIcon = findViewById(R.id.menu_icon)
        searchIcon = findViewById(R.id.search_icon)
        addBookButton = findViewById(R.id.addBook)

        val recyclerView = findViewById<RecyclerView>(R.id.bookRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        bookAdapter = SectionedInventoryAdapter(mutableListOf()) { loadInventory(userId) }
        recyclerView.adapter = bookAdapter // Ensure adapter is attached

        headerLayout.setBackgroundColor(android.graphics.Color.WHITE)
        logoImageView.setImageResource(R.drawable.app_logo_white_right)
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

        loadInventory(userId)

        addBookButton.setOnClickListener { showAddBookDialog(userId) }

        fun applyExitAnimationAndNavigate(targetActivity: Class<*>, toastMessage: String) {
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
                    startActivity(Intent(this@InventoryPage, targetActivity))
                    finish()
                }
            })
            rootLayout.startAnimation(outAnimationSet)
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        menuIcon.setOnClickListener { applyExitAnimationAndNavigate(MenuPage::class.java, "Navigating to MenuPage") }
        searchIcon.setOnClickListener { applyExitAnimationAndNavigate(SearchPage::class.java, "Navigating to SearchPage") }
        
        // Logo click to navigate to HomePage
        logoImageView.setOnClickListener {
            applyExitAnimationAndNavigate(HomePage::class.java, "Navigating to HomePage")
        }
    }

    private fun loadInventory(userId: String) {
        bookList.clear() // Ensure list is cleared before loading
        Log.d("InventoryPage", "Fetching inventory for user: $userId")
        database.child("Inventory").child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.d("InventoryPage", "No books found in inventory for user: $userId")
                    bookAdapter.submitBooks(bookList)
                    return
                }
                Log.d("InventoryPage", "Found ${snapshot.childrenCount} book IDs in inventory")
                val totalBooks = snapshot.childrenCount.toInt()
                var booksProcessed = 0

                snapshot.children.forEach { bookSnapshot ->
                    val bookId = bookSnapshot.key ?: return@forEach
                    Log.d("InventoryPage", "Processing bookId: $bookId")
                    database.child("book").child(bookId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(bookSnapshot: DataSnapshot) {
                            if (bookSnapshot.exists()) {
                                val image = bookSnapshot.child("image").getValue(String::class.java)
                                val name = bookSnapshot.child("name").getValue(String::class.java)
                                val author = bookSnapshot.child("author").getValue(String::class.java)
                                val description = bookSnapshot.child("description").getValue(String::class.java)
                                val categories = bookSnapshot.child("categories").children.mapNotNull { it.getValue(String::class.java) }
                                val book = InventoryBook(bookId, image, name, author, description, categories)
                                bookList.add(book) // Add to the same list instance
                                Log.d("InventoryPage", "Added book to list: $name")
                            } else {
                                Log.w("InventoryPage", "Book not found in /book for bookId: $bookId")
                            }
                            booksProcessed++
                            if (booksProcessed == totalBooks) {
                                Log.d("InventoryPage", "All books processed, updating adapter with ${bookList.size} books")
                                bookAdapter.submitBooks(bookList.toList()) // Use toList() to pass a copy
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
                            Log.e("InventoryPage", "Failed to load book $bookId: ${error.message}")
                            booksProcessed++
                            if (booksProcessed == totalBooks) {
                                Log.d("InventoryPage", "All books processed (with errors), updating adapter with ${bookList.size} books")
                                bookAdapter.submitBooks(bookList.toList())
                            }
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("InventoryPage", "Failed to load inventory: ${error.message}")
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    startActivity(Intent(this@InventoryPage, LogInPage::class.java))
                    finish()
                }
            }
        })
    }

    private fun showAddBookDialog(userId: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_book, null)
        val bookNameInput = dialogView.findViewById<EditText>(R.id.bookNameInput)
        val bookAuthorInput = dialogView.findViewById<EditText>(R.id.bookAuthorInput)
        val bookDescriptionInput = dialogView.findViewById<EditText>(R.id.bookDescriptionInput)
        val bookCategorySpinner = dialogView.findViewById<Spinner>(R.id.bookCategorySpinner)
        val bookImagePreview = dialogView.findViewById<ImageView>(R.id.bookImagePreview)
        val chooseImageButton = dialogView.findViewById<Button>(R.id.chooseImageButton)
        val imageStatusText = dialogView.findViewById<TextView>(R.id.imageStatusText)
        val imageUploadProgress = dialogView.findViewById<ProgressBar>(R.id.imageUploadProgress)
        val saveButton = dialogView.findViewById<Button>(R.id.saveButton)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancelButton)
        var uploadedImageUrl: String? = null

        // Define categories
        val categories = listOf(
            "Fiction", "Non-Fiction", "Mystery", "Romance", "Science Fiction",
            "Fantasy", "Horror", "Thriller", "Biography", "History",
            "Self-Help", "Business", "Education", "Children's", "Young Adult",
            "Poetry", "Drama", "Comedy", "Adventure", "Classic"
        )

        // Setup spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        bookCategorySpinner.adapter = adapter

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Add New Book")
            .setView(dialogView)
            .create()

        chooseImageButton.setOnClickListener {
            launchImagePickerForDialog(
                preview = bookImagePreview,
                status = imageStatusText,
                progress = imageUploadProgress
            ) { url ->
                uploadedImageUrl = url
            }
        }

        dialog.setOnDismissListener {
            clearImageTargets()
        }

        saveButton.setOnClickListener {
            val name = bookNameInput.text.toString().trim()
            val author = bookAuthorInput.text.toString().trim()
            val description = bookDescriptionInput.text.toString().trim()
            val selectedCategory = bookCategorySpinner.selectedItem?.toString() ?: ""

            if (name.isEmpty()) {
                bookNameInput.error = "Book name is required"
                return@setOnClickListener
            }

            if (selectedCategory.isEmpty()) {
                Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val categories = listOf(selectedCategory)

            val imageUrl = uploadedImageUrl
            if (imageUrl.isNullOrEmpty()) {
                Toast.makeText(this, "Please upload a cover photo", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val bookId = database.child("book").push().key ?: return@setOnClickListener
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            val bookData = mapOf(
                "image" to imageUrl,
                "name" to name,
                "author" to (if (author.isNotEmpty()) author else null),
                "description" to (if (description.isNotEmpty()) description else null),
                "categories" to categories,
                "ownerId" to userId // Add ownerId to the book node
            )

            database.child("book").child(bookId).setValue(bookData)
                .addOnSuccessListener {
                    database.child("Inventory").child(userId).child(bookId).setValue(true)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Book added successfully", Toast.LENGTH_SHORT).show()
                            loadInventory(userId)
                            dialog.dismiss()
                        }
                        .addOnFailureListener { error ->
                            Log.e("InventoryPage", "Failed to add book to inventory: ${error.message}")
                            Toast.makeText(this, "Failed to add book to inventory: ${error.message}", Toast.LENGTH_SHORT).show()
                        }
                }
                .addOnFailureListener { error ->
                    Log.e("InventoryPage", "Failed to add book: ${error.message}")
                    Toast.makeText(this, "Failed to add book: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun launchImagePickerForDialog(
        preview: ImageView,
        status: TextView,
        progress: ProgressBar,
        onResult: (String?) -> Unit
    ) {
        pendingImagePreview = preview
        pendingImageStatus = status
        pendingImageProgress = progress
        pendingImageResult = onResult
        status.text = "Opening gallery..."
        imagePickerLauncher.launch("image/*")
    }

    private fun uploadImageToCloudinary(uri: Uri) {
        val preview = pendingImagePreview
        val status = pendingImageStatus
        val progress = pendingImageProgress

        if (preview == null || status == null || progress == null) {
            return
        }

        progress.visibility = View.VISIBLE
        status.text = "Uploading cover..."

        try {
            @Suppress("DEPRECATION")
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val imageBytes = outputStream.toByteArray()

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "book_cover.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .addFormDataPart("upload_preset", BOOK_UPLOAD_PRESET)
                .addFormDataPart("folder", BOOK_IMAGE_FOLDER)
                .addFormDataPart("public_id", "book_${UUID.randomUUID()}")
                .build()

            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
                .post(requestBody)
                .build()

            httpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        progress.visibility = View.GONE
                        status.text = "Upload failed"
                        Toast.makeText(this@InventoryPage, "Image upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        pendingImageResult?.invoke(null)
                        clearImageTargets()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val bodyString = it.body?.string()
                        if (!it.isSuccessful || bodyString.isNullOrEmpty()) {
                            runOnUiThread {
                                progress.visibility = View.GONE
                                status.text = "Upload failed"
                                Toast.makeText(this@InventoryPage, "Image upload failed", Toast.LENGTH_SHORT).show()
                                pendingImageResult?.invoke(null)
                                clearImageTargets()
                            }
                            return
                        }

                        val secureUrl = JSONObject(bodyString).optString("secure_url")
                        runOnUiThread {
                            progress.visibility = View.GONE
                            if (secureUrl.isNotEmpty()) {
                                status.text = "Image uploaded"
                                Glide.with(this@InventoryPage)
                                    .load(secureUrl)
                                    .centerCrop()
                                    .into(preview)
                                pendingImageResult?.invoke(secureUrl)
                            } else {
                                status.text = "Upload failed"
                                Toast.makeText(this@InventoryPage, "Image upload failed", Toast.LENGTH_SHORT).show()
                                pendingImageResult?.invoke(null)
                            }
                            clearImageTargets()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            progress.visibility = View.GONE
            status.text = "Upload failed"
            Toast.makeText(this, "Unable to process image: ${e.message}", Toast.LENGTH_SHORT).show()
            pendingImageResult?.invoke(null)
            clearImageTargets()
        }
    }

    private fun clearImageTargets() {
        pendingImagePreview = null
        pendingImageStatus = null
        pendingImageProgress = null
        pendingImageResult = null
    }
}