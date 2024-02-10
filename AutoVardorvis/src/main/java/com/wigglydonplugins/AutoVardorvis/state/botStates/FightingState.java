package com.wigglydonplugins.AutoVardorvis.state.botStates;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.piggyplugins.PiggyUtils.API.PrayerUtil;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.Optional;

import com.wigglydonplugins.AutoVardorvis.AutoVardorvisPlugin.MainClassContext;
import net.runelite.api.widgets.Widget;

public class FightingState {

    private static final String VARDORVIS = "Vardorvis";
    private static int rangeTicks;
    private static int rangeCooldown;

    public static void execute(MainClassContext context) {
        Client client = context.getClient();
        rangeTicks = context.getRangeTicks();
        rangeCooldown = context.getRangeCooldown();


        System.out.println("fighting state");
        List<NPC> newAxes = NPCs.search().withId(12225).result();
        List<NPC> activeAxes = NPCs.search().withId(12227).result();
        Optional<NPC> vardorvis = NPCs.search().nameContains(VARDORVIS).first();

        WorldPoint playerTile = client.getLocalPlayer().getWorldLocation();
        Optional<TileObject> safeRock = TileObjects.search().withAction("Leave").first();

        if (client.getGameState() != GameState.LOGGED_IN || !isInFight(client)) {
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
            NPCs.search().nameContains(VARDORVIS).first().ifPresent(npc -> {
                NPCInteraction.interact(npc, "Attack");
            });
        }



    }
    private static void doBloodCaptcha() {
        List<Widget> captchaBlood = Widgets.search().filter(widget -> widget.getParentId() != 9764864).hiddenState(false).withAction("Destroy").result();
        if (!captchaBlood.isEmpty()) {
            captchaBlood.forEach(x -> {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetAction(x, "Destroy");
            });
        }
    }
    private static boolean isInFight(Client client) {
        return client.isInInstancedRegion() && NPCs.search().nameContains(VARDORVIS).nearestToPlayer().isPresent();
    }
    private static void turnOffPrayers() {
        if (PrayerUtil.isPrayerActive(Prayer.PIETY)) {
            PrayerUtil.togglePrayer(Prayer.PIETY);
        }
        if (PrayerUtil.isPrayerActive(Prayer.PROTECT_FROM_MELEE)) {
            PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MELEE);
        }
    }
    private static void autoPray() {
        if (rangeTicks > 0) {
            rangeTicks--;
            if (rangeTicks == 0) {
                rangeCooldown = 3;
            }
        }

        if (rangeTicks == 0) {
            if (rangeCooldown > 0) {
                rangeCooldown--;
            }
        }
        handleRangeFirstGameTick();
    }
    private static void handleRangeFirstGameTick() {
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
}

