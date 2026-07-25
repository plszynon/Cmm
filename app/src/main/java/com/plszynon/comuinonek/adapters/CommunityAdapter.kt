package com.plszynon.comuinonek.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.plszynon.comuinonek.R
import com.plszynon.comuinonek.databinding.ItemCommunityBinding
import com.plszynon.comuinonek.models.Community

class CommunityAdapter(
    private val onClick: (Community) -> Unit
) : RecyclerView.Adapter<CommunityAdapter.ViewHolder>() {

    private val items = mutableListOf<Community>()

    fun submitList(newItems: List<Community>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommunityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemCommunityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(community: Community) {
            binding.communityName.text = community.name
            binding.communityDescription.text = community.description.ifBlank { "No description yet." }
            binding.communityMemberCount.text = "${community.memberCount} member${if (community.memberCount == 1L) "" else "s"}"

            if (community.isPrivate) {
                binding.privacyBadge.text = "PRIVATE"
                binding.privacyBadge.setBackgroundResource(R.drawable.badge_private_bg)
            } else {
                binding.privacyBadge.text = "PUBLIC"
                binding.privacyBadge.setBackgroundResource(R.drawable.badge_public_bg)
            }

            binding.root.setOnClickListener { onClick(community) }
        }
    }
}
