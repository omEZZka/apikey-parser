package com.aai.assistant

import android.app.Application
import com.aai.assistant.data.api.OpenAiClient
import com.aai.assistant.data.db.AppDatabase
import com.aai.assistant.data.prefs.SettingsStore
import com.aai.assistant.data.repo.ChatRepository
import com.aai.assistant.tools.ToolRegistry

class App : Application() {

    lateinit var settingsStore: SettingsStore
        private set
    lateinit var database: AppDatabase
        private set
    lateinit var client: OpenAiClient
        private set
    lateinit var toolRegistry: ToolRegistry
        private set
    lateinit var repository: ChatRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        settingsStore = SettingsStore(this)
        database = AppDatabase.get(this)
        client = OpenAiClient()
        toolRegistry = ToolRegistry(this)
        repository = ChatRepository(database.chatDao(), client, settingsStore, toolRegistry)
    }

    companion object {
        lateinit var instance: App
            private set
    }
}