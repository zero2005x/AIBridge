# AIBridge Android Chat App

一個基於 Android 的智能聊天應用程式，整合 Portal API 實現 AI 對話功能。

## 📱 功能特色

- **Portal API 整合**：支援多個 Portal ID 的動態切換
- **會話管理**：自動處理會話過期和重新認證
- **錯誤處理**：完善的 403 權限錯誤處理和用戶指導
- **多模式支援**：支援生產模式和測試模式
- **自動重試**：Portal ID 權限失敗時自動嘗試備選 ID

## 🚀 主要解決的問題

### 1. 403 Forbidden 錯誤修復

- ✅ 修正 Portal API 端點從 `/form` 到 `/completion`
- ✅ 實現正確的 POST 請求格式和標頭
- ✅ 匹配成功的 Web 介面行為

### 2. 會話過期處理

- ✅ 自動檢測登入頁面回應
- ✅ 觸發重新認證流程
- ✅ 無縫重試聊天請求

### 3. Portal ID 權限管理

- ✅ 智能 Portal ID 選擇和備選機制
- ✅ 詳細的權限錯誤分析和用戶指導
- ✅ 動態權限檢測和管理

## 🏗️ 技術架構

- **語言**：Kotlin
- **架構**：MVVM + Repository Pattern
- **依賴注入**：Dagger Hilt
- **網路**：OkHttp + Gson
- **UI**：Jetpack Compose

## 📂 專案結構

```
android-app/
├── app/src/main/java/com/aibridge/chat/
│   ├── config/          # API 配置
│   ├── data/
│   │   ├── api/         # API 服務層
│   │   └── repository/  # 資料存取層
│   ├── domain/model/    # 領域模型
│   ├── presentation/    # UI 層
│   └── utils/           # 工具類
└── docs/                # 文件
```

## 🔧 關鍵檔案

- `PortalApiService.kt` - Portal API 整合服務
- `AiChatApiService.kt` - 聊天 API 主要服務
- `AuthRepository.kt` - 認證和會話管理
- `ApiConfig.kt` - API 配置和模式管理

## 📋 設置步驟

1. **克隆專案**

   ```bash
   git clone [repository-url]
   cd AIBridge
   ```

2. **配置 API**

   - 修改 `ApiConfig.kt` 中的 `BACKEND_BASE_URL`
   - 設置適當的 Portal ID

3. **建置應用程式**

   ```bash
   cd android-app
   ./gradlew assembleDebug
   ```

4. **安裝 APK**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## 🔍 除錯和日誌

使用 logcat 監控應用程式運行：

```bash
adb logcat -s AuthRepository,AuthApiService,PortalApiService,AiChatApiService,ChatViewModel
```

## 📚 文件

- [Portal 權限指南](PORTAL_PERMISSIONS_GUIDE.md)
- [解決方案摘要](SOLUTION_SUMMARY.md)
- [會話過期修復](SESSION_EXPIRY_FIX.md)
- [應用程式設置指南](android-app/APP_SETUP_GUIDE.md)
- [建置成功報告](android-app/BUILD_SUCCESS.md)

## 🐛 已知問題和解決方案

### Portal API 403 錯誤

- **原因**：錯誤的端點或權限不足
- **解決**：使用正確的 `/completion` 端點和適當的 Portal ID

### 會話過期

- **原因**：長時間無活動導致會話失效
- **解決**：自動檢測和重新認證機制

### JSON 解析錯誤

- **原因**：收到 HTML 回應而非 JSON
- **解決**：登入頁面檢測和會話處理

## 🤝 貢獻指南

1. Fork 專案
2. 創建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交變更 (`git commit -m 'Add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 開啟 Pull Request

## 📄 授權

此專案採用 MIT 授權條款 - 詳見 [LICENSE](LICENSE) 文件

## 🔗 相關連結

- [Android 開發指南](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Dagger Hilt](https://dagger.dev/hilt/)

## 📧 聯絡資訊

如有問題或建議，請創建 Issue 或聯絡專案維護者。

---

**注意**：這個專案解決了 Portal API 整合中的關鍵 403 錯誤和會話管理問題，提供了穩定可靠的 AI 聊天體驗。
