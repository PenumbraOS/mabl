package com.penumbraos.mabl.sdk;

interface ISttCallback {
    void onPartialTranscription(String partialText);
    void onFinalTranscription(String finalText);
    void onError(String errorMessage);
}