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
- **Настройки**: Base URL, API Key, Model ID (+ автозагрузка `GET /models`), системный промт, temperature, max_tokens, лимит шагов агента.
- **Управление устройством** (AccessibilityService): `get_screen_state`, `tap`, `long_press`, `swipe`, `type_text` (SET_TEXT + clipboard fallback), `global_action` (BACK/HOME/RECENTS/NOTIFICATIONS/…), `open_app`.
- **Локальные инструменты**: `run_shell` (ProcessBuilder, таймаут), `file_read` / `file_write` / `file_list` / `file_delete`.
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