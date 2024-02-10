package com.wigglydonplugins.AutoVardorvis;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Provides;
import com.piggyplugins.PiggyUtils.API.PrayerUtil;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.annotations.Varbit;
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
    }

    @Override
    protected void shutDown() throws Exception {
        totalKills = 0;
        overlayManager.remove(overlay);
        drankSuperCombat = false;
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
    @Subscribe
    private void onGameTick(GameTick event) {
        long currentTime = System.currentTimeMillis();
        elapsedTime = currentTime - startTime;
        overlay.updateKillsPerHour();


        List<NPC> newAxes = NPCs.search().withId(12225).result();
        List<NPC> activeAxes = NPCs.search().withId(12227).result();
        Optional<NPC> vardorvis = NPCs.search().nameContains(VARDOVIS).first();

        WorldPoint playerTile = client.getLocalPlayer().getWorldLocation();
        Optional<TileObject> safeRock =TileObjects.search().withAction("Leave").first();

        if (client.getGameState() != GameState.LOGGED_IN || !isInFight()) {
            turnOffPrayers();
            return;
        }

        autoPray();
        if (!PrayerUtil.isPrayerActive(Prayer.PIETY)) {
            PrayerUtil.togglePrayer(Prayer.PIETY);
        }
        doBloodCaptcha();


        //initial attack
        if (vardorvis.isPresent() && safeTile != null) {
            if (vardorvis.get().getWorldLocation().getX() == safeTile.getX() + 4
                    && vardorvis.get().getWorldLocation().getY() == safeTile.getY() - 1
                    && vardorvis.get().getAnimation() == -1
            ) {
                vardorvis.ifPresent(npc -> {
                    NPCInteraction.interact(npc, "Attack");
                    if (!drankSuperCombat) {
                        Inventory.search().nameContains("Divine super combat").first().ifPresent(potion -> {
                            InventoryInteraction.useItem(potion, "Drink");
                            drankSuperCombat = true;
                        });
                    }
                });
                return;
            } else if (vardorvis.get().getWorldLocation().getX() != safeTile.getX() + 1) {
                movePlayerToTile(safeTile);
            }
        }

        if (!newAxes.isEmpty()) {
            newAxes.forEach((axe) -> {
                if (axe.getWorldLocation().getX() == safeTile.getX() - 1 && axe.getWorldLocation().getY() == safeTile.getY() - 1) {
                    handleAxeMove();
                }
            });
        }

        if (!activeAxes.isEmpty()) {
            activeAxes.forEach((axe) -> {
                if (axe.getWorldLocation().getX() == safeTile.getX() + 1 && axe.getWorldLocation().getY() == safeTile.getY() - 1) {
                    axeTicks = 1;
                    handleAxeMove();
                }
            });
        }

        if (safeRock.isPresent()) {
            WorldPoint safeRockLocation = safeRock.get().getWorldLocation();
            safeTile = new WorldPoint(safeRockLocation.getX() + 6, safeRockLocation.getY() - 10, 0);
            axeMoveTile = new WorldPoint(safeTile.getX() + 2, safeTile.getY() - 2, 0);
        }

        if (safeTile != null) {
            if (playerTile.getX() != safeTile.getX() || playerTile.getY() != safeTile.getY()) {
                movePlayerToTile(safeTile);
                return;
            } else {
                eat(config.EATAT());
                drinkPrayer(config.DRINKPRAYERAT());
            }
        }

        if (!client.getLocalPlayer().isInteracting()) {
            NPCs.search().nameContains(VARDOVIS).first().ifPresent(npc -> {
                NPCInteraction.interact(npc, "Attack");
            });
        }
    }



    private boolean drankSuperCombat = false;
    @Subscribe
    private void onVarbitChanged(VarbitChanged event) {
        if (event.getVarpId() == Varbits.DIVINE_SUPER_COMBAT) {
            drankSuperCombat = true;
            if (event.getValue() <= 100000000) {
                Inventory.search().nameContains("Divine super combat").first().ifPresent(potion -> {
                    InventoryInteraction.useItem(potion, "Drink");
                });
            }
        }
    }

    @Subscribe
    private void onProjectileMoved(ProjectileMoved event) {
        if (client.getGameState() != GameState.LOGGED_IN || !isInFight()) {
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

    public boolean isInFight() {
        return client.isInInstancedRegion() && NPCs.search().nameContains(VARDOVIS).nearestToPlayer().isPresent();
    }
}
