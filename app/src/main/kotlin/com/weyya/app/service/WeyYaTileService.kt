package com.weyya.app.service

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.weyya.app.R
import com.weyya.app.data.prefs.UserPreferences
import com.weyya.app.domain.model.BlockingMode
import com.weyya.app.ui.common.labelRes
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
        val (isActive, mode) = runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            val prefs = entryPoint().userPreferences()
            Pair(prefs.isActive.first(), prefs.blockingMode.first())
        }
        updateTile(isActive, mode)
    }

    override fun onClick() {
        super.onClick()
        // Toggle and read mode in a single blocking section, then paint the new state —
        // no second DataStore read just to re-fetch what we already wrote.
        val (isActive, mode) = runBlocking(kotlinx.coroutines.Dispatchers.IO) {
            val prefs = entryPoint().userPreferences()
            val newActive = !prefs.isActive.first()
            prefs.setActive(newActive)
            Pair(newActive, prefs.blockingMode.first())
        }
        updateTile(isActive, mode)
    }

    private fun updateTile(isActive: Boolean, mode: BlockingMode) {
        val tile = qsTile ?: return
        tile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = if (!isActive) getString(R.string.protection_off) else getString(mode.labelRes())
        tile.updateTile()
    }
}
