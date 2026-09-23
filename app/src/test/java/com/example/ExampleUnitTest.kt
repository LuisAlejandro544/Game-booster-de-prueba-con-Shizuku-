package com.example

import com.example.data.GameItem
import com.example.data.InstalledApp
import com.example.shizuku.GameRenderState
import com.example.viewmodel.BoosterUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas unitarias de estados de renderizado, biblioteca y configuración gráfica.
 */
class ExampleUnitTest {
    @Test
    fun defaultGameRenderState_hasNativeValues() {
        val defaultState = GameRenderState()
        assertEquals(1.0f, defaultState.renderScale, 0.001f)
        assertFalse(defaultState.isDownscaleActive)
        assertFalse(defaultState.isMsaaDisabled)
        assertFalse(defaultState.isOperating)
    }

    @Test
    fun customGameRenderState_reflectsDownscaleAndMsaa() {
        val activeState = GameRenderState(
            renderScale = 0.70f,
            isDownscaleActive = true,
            isMsaaDisabled = true,
            statusMessage = "Render scale activo: 70%"
        )
        assertEquals(0.70f, activeState.renderScale, 0.001f)
        assertTrue(activeState.isDownscaleActive)
        assertTrue(activeState.isMsaaDisabled)
        assertEquals("Render scale activo: 70%", activeState.statusMessage)
    }

    @Test
    fun boosterUiState_startsWithoutSimulatedGames() {
        val state = BoosterUiState()
        // La biblioteca ya no tiene juegos simulados precargados
        assertTrue("La lista de juegos inicial debe estar vacía para que el usuario agregue los suyos", state.gamesList.isEmpty())
        assertFalse(state.isAppPickerOpen)
        assertFalse(state.isScanningApps)
    }

    @Test
    fun installedAppAndGameItem_initialization() {
        val app = InstalledApp(
            packageName = "com.sample.game",
            appName = "Sample Game",
            isGame = true
        )
        assertEquals("com.sample.game", app.packageName)
        assertEquals("Sample Game", app.appName)
        assertTrue(app.isGame)

        val game = GameItem(
            id = "123",
            name = app.appName,
            packageName = app.packageName,
            targetResolutionText = "720p @ 320 DPI"
        )
        assertEquals("123", game.id)
        assertEquals("Sample Game", game.name)
        assertEquals("com.sample.game", game.packageName)
    }
}
