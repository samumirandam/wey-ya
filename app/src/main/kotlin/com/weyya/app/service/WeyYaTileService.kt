package com.weyya.app.service

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.weyya.app.R
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
        runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            val prefs = entryPoint().userPreferences()
            prefs.setActive(!prefs.isActive.first())
        }
        updateTile()
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val (isActive, mode) = runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            val prefs = entryPoint().userPreferences()
            Pair(prefs.isActive.first(), prefs.blockingMode.first())
        }

        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = when {
            !isActive -> getString(R.string.protection_off)
            mode == BlockingMode.ALL_CALLERS -> getString(R.string.mode_all)
            else -> getString(R.string.mode_unknown)
        }
        tile.updateTile()
    }
}
