package com.ghostfrag.app.shizuku;

oneway interface IGhostfragCallback {
    void onLog(String line);
    void onComplete(int exitCode);
}
