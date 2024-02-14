package com.wigglydonplugins.AutoCrafting;

import com.google.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.concurrent.TimeUnit;
import net.runelite.api.Client;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;

public class AutoCraftingOverlay extends OverlayPanel {

    private final Client client;
    private final SpriteManager spriteManager;
    private final AutoCraftingPlugin plugin;

    private double killsPerHour = 0.0;




    @Inject
    private AutoCraftingOverlay(Client client, SpriteManager spriteManager, AutoCraftingPlugin plugin) {
        super(plugin);
        this.client = client;
        this.spriteManager = spriteManager;
        this.plugin = plugin;
        setPosition(OverlayPosition.BOTTOM_RIGHT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics2D) {
        String killsText;
        if (killsPerHour != 0.0) {
            killsText = String.format("%.1f", killsPerHour);
        } else {
            killsText = "0.0";
        }
        panelComponent.getChildren().clear();

        return super.render(graphics2D);
    }

    private LineComponent buildLine(String left, String right) {
        return LineComponent.builder()
                .left(left)
                .right(right)
                .leftColor(Color.WHITE)
                .rightColor(Color.YELLOW)
                .build();
    }

    private String formatTime(Long timeInMillis) {
        long hours = TimeUnit.MILLISECONDS.toHours(timeInMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis) % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
