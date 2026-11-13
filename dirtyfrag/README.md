# GhostFrag 附属包

这是 GhostFrag 的可选附属 APK，基于 DirtyFrag 的 CVE-2026-43284 漏洞链。
主 App 会检测它、内核 KMI 和带 `libksud.so` 的 SU 管理器，并在选择附属包引擎时启动它。

`applicationId` 保持 `df.root`：8 个预编译 LKM 里写死了
`/data/user_de/0/df.root/...`，改包名会导致模块加载失败。

构建：

```sh
./gradlew :dirtyfrag:assembleDebug
```

产物：`dirtyfrag/build/outputs/apk/debug/dirtyfrag-debug.apk`

上游 DirtyFrag 的完整说明见 [UPSTREAM_README.md](UPSTREAM_README.md)。
