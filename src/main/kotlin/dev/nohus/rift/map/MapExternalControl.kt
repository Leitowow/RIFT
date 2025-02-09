package dev.nohus.rift.map

import dev.nohus.rift.DataEvent
import dev.nohus.rift.windowing.WindowManager
import dev.nohus.rift.windowing.WindowManager.RiftWindow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single

@Single
class MapExternalControl(
    private val windowManager: WindowManager,
) {

    private val _event = MutableStateFlow<DataEvent<MapExternalControlEvent>?>(null)
    val event = _event.asStateFlow()

    sealed interface MapExternalControlEvent {
        data class ShowSystem(val solarSystemId: Int) : MapExternalControlEvent
        data class ShowSystemOnRegionMap(val solarSystemId: Int) : MapExternalControlEvent
    }

    fun showSystem(solarSystemId: Int) {
        windowManager.onWindowOpen(RiftWindow.Map)
        _event.tryEmit(DataEvent(MapExternalControlEvent.ShowSystem(solarSystemId)))
    }

    fun showSystemOnRegionMap(solarSystemId: Int) {
        windowManager.onWindowOpen(RiftWindow.Map)
        _event.tryEmit(DataEvent(MapExternalControlEvent.ShowSystemOnRegionMap(solarSystemId)))
    }
}
