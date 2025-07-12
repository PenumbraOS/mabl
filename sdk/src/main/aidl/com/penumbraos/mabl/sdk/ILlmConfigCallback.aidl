package com.penumbraos.mabl.sdk;

interface ILlmConfigCallback {
    void onConfigsLoaded(in String[] configs);
    void onError(String error);
}