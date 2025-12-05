package com.smdproject.TradeATale

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.DateFormat
import java.util.Date

data class ChatMessage(
    val id: String = "",
    val senderId: String? = null,
    val receiverId: String? = null,
    val text: String? = null,
    val timestamp: Long = 0
)

class ChatMessageAdapter(private val currentUserId: String) :
    RecyclerView.Adapter<ChatMessageAdapter.MessageViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    fun submitMessages(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages.sortedBy { it.timestamp })
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(messages[position])
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val container: LinearLayout = itemView.findViewById(R.id.messageContainer)
        private val messageText: TextView = itemView.findViewById(R.id.messageText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)

        fun bind(message: ChatMessage) {
            messageText.text = message.text ?: ""
            timestampText.text = if (message.timestamp > 0) {
                try {
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(message.timestamp))
                } catch (e: Exception) {
                    ""
                }
            } else {
                ""
            }

            val params = container.layoutParams as FrameLayout.LayoutParams
            val context = itemView.context
            if (message.senderId == currentUserId) {
                params.gravity = Gravity.END
                container.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_message_outgoing)
                messageText.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                timestampText.setTextColor(ContextCompat.getColor(context, android.R.color.white))
            } else {
                params.gravity = Gravity.START
                container.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_message_incoming)
                messageText.setTextColor(ContextCompat.getColor(context, android.R.color.black))
                timestampText.setTextColor(
                    ContextCompat.getColor(
                        context,
                        android.R.color.darker_gray
                    )
                )
            }
            container.layoutParams = params
        }
    }
}

