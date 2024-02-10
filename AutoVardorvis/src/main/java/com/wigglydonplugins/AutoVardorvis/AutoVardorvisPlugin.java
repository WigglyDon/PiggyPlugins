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
    private static final String VARDOVIS = "Vardorvis";
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
    private boolean needsToEat(int at) {
        return client.getBoostedSkillLevel(Skill.HITPOINTS) <= at;
    }


    private void teleToHouse() {
        InventoryInteraction.useItem("Teleport to house", "Break");
        drankSuperCombat = false;
    }

    private boolean needsToDrinkPrayer(int at) {
        return client.getBoostedSkillLevel(Skill.PRAYER) <= at;
    }
    private void eat(int at) {
        if (needsToEat(at)) {
            Inventory.search().withAction("Eat").result().stream()
                    .findFirst()
                    .ifPresentOrElse(food -> InventoryInteraction.useItem(food, "Eat"),
                            () -> teleToHouse()
                            );
        }
    }

    private void drinkPrayer(int at) {
        if (needsToDrinkPrayer(at)) {
            Inventory.search().nameContains("Prayer potion").result().stream()
                    .findFirst()
                    .ifPresentOrElse(prayerPotion -> InventoryInteraction.useItem(prayerPotion, "Drink"),
                            () -> teleToHouse()
                            );
        }
    }

    private void autoPray() {
        if (rangeTicks > 0) {
            rangeTicks--;
            if (rangeTicks == 0) {
                rangeCooldown = 3;
            }
        }

        if (rangeTicks == 0) {
            rangeProjectile = null;
            if (rangeCooldown > 0) {
                rangeCooldown--;
            }
        }
        handleRangeFirstGameTick();
    }

    WorldPoint safeTile = null;
    WorldPoint axeMoveTile = null;
    private int axeTicks = 0;

    private void handleAxeMove() {
        switch (axeTicks) {
            case 0:
                break;
            case 1:
                movePlayerToTile(axeMoveTile);
                break;
        }
        if (axeTicks == 1) {
            axeTicks = 0;
        } else {
            axeTicks++;
        }
    }
    private void movePlayerToTile(WorldPoint tile) {
        MousePackets.queueClickPacket();
        MovementPackets.queueMovement(tile);
    }

    private void handleRangeFirstGameTick() {
        if (rangeTicks > 0) {
            if (!PrayerUtil.isPrayerActive(Prayer.PROTECT_FROM_MISSILES)) {
                PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MISSILES);
            }
        } else {
            if (!PrayerUtil.isPrayerActive(Prayer.PROTECT_FROM_MELEE)) {
                PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MELEE);
            }
        }
    }

    private void turnOffPrayers() {
        if (PrayerUtil.isPrayerActive(Prayer.PIETY)) {
            PrayerUtil.togglePrayer(Prayer.PIETY);
        }
        if (PrayerUtil.isPrayerActive(Prayer.PROTECT_FROM_MELEE)) {
            PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MELEE);
        }
    }








    //YEET
    public class MainClassContext {
        private final Client client;

        public MainClassContext(Client client) {
            this.client = client;
        }

        public Client getClient() {
            return client;
        }
    }

    //YEET

    private void handleBotState(State botState) {
        if (botState == null) {
            System.out.println("Null state...");
            return;
        }
        MainClassContext context = new MainClassContext(client);
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
    private void doBloodCaptcha() {
        List<Widget> captchaBlood = Widgets.search().filter(widget -> widget.getParentId() != 9764864).hiddenState(false).withAction("Destroy").result();
        if (!captchaBlood.isEmpty()) {
            captchaBlood.forEach(x -> {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetAction(x, "Destroy");
            });
        }
    }

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
