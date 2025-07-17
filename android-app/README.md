# AI Chat Multi-Service Android APP

基於原有 Web 版本的 AI Chat Multi-Service Platform 完整移植到 Android 原生應用程式。

## 🚀 功能特色

### 核心功能

- 🤖 **多 AI 服務支援**: 整合 27+ 種 AI 服務 (ChatGPT、Claude、Gemini、Mistral 等)
- 🔐 **Portal 服務**: 支援原始 Portal 服務模式
- 💬 **即時對話**: 類似 WhatsApp 的聊天界面
- 📝 **Markdown 支援**: 完整的 Markdown 渲染和程式碼高亮
- 📱 **Material Design 3**: 現代化的 UI 設計

### 安全功能

- 🔒 **端到端加密**: 使用 Android Keystore 安全存儲 API Keys
- 👆 **生物識別**: 支援指紋和臉部解鎖
- 🔐 **應用鎖定**: 自動鎖定和手動鎖定功能
- 🛡️ **安全通信**: 所有網路請求使用 HTTPS 和證書固定

### 資料管理

- 💾 **離線存儲**: Room 資料庫本地存儲聊天記錄
- ☁️ **備份還原**: JSON 格式完整備份/還原
- 🔄 **跨裝置同步**: 支援備份檔案分享
- 📊 **資料統計**: 詳細的使用統計和分析

### 用戶體驗

- 🌙 **主題支援**: 深色/淺色/跟隨系統主題
- 🔔 **智能通知**: 後台回應完成通知
- ⚙️ **豐富設定**: 20+ 個可自訂設定選項
- 🎨 **自適應 UI**: 支援平板和多種螢幕尺寸

## 🏗️ 技術架構

### 開發框架

- **語言**: Kotlin 100%
- **UI**: Jetpack Compose + Material Design 3
- **架構**: MVVM + Clean Architecture
- **依賴注入**: Hilt
- **非同步**: Kotlin Coroutines + Flow

### 核心庫

```gradle
// UI 框架
androidx.compose:compose-bom:2023.10.01
androidx.compose.material3:material3
androidx.activity:activity-compose

// 架構組件
androidx.lifecycle:lifecycle-viewmodel-compose
androidx.navigation:navigation-compose
androidx.hilt:hilt-navigation-compose

// 資料庫
androidx.room:room-runtime
androidx.room:room-ktx

// 網路
com.squareup.retrofit2:retrofit
com.squareup.okhttp3:okhttp
com.squareup.okhttp3:logging-interceptor

// 安全
androidx.security:security-crypto
androidx.biometric:biometric

// 其他
androidx.work:work-runtime-ktx (背景任務)
androidx.documentfile:documentfile (檔案管理)
```

## 📁 專案結構

```
app/src/main/java/com/aibridge/chat/
├── data/                   # 資料層
│   ├── api/               # API 服務
│   ├── database/          # Room 資料庫
│   └── repository/        # 資料倉儲
├── domain/                # 領域層
│   └── model/            # 資料模型
├── presentation/          # 展示層
│   ├── ui/               # Compose UI 組件
│   ├── viewmodel/        # ViewModels
│   └── theme/            # 主題設定
├── security/             # 安全管理
├── utils/                # 工具類
└── di/                   # 依賴注入
```

## 🔧 建置說明

### 環境需求

- Android Studio 最新版本
- Kotlin 1.9+
- Android SDK 34
- 最低支援 Android 7.0 (API 24)

### 建置步驟

1. Clone 專案

```bash
git clone <repository-url>
cd AIBridge/android-app
```

2. 設定 API 配置

```kotlin
// 編輯 app/src/main/java/com/aibridge/chat/config/ApiConfig.kt
object ApiConfig {
    const val DEFAULT_BASE_URL = "你的後端API地址"
}
```

3. 建置專案

```bash
./gradlew assembleDebug
```

4. 安裝到設備

```bash
./gradlew installDebug
```

## 📱 使用說明

### 首次設定

1. 開啟應用並創建第一個對話
2. 設定 Portal 服務憑證 (用戶名/密碼/基礎 URL)
3. 或者添加 API Keys 用於直接服務存取

### 基本操作

- **開始對話**: 點擊 ➕ 按鈕創建新對話
- **切換服務**: 在對話設定中選擇不同的 AI 服務
- **備份資料**: 設定 → 備份 → 匯出備份檔案
- **主題切換**: 設定 → 外觀 → 主題模式

### 進階功能

- **Markdown**: 自動渲染程式碼區塊和格式
- **長按訊息**: 複製、重新生成、分享
- **生物識別**: 設定 → 安全 → 啟用指紋解鎖
- **自動備份**: 設定 → 備份 → 啟用自動備份

## 🔒 隱私與安全

- ✅ **本地優先**: 所有資料主要存儲在設備本地
- ✅ **加密存儲**: API Keys 使用 Android Keystore 加密
- ✅ **無追蹤**: 不收集用戶行為資料
- ✅ **GDPR 合規**: 支援資料匯出和刪除
- ✅ **開源透明**: 程式碼完全開放檢視

## 📊 效能特色

- **啟動速度**: < 2 秒冷啟動
- **記憶體使用**: 平均 < 100MB
- **電池優化**: 智能後台管理
- **網路效率**: 請求壓縮和快取
- **離線功能**: 可離線瀏覽歷史對話

## 🐛 問題回報

如果您遇到問題，請提供以下資訊：

1. Android 版本和設備型號
2. 應用版本號
3. 具體錯誤情況和步驟
4. 相關的錯誤日誌 (可在設定 → 診斷中找到)

## 📝 版本歷史

### v1.0.0 (目前版本)

- ✅ 基礎聊天功能
- ✅ Portal 服務整合
- ✅ 安全存儲系統
- ✅ 備份還原功能
- ✅ Material Design 3 UI
- ✅ 生物識別認證
- ✅ Markdown 渲染
- ✅ 多主題支援

## 🤝 貢獻指南

歡迎提交 Issue 和 Pull Request！

### 開發流程

1. Fork 專案
2. 創建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交變更 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 開啟 Pull Request

## 📄 授權條款

本專案採用 MIT 授權條款 - 詳見 [LICENSE](LICENSE) 文件

## 🙏 致謝

- Material Design 團隊提供優秀的設計規範
- Jetpack Compose 團隊的現代 UI 框架
- 所有開源貢獻者的努力
