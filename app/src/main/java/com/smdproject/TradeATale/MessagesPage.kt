package com.smdproject.TradeATale

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MessagesPage : AppCompatActivity() {
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoImageView: ImageView
    private lateinit var menuIcon: ImageView
    private lateinit var searchIcon: ImageView
    private lateinit var conversationsRecyclerView: RecyclerView
    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private val conversations = mutableListOf<Conversation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_messages_page)

        // Handle window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance(FirebaseConfig.REALTIME_DB_URL).reference

        // Initialize views
        rootLayout = findViewById(R.id.main)
        headerLayout = findViewById(R.id.header)
        logoImageView = findViewById(R.id.logoImageView)
        menuIcon = findViewById(R.id.menu_icon)
        searchIcon = findViewById(R.id.search_icon)
        conversationsRecyclerView = findViewById(R.id.conversationsRecyclerView)

        // Set initial state for animation (white header and orange logo)
        headerLayout.setBackgroundColor(android.graphics.Color.WHITE)
        logoImageView.setImageResource(R.drawable.app_logo_orange_right)
        menuIcon.setImageResource(R.drawable.menu_logo)
        searchIcon.setImageResource(R.drawable.search_logo)

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

        // Change header background to AppPrimary and logo to white_right after animation ends
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

                // Change logo and icons to white variants after animation
                logoImageView.setImageResource(R.drawable.app_logo_orange_right)
                menuIcon.setImageResource(R.drawable.menu_logo)
                searchIcon.setImageResource(R.drawable.search_logo)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        // Setup RecyclerView (after function is defined)
        conversationAdapter = ConversationAdapter(conversations) { conversation ->
            val extras = Bundle().apply {
                putString("chat_user_id", conversation.partnerId)
                putString("chat_user_name", conversation.partnerName)
            }
            applyExitAnimationAndNavigate(ChatPage::class.java, "Opening chat", extras)
        }
        conversationsRecyclerView.layoutManager = LinearLayoutManager(this)
        conversationsRecyclerView.adapter = conversationAdapter

        // Set click listeners for navigation
        menuIcon.setOnClickListener {
            applyExitAnimationAndNavigate(MenuPage::class.java, "Navigating to MenuPage")
        }

        searchIcon.setOnClickListener {
            applyExitAnimationAndNavigate(SearchPage::class.java, "Navigating to SearchPage")
        }

        // Logo click to navigate to HomePage
        logoImageView.setOnClickListener {
            applyExitAnimationAndNavigate(HomePage::class.java, "Navigating to HomePage")
        }

        // Load conversations
        loadConversations()
    }

    override fun onResume() {
        super.onResume()
        // Refresh conversations when returning to this page
        loadConversations()
    }

    private fun applyExitAnimationAndNavigate(targetActivity: Class<*>, toastMessage: String, extras: Bundle? = null) {
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
                val intent = Intent(this@MessagesPage, targetActivity)
                extras?.let { intent.putExtras(it) }
                startActivity(intent)
                finish()
            }
        })

        rootLayout.startAnimation(outAnimationSet)
        Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show()
    }

    private fun loadConversations() {
        val currentUserId = auth.currentUser?.uid ?: return

        database.child("Chats").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversationMap = mutableMapOf<String, Conversation>()

                snapshot.children.forEach { conversationSnapshot ->
                    val conversationId = conversationSnapshot.key ?: return@forEach
                    if (!conversationId.contains(currentUserId)) return@forEach

                    // Extract partner ID
                    val partnerId = if (conversationId.startsWith(currentUserId)) {
                        conversationId.substringAfter("_")
                    } else {
                        conversationId.substringBefore("_")
                    }

                    if (partnerId == currentUserId) return@forEach

                    // Get last message
                    val messagesSnapshot = conversationSnapshot.child("messages")
                    var lastMessage = "No messages yet"
                    var lastMessageTime = 0L
                    var unreadCount = 0

                    messagesSnapshot.children.forEach { messageSnapshot ->
                        val timestamp = messageSnapshot.child("timestamp").getValue(Long::class.java)
                            ?: messageSnapshot.child("timestamp").getValue(String::class.java)?.toLongOrNull()
                            ?: 0L
                        val senderId = messageSnapshot.child("senderId").getValue(String::class.java)
                        val text = messageSnapshot.child("text").getValue(String::class.java)

                        if (timestamp > lastMessageTime) {
                            lastMessageTime = timestamp
                            lastMessage = text ?: "No messages yet"
                        }

                        // Count unread messages (messages where current user is receiver)
                        if (senderId != currentUserId && messageSnapshot.child("read").getValue(Boolean::class.java) != true) {
                            unreadCount++
                        }
                    }

                    // Get partner name and profile picture
                    database.child("users").child(partnerId).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(userSnapshot: DataSnapshot) {
                            val partnerName = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown User"
                            val profilePublicId = userSnapshot.child("profilePublicId").getValue(String::class.java)
                            conversationMap[conversationId] = Conversation(
                                conversationId = conversationId,
                                partnerId = partnerId,
                                partnerName = partnerName,
                                partnerProfilePicture = profilePublicId,
                                lastMessage = lastMessage,
                                lastMessageTime = lastMessageTime,
                                unreadCount = unreadCount
                            )

                            // Update adapter when all conversations are loaded
                            if (conversationMap.size == snapshot.children.count { it.key?.contains(currentUserId) == true }) {
                                conversationAdapter.updateConversations(conversationMap.values.toList())
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Use default name
                            conversationMap[conversationId] = Conversation(
                                conversationId = conversationId,
                                partnerId = partnerId,
                                partnerName = "Unknown User",
                                partnerProfilePicture = null,
                                lastMessage = lastMessage,
                                lastMessageTime = lastMessageTime,
                                unreadCount = unreadCount
                            )
                            conversationAdapter.updateConversations(conversationMap.values.toList())
                        }
                    })
                }

                if (conversationMap.isEmpty()) {
                    conversationAdapter.updateConversations(emptyList())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MessagesPage, "Failed to load conversations: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}