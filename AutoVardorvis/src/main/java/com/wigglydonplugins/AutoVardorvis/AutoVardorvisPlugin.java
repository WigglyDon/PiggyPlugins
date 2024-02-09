package com.wigglydonplugins.AutoVardorvis;

import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
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
    private static final int MAGE_PROJECTILE = 2520; // thx for grabbing this 4 me @sdeenginer

    private static final String VARDOVIS = "Vardorvis";
    private static final String VARDOVIS_HEAD = "Vardorvis' Head";

    private Projectile rangeProjectile;
    private Projectile mageProjectile;

    private int rangeTicks = 0;
    private int mageTicks = 0;
    private int rangeCooldown = 0;
    private int mageCooldown = 0;
    private boolean mageFirst;

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

    private void handleMageFirstGameTick() {
        if (mageTicks > 0) {
            if (!PrayerUtil.isPrayerActive(Prayer.PROTECT_FROM_MAGIC)) {
                PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MAGIC);
            }
        } else if (rangeTicks > 0) {
            if (!PrayerUtil.isPrayerActive(Prayer.PROTECT_FROM_MISSILES)) {
                PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MISSILES);
            }
        } else {
            if (!PrayerUtil.isPrayerActive(Prayer.PROTECT_FROM_MELEE)) {
                PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MELEE);
            }
        }
    }

    private void handleRangeFirstGameTick() {
        if (rangeTicks > 0) {
            if (!PrayerUtil.isPrayerActive(Prayer.PROTECT_FROM_MISSILES)) {
                PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MISSILES);
            }
        } else if (mageTicks > 0) {
            if (!PrayerUtil.isPrayerActive(Prayer.PROTECT_FROM_MAGIC)) {
                PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MAGIC);
            }
        } else {
            if (!PrayerUtil.isPrayerActive(Prayer.PROTECT_FROM_MELEE)) {
                PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MELEE);
            }
        }
    }

    private boolean inVardorvisArea() {
        return (!NPCs.search().nameContains(VARDOVIS).result().isEmpty() && client.isInInstancedRegion());
    }

    WorldPoint safeTile = null;
    @Subscribe
    private void onGameTick(GameTick event) {

        if (!inVardorvisArea()) {
            System.out.println("not in vardorvis area");
            return;
        }

        WorldPoint playerTile = client.getLocalPlayer().getWorldLocation();
        Optional<TileObject> safeRock =TileObjects.search().withAction("Leave").first();

        if (safeRock.isPresent()) {
            WorldPoint safeRockLocation = safeRock.get().getWorldLocation();
            safeTile = new WorldPoint(safeRockLocation.getX() + 6, safeRockLocation.getY() - 10, 0);
        }

        if (safeTile != null) {
            if (playerTile.getX() != safeTile.getX() || playerTile.getY() != safeTile.getY()) {
                MousePackets.queueClickPacket();
                MovementPackets.queueMovement(safeTile);
                System.out.println("moving to safe tile");
            }
        }

        if (!client.getLocalPlayer().isInteracting()) {
            NPCs.search().nameContains(VARDOVIS).first().ifPresent(vardorvis -> {
                NPCInteraction.interact(vardorvis, "Attack");
                System.out.println("attack vardorvis");
            });
        }



        List<NPC> newAxes = NPCs.search().withId(12225).result();
        List<NPC> activeAxes = NPCs.search().withId(12227).result();

        if (!newAxes.isEmpty()) {
            newAxes.forEach((axe) -> System.out.println("newAxe locations: " + axe.getWorldLocation()));
        }

        if (!activeAxes.isEmpty()) {
            System.out.println("activeAxes: " + activeAxes);
        }

        ///////////////////////////////////////////////////////////

        if (client.getGameState() != GameState.LOGGED_IN || !isInFight()) {
            return;
        }

        if (mageTicks > 0) {
            mageTicks--;
            if (mageTicks == 0) {
                mageCooldown = 3;
                if (mageFirst) {
                    mageFirst = false;
                }
            }
        }

        if (rangeTicks > 0) {
            rangeTicks--;
            if (rangeTicks == 0) {
                rangeCooldown = 3;
            }
        }

        if (mageTicks == 0) {
            mageProjectile = null;
            if (mageCooldown > 0) {
                mageCooldown--;
            }
        }

        if (rangeTicks == 0) {
            rangeProjectile = null;
            if (rangeCooldown > 0) {
                rangeCooldown--;
            }
        }

        if (config.autoPray()) {
            if (mageFirst) {
                handleMageFirstGameTick();
            } else {
                handleRangeFirstGameTick();
            }
        }
        doBloodCaptcha();

    }

    @Subscribe
    private void onProjectileMoved(ProjectileMoved event) {
        if (client.getGameState() != GameState.LOGGED_IN || !isInFight()) {
            return;
        }

        Projectile projectile = event.getProjectile();
        if (projectile.getId() == MAGE_PROJECTILE) {
            if (mageProjectile == null && mageCooldown == 0) {
                mageTicks = 4;
                mageProjectile = projectile;
                if (rangeProjectile == null) {
                    mageFirst = true;
                }
            }
        }

        if (projectile.getId() == RANGE_PROJECTILE) {
            if (rangeProjectile == null && rangeCooldown == 0) {
                rangeTicks = 4;
                rangeProjectile = projectile;
                if (mageProjectile == null) {
                    mageFirst = false;
                }
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
        if (mageTicks > 0) {
            return SpriteID.PRAYER_PROTECT_FROM_MAGIC;
        }
        if (rangeTicks > 0) {
            return SpriteID.PRAYER_PROTECT_FROM_MISSILES;
        }

        return SpriteID.PRAYER_PROTECT_FROM_MELEE;
    }

    public Prayer getCorrectPrayer() {
        if (mageTicks > 0) {
            return Prayer.PROTECT_FROM_MAGIC;
        }

        if (rangeTicks > 0) {
            return Prayer.PROTECT_FROM_MISSILES;
        }

        return Prayer.PROTECT_FROM_MELEE;
    }

    public boolean isInFight() {
        return client.isInInstancedRegion() && NPCs.search().nameContains(VARDOVIS).nearestToPlayer().isPresent();
    }
}
