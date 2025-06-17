package com.penumbraos.mabl.sdk;

import com.penumbraos.mabl.sdk.ISttCallback;

interface ISttService {
    void startListening(in ISttCallback callback);
    void stopListening();
}