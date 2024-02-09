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
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ProjectileMoved;
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
        description = "Tells you what to pray against or auto prays at vardovis"
)
public class AutoVardorvisPlugin extends Plugin {

    private static final int RANGE_PROJECTILE = 2521;
    private static final String VARDOVIS = "Vardorvis";
    private Projectile rangeProjectile;
    private int rangeTicks = 0;
    private int rangeCooldown = 0;

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
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        overlayManager.remove(overlay);
    }
    private boolean needsToEat(int at) {
        return client.getBoostedSkillLevel(Skill.HITPOINTS) <= at;
    }

    private boolean needsToDrinkPrayer(int at) {
        return client.getBoostedSkillLevel(Skill.PRAYER) <= at;
    }
    private void eat(int at) {
        if (needsToEat(at)) {
            System.out.println("ATE FOOD");
            Inventory.search().withAction("Eat").result().stream()
                    .findFirst()
                    .ifPresent(food -> InventoryInteraction.useItem(food, "Eat"));
        }
    }

    private void drinkPrayer(int at) {
        if (needsToDrinkPrayer(at)) {
            System.out.println("DRANK POT");
            Inventory.search().nameContains("Prayer potion").result().stream()
                    .findFirst()
                    .ifPresent(prayerPotion -> InventoryInteraction.useItem(prayerPotion, "Drink"));
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

        if (config.autoPray()) {
            handleRangeFirstGameTick();
        }
    }

    WorldPoint safeTile = null;
    WorldPoint axeMoveTile = null;
    private int axeTicks = 0;

    private void handleAxeMove() {
        System.out.println("axe ticks: " + axeTicks);
        switch (axeTicks) {
            case 0:

                break;
            case 1:
                movePlayerToTile(axeMoveTile);
                break;
            case 2:
                movePlayerToTile(safeTile);
                break;
        }
        axeTicks ++;
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
        if (client.getGameState() != GameState.LOGGED_IN || !isInFight()) {
            turnOffPrayers();
            return;
        }

        autoPray();
        doBloodCaptcha();

        WorldPoint playerTile = client.getLocalPlayer().getWorldLocation();
        Optional<TileObject> safeRock =TileObjects.search().withAction("Leave").first();

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
                eat(75);
                drinkPrayer(25);
            }
        }

        if (!PrayerUtil.isPrayerActive(Prayer.PIETY)) {
            PrayerUtil.togglePrayer(Prayer.PIETY);
        }

        if (!client.getLocalPlayer().isInteracting()) {
            NPCs.search().nameContains(VARDOVIS).first().ifPresent(vardorvis -> {
                NPCInteraction.interact(vardorvis, "Attack");
            });
            return;
        }

        List<NPC> newAxes = NPCs.search().withId(12225).result();
        List<NPC> activeAxes = NPCs.search().withId(12227).result();

        if (!newAxes.isEmpty()) {
            newAxes.forEach((axe) -> {
                if (axe.getWorldLocation().getX() + 1 == safeTile.getX() && axe.getWorldLocation().getY() + 1 == safeTile.getY()) {
                    handleAxeMove();
                } else {
                    axeTicks = 0;
                }
            });
        }

        if (!activeAxes.isEmpty()) {
//            System.out.println("activeAxes: " + activeAxes);
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

    /**
     * 1 tick blood captcha. Thanks @Lunatik
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
