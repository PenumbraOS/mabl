package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ILlmCallback;
import com.penumbraos.mabl.sdk.ToolDefinition;

interface ILlmService {
    void generateResponse(String prompt, ILlmCallback callback);
    void setAvailableTools(in ToolDefinition[] tools);
}