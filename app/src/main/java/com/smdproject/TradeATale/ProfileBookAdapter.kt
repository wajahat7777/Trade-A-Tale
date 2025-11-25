package com.smdproject.TradeATale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

data class Book(val bookId: String, val image: String?, val name: String?, val author: String?)


class BookAdapter(private var books: List<Book> = emptyList()) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private var onItemClickListener: ((Book) -> Unit)? = null
    private val client = OkHttpClient()
    private val GOOGLE_API_KEY = "AIzaSyDW-Hweo3zlykmB-PGYtywJOdDVMTjijlk" // Replace with your Google Books API key

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.profile_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = books[position]
        holder.bind(book)
    }

    override fun getItemCount(): Int = books.size

    fun submitBooks(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }

    fun setOnItemClickListener(listener: (Book) -> Unit) {
        onItemClickListener = listener
    }

    inner class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val bookImage: ImageView = itemView.findViewById(R.id.bookImage)
        private val bookName: TextView = itemView.findViewById(R.id.bookName)
        private val bookAuthor: TextView = itemView.findViewById(R.id.bookAuthor)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClickListener?.invoke(books[position])
                }
            }
        }

        fun bind(book: Book) {
            val bookTitle = book.name?.takeIf { it.isNotEmpty() } ?: "Unknown Title"
            val storedImage = book.image?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            if (storedImage != null) {
                Glide.with(itemView.context)
                    .load(storedImage)
                    .placeholder(R.drawable.default_book_cover)
                    .error(R.drawable.default_book_cover)
                    .into(bookImage)
            } else {
                fetchBookCoverFromGoogleBooks(bookTitle) { imageUrl ->
                    if (imageUrl != null) {
                        Glide.with(itemView.context)
                            .load(imageUrl)
                            .placeholder(R.drawable.default_book_cover)
                            .into(bookImage)
                    } else {
                        bookImage.setImageResource(R.drawable.default_book_cover)
                    }
                }
            }

            bookName.text = bookTitle
            bookAuthor.text = book.author?.takeIf { it.isNotEmpty() } ?: "Unknown Author"
        }

        private fun fetchBookCoverFromGoogleBooks(title: String, callback: (String?) -> Unit) {
            val encodedTitle = title.replace(" ", "+")
            val url = "https://www.googleapis.com/books/v1/volumes?q=intitle:$encodedTitle&key=$GOOGLE_API_KEY"

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Handle failure by returning null to callback
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

                            // Get the first book's cover image URL
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