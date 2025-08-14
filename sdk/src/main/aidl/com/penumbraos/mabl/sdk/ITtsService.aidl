package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ITtsCallback;

interface ITtsService {
    void registerCallback(in ITtsCallback callback);
    /**
    * Stop any utterances currently being spoken and immediately begin speaking the given text
    */
    void speakImmediately(String text);
    /**
    * Appends to the current utterance and continues speaking it. Can receive single or multiple words
    */
    void speakIncremental(String text);
    /**
    * Stop any utterances currently being spoken
    */
    void stopSpeaking();
}