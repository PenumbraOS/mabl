package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ILlmCallback;
import com.penumbraos.mabl.sdk.ToolDefinition;
import com.penumbraos.mabl.sdk.ConversationMessage;

interface ILlmService {
    void generateResponse(in ConversationMessage[] messages, in ToolDefinition[] tools, ILlmCallback callback);
    void setAvailableTools(in ToolDefinition[] tools);
}