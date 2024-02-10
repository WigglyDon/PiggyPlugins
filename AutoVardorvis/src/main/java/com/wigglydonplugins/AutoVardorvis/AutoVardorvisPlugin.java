package com.wigglydonplugins.AutoVardorvis;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.InteractionApi.InventoryInteraction;
import com.google.inject.Provides;
import com.wigglydonplugins.AutoVardorvis.state.StateHandler.State;
import com.wigglydonplugins.AutoVardorvis.state.StateHandler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import com.google.inject.Inject;

@Slf4j
@PluginDescriptor(
        name = "<html><font color=\"#FF0000\">[WD]</font>Auto Vardorvis</html>",
        description = "Automated vardorvis killer"
)
public class AutoVardorvisPlugin extends Plugin {




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

    @Getter
    @Setter
    public static class MainClassContext {
        private final Client client;
        private final AutoVardorvisConfig config;
        private final boolean drankSuperCombat;

        public MainClassContext(Client client, AutoVardorvisConfig config, boolean drankSuperCombat) {
            this.client = client;
            this.config = config;
            this.drankSuperCombat = drankSuperCombat;
        }
    }

    private void handleBotState(State botState) {
        if (botState == null) {
            System.out.println("Null state...");
            return;
        }
        MainClassContext context = new MainClassContext(client, config, drankSuperCombat);
        StateHandler.handleState(botState, context);
    }
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
    private void onChatMessage(ChatMessage e) {
        if (e.getMessage().contains("Your Vardorvis kill count is:")) {
            totalKills ++;
        }
    }
}
