package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.LlmResponse;

interface ILlmCallback {
    void onResponse(in LlmResponse response);
}