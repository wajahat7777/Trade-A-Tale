package com.smdproject.TradeATale

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class EditProfilePage : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var rootLayout: RelativeLayout
    private lateinit var headerLayout: RelativeLayout
    private lateinit var logoImageView: ImageView
    private lateinit var profileImage: CircleImageView
    private lateinit var nameEditText: EditText
    private lateinit var usernameEditText: EditText
    private lateinit var contactEditText: EditText
    private lateinit var bioEditText: EditText
    private lateinit var usernameDisplay: TextView
    private lateinit var updateButton: Button

    private var userId: String? = null
    private var currentProfilePublicId: String? = null
    private var currentProfileImageUrl: String? = null

    private val CLOUD_NAME = "ddpt74pga"
    private val UPLOAD_PRESET = "ShelfShare"

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val imageUri: Uri? = result.data?.data
            imageUri?.let {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                    profileImage.setImageBitmap(bitmap)
                    if (userId != null) {
                        uploadImageToWebApi(bitmap)
                    } else {
                        runOnUiThread {
                            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_edit_profile_page)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            auth = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance(FirebaseConfig.REALTIME_DB_URL)

            rootLayout = findViewById(R.id.main)
            headerLayout = findViewById(R.id.header)
            logoImageView = findViewById(R.id.logoImageView)
            profileImage = findViewById(R.id.ProfilePicture)
            nameEditText = findViewById(R.id.nameEditText)
            usernameEditText = findViewById(R.id.usernameEditText)
            contactEditText = findViewById(R.id.contactEditText)
            bioEditText = findViewById(R.id.bioEditText)
            usernameDisplay = findViewById(R.id.Username)
            updateButton = findViewById(R.id.myBtn)

            userId = auth.currentUser?.uid
            if (userId == null) {
                runOnUiThread {
                    Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                }
                finish()
                return
            }

            headerLayout.setBackgroundColor(android.graphics.Color.WHITE)
            logoImageView.setImageResource(R.drawable.app_logo_orange_right)

            val slideInFromRight = TranslateAnimation(1000f, 0f, 0f, 0f)
            slideInFromRight.duration = 1000

            val fadeIn = AlphaAnimation(0f, 1f)
            fadeIn.duration = 1000

            val animationSet = AnimationSet(true)
            animationSet.addAnimation(slideInFromRight)
            animationSet.addAnimation(fadeIn)

            rootLayout.startAnimation(animationSet)

            animationSet.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
                override fun onAnimationStart(animation: android.view.animation.Animation?) {}

                override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                    val colorFrom = android.graphics.Color.WHITE
                    val colorTo = resources.getColor(R.color.AppPrimary, theme)
                    val colorAnimation = ValueAnimator.ofObject(android.animation.ArgbEvaluator(), colorFrom, colorTo)
                    colorAnimation.duration = 1000
                    colorAnimation.addUpdateListener { animator ->
                        headerLayout.setBackgroundColor(animator.animatedValue as Int)
                    }
                    colorAnimation.start()

                    logoImageView.setImageResource(R.drawable.app_logo_orange_right)
                }

                override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
            })

            loadUserData(userId!!)
            fetchProfileImage(userId!!)

            profileImage.setOnClickListener {
                openImagePicker()
            }

            updateButton.setOnClickListener {
                updateUserData(userId!!)
            }
        } catch (e: Exception) {
            runOnUiThread {
                Toast.makeText(this, "Error in onCreate: ${e.message}", Toast.LENGTH_LONG).show()
            }
            e.printStackTrace()
            finish()
        }
    }

    private fun loadUserData(userId: String) {
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val contact = snapshot.child("contact").getValue(String::class.java) ?: "123456789"
                        val name = snapshot.child("name").getValue(String::class.java) ?: "Muhammad Omer"
                        val username = snapshot.child("username").getValue(String::class.java) ?: "Muhammad Omer"
                        val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                        val publicId = snapshot.child("profilePublicId").getValue(String::class.java)

                        runOnUiThread {
                            nameEditText.setHint(name)
                            usernameEditText.setHint(username)
                            contactEditText.setHint(contact)
                            bioEditText.setHint(bio.takeIf { it.isNotEmpty() } ?: "Write your bio...")
                            usernameDisplay.text = name
                            if (publicId != null) {
                                currentProfilePublicId = publicId
                                currentProfileImageUrl = "https://res.cloudinary.com/$CLOUD_NAME/image/upload/${publicId}"
                                Glide.with(this@EditProfilePage)
                                    .load(currentProfileImageUrl)
                                    .placeholder(R.drawable.default_profile_pic)
                                    .into(profileImage)
                            }
                        }
                    } else {
                        runOnUiThread {
                            nameEditText.setHint("Muhammad Omer")
                            usernameEditText.setHint("Muhammad Omer")
                            contactEditText.setHint("123456789")
                            bioEditText.setHint("Write your bio...")
                            usernameDisplay.text = "Muhammad Omer"
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    runOnUiThread {
                        Toast.makeText(this@EditProfilePage, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
                        nameEditText.setHint("Muhammad Omer")
                        usernameEditText.setHint("Muhammad Omer")
                        contactEditText.setHint("123456789")
                        bioEditText.setHint("Write your bio...")
                        usernameDisplay.text = "Muhammad Omer"
                    }
                }
            })
    }

    private fun fetchProfileImage(userId: String) {
        database.reference.child("users").child(userId).child("profilePublicId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        currentProfilePublicId = snapshot.getValue(String::class.java)
                        if (currentProfilePublicId != null) {
                            currentProfileImageUrl = "https://res.cloudinary.com/$CLOUD_NAME/image/upload/${currentProfilePublicId}"
                            runOnUiThread {
                                Glide.with(this@EditProfilePage)
                                    .load(currentProfileImageUrl)
                                    .placeholder(R.drawable.default_profile_pic)
                                    .into(profileImage)
                                Toast.makeText(this@EditProfilePage, "Profile image fetched", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@EditProfilePage, "No profile public ID found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@EditProfilePage, "No profile image found", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    runOnUiThread {
                        Toast.makeText(this@EditProfilePage, "Failed to fetch profile image: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun uploadImageToWebApi(bitmap: Bitmap) {
        if (userId == null) {
            runOnUiThread {
                Toast.makeText(this@EditProfilePage, "User not authenticated", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()

        val client = OkHttpClient()
        val fixedPublicId = "ShelfShare/$userId/profileImage/profile"
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "profile.jpg",
                byteArray.toRequestBody("image/jpeg".toMediaType())
            )
            .addFormDataPart("upload_preset", UPLOAD_PRESET)
            .addFormDataPart("public_id", fixedPublicId)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EditProfilePage, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        if (responseBody.isNullOrEmpty()) {
                            runOnUiThread {
                                Toast.makeText(this@EditProfilePage, "Empty response from Cloudinary", Toast.LENGTH_SHORT).show()
                            }
                            return
                        }

                        val json = JSONObject(responseBody)
                        currentProfilePublicId = json.optString("public_id")
                        currentProfileImageUrl = json.optString("secure_url")
                        if (currentProfilePublicId != null) {
                            database.reference.child("users").child(userId!!).child("profilePublicId").setValue(currentProfilePublicId)
                                .addOnSuccessListener {
                                    runOnUiThread {
                                        Glide.with(this@EditProfilePage)
                                            .load(currentProfileImageUrl)
                                            .placeholder(R.drawable.default_profile_pic)
                                            .into(profileImage)
                                        Toast.makeText(this@EditProfilePage, "Profile image uploaded", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener {
                                    runOnUiThread {
                                        Toast.makeText(this@EditProfilePage, "Failed to save public ID: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@EditProfilePage, "No public ID in response", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@EditProfilePage, "Image upload failed: ${response.code} - ${response.body?.string()}", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@EditProfilePage, "Error parsing upload response: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    response.close()
                }
            }
        })
    }

    private fun checkUsernameUnique(newUsername: String, currentUserId: String, onResult: (Boolean) -> Unit) {
        database.reference.child("users")
            .orderByChild("username")
            .equalTo(newUsername)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (userSnapshot in snapshot.children) {
                            val userKey = userSnapshot.key
                            if (userKey != currentUserId) {
                                onResult(false)
                                return
                            }
                        }
                        onResult(true)
                    } else {
                        onResult(true)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    runOnUiThread {
                        Toast.makeText(this@EditProfilePage, "Error checking username: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                    onResult(false)
                }
            })
    }

    private fun updateUserData(userId: String) {
        database.reference.child("users").child(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val currentName = snapshot.child("name").getValue(String::class.java) ?: "Muhammad Omer"
                        val currentUsername = snapshot.child("username").getValue(String::class.java) ?: "Muhammad Omer"
                        val currentContact = snapshot.child("contact").getValue(String::class.java) ?: "123456789"
                        val currentBio = snapshot.child("bio").getValue(String::class.java) ?: ""

                        val updatedName = nameEditText.text.toString().trim().ifEmpty { currentName }
                        val updatedUsernameInput = usernameEditText.text.toString().trim()
                        val updatedUsername = if (updatedUsernameInput.isNotEmpty()) updatedUsernameInput else currentUsername
                        val updatedContact = contactEditText.text.toString().trim().ifEmpty { currentContact }
                        val updatedBio = bioEditText.text.toString().trim().ifEmpty { currentBio }

                        if (updatedUsername != currentUsername && updatedUsername.isNotEmpty()) {
                            checkUsernameUnique(updatedUsername, userId) { isUnique ->
                                if (!isUnique) {
                                    runOnUiThread {
                                        Toast.makeText(this@EditProfilePage, "Username already taken", Toast.LENGTH_SHORT).show()
                                    }
                                    return@checkUsernameUnique
                                }
                                performUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio)
                            }
                        } else {
                            performUpdate(userId, updatedName, updatedUsername, updatedContact, updatedBio)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    runOnUiThread {
                        Toast.makeText(this@EditProfilePage, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    private fun performUpdate(userId: String, updatedName: String, updatedUsername: String, updatedContact: String, updatedBio: String) {
        val updates = mapOf(
            "name" to updatedName,
            "username" to updatedUsername,
            "contact" to updatedContact,
            "bio" to updatedBio
        )

        database.reference.child("users").child(userId).updateChildren(updates)
            .addOnSuccessListener {
                runOnUiThread {
                    Toast.makeText(this@EditProfilePage, "Profile updated successfully", Toast.LENGTH_SHORT).show()
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
                            val intent = Intent(this@EditProfilePage, ProfilePage::class.java)
                            startActivity(intent)
                            finish()
                        }
                    })

                    rootLayout.startAnimation(outAnimationSet)
                }
            }
            .addOnFailureListener {
                runOnUiThread {
                    Toast.makeText(this@EditProfilePage, "Failed to update profile", Toast.LENGTH_SHORT).show()
                }
            }
    }
}