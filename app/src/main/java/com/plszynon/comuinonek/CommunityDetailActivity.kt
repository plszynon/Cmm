package com.plszynon.comuinonek

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.plszynon.comuinonek.adapters.PostAdapter
import com.plszynon.comuinonek.databinding.ActivityCommunityDetailBinding
import com.plszynon.comuinonek.models.Community
import com.plszynon.comuinonek.models.Post

class CommunityDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommunityDetailBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var postAdapter: PostAdapter

    private var communityId: String = ""
    private var community: Community? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        communityId = intent.getStringExtra("communityId") ?: ""
        if (communityId.isEmpty()) {
            finish()
            return
        }

        postAdapter = PostAdapter()
        binding.postRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.postRecyclerView.adapter = postAdapter

        binding.backButton.setOnClickListener { finish() }
        binding.postButton.setOnClickListener { submitPost() }
        binding.submitJoinCodeButton.setOnClickListener { attemptJoinWithCode() }
        binding.joinPublicButton.setOnClickListener { joinPublicCommunity() }

        loadCommunity()
    }

    private fun loadCommunity() {
        db.collection("communities").document(communityId).get()
            .addOnSuccessListener { doc ->
                val loaded = doc.toObject(Community::class.java)?.apply { id = doc.id }
                if (loaded == null) {
                    Toast.makeText(this, "Community not found", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }
                community = loaded
                binding.detailName.text = loaded.name
                binding.detailDescription.text = loaded.description.ifBlank { "No description yet." }
                binding.detailMemberCount.text = "${loaded.memberCount} member${if (loaded.memberCount == 1L) "" else "s"}"
                checkMembership()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Couldn't load community: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun checkMembership() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("communities").document(communityId)
            .collection("members").document(uid).get()
            .addOnSuccessListener { memberDoc ->
                if (memberDoc.exists()) {
                    showFeed()
                } else {
                    showJoinPrompt()
                }
            }
    }

    private fun showJoinPrompt() {
        binding.feedSection.visibility = View.GONE
        val isPrivate = community?.isPrivate == true
        binding.joinCodeSection.visibility = if (isPrivate) View.VISIBLE else View.GONE
        binding.joinPublicButton.visibility = if (isPrivate) View.GONE else View.VISIBLE
    }

    private fun showFeed() {
        binding.joinCodeSection.visibility = View.GONE
        binding.joinPublicButton.visibility = View.GONE
        binding.feedSection.visibility = View.VISIBLE
        loadPosts()
    }

    private fun loadPosts() {
        db.collection("communities").document(communityId)
            .collection("posts")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(200)
            .get()
            .addOnSuccessListener { snapshot ->
                val posts = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.apply { id = doc.id }
                }
                postAdapter.submitList(posts)
                binding.postRecyclerView.scrollToPosition(maxOf(0, posts.size - 1))
            }
    }

    private fun submitPost() {
        val text = binding.postInput.text.toString().trim()
        if (text.isEmpty()) return
        val user = auth.currentUser ?: return

        val postData = hashMapOf(
            "authorId" to user.uid,
            "authorName" to (user.displayName ?: "Member"),
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )

        db.collection("communities").document(communityId)
            .collection("posts").add(postData)
            .addOnSuccessListener {
                binding.postInput.setText("")
                loadPosts()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Couldn't post: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun joinPublicCommunity() {
        joinCurrentCommunity()
    }

    private fun attemptJoinWithCode() {
        val enteredCode = binding.joinCodeInput.text.toString().trim().uppercase()
        val actualCode = community?.joinCode ?: ""
        if (enteredCode.isEmpty()) return

        if (enteredCode == actualCode) {
            joinCurrentCommunity()
        } else {
            Toast.makeText(this, "That code doesn't match", Toast.LENGTH_SHORT).show()
        }
    }

    private fun joinCurrentCommunity() {
        val user = auth.currentUser ?: return
        val c = community ?: return
        val uid = user.uid

        val memberRef = db.collection("communities").document(communityId)
            .collection("members").document(uid)
        val myCommunityRef = db.collection("users").document(uid)
            .collection("myCommunities").document(communityId)
        val communityRef = db.collection("communities").document(communityId)

        val memberData = hashMapOf(
            "uid" to uid,
            "name" to (user.displayName ?: "Member"),
            "joinedAt" to FieldValue.serverTimestamp()
        )
        val myCommunityData = hashMapOf(
            "name" to c.name,
            "isPrivate" to c.isPrivate,
            "role" to "member"
        )

        memberRef.set(memberData)
            .addOnSuccessListener {
                myCommunityRef.set(myCommunityData)
                communityRef.update("memberCount", FieldValue.increment(1))
                Toast.makeText(this, "Joined ${c.name}!", Toast.LENGTH_SHORT).show()
                showFeed()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Couldn't join: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }
}
