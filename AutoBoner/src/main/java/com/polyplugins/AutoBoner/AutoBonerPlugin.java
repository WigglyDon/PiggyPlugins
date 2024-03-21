package com.polyplugins.AutoBoner;


import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.Collections.query.NPCQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.TileObjectInteraction;
import com.example.Packets.*;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.InventoryUtil;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;
import com.example.Packets.WidgetPackets;

import java.io.Console;
import java.util.Objects;
import java.util.Optional;

@PluginDescriptor(
        name = "<html><font color=\"#7ecbf2\">[PJ]</font>AutoBoner</html>",
        description = "Its an automated boner, it does shit",
        enabledByDefault = false,
        tags = {"poly", "plugin"}
)
@Slf4j
public class AutoBonerPlugin extends Plugin {
    @Inject
    private Client client;
    @Inject
    private AutoBonerConfig config;
    @Inject
    private AutoBonerOverlay overlay;
    @Inject
    private KeyManager keyManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientThread clientThread;
    private boolean started = true;
    public int timeout = 0;

    @Provides
    private AutoBonerConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoBonerConfig.class);
    }
    @Override
    protected void startUp() throws Exception {
        keyManager.registerKeyListener(toggle);
        overlayManager.add(overlay);
        timeout = 0;
    }

    @Override
    protected void shutDown() throws Exception {
        keyManager.unregisterKeyListener(toggle);
        overlayManager.remove(overlay);
        timeout = 0;
        started = false;
    }

    @Subscribe
    private void onGameTick(GameTick event) {
        if (timeout > 0) {
            timeout--;
            return;
        }
        if (client.getGameState() != GameState.LOGGED_IN || !started) {
            return;
        }

        Inventory.search().onlyUnnoted().nameContains(config.boneName()).first().ifPresent(bone -> {
            TileObjects.search().nameContains(config.altarName()).nearestToPlayer().ifPresent(altar -> {
                MousePackets.queueClickPacket();
                MousePackets.queueClickPacket();
                ObjectPackets.queueWidgetOnTileObject(bone, altar);
            });
        });

        if (Inventory.search().onlyUnnoted().nameContains(config.boneName()).empty()) {
            Inventory.search().onlyNoted().nameContains(config.boneName()).first().ifPresent(note -> {
                NPCs.search().nameContains("Elder Chaos druid").first().ifPresent(npc -> {
                    Widgets.search().withTextContains("Exchange All:").first().ifPresent(widget -> {
                        MousePackets.queueClickPacket();
                        WidgetPackets.queueResumePause(widget.getId(), 3);
                        timeout = 1;
                    });
                    MousePackets.queueClickPacket();
                    NPCPackets.queueWidgetOnNPC(npc, note);


                });
            });
        }

        TileObjects.search().nameContains("Large door").first().ifPresent(door -> {
            MousePackets.queueClickPacket();
            TileObjectInteraction.interact(door, "Open");
        });
    }


    // Elder Chaos druid
    @Subscribe
    private void onPlayerSpawned(PlayerSpawned playerSpawned) {

        Player p = playerSpawned.getPlayer();
        if (!p.getName().equals("Beosot")) {
            System.out.println("player spotted: " + playerSpawned.getPlayer().getName());
            //logout packet
            WidgetPackets.queueWidgetActionPacket(1, 11927560, -1, -1);
        }
    }


    private final HotkeyListener toggle = new HotkeyListener(() -> config.toggle()) {
        @Override
        public void hotkeyPressed() {
            toggle();
        }
    };

    public void toggle() {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }
        started = !started;
    }
}
