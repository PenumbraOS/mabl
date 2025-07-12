package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ILlmConfigCallback;

interface ILlmConfigService {
    void getAvailableConfigs(ILlmConfigCallback callback);
}