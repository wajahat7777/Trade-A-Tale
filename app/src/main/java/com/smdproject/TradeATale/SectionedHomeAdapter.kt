package com.smdproject.TradeATale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView

class SectionedHomeAdapter(
    private val categoryBooksMap: Map<String, List<Book>>,
    private val onBookClick: (Book) -> Unit
) : RecyclerView.Adapter<SectionedHomeAdapter.ViewHolder>() {

    private val categories = categoryBooksMap.keys.sorted()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_section, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        val books = categoryBooksMap[category] ?: emptyList()
        holder.bind(category, books)
    }

    override fun getItemCount(): Int = categories.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryTitle: TextView = itemView.findViewById(R.id.categoryTitle)
        private val booksRecyclerView: RecyclerView = itemView.findViewById(R.id.booksRecyclerView)
        private val bookAdapter = BookAdapter()

        init {
            booksRecyclerView.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            booksRecyclerView.adapter = bookAdapter
            LinearSnapHelper().attachToRecyclerView(booksRecyclerView)
            
            bookAdapter.setOnItemClickListener { book ->
                onBookClick(book)
            }
        }

        fun bind(category: String, books: List<Book>) {
            categoryTitle.text = category
            bookAdapter.submitBooks(books)
        }
    }
}

