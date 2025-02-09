package dev.nohus.rift.windowing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import dev.nohus.rift.Event
import dev.nohus.rift.about.AboutWindow
import dev.nohus.rift.alerts.list.AlertsWindow
import dev.nohus.rift.assets.AssetsWindow
import dev.nohus.rift.characters.CharactersWindow
import dev.nohus.rift.configurationpack.ConfigurationPackReminderWindow
import dev.nohus.rift.intel.IntelWindow
import dev.nohus.rift.intel.settings.IntelSettingsWindow
import dev.nohus.rift.jabber.JabberInputModel
import dev.nohus.rift.jabber.JabberWindow
import dev.nohus.rift.map.MapWindow
import dev.nohus.rift.map.settings.MapSettingsWindow
import dev.nohus.rift.neocom.NeocomWindow
import dev.nohus.rift.pings.PingsWindow
import dev.nohus.rift.settings.SettingsInputModel
import dev.nohus.rift.settings.SettingsWindow
import dev.nohus.rift.settings.persistence.Settings
import dev.nohus.rift.settings.persistence.WindowPlacement
import dev.nohus.rift.utils.Pos
import dev.nohus.rift.utils.Size
import dev.nohus.rift.whatsnew.WhatsNewWindow
import dev.nohus.rift.windowing.WindowManager.RiftWindowState
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.skiko.MainUIDispatcher
import org.koin.core.annotation.Single
import java.time.Instant

@Single
class WindowManager(
    private val settings: Settings,
) {

    @Serializable
    enum class RiftWindow {
        Neocom, Intel, IntelSettings, Settings, Map, MapSettings, Characters, Pings, Alerts, About, Jabber,
        ConfigurationPackReminder, Assets, WhatsNew
    }

    data class RiftWindowState(
        val window: RiftWindow? = null,
        val inputModel: Any? = null,
        val windowState: WindowState,
        val isVisible: Boolean,
        val minimumSize: Pair<Int?, Int?>,
        val openTimestamp: Instant = Instant.now(),
        val bringToFrontEvent: Event? = null,
    )

    private data class WindowSizing(
        val defaultSize: Pair<Int?, Int?>,
        val minimumSize: Pair<Int?, Int?>,
    )

    private val persistentWindows = listOf(
        RiftWindow.Neocom,
        RiftWindow.Intel,
        RiftWindow.Map,
        RiftWindow.Characters,
        RiftWindow.Alerts,
        RiftWindow.Pings,
        RiftWindow.Jabber,
    )
    private var states: MutableState<Map<RiftWindow, RiftWindowState>> = mutableStateOf(emptyMap())

    init {
        if (settings.isSetupWizardFinished) {
            if (settings.isRememberOpenWindows) {
                settings.openWindows.forEach { onWindowOpen(it) }
            } else {
                onWindowOpen(RiftWindow.Neocom)
            }
        }
    }

    suspend fun saveWindowPlacements() = withContext(MainUIDispatcher) {
        rememberWindowPlacements()
    }

    @Composable
    fun composeWindows() {
        for ((window, state) in states.value) {
            CompositionLocalProvider(LocalRiftWindowState provides state) {
                when (window) {
                    RiftWindow.Neocom -> NeocomWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Neocom) })
                    RiftWindow.Intel -> IntelWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Intel) }, onTuneClick = { onWindowOpen(RiftWindow.IntelSettings) })
                    RiftWindow.IntelSettings -> IntelSettingsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.IntelSettings) })
                    RiftWindow.Settings -> SettingsWindow(state.inputModel as? SettingsInputModel ?: SettingsInputModel.Normal, state, onCloseRequest = { onWindowClose(RiftWindow.Settings) })
                    RiftWindow.Map -> MapWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Map) }, onTuneClick = { onWindowOpen(RiftWindow.MapSettings) })
                    RiftWindow.MapSettings -> MapSettingsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.MapSettings) })
                    RiftWindow.Characters -> CharactersWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Characters) })
                    RiftWindow.Alerts -> AlertsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Alerts) })
                    RiftWindow.About -> AboutWindow(state, onCloseRequest = { onWindowClose(RiftWindow.About) })
                    RiftWindow.Jabber -> JabberWindow(state.inputModel as? JabberInputModel ?: JabberInputModel.None, state, onCloseRequest = { onWindowClose(RiftWindow.Jabber) })
                    RiftWindow.Pings -> PingsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Pings) })
                    RiftWindow.ConfigurationPackReminder -> ConfigurationPackReminderWindow(state, onCloseRequest = { onWindowClose(RiftWindow.ConfigurationPackReminder) })
                    RiftWindow.Assets -> AssetsWindow(state, onCloseRequest = { onWindowClose(RiftWindow.Assets) })
                    RiftWindow.WhatsNew -> WhatsNewWindow(state, onCloseRequest = { onWindowClose(RiftWindow.WhatsNew) })
                }
            }
        }
    }

    fun onWindowOpen(window: RiftWindow, inputModel: Any? = null) {
        val state = states.value[window]?.copy(
            inputModel = inputModel,
            isVisible = true,
            openTimestamp = Instant.now(),
            bringToFrontEvent = Event(),
        ) ?: run {
            val sizing = getWindowOpenSizing(window)
            val state = WindowState(
                width = sizing.defaultSize.first?.dp ?: Dp.Unspecified,
                height = sizing.defaultSize.second?.dp ?: Dp.Unspecified,
                position = getWindowOpenPosition(window),
            )
            RiftWindowState(
                window = window,
                inputModel = inputModel,
                windowState = state,
                isVisible = true,
                minimumSize = sizing.minimumSize,
            )
        }
        settings.openWindows += window
        states.value += window to state
    }

    fun onWindowClose(window: RiftWindow) {
        settings.openWindows -= window
        if (window in persistentWindows) {
            states.value[window]?.let {
                states.value += window to it.copy(isVisible = false)
            }
        } else {
            states.value -= window
        }
    }

    private fun getWindowOpenSizing(window: RiftWindow): WindowSizing {
        val saved = if (settings.isRememberWindowPlacement) {
            settings.windowPlacements[window]?.size?.let { it.width to it.height }
        } else {
            null
        }
        return when (window) {
            RiftWindow.Neocom -> WindowSizing(defaultSize = (200 to null), minimumSize = 200 to null)
            RiftWindow.Intel -> WindowSizing(defaultSize = saved ?: (800 to 500), minimumSize = 400 to 200)
            RiftWindow.IntelSettings -> WindowSizing(defaultSize = (400 to null), minimumSize = 400 to null)
            RiftWindow.Settings -> WindowSizing(defaultSize = (820 to null), minimumSize = 820 to null)
            RiftWindow.Map -> WindowSizing(defaultSize = saved ?: (800 to 800), minimumSize = 350 to 300)
            RiftWindow.MapSettings -> WindowSizing(defaultSize = (400 to 450), minimumSize = 400 to 450)
            RiftWindow.Characters -> WindowSizing(defaultSize = saved ?: (420 to 400), minimumSize = 350 to 300)
            RiftWindow.Alerts -> WindowSizing(defaultSize = saved ?: (500 to 500), minimumSize = 500 to 500)
            RiftWindow.About -> WindowSizing(defaultSize = (450 to null), minimumSize = (450 to null))
            RiftWindow.Jabber -> WindowSizing(defaultSize = saved ?: (400 to 500), minimumSize = (200 to 200))
            RiftWindow.Pings -> WindowSizing(defaultSize = saved ?: (440 to 500), minimumSize = (440 to 300))
            RiftWindow.ConfigurationPackReminder -> WindowSizing(defaultSize = (450 to null), minimumSize = (450 to null))
            RiftWindow.Assets -> WindowSizing(defaultSize = saved ?: (500 to 500), minimumSize = (500 to 300))
            RiftWindow.WhatsNew -> WindowSizing(defaultSize = saved ?: (450 to 500), minimumSize = (450 to 500))
        }
    }

    private fun getWindowOpenPosition(window: RiftWindow): WindowPosition {
        val saved = if (settings.isRememberWindowPlacement) { settings.windowPlacements[window]?.position } else null
        saved ?: return WindowPosition.PlatformDefault
        return WindowPosition(saved.x.dp, saved.y.dp)
    }

    private fun rememberWindowPlacements() {
        states.value.forEach { (window, state) ->
            settings.windowPlacements += window to WindowPlacement(
                position = state.windowState.position.let { Pos(it.x.value.toInt(), it.y.value.toInt()) },
                size = state.windowState.size.let { Size(it.width.value.toInt(), it.height.value.toInt()) },
            )
        }
    }
}

val LocalRiftWindowState: ProvidableCompositionLocal<RiftWindowState?> = staticCompositionLocalOf { null }
val LocalRiftWindow: ProvidableCompositionLocal<ComposeWindow?> = staticCompositionLocalOf { null }
