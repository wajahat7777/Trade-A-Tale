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

data class InventoryBook(val bookId: String, val image: String?, val name: String?, val author: String?, val description: String?, val categories: List<String>?)

class InventoryBookAdapter(
    private var books: MutableList<InventoryBook> = mutableListOf(),
    private val onBookDeleted: () -> Unit
) : RecyclerView.Adapter<InventoryBookAdapter.BookViewHolder>() {

    private var onItemClickListener: ((InventoryBook) -> Unit)? = null
    private val client = OkHttpClient()
    private val GOOGLE_API_KEY = "AIzaSyDW-Hweo3zlykmB-PGYtywJOdDVMTjijlk"
    private val TAG = "InventoryBookAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.inventory_book, parent, false)
        Log.d(TAG, "Creating ViewHolder for position: $viewType")
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        Log.d(TAG, "Binding book at position $position: ${book.name}, Author: ${book.author}, Description: ${book.description}, Categories: ${book.categories}")
        holder.bind(book)
    }

    override fun getItemCount(): Int = books.size

    fun submitBooks(newBooks: List<InventoryBook>) {
        books.clear()
        books.addAll(newBooks)
        Log.d(TAG, "Submitting ${books.size} books to adapter")
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (InventoryBook) -> Unit) {
        onItemClickListener = listener
        Log.d(TAG, "Item click listener set")
    }

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bookImage: ImageView = itemView.findViewById(R.id.bookImage)
        private val bookName: TextView = itemView.findViewById(R.id.bookName)
        private val bookAuthor: TextView = itemView.findViewById(R.id.bookAuthor)
        private val bookDescription: TextView = itemView.findViewById(R.id.bookDescription)
        private val bookCategory: TextView = itemView.findViewById(R.id.bookCategory)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        init {
            // Set click listener on the entire item view for selecting the book
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val book = books[position]
                    Log.d(TAG, "Item clicked at position $position: ${book.name}, ID: ${book.bookId}")
                    onItemClickListener?.invoke(book)
                }
            }
        }

        fun bind(book: InventoryBook) {
            val bookTitle = book.name?.takeIf { it.isNotEmpty() } ?: "Unknown Title"
            Log.d(TAG, "Binding book: $bookTitle with data - Author: ${book.author}, Description: ${book.description}, Categories: ${book.categories}")
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
                        val position = adapterPosition
                        if (position != RecyclerView.NO_POSITION) {
                            books.removeAt(position)
                            notifyItemRemoved(position)
                            onBookDeleted()
                            Toast.makeText(itemView.context, "Book deleted successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(itemView.context, "Failed to delete book: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        private fun loadBookImage(title: String, storedImage: String?) {
            val imageUrl = storedImage?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            if (imageUrl != null) {
                Log.d(TAG, "Loading stored image URL for $title: $imageUrl")
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
                            Log.d(TAG, "Loading fallback Google image for $title: $remoteUrl")
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
                            Log.w(TAG, "No image URL found for: $title")
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
            Log.d(TAG, "Fetching book cover from: $url")

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "API call failed: ${e.message}")
                    itemView.post { callback(null) }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string()
                            Log.d(TAG, "API response: $responseBody")
                            if (responseBody.isNullOrEmpty()) {
                                Log.w(TAG, "Empty response body")
                                itemView.post { callback(null) }
                                return
                            }

                            val json = JSONObject(responseBody)
                            val items = json.optJSONArray("items")
                            if (items == null || items.length() == 0) {
                                Log.w(TAG, "No items found for title: $title")
                                itemView.post { callback(null) }
                                return
                            }

                            val firstItem = items.getJSONObject(0)
                            val volumeInfo = firstItem.optJSONObject("volumeInfo")
                            val imageLinks = volumeInfo?.optJSONObject("imageLinks")
                            val thumbnail = imageLinks?.optString("thumbnail")?.replace("http://", "https://")
                            Log.d(TAG, "Thumbnail URL: $thumbnail")
                            itemView.post { callback(thumbnail) }
                        } else {
                            Log.e(TAG, "Unsuccessful response: ${response.code} - ${response.body?.string()}")
                            itemView.post { callback(null) }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing response: ${e.message}")
                        itemView.post { callback(null) }
                    } finally {
                        response.close()
                    }
                }
            })
        }
    }
}