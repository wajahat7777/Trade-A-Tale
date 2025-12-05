package com.smdproject.TradeATale

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class EmailVerificationPage : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_email_verification)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LogInPage::class.java))
            finish()
            return
        }

        val emailText = findViewById<TextView>(R.id.emailText)
        emailText.text = user.email ?: "Unknown email"

        val resendButton = findViewById<Button>(R.id.resendButton)
        val continueButton = findViewById<Button>(R.id.continueButton)
        val signOutButton = findViewById<Button>(R.id.signOutButton)

        resendButton.setOnClickListener {
            user.sendEmailVerification()
                .addOnSuccessListener {
                    Toast.makeText(this, "Verification email resent.", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this, "Failed to resend: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }

        continueButton.setOnClickListener {
            user.reload()
                .addOnSuccessListener {
                    if (user.isEmailVerified) {
                        Toast.makeText(this, "Email verified!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, HomePage::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Still not verified. Check your inbox.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { error ->
                    Toast.makeText(this, "Failed to refresh: ${error.message}", Toast.LENGTH_SHORT).show()
                }
        }

        signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LogInPage::class.java))
            finish()
        }
    }
}


