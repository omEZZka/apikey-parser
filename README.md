# aAi — Android AI Agent Assistant

AI-ассистент с полным управлением Android-устройством, агентским чатом и локальными инструментами.

## Стек
- Kotlin + Jetpack Compose (Material 3, тёмная тема)
- MVVM + Clean Architecture (`data` / `domain` / `ui` / `service` / `tools`)
- Ktor Client (OkHttp engine) — SSE streaming, OpenAI-совместимый `/v1/chat/completions` с Function Calling
- Room — история чатов и сессий
- EncryptedSharedPreferences (Android Keystore) — API-ключи и настройки
- minSdk 26, targetSdk 35

## Возможности
- **Чат**: streaming-вывод, Markdown (заголовки, списки, код с кнопкой «Копировать»), сворачиваемые блоки Tool Calls (аргументы + результат), Navigation Drawer с сессиями (создание/переименование/удаление).
- **Настройки**: пресеты провайдеров (OpenAI, OpenRouter, DeepSeek, Groq, Gemini, Mistral, локальный), Base URL, API Key (показ/скрытие), проверка соединения, Model ID + поиск по списку `GET /models`, системный промт со сбросом, temperature, max_tokens, лимит шагов агента, статусы разрешений ON/OFF.
- **Управление устройством** (AccessibilityService): `get_screen_state`, `tap`, `long_press`, `swipe`, `type_text` (SET_TEXT + clipboard fallback), `global_action` (BACK/HOME/RECENTS/NOTIFICATIONS/…), `open_app`.
- **Интернет из приложения**: `web_search` (DuckDuckGo без ключа), `web_read` (страница → чистый текст), `http_request` (любой REST API: GET/POST/PUT/DELETE, заголовки, тело).
- **Локальные инструменты**: `run_shell` (ProcessBuilder, таймаут), `file_read` / `file_write` / `file_list` / `file_delete`.
- **Фото в чат**: 📎 до 4 фото на сообщение (Photo Picker), автосжатие до 1280px JPEG, EXIF-поворот, мультимодальный `image_url` (base64) в OpenAI-совместимом формате; превью перед отправкой, отображение в истории (Room v2).
- **Остановка агента**: кнопка «Остановить агента» в чате и «Стоп» в мини-чате — мгновенная отмена корутины (стрим + инструменты), корректное завершение tool_calls в истории, кнопка «Продолжить» после остановки.
- **SVG-дизайн**: весь UI на собственных vector drawable иконках (31 шт., `res/drawable/ic_*`), библиотека material-icons удалена из зависимостей — APK меньше.
- **Утилиты агента**: `wait` (пауза до 10с), `clipboard` (чтение/запись буфера обмена), копирование любого ответа ассистента тапом по иконке.
- **Мини-чат поверх окон**: кнопка ⧉ рядом с настройками сворачивает приложение в плавающую панель; перетаскивание за шапку; через 8с бездействия (когда агент не работает) панель превращается в «таблетку» — только ввод + отправка, частично спрятанную за правый край экрана.
- **ReAct-цикл**: модель вызывает инструменты, получает результаты и продолжает, пока задача не решена (лимит итераций настраивается).
- **Оверлей**: плавающая кнопка (SYSTEM_ALERT_WINDOW) + компактная нижняя панель поверх любого приложения.
- **Системный ассистент**: `ACTION_ASSIST` + VoiceInteractionService.

## Сборка
CI: GitHub Actions (`.github/workflows/build.yml`) — JDK 17, `./gradlew assembleDebug`, APK в Artifacts (`aAi-debug-apk`).

Локально:
```bash
./gradlew assembleDebug
```

## Разрешения на устройстве
1. **Специальные возможности**: Настройки → Специальные возможности → aAi Device Control → Вкл.
2. **Поверх других приложений**: Настройки → Приложения → aAi Agent → Поверх других приложений → Разрешить (или кнопка в экране Настройки приложения).
3. **Доступ ко всем файлам**: Настройки → Приложения → aAi Agent → Доступ ко всем файлам → Разрешить (Android 11+).
4. **Ассистент по умолчанию** (опционально): Настройки → Приложения по умолчанию → Цифровой ассистент → aAi Agent.

Все переходы доступны прямо с экрана «Настройки» в приложении.