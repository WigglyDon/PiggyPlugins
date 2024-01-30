package com.jrplugins.autoVorkath

import net.runelite.api.Client
import net.runelite.client.ui.overlay.Overlay
import net.runelite.client.ui.overlay.OverlayLayer
import net.runelite.client.ui.overlay.OverlayPosition
import net.runelite.client.ui.overlay.components.LineComponent
import net.runelite.client.ui.overlay.components.PanelComponent
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics2D
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AutoVorkathOverlay @Inject private constructor(private val client: Client, plugin: AutoVorkathPlugin) :
    Overlay() {
    private val panelComponent = PanelComponent()
    private val slPanel = PanelComponent()
    private val plugin: AutoVorkathPlugin = plugin

    init {
        position = OverlayPosition.BOTTOM_LEFT
        layer = OverlayLayer.ABOVE_SCENE
        isDragTargetable = true
    }

    override fun render(graphics: Graphics2D): Dimension {
        panelComponent.children.clear()
        slPanel.children.clear()

        val state = buildLine("State: ", plugin.botState.toString())
        val elapsedTime = buildLine("Runtime: ", formatTime(plugin.elapsedTime))

        panelComponent.children.addAll(listOf(state))
        panelComponent.children.addAll(listOf(elapsedTime))

        return panelComponent.render(graphics)
    }

    private fun formatTime(timeInMillis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60

        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }



    /**
     * Builds a line component with the given left and right text
     *
     * @param left
     * @param right
     * @return Returns a built line component with White left text and Yellow right text
     */
    private fun buildLine(left: String, right: String): LineComponent {
        return LineComponent.builder()
            .left(left)
            .right(right)
            .leftColor(Color.WHITE)
            .rightColor(Color.YELLOW)
            .build()
    }
}
