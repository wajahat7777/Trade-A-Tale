package com.smdproject.TradeATale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class BarterRequestAdapter(
    private val requests: MutableList<BarterRequest>,
    private val onRequestAction: (BarterRequest) -> Unit
) : RecyclerView.Adapter<BarterRequestAdapter.RequestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_barter_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requests[position])
    }

    override fun getItemCount(): Int = requests.size

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val requesterName: TextView = itemView.findViewById(R.id.requesterName)
        private val requesterBookImage: ImageView = itemView.findViewById(R.id.requesterBookImage)
        private val requesterBookName: TextView = itemView.findViewById(R.id.requesterBookName)
        private val yourBookImage: ImageView = itemView.findViewById(R.id.yourBookImage)
        private val yourBookName: TextView = itemView.findViewById(R.id.yourBookName)
        private val acceptButton: Button = itemView.findViewById(R.id.acceptButton)
        private val rejectButton: Button = itemView.findViewById(R.id.rejectButton)

        fun bind(request: BarterRequest) {
            requesterName.text = "${request.requesterName} wants to trade:"
            requesterBookName.text = request.requesterBookName
            yourBookName.text = "Your book: ${request.yourBookName}"

            // Load images
            if (!request.requesterBookImage.isNullOrEmpty() && 
                (request.requesterBookImage.startsWith("http://") || request.requesterBookImage.startsWith("https://"))) {
                Glide.with(itemView.context)
                    .load(request.requesterBookImage)
                    .placeholder(R.drawable.default_book_cover)
                    .error(R.drawable.default_book_cover)
                    .into(requesterBookImage)
            } else {
                requesterBookImage.setImageResource(R.drawable.default_book_cover)
            }

            if (!request.yourBookImage.isNullOrEmpty() && 
                (request.yourBookImage.startsWith("http://") || request.yourBookImage.startsWith("https://"))) {
                Glide.with(itemView.context)
                    .load(request.yourBookImage)
                    .placeholder(R.drawable.default_book_cover)
                    .error(R.drawable.default_book_cover)
                    .into(yourBookImage)
            } else {
                yourBookImage.setImageResource(R.drawable.default_book_cover)
            }

            acceptButton.setOnClickListener {
                acceptRequest(request)
            }

            rejectButton.setOnClickListener {
                rejectRequest(request)
            }
        }

        private fun acceptRequest(request: BarterRequest) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val database = FirebaseDatabase
                .getInstance(FirebaseConfig.REALTIME_DB_URL)
                .reference

            // Move books between inventories
            database.child("Inventory").child(userId).child(request.yourBookId).removeValue()
            database.child("Inventory").child(request.requesterId).child(request.requesterBookId).removeValue()
            database.child("Inventory").child(userId).child(request.requesterBookId).setValue(true)
            database.child("Inventory").child(request.requesterId).child(request.yourBookId).setValue(true)

            // Remove the request
            database.child("BarterRequest").child(userId).child(request.requestId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(itemView.context, "Barter request accepted!", Toast.LENGTH_SHORT).show()
                    requests.removeAt(adapterPosition)
                    notifyItemRemoved(adapterPosition)
                }
        }

        private fun rejectRequest(request: BarterRequest) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val database = FirebaseDatabase
                .getInstance(FirebaseConfig.REALTIME_DB_URL)
                .reference

            database.child("BarterRequest").child(userId).child(request.requestId).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(itemView.context, "Barter request rejected", Toast.LENGTH_SHORT).show()
                    requests.removeAt(adapterPosition)
                    notifyItemRemoved(adapterPosition)
                }
        }
    }
}

