package com.penumbraos.mabl.sdk;

parcelable ToolCall {
    String id;
    String name;
    String parameters;
    /**
     * If true, result will be given to LLM. If false, result will be given to TTS directly
     */
    boolean isLLM;
}