package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ToolCall;

parcelable LlmResponse {
    String text;
    ToolCall[] toolCalls;
}