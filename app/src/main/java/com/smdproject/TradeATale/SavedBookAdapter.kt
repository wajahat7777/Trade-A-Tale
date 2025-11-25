package com.smdproject.TradeATale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SavedBookAdapter(private val books: List<SavedBook>) :
    RecyclerView.Adapter<SavedBookAdapter.SavedBookViewHolder>() {

    class SavedBookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookName: TextView = itemView.findViewById(android.R.id.text1)
        val bookAuthor: TextView = itemView.findViewById(android.R.id.text2)
        val bookDescription: TextView = itemView.findViewById(R.id.book_description)
        val bookCategories: TextView = itemView.findViewById(R.id.book_categories)
        val bookOwnerId: TextView = itemView.findViewById(R.id.book_owner_id)
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
        holder.bookOwnerId.text = book.ownerId.ifEmpty { "Not specified" }
    }

    override fun getItemCount(): Int = books.size
}