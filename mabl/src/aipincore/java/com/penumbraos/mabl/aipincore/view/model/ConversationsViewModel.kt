package com.penumbraos.mabl.aipincore.view.model

import androidx.lifecycle.ViewModel
import com.penumbraos.mabl.data.types.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ConversationsViewModel(private val viewModel: PlatformViewModel) : ViewModel() {
    val conversationsWithInjectedTitle: Flow<List<Conversation>> =
        viewModel.database.conversationDao()
            .getConversationsWithFirstUserMessage()
            .map { conversations ->
                conversations.map { conversationWithFirstMessage ->
                    val conversation = conversationWithFirstMessage.conversation
                    conversation.copy(
                        title = conversationWithFirstMessage.firstUserMessage ?: conversation.title
                    )
                }
            }

    fun openConversation(id: String) {
        viewModel.navViewModel.pushView(ConversationDisplayNav(id))
    }
}