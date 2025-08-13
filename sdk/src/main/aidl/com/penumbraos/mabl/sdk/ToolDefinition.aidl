package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ToolParameter;

parcelable ToolDefinition {
    String name;
    String description;
    ToolParameter[] parameters;
    /**
     * Whether the tool is a priority tool. Priority tools will always attempt to be included in the tool list passed to the LLM
     */
    boolean isPriority;
}