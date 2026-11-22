# QMDL Android

基于 Jetpack Compose + Material Design 3 Expressive 构建的 Android 音乐下载器（最低支持 Android 8.0 / API 26），直接内置 `qqmusic-dl` Rust CLI 作为本地后端引擎，也支持连接局域网/远程 `qqmusic-dl web` 服务。

## 核心特性

- **Material Design 3 Expressive**：采用 `MaterialExpressiveTheme`、`MotionScheme.expressive()`、`LinearWavyProgressIndicator`、`LoadingIndicator`、Dynamic Color（Android 12+）以及 Android 8–11 专属翡翠主题。
- **双模式后端**：
  - **内置 CLI 引擎**：交叉编译 `qqmusic-dl`（`arm64-v8a` & `x86_64`）打包在 `jniLibs/*/libqmdl.so`，通过前台服务运行本地 HTTP API。
  - **远程服务端**：可无缝切换到 PC/NAS 上运行的 `qqmusic-dl web` 实例。
- **完整功能**：歌曲搜索/分页/历史、多选批量入队、在线直链解析与试听、下载队列实时波形进度、本地曲库管理与 Media3 ExoPlayer 播放、导出到系统 `Music/QMDL`、扫码登录（含保存二维码到相册便于单机扫码）与 Cookie 导入。

## 构建

```bash
# 重新编译内置 Rust CLI 后端（可选，仓库已附带编译产物）
./scripts/build-backend.sh /home/hhg/QQ-Music-DL

# 运行单元测试并构建 Debug APK
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew testDebugUnitTest assembleDebug
```

## 开源协议

本项目采用 [GNU Affero General Public License v3.0 or later (AGPL-3.0-or-later)](LICENSE) 开源协议。

