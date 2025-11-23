package com.example.geminichat.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.RecyclerView
import com.example.geminichat.R
import com.example.geminichat.data.ChatMessage
import com.example.geminichat.databinding.ItemChatAiBinding
import com.example.geminichat.databinding.ItemChatUserBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatAdapter(
    private val messages: MutableList<ChatMessage> = mutableListOf(),
    private val onCopyClick: (String) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_AI = 2
    }

    fun submitList(newMessages: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    fun updateStreamingMessage(message: ChatMessage) {
        if (messages.isNotEmpty() && messages.last().id == -1L) {
            // Update existing temporary message
            messages[messages.size - 1] = message
            notifyItemChanged(messages.size - 1)
        } else {
            // Add new temporary message
            messages.add(message)
            notifyItemInserted(messages.size - 1)
        }
    }

    fun addMessage(message: ChatMessage) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_AI
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_USER) {
            val binding = ItemChatUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            UserViewHolder(binding)
        } else {
            val binding = ItemChatAiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            AiViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        // Apply animation only to the last item
        if (position == messages.size - 1) {
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.item_animation_fall_down)
            holder.itemView.startAnimation(animation)
        }

        if (holder is UserViewHolder) {
            holder.bind(message)
        } else if (holder is AiViewHolder) {
            holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class UserViewHolder(private val binding: ItemChatUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvUserMessage.text = message.text
            binding.tvUserTimestamp.text = formatTimestamp(message.timestamp)
        }
    }

    inner class AiViewHolder(private val binding: ItemChatAiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: ChatMessage) {
            binding.tvAiMessage.text = message.text
            binding.tvAiTimestamp.text = formatTimestamp(message.timestamp)

            binding.btnCopy.setOnClickListener {
                onCopyClick(message.text)
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
