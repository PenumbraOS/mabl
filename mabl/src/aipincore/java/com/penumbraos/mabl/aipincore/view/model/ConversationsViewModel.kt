package com.penumbraos.mabl.aipincore.view.model

import androidx.lifecycle.ViewModel
import com.penumbraos.mabl.data.types.Conversation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ConversationsViewModel(private val viewModel: PlatformViewModel) : ViewModel() {
    val conversationsWithInjectedTitle: Flow<List<Conversation>> = flow {
        viewModel.database.conversationDao().getAllConversations().collect {
            emit(it.map {
                val firstMessage = viewModel.database.conversationDao().getFirstUserMessage(it.id)
                Conversation(
                    id = it.id,
                    title = firstMessage ?: it.title,
                    createdAt = it.createdAt,
                    lastActivity = it.lastActivity,
                    isActive = it.isActive
                )
            })
        }
    }

    fun openConversation(id: String) {
        viewModel.navViewModel.pushView(ConversationDisplayNav(id))
    }
}