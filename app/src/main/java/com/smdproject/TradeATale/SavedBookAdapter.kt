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
import java.net.URLEncoder

class SavedBookAdapter(private val books: List<SavedBook>) :
    RecyclerView.Adapter<SavedBookAdapter.SavedBookViewHolder>() {

    private val client = OkHttpClient()
    private val GOOGLE_API_KEY = "AIzaSyDW-Hweo3zlykmB-PGYtywJOdDVMTjijlk"

    class SavedBookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookImage: ImageView = itemView.findViewById(R.id.bookImage)
        val bookName: TextView = itemView.findViewById(android.R.id.text1)
        val bookAuthor: TextView = itemView.findViewById(android.R.id.text2)
        val bookDescription: TextView = itemView.findViewById(R.id.book_description)
        val bookCategories: TextView = itemView.findViewById(R.id.book_categories)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedBookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.saved_book_item, parent, false)
        return SavedBookViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedBookViewHolder, position: Int) {
        val book = books[position]
        holder.bookName.text = book.name
        holder.bookAuthor.text = book.author
        holder.bookDescription.text = book.description
        holder.bookCategories.text = book.categories.ifEmpty { "No categories" }
        
        // Load book image
        loadBookImage(holder, book.name, book.image)
    }

    private fun loadBookImage(holder: SavedBookViewHolder, title: String, storedImage: String?) {
        val imageUrl = storedImage?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
        if (imageUrl != null) {
            Glide.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(R.drawable.default_book_cover)
                .error(R.drawable.default_book_cover)
                .into(holder.bookImage)
        } else {
            fetchBookCoverFromGoogleBooks(title) { remoteUrl ->
                holder.itemView.post {
                    if (remoteUrl != null) {
                        Glide.with(holder.itemView.context)
                            .load(remoteUrl)
                            .placeholder(R.drawable.default_book_cover)
                            .error(R.drawable.default_book_cover)
                            .into(holder.bookImage)
                    } else {
                        holder.bookImage.setImageResource(R.drawable.default_book_cover)
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
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody.isNullOrEmpty()) {
                            callback(null)
                            return
                        }

                        val json = JSONObject(responseBody)
                        val items = json.optJSONArray("items")
                        if (items == null || items.length() == 0) {
                            callback(null)
                            return
                        }

                        val firstItem = items.getJSONObject(0)
                        val volumeInfo = firstItem.optJSONObject("volumeInfo")
                        val imageLinks = volumeInfo?.optJSONObject("imageLinks")
                        val thumbnail = imageLinks?.optString("thumbnail")?.replace("http://", "https://")
                        callback(thumbnail)
                    } else {
                        callback(null)
                    }
                } catch (e: Exception) {
                    callback(null)
                } finally {
                    response.close()
                }
            }
        })
    }

    override fun getItemCount(): Int = books.size
}