package com.plszynon.comuinonek

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.plszynon.comuinonek.databinding.ActivityCreateCommunityBinding
import kotlin.random.Random

class CreateCommunityActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCommunityBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCommunityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createButton.setOnClickListener { createCommunity() }
    }

    private fun createCommunity() {
        val name = binding.nameInput.text.toString().trim()
        val description = binding.descriptionInput.text.toString().trim()
        val isPrivate = binding.privateSwitch.isChecked

        if (name.isEmpty()) {
            Toast.makeText(this, "Give your community a name", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser ?: return
        val joinCode = if (isPrivate) generateJoinCode() else ""

        setLoading(true)

        val communityRef = db.collection("communities").document()
        val communityData = hashMapOf(
            "name" to name,
            "description" to description,
            "isPrivate" to isPrivate,
            "joinCode" to joinCode,
            "ownerId" to user.uid,
            "ownerName" to (user.displayName ?: "Member"),
            "memberCount" to 1L,
            "createdAt" to FieldValue.serverTimestamp()
        )

        communityRef.set(communityData)
            .addOnSuccessListener {
                // Owner is automatically a member
                val memberData = hashMapOf(
                    "uid" to user.uid,
                    "name" to (user.displayName ?: "Member"),
                    "joinedAt" to FieldValue.serverTimestamp()
                )
                communityRef.collection("members").document(user.uid).set(memberData)

                val myCommunityData = hashMapOf(
                    "name" to name,
                    "isPrivate" to isPrivate,
                    "role" to "owner"
                )
                db.collection("users").document(user.uid)
                    .collection("myCommunities").document(communityRef.id)
                    .set(myCommunityData)
                    .addOnSuccessListener {
                        setLoading(false)
                        if (isPrivate) {
                            showJoinCodeDialog(name, joinCode)
                        } else {
                            Toast.makeText(this, "$name created!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
            }
            .addOnFailureListener {
                setLoading(false)
                Toast.makeText(this, "Couldn't create community: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showJoinCodeDialog(communityName: String, code: String) {
        AlertDialog.Builder(this)
            .setTitle("$communityName created!")
            .setMessage("Share this join code with people you want to invite:\n\n$code")
            .setPositiveButton("Done") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun generateJoinCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
    }

    private fun setLoading(loading: Boolean) {
        binding.createProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.createButton.isEnabled = !loading
    }
}
