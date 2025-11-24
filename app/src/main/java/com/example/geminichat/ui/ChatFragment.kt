package com.example.geminichat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.geminichat.databinding.FragmentChatBinding
import com.example.geminichat.ui.adapter.ChatAdapter
import com.example.geminichat.ui.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.geminichat.data.ChatMessage

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeData()
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter { textToCopy ->
            copyToClipboard(textToCopy)
        }
        binding.rvChat.apply {
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true // Start from bottom
            }
            adapter = chatAdapter
        }
    }

    private fun setupListeners() {
        binding.btnSend.setOnClickListener {
            val text = binding.etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendMessage(text)
                binding.etMessage.text.clear()
            }
        }

        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        binding.btnClearHistory.setOnClickListener {
            viewModel.clearHistory()
            Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeData() {
        // Observe Database History
        lifecycleScope.launch {
            viewModel.chatHistory.collect { messages ->
                chatAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    binding.rvChat.smoothScrollToPosition(messages.size - 1)
                }
            }
        }

        // Observe Streaming Response (for the "typing" effect of current message)
        lifecycleScope.launch {
            viewModel.currentStreamingResponse.collect { currentText ->
                if (currentText.isNotEmpty()) {
                    // Create a temporary message to show the stream
                    val tempMessage = ChatMessage(
                        id = -1L, // Temporary ID
                        text = currentText,
                        isUser = false,
                        modelUsed = "Typing..."
                    )

                    // We need to merge this with the existing history.
                    // Since the DB updates are asynchronous, we take the current list from adapter
                    // (which represents DB state) and append the stream.
                    // Note: This is a visual hack. A cleaner way is to have a composite state in ViewModel.
                    // But for this structure, this works.

                    // We get the current list from adapter logic indirectly or assume we are at end.
                    // Ideally, ChatAdapter should support "addOrUpdateLastAiMessage".

                    chatAdapter.updateStreamingMessage(tempMessage)
                    binding.rvChat.smoothScrollToPosition(chatAdapter.itemCount - 1)
                }
            }
        }
    }

    private fun showSettingsDialog() {
        SettingsBottomSheet().show(parentFragmentManager, "SettingsBottomSheet")
    }

    private fun copyToClipboard(text: String) {
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Gemini Chat", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
