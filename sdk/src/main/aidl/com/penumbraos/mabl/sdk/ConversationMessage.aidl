package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ToolCall;

parcelable ConversationMessage {
    // "user", "assistant", "tool"
    String type;
    String content;
    // Optional
    ParcelFileDescriptor imageFile;
    ToolCall[] toolCalls;
    String toolCallId;
}