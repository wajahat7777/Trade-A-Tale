package com.smdproject.TradeATale

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import java.text.DateFormat
import java.util.Date

data class Conversation(
    val conversationId: String,
    val partnerId: String,
    val partnerName: String,
    val partnerProfilePicture: String? = null,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int = 0
)

class ConversationAdapter(
    private val conversations: MutableList<Conversation>,
    private val onConversationClick: (Conversation) -> Unit
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        holder.bind(conversations[position])
    }

    override fun getItemCount(): Int = conversations.size

    fun updateConversations(newConversations: List<Conversation>) {
        conversations.clear()
        conversations.addAll(newConversations.sortedByDescending { it.lastMessageTime })
        notifyDataSetChanged()
    }

    inner class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profilePicture: CircleImageView = itemView.findViewById(R.id.profilePicture)
        private val partnerNameText: TextView = itemView.findViewById(R.id.partnerNameText)
        private val lastMessageText: TextView = itemView.findViewById(R.id.lastMessageText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val unreadBadge: TextView = itemView.findViewById(R.id.unreadBadge)
        private val conversationItem: LinearLayout = itemView.findViewById(R.id.conversationItem)

        fun bind(conversation: Conversation) {
            partnerNameText.text = conversation.partnerName
            lastMessageText.text = conversation.lastMessage
            timeText.text = if (conversation.lastMessageTime > 0) {
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(conversation.lastMessageTime))
            } else {
                ""
            }

            // Load profile picture
            if (!conversation.partnerProfilePicture.isNullOrEmpty()) {
                val imageUrl = if (conversation.partnerProfilePicture.startsWith("http://") || 
                    conversation.partnerProfilePicture.startsWith("https://")) {
                    conversation.partnerProfilePicture
                } else {
                    "https://res.cloudinary.com/ddpt74pga/image/upload/${conversation.partnerProfilePicture}"
                }
                Glide.with(itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.default_profile_pic)
                    .error(R.drawable.default_profile_pic)
                    .into(profilePicture)
            } else {
                profilePicture.setImageResource(R.drawable.default_profile_pic)
            }

            if (conversation.unreadCount > 0) {
                unreadBadge.text = conversation.unreadCount.toString()
                unreadBadge.visibility = View.VISIBLE
            } else {
                unreadBadge.visibility = View.GONE
            }

            conversationItem.setOnClickListener {
                onConversationClick(conversation)
            }
        }
    }
}

