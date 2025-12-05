package com.smdproject.TradeATale

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

data class InventorySection(
    val category: String,
    val books: MutableList<InventoryBook>
)

class SectionedInventoryAdapter(
    private var sections: MutableList<InventorySection> = mutableListOf(),
    private val onBookDeleted: () -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val client = OkHttpClient()
    private val GOOGLE_API_KEY = "AIzaSyDW-Hweo3zlykmB-PGYtywJOdDVMTjijlk"
    private val TAG = "SectionedInventoryAdapter"
    private val TYPE_HEADER = 0
    private val TYPE_BOOK = 1

    override fun getItemViewType(position: Int): Int {
        var currentPosition = 0
        sections.forEach { section ->
            if (currentPosition == position) {
                return TYPE_HEADER
            }
            currentPosition++
            section.books.forEach { _ ->
                if (currentPosition == position) {
                    return TYPE_BOOK
                }
                currentPosition++
            }
        }
        return TYPE_BOOK
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_section_header, parent, false)
                SectionHeaderViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.inventory_book, parent, false)
                BookViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var currentPosition = 0
        sections.forEach { section ->
            if (currentPosition == position) {
                (holder as SectionHeaderViewHolder).bind(section.category)
                return
            }
            currentPosition++
            section.books.forEachIndexed { bookIndex, book ->
                if (currentPosition == position) {
                    (holder as BookViewHolder).bind(book, section, bookIndex)
                    return
                }
                currentPosition++
            }
        }
    }

    override fun getItemCount(): Int {
        var count = 0
        sections.forEach { section ->
            count += 1 // Header
            count += section.books.size // Books
        }
        return count
    }

    fun submitBooks(books: List<InventoryBook>) {
        val groupedByCategory = books.groupBy { book ->
            book.categories?.firstOrNull() ?: "Uncategorized"
        }
        
        sections.clear()
        groupedByCategory.forEach { (category, categoryBooks) ->
            sections.add(InventorySection(category, categoryBooks.toMutableList()))
        }
        
        // Sort sections alphabetically
        sections.sortBy { it.category }
        
        notifyDataSetChanged()
    }

    inner class SectionHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryTitle: TextView = itemView.findViewById(R.id.categoryTitle)

        fun bind(category: String) {
            categoryTitle.text = category
        }
    }

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bookImage: ImageView = itemView.findViewById(R.id.bookImage)
        private val bookName: TextView = itemView.findViewById(R.id.bookName)
        private val bookAuthor: TextView = itemView.findViewById(R.id.bookAuthor)
        private val bookDescription: TextView = itemView.findViewById(R.id.bookDescription)
        private val bookCategory: TextView = itemView.findViewById(R.id.bookCategory)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        fun bind(book: InventoryBook, section: InventorySection, bookIndex: Int) {
            val bookTitle = book.name?.takeIf { it.isNotEmpty() } ?: "Unknown Title"
            loadBookImage(bookTitle, book.image)

            bookName.text = bookTitle
            bookAuthor.text = book.author?.takeIf { it.isNotEmpty() } ?: "Unknown Author"
            bookDescription.text = book.description?.takeIf { it.isNotEmpty() } ?: "No description available"
            bookCategory.text = book.categories?.joinToString(", ")?.takeIf { it.isNotEmpty() } ?: "No categories"

            deleteButton.setOnClickListener {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener
                val database = FirebaseDatabase
                    .getInstance(FirebaseConfig.REALTIME_DB_URL)
                    .reference

                database.child("Inventory").child(userId).child(book.bookId).removeValue()
                    .addOnSuccessListener {
                        section.books.removeAt(bookIndex)
                        if (section.books.isEmpty()) {
                            sections.remove(section)
                        }
                        notifyDataSetChanged()
                        onBookDeleted()
                        Toast.makeText(itemView.context, "Book deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(itemView.context, "Failed to delete book: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        private fun loadBookImage(title: String, storedImage: String?) {
            val imageUrl = storedImage?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            if (imageUrl != null) {
                Glide.with(itemView.context)
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
                fetchBookCoverFromGoogleBooks(title) { remoteUrl ->
                    itemView.post {
                        if (remoteUrl != null) {
                            Glide.with(itemView.context)
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
                            Glide.with(itemView.context)
                                .asBitmap()
                                .load(R.drawable.default_book_cover)
                                .transition(BitmapTransitionOptions.withCrossFade())
                                .into(bookImage)
                        }
                    }
                }
            }
        }

        private fun fetchBookCoverFromGoogleBooks(title: String, callback: (String?) -> Unit) {
            val encodedTitle = URLEncoder.encode(title, "UTF-8").replace("+", "%20")
            val url = "https://www.googleapis.com/books/v1/volumes?q=intitle:$encodedTitle&key=$GOOGLE_API_KEY"

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    itemView.post { callback(null) }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string()
                            if (responseBody.isNullOrEmpty()) {
                                itemView.post { callback(null) }
                                return
                            }

                            val json = JSONObject(responseBody)
                            val items = json.optJSONArray("items")
                            if (items == null || items.length() == 0) {
                                itemView.post { callback(null) }
                                return
                            }

                            val firstItem = items.getJSONObject(0)
                            val volumeInfo = firstItem.optJSONObject("volumeInfo")
                            val imageLinks = volumeInfo?.optJSONObject("imageLinks")
                            val thumbnail = imageLinks?.optString("thumbnail")?.replace("http://", "https://")
                            itemView.post { callback(thumbnail) }
                        } else {
                            itemView.post { callback(null) }
                        }
                    } catch (e: Exception) {
                        itemView.post { callback(null) }
                    } finally {
                        response.close()
                    }
                }
            })
        }
    }
}

