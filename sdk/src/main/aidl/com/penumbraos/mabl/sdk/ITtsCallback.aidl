package com.penumbraos.mabl.sdk;

interface ITtsCallback {
    void onSpeechStarted();
    void onSpeechFinished();
    void onError(String errorMessage);
}