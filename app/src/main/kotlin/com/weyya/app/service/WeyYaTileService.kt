package com.weyya.app.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.weyya.app.MainActivity
import com.weyya.app.data.prefs.UserPreferences
import com.weyya.app.domain.model.BlockingMode
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class WeyYaTileService : TileService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface TileEntryPoint {
        fun userPreferences(): UserPreferences
    }

    private fun entryPoint(): TileEntryPoint =
        EntryPointAccessors.fromApplication(applicationContext, TileEntryPoint::class.java)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val prefs = entryPoint().userPreferences()
        val current = runBlocking { prefs.isActive.first() }
        runBlocking { prefs.setActive(!current) }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val prefs = entryPoint().userPreferences()
        val isActive = runBlocking { prefs.isActive.first() }
        val mode = runBlocking { prefs.blockingMode.first() }

        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = when {
            !isActive -> "OFF"
            mode == BlockingMode.ALL_CALLERS -> "Bloquear todo"
            else -> "Desconocidos"
        }
        tile.updateTile()
    }
}
