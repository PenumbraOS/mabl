package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.LlmResponse;

interface ILlmCallback {
    void onPartialResponse(String newToken);
    void onCompleteResponse(in LlmResponse response);
    void onError(String error);
}