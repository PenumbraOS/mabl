package com.penumbraos.mabl.aipincore.server

import com.penumbraos.mabl.aipincore.server.types.ConversationWithMessages
import com.penumbraos.mabl.data.types.Conversation
import com.penumbraos.mabl.services.AllControllers
import com.penumbraos.sdk.PenumbraClient
import com.penumbraos.sdk.api.types.HttpEndpointHandler
import com.penumbraos.sdk.api.types.HttpRequest
import com.penumbraos.sdk.api.types.HttpResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val TAG = "HttpServer"
private const val HTTP_ID = "mabl"

class HttpServer(
    private val allControllers: AllControllers,
    private val coroutineScope: CoroutineScope,
    private val client: PenumbraClient,
) {
    init {
        coroutineScope.launch {
            client.waitForBridge()

            client.settings.registerHttpEndpoint(
                HTTP_ID,
                "/api/conversation",
                "GET",
                object : HttpEndpointHandler {
                    override suspend fun handleRequest(request: HttpRequest): HttpResponse {
                        val conversations =
                            allControllers.conversationRepository.getAllConversations()
                        return HttpResponse(
                            body = Json.encodeToString<List<Conversation>>(
                                conversations
                            )
                        )
                    }
                })

            client.settings.registerHttpEndpoint(
                HTTP_ID,
                "/api/conversation/{conversationId}",
                "GET",
                object : HttpEndpointHandler {
                    override suspend fun handleRequest(request: HttpRequest): HttpResponse {
                        val conversationId = request.pathParams["conversationId"]

                        if (conversationId == null) {
                            return HttpResponse(
                                statusCode = 400,
                                body = Json.encodeToString(mapOf("error" to "Missing conversationId parameter"))
                            )
                        }

                        val conversation =
                            allControllers.conversationRepository.getConversation(conversationId)

                        if (conversation == null) {
                            return HttpResponse(
                                statusCode = 404,
                                body = Json.encodeToString(mapOf("error" to "Conversation not found"))
                            )
                        }

                        val messages =
                            allControllers.conversationRepository.getConversationMessages(
                                conversationId
                            )

                        return HttpResponse(
                            body = Json.encodeToString(
                                ConversationWithMessages(
                                    id = conversation.id,
                                    title = conversation.title,
                                    createdAt = conversation.createdAt,
                                    lastActivity = conversation.lastActivity,
                                    isActive = conversation.isActive,
                                    messages = messages
                                )
                            )
                        )
                    }
                })
        }
    }
}