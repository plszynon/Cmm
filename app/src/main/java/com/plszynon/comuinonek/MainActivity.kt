package com.plszynon.comuinonek

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        val target = if (currentUser != null) HomeActivity::class.java else LoginActivity::class.java

        startActivity(Intent(this, target))
        finish()
    }
}
