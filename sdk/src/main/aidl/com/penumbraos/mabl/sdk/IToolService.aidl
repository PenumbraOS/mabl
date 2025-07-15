package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ToolCall;
import com.penumbraos.mabl.sdk.IToolCallback;
import com.penumbraos.mabl.sdk.ToolDefinition;

interface IToolService {
    void executeTool(in ToolCall call, IToolCallback callback);
    ToolDefinition[] getToolDefinitions();
}