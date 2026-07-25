package com.plszynon.comuinonek

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.plszynon.comuinonek.adapters.CommunityAdapter
import com.plszynon.comuinonek.databinding.ActivityHomeBinding
import com.plszynon.comuinonek.models.Community

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: CommunityAdapter

    private var showingMyCommunities = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        adapter = CommunityAdapter { community -> onCommunityClicked(community) }
        binding.communityRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.communityRecyclerView.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener { loadCurrentTab() }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                showingMyCommunities = tab?.position == 0
                loadCurrentTab()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.createCommunityFab.setOnClickListener {
            startActivity(Intent(this, CreateCommunityActivity::class.java))
        }

        binding.joinByCodeButton.setOnClickListener { showJoinByCodeDialog() }

        binding.signOutButton.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }

        loadCurrentTab()
    }

    override fun onResume() {
        super.onResume()
        loadCurrentTab()
    }

    private fun loadCurrentTab() {
        if (showingMyCommunities) loadMyCommunities() else loadDiscoverCommunities()
    }

    private fun loadMyCommunities() {
        val uid = auth.currentUser?.uid ?: return
        binding.swipeRefresh.isRefreshing = true

        db.collection("users").document(uid).collection("myCommunities")
            .get()
            .addOnSuccessListener { snapshot ->
                val ids = snapshot.documents.map { it.id }
                if (ids.isEmpty()) {
                    adapter.submitList(emptyList())
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.emptyStateText.text = "You haven't joined any communities yet."
                    binding.swipeRefresh.isRefreshing = false
                    return@addOnSuccessListener
                }
                // Fetch full community docs for each membership (small lists; simple loop is fine)
                val results = mutableListOf<Community>()
                var remaining = ids.size
                for (id in ids) {
                    db.collection("communities").document(id).get()
                        .addOnSuccessListener { doc ->
                            val community = doc.toObject(Community::class.java)
                            if (community != null) {
                                community.id = doc.id
                                results.add(community)
                            }
                            remaining--
                            if (remaining == 0) {
                                binding.emptyStateText.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                                adapter.submitList(results.sortedByDescending { it.createdAt })
                                binding.swipeRefresh.isRefreshing = false
                            }
                        }
                        .addOnFailureListener {
                            remaining--
                            if (remaining == 0) binding.swipeRefresh.isRefreshing = false
                        }
                }
            }
            .addOnFailureListener {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Couldn't load your communities: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadDiscoverCommunities() {
        binding.swipeRefresh.isRefreshing = true
        db.collection("communities")
            .whereEqualTo("isPrivate", false)
            .limit(50)
            .get()
            .addOnSuccessListener { snapshot ->
                val results = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Community::class.java)?.apply { id = doc.id }
                }
                binding.emptyStateText.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                binding.emptyStateText.text = "No public communities yet. Be the first to create one!"
                adapter.submitList(results.sortedByDescending { it.createdAt })
                binding.swipeRefresh.isRefreshing = false
            }
            .addOnFailureListener {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this, "Couldn't load communities: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun onCommunityClicked(community: Community) {
        val intent = Intent(this, CommunityDetailActivity::class.java)
        intent.putExtra("communityId", community.id)
        startActivity(intent)
    }

    private fun showJoinByCodeDialog() {
        val input = EditText(this)
        input.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        input.hint = "Enter join code"

        AlertDialog.Builder(this)
            .setTitle("Join a private community")
            .setView(input)
            .setPositiveButton("Join") { _, _ ->
                val code = input.text.toString().trim().uppercase()
                if (code.isNotEmpty()) joinByCode(code)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun joinByCode(code: String) {
        db.collection("communities")
            .whereEqualTo("joinCode", code)
            .limit(1)
            .get()
            .addOnSuccessListener { snapshot ->
                val doc = snapshot.documents.firstOrNull()
                if (doc == null) {
                    Toast.makeText(this, "No community found with that code", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val community = doc.toObject(Community::class.java)?.apply { id = doc.id }
                if (community != null) joinCommunity(community)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error checking code: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun joinCommunity(community: Community) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val name = user.displayName ?: "Member"

        val memberRef = db.collection("communities").document(community.id)
            .collection("members").document(uid)
        val myCommunityRef = db.collection("users").document(uid)
            .collection("myCommunities").document(community.id)
        val communityRef = db.collection("communities").document(community.id)

        memberRef.get().addOnSuccessListener { existing ->
            if (existing.exists()) {
                Toast.makeText(this, "You're already a member of ${community.name}", Toast.LENGTH_SHORT).show()
                onCommunityClicked(community)
                return@addOnSuccessListener
            }

            val memberData = hashMapOf(
                "uid" to uid,
                "name" to name,
                "joinedAt" to FieldValue.serverTimestamp()
            )
            val myCommunityData = hashMapOf(
                "name" to community.name,
                "isPrivate" to community.isPrivate,
                "role" to "member"
            )

            memberRef.set(memberData)
            myCommunityRef.set(myCommunityData)
            communityRef.update("memberCount", FieldValue.increment(1))

            Toast.makeText(this, "Joined ${community.name}!", Toast.LENGTH_SHORT).show()
            onCommunityClicked(community)
        }
    }
}
