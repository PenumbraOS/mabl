package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ToolCall;

parcelable ConversationMessage {
    // "user", "assistant", "tool"
    String type;
    String content;
    ToolCall[] toolCalls;
    String toolCallId;
}