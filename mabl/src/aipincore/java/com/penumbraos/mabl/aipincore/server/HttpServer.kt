package com.penumbraos.mabl.aipincore.server

import android.os.ParcelFileDescriptor
import com.penumbraos.mabl.aipincore.server.types.AugmentedConversation
import com.penumbraos.mabl.aipincore.server.types.MessageWithImages
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
                            ).toByteArray()
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
                                    .toByteArray()
                            )
                        }

                        val conversation =
                            allControllers.conversationRepository.getConversation(conversationId)

                        if (conversation == null) {
                            return HttpResponse(
                                statusCode = 404,
                                body = Json.encodeToString(mapOf("error" to "Conversation not found"))
                                    .toByteArray()
                            )
                        }

                        val messages =
                            allControllers.conversationRepository.getConversationMessages(
                                conversationId
                            )

                        val messagesWithImages = messages.map { message ->
                            val images =
                                allControllers.conversationImageRepository.getImagesForMessage(
                                    message.id
                                )
                            MessageWithImages(
                                id = message.id,
                                conversationId = message.conversationId,
                                type = message.type,
                                content = message.content,
                                toolCalls = message.toolCalls,
                                toolCallId = message.toolCallId,
                                timestamp = message.timestamp,
                                images = images
                            )
                        }

                        return HttpResponse(
                            body = Json.encodeToString(
                                AugmentedConversation(
                                    id = conversation.id,
                                    title = conversation.title,
                                    createdAt = conversation.createdAt,
                                    lastActivity = conversation.lastActivity,
                                    isActive = conversation.isActive,
                                    messages = messagesWithImages
                                )
                            ).toByteArray()
                        )
                    }
                })

            client.settings.registerHttpEndpoint(
                HTTP_ID,
                "/api/image/{fileName}",
                "GET",
                object : HttpEndpointHandler {
                    override suspend fun handleRequest(request: HttpRequest): HttpResponse {
                        val fileName = request.pathParams["fileName"]

                        if (fileName == null) {
                            return HttpResponse(
                                statusCode = 400,
                                body = Json.encodeToString(mapOf("error" to "Missing fileName parameter"))
                                    .toByteArray()
                            )
                        }

                        val image =
                            allControllers.conversationImageRepository.getImageByFileName(fileName)

                        if (image == null) {
                            return HttpResponse(
                                statusCode = 404,
                                body = Json.encodeToString(mapOf("error" to "Image not found"))
                                    .toByteArray()
                            )
                        }

                        val imageFile =
                            allControllers.conversationImageRepository.getImageFile(fileName)

                        if (!imageFile.exists()) {
                            return HttpResponse(
                                statusCode = 404,
                                body = Json.encodeToString(mapOf("error" to "Image file not found on disk"))
                                    .toByteArray()
                            )
                        }

                        return HttpResponse(
                            statusCode = 200,
                            headers = mapOf(
                                "Content-Type" to image.mimeType,
                                "Content-Length" to image.fileSizeBytes.toString(),
                                "Cache-Control" to "public, max-age=3600" // Cache for 1 hour
                            ),
                            file = ParcelFileDescriptor.open(
                                imageFile,
                                ParcelFileDescriptor.MODE_READ_ONLY
                            )
                        )
                    }
                })
        }
    }
}