package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ILlmCallback;

interface ILlmService {
    void generateResponse(String prompt, ILlmCallback callback);
}