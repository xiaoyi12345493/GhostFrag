package com.ghostfrag.app.shizuku;

import com.ghostfrag.app.shizuku.IGhostfragCallback;
import com.ghostfrag.app.shizuku.IGhostfragStatusCallback;

interface IGhostfragUserService {
    void destroy() = 16777114;
    void runExploit(int primaryCpu, int consumerCpu, boolean safeMode, boolean forceAttack, in byte[] profileBlob, @nullable String debugDir, IGhostfragCallback callback, IGhostfragStatusCallback statusCallback) = 2;
    void runExploitAsManager(int primaryCpu, int consumerCpu, boolean safeMode, boolean forceAttack, in byte[] profileBlob, @nullable String debugDir, @nullable String managerPackage, IGhostfragCallback callback, IGhostfragStatusCallback statusCallback) = 3;
}
