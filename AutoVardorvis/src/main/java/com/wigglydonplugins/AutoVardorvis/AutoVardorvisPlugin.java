package com.wigglydonplugins.AutoVardorvis;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.PrayerUtil;
import com.wigglydonplugins.AutoVardorvis.state.StateHandler.State;
import com.wigglydonplugins.AutoVardorvis.state.StateHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import com.google.inject.Inject;

import java.util.List;
import java.util.Optional;

@Slf4j
@PluginDescriptor(
        name = "<html><font color=\"#FF0000\">[WD]</font>Auto Vardorvis</html>",
        description = "Automated vardorvis killer"
)
public class AutoVardorvisPlugin extends Plugin {

    private static final int RANGE_PROJECTILE = 2521;
    private Projectile rangeProjectile;
    private int rangeTicks = 0;
    private int rangeCooldown = 0;


    int totalKills = 0;
    long startTime = System.currentTimeMillis();
    long elapsedTime = 0;
    boolean running = false;
    int tickDelay = 0;

    State botState = null;
    private boolean drankSuperCombat = false;

    @Inject
    private Client client;
    @Inject
    private SpriteManager spriteManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private AutoVardorvisOverlay overlay;
    @Inject
    private AutoVardorvisConfig config;

    @Provides
    private AutoVardorvisConfig getConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoVardorvisConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        startTime = System.currentTimeMillis();
        overlayManager.add(overlay);
        running = client.getGameState() == GameState.LOGGED_IN;
        botState = State.FIGHTING;
    }

    @Override
    protected void shutDown() throws Exception {
        totalKills = 0;
        overlayManager.remove(overlay);
        drankSuperCombat = false;
        running = false;
        botState = null;
    }

    //YEET
    @Getter
    public static class MainClassContext {
        private final Client client;
        private final AutoVardorvisConfig config;
        private final int rangeTicks;
        private final int rangeCooldown;
        private final boolean drankSuperCombat;

        public MainClassContext(Client client, AutoVardorvisConfig config, int rangeTicks, int rangeCooldown, boolean drankSuperCombat) {
            this.client = client;
            this.config = config;
            this.rangeTicks = rangeTicks;
            this.rangeCooldown = rangeCooldown;
            this.drankSuperCombat = drankSuperCombat;
        }

    }

    private void handleBotState(State botState) {
        if (botState == null) {
            System.out.println("Null state...");
            return;
        }
        MainClassContext context = new MainClassContext(client, config, rangeTicks, rangeCooldown, drankSuperCombat);
        StateHandler.handleState(botState, context);
    }

    //YEET

    @Subscribe
    private void onGameTick(GameTick event) {
        long currentTime = System.currentTimeMillis();
        elapsedTime = currentTime - startTime;
        overlay.updateKillsPerHour();

        if (running) {
            if (tickDelay > 0) {
                tickDelay--;
                return;
            }
            handleBotState(botState);
        }
    }

    @Subscribe
    private void onVarbitChanged(VarbitChanged event) {
        if (event.getVarbitId() == Varbits.DIVINE_SUPER_COMBAT) {
            drankSuperCombat = true;
            if (event.getValue() <= 10) {
                Inventory.search().nameContains("Divine super combat").first().ifPresent(potion -> {
                    InventoryInteraction.useItem(potion, "Drink");
                });
            }
        }
    }

    @Subscribe
    private void onProjectileMoved(ProjectileMoved event) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        Projectile projectile = event.getProjectile();

        if (projectile.getId() == RANGE_PROJECTILE) {
            if (rangeProjectile == null && rangeCooldown == 0) {
                rangeTicks = 4;
                rangeProjectile = projectile;
            }
        }
    }

    @Subscribe
    private void onChatMessage(ChatMessage e) {
        if (e.getMessage().contains("Your Vardorvis kill count is:")) {
            totalKills ++;
        }
    }

    /**
     * 1 tick blood captcha. Thanks, @Lunatik
     */


    public int getPrayerSprite() {
        if (rangeTicks > 0) {
            return SpriteID.PRAYER_PROTECT_FROM_MISSILES;
        }

        return SpriteID.PRAYER_PROTECT_FROM_MELEE;
    }

    public Prayer getCorrectPrayer() {
        if (rangeTicks > 0) {
            return Prayer.PROTECT_FROM_MISSILES;
        }

        return Prayer.PROTECT_FROM_MELEE;
    }
}
