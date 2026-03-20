# WeatherSampleAndroid

這是一款基於 MVVM 架構的 Android 天氣應用程式，主要提供當下所在位置與全球所選城市的目前天氣及未來一週天氣預報。

## 系統架構 (Architecture)

本專案使用 **MVVM (Model-View-ViewModel)** 架構，並結合 Android Jetpack 原生元件進行開發，徹底分離 UI 視圖與商業邏輯：

- **Model (資料層)**: 
  - 核心為 `WeatherApiClient`，負責透過 `OkHttp` 與 Open-Meteo API 進行網路通訊，取得天氣原始 JSON 資料。
  - 隨後利用 `Gson` 自動將字串反序列化轉換為 `WeatherResponse` 特定的 Data Class。
- **ViewModel (邏輯與狀態層)**: 
  - `MainViewModel` 處理所有的商業邏輯與狀態保存（如溫度換算、日期格式化等）。
  - 全面使用 `LiveData` 進行屬性綁定。當 API 回傳或發生 Exception 錯誤時，直接更新內部變數，自動渲染至前端。
- **View (UI 顯示層)**: 
  - 包含 `MainActivity` 與其託管的 `CurrentCityFragment`、`SelectedCityFragment`。
  - 完全基於 `DataBinding`，在 Layout XML 內直接讀取 `viewModel` 的參數，不需要手動 `setText()` 即可即時動態刷新介面。

## 使用到的核心函式庫 (Libraries)

- **OkHttp (v4.12.0)**: 非同步網路請求，統一 30 秒 Timeout 機制處理。
- **Gson (v2.11.0)**: JSON 資料轉換工具。
- **Kotlin Coroutines (協程)**: 以 `viewModelScope` 與 `Dispatchers.IO` 處理所有的非同步網路請求與反向地理編碼 (Geocoder) 任務。
- **Google Play Services Location**: `FusedLocationProviderClient` 獲取高精確度的裝置 GPS 即時定位。
- **MPAndroidChart (v3.1.0)**: 渲染未來一週天氣預報中的「折線圖 (LineChart)」。
- **Android Jetpack 元件**:
  - `ViewModel` & `LiveData`: 狀態與生命週期管理。
  - `DataBinding`: UI 宣告式綁定層。
  - `ViewPager2` & `TabLayout`: 首頁的左右滑動分頁功能。

## 開發與接手指南 (Development Guide)

1. **定位與反向地理編碼 (Location & Geocoder)**
   程式啟動後會檢查 `ACCESS_FINE_LOCATION` 權限。成功取得定位後，`MainActivity` 會將經緯度放入 `Geocoder` 解析出當前的繁體/英文「城市名稱」，並顯示在 UI 上。若定位失敗或拒絕權限，則會進入顯示自選城市的對話框並讓使用者自行選擇城市流程。
   
2. **多語系支援 (Multi-language)**
   目前製作繁體中文與英文版本
   
3. **錯誤與例外處理 (Error Handling)**
   `WeatherApiClient` 攔截 HTTP `4xx` (Client Error) 與 `5xx` (Server Error) 並轉拋出 Exception。`MainViewModel` catch此類錯誤後，會傳值給 `errorMessage` LiveData，進而在 `MainActivity` 中彈出錯誤警告提示 Dialog。