package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ITtsCallback;

interface ITtsService {
    void speak(String text, in ITtsCallback callback);
}