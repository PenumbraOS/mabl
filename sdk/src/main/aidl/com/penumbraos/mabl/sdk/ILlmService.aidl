package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ILlmCallback;
import com.penumbraos.mabl.sdk.ToolDefinition;
import com.penumbraos.mabl.sdk.BinderConversationMessage;

interface ILlmService {
    void generateResponse(in BinderConversationMessage[] messages, in ToolDefinition[] tools, ILlmCallback callback);
    void setAvailableTools(in ToolDefinition[] tools);
}