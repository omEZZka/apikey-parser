package com.aai.assistant.service

import android.content.Intent
import android.os.Bundle
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService

class AgentVoiceInteractionService : VoiceInteractionService() {
    override fun onReady() {
        super.onReady()
    }
}

class AgentVoiceInteractionSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = AgentSession(this)
}

class AgentSession(context: android.content.Context) : VoiceInteractionSession(context) {
    override fun onHandleAssist(state: AssistState) {
        super.onHandleAssist(state)
        val intent = Intent(context, AssistOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        finish()
    }
}