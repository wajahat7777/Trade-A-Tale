package com.smdproject.TradeATale

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ServerValue

class ChatPage : AppCompatActivity() {
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoTextView: TextView
    private lateinit var backNavigation: ImageView
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var messageAdapter: ChatMessageAdapter
    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private var chatUserId: String? = null
    private var chatUserName: String? = null
    private var conversationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chat_page)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LogInPage::class.java))
            finish()
            return
        }
        database = FirebaseDatabase
            .getInstance(FirebaseConfig.REALTIME_DB_URL)
            .reference

        chatUserId = intent.getStringExtra("chat_user_id")
        chatUserName = intent.getStringExtra("chat_user_name")

        if (chatUserId.isNullOrBlank()) {
            Toast.makeText(this, "No chat user selected", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoTextView = findViewById(R.id.logoImageView)
        backNavigation = findViewById(R.id.back_navigation)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)

        messageAdapter = ChatMessageAdapter(currentUser.uid)
        messagesRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatPage).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }

        setHeaderTitle()
        listenForPartnerName()
        prepareConversation(currentUser.uid)

        // Set initial state for animation (white header and orange text)
        headerLayout.setBackgroundColor(android.graphics.Color.WHITE)
        logoTextView.setTextColor(android.graphics.Color.parseColor("#EA3E23")) // Orange color
        backNavigation.setImageResource(R.drawable.back_navigation)

        // Create a TranslateAnimation to slide in from the right
        val slideInFromRight = TranslateAnimation(
            1000f,  // Start from 1000px to the right
            0f,     // End at its normal position
            0f,     // No vertical movement
            0f
        )
        slideInFromRight.duration = 1000 // 1 second

        // Create an AlphaAnimation for fading in
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 1000 // 1 second

        // Combine both animations into an AnimationSet
        val animationSet = AnimationSet(true)
        animationSet.addAnimation(slideInFromRight)
        animationSet.addAnimation(fadeIn)

        // Apply the animation to the root layout
        rootLayout.startAnimation(animationSet)

        // Change header background to AppPrimary and text to white after animation ends
        animationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                // Fade header background from white to AppPrimary
                val colorFrom = android.graphics.Color.WHITE
                val colorTo = resources.getColor(R.color.AppPrimary, theme)
                val colorAnimation = ValueAnimator.ofObject(android.animation.ArgbEvaluator(), colorFrom, colorTo)
                colorAnimation.duration = 1000 // 1 second
                colorAnimation.addUpdateListener { animator ->
                    headerLayout.setBackgroundColor(animator.animatedValue as Int)
                }
                colorAnimation.start()

                // Change text color to white after animation
                logoTextView.setTextColor(resources.getColor(R.color.white, theme))
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        sendButton.setOnClickListener {
            val text = messageInput.text.toString().trim()
            val partnerId = chatUserId
            val convId = conversationId
            if (text.isEmpty() || partnerId.isNullOrEmpty() || convId.isNullOrEmpty()) {
                return@setOnClickListener
            }
            val messageData = mapOf(
                "senderId" to currentUser.uid,
                "receiverId" to partnerId,
                "text" to text,
                "timestamp" to ServerValue.TIMESTAMP
            )
            database.child("Chats").child(convId).child("messages").push()
                .setValue(messageData)
                .addOnSuccessListener {
                    messageInput.text.clear()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
                }
        }

        // Helper function to apply exit animation and navigate
        fun applyExitAnimationAndNavigate(targetActivity: Class<*>, toastMessage: String) {
            val fadeOut = AlphaAnimation(1f, 0f)
            fadeOut.duration = 2000
            val slideOutToRight = TranslateAnimation(0f, 1000f, 0f, 0f)
            slideOutToRight.duration = 2000
            val outAnimationSet = AnimationSet(true)
            outAnimationSet.addAnimation(fadeOut)
            outAnimationSet.addAnimation(slideOutToRight)

            outAnimationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {
                    rootLayout.isEnabled = false
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    rootLayout.visibility = View.GONE
                    val intent = Intent(this@ChatPage, targetActivity)
                    startActivity(intent)
                    finish()
                }
            })

            rootLayout.startAnimation(outAnimationSet)
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
        }

        // Set click listener for back navigation
        backNavigation.setOnClickListener {
            applyExitAnimationAndNavigate(MessagesPage::class.java, "Navigating to MessagesPage")
        }
    }

    private fun setHeaderTitle() {
        logoTextView.text = chatUserName ?: "Chat"
    }

    private fun listenForPartnerName() {
        val partnerId = chatUserId ?: return
        database.child("users").child(partnerId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val name = snapshot.child("name").getValue(String::class.java)
                    if (!name.isNullOrEmpty()) {
                        chatUserName = name
                        logoTextView.text = name
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Ignore and keep existing title
                }
            })
    }

    private fun prepareConversation(currentUserId: String) {
        val partnerId = chatUserId ?: return
        conversationId = if (currentUserId < partnerId) {
            "${currentUserId}_$partnerId"
        } else {
            "${partnerId}_$currentUserId"
        }

        database.child("Chats").child(conversationId!!).child("messages")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull { child ->
                        child.getValue(ChatMessage::class.java)?.copy(id = child.key ?: "")
                    }
                    messageAdapter.submitMessages(messages)
                    if (messages.isNotEmpty()) {
                        messagesRecyclerView.scrollToPosition(messages.size - 1)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ChatPage, "Failed to load messages", Toast.LENGTH_SHORT).show()
                }
            })
    }
}