package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ToolParameter;

parcelable ToolDefinition {
    String name;
    String description;
    ToolParameter[] parameters;
    /**
     * Example user utterances for offline intent classification. Presence of examples implies
     * the tool can be matched without cloud LLM support.
     */
    String[] examples;
    /**
     * Whether the tool is a priority tool. Priority tools will always attempt to be included in the tool list passed to the LLM
     */
    boolean isPriority;
}