package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ToolCall;

parcelable BinderConversationMessage {
    // "user", "assistant", "tool"
    String type;
    String content;
    // Optional
    ParcelFileDescriptor imageFile;
    ToolCall[] toolCalls;
    String toolCallId;
}