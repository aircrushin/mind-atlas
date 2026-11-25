package com.aircrushin.mindatlas

import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aircrushin.mindatlas.ui.AssistantWindow
import com.aircrushin.mindatlas.ui.CardWindow
import com.aircrushin.mindatlas.ui.GraphVolume
import com.aircrushin.mindatlas.ui.NotesWindow
import com.aircrushin.mindatlas.ui.ReaderWindow
import com.aircrushin.mindatlas.ui.SettingsWindow
import com.pico.spatial.ui.design.PicoTheme
import com.pico.spatial.ui.foundation.dsl.DefaultWindowContainer
import com.pico.spatial.ui.foundation.dsl.Form
import com.pico.spatial.ui.foundation.dsl.SpatialAppScope
import com.pico.spatial.ui.foundation.dsl.WindowContainer
import com.pico.spatial.ui.foundation.dsl.WindowContainerSize
import com.pico.spatial.ui.foundation.dsl.WorldScale
import com.pico.spatial.ui.platform.resize.ContainerResizeType

/** Window container ids. Every panel of the workbench is its own spatial window. */
object Windows {
    const val NOTES = "notes"
    const val ASSISTANT = "assistant"
    const val GRAPH = "graph"
    const val CARD = "card"
    const val SETTINGS = "settings"
    const val ARG_CARD_ID = "card_id"
}

val GRAPH_W = 1100.dp
val GRAPH_H = 800.dp
val GRAPH_D = 600.dp

fun mindAtlasApp(scope: SpatialAppScope) =
    with(scope) {
        // 1. Reader / library: the main window.
        DefaultWindowContainer {
            PicoTheme { ReaderWindow(Modifier.windowConstraints(minWidth = 1100.dp, minHeight = 680.dp)) }
        }
        // 2. Notes window.
        WindowContainer(
            id = Windows.NOTES,
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 640.dp, height = 760.dp),
            resizeType = ContainerResizeType.ContentMinSize,
        ) {
            PicoTheme { NotesWindow(Modifier.windowConstraints(minWidth = 520.dp, minHeight = 560.dp)) }
        }
        // 3. AI assistant window.
        WindowContainer(
            id = Windows.ASSISTANT,
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 640.dp, height = 760.dp),
            resizeType = ContainerResizeType.ContentMinSize,
        ) {
            PicoTheme { AssistantWindow(Modifier.windowConstraints(minWidth = 520.dp, minHeight = 560.dp)) }
        }
        // 4. Knowledge graph: a volumetric window, nodes float at different depths.
        WindowContainer(
            id = Windows.GRAPH,
            form = Form.Volumetric,
            defaultSize = WindowContainerSize(width = GRAPH_W, height = GRAPH_H, depth = GRAPH_D),
            resizeType = ContainerResizeType.ContentSize,
            worldScale = WorldScale.Fixed,
            enableMaterialBackground = false,
        ) {
            PicoTheme {
                GraphVolume(Modifier.windowConstraints(width = GRAPH_W, height = GRAPH_H, depth = GRAPH_D))
            }
        }
        // 5. A knowledge card popped out into its own window (one window per card, keyed by tag).
        WindowContainer(
            id = Windows.CARD,
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 520.dp, height = 600.dp),
            resizeType = ContainerResizeType.ContentMinSize,
        ) {
            PicoTheme {
                CardWindow(
                    cardId = bundle?.getString(Windows.ARG_CARD_ID),
                    modifier = Modifier.windowConstraints(minWidth = 460.dp, minHeight = 480.dp),
                )
            }
        }
        // 6. Settings (AI endpoint).
        WindowContainer(
            id = Windows.SETTINGS,
            form = Form.Planar,
            defaultSize = WindowContainerSize(width = 640.dp, height = 640.dp),
            resizeType = ContainerResizeType.ContentMinSize,
        ) {
            PicoTheme { SettingsWindow(Modifier.windowConstraints(minWidth = 560.dp, minHeight = 560.dp)) }
        }
    }
