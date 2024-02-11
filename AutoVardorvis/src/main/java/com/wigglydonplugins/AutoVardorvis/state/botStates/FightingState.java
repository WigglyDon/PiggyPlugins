package com.wigglydonplugins.AutoVardorvis.state.botStates;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.example.Packets.WidgetPackets;
import com.piggyplugins.PiggyUtils.API.PrayerUtil;
import com.wigglydonplugins.AutoVardorvis.AutoVardorvisConfig;
import com.wigglydonplugins.AutoVardorvis.AutoVardorvisPlugin.MainClassContext;
import java.util.List;
import java.util.Optional;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.Prayer;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;

public class FightingState {

  private final String VARDORVIS = "Vardorvis";
  private Client client;
  private static WorldPoint safeTile = null;
  private static WorldPoint axeMoveTile = null;
  private boolean drankSuperCombat;
  private static int axeTicks = 0;

  public void execute(MainClassContext context) {
    client = context.getClient();
    AutoVardorvisConfig config = context.getConfig();

    drankSuperCombat = context.isDrankSuperCombat();

    List<NPC> newAxes = NPCs.search().withId(12225).result();
    List<NPC> activeAxes = NPCs.search().withId(12227).result();
    Optional<NPC> vardorvis = NPCs.search().nameContains(VARDORVIS).first();

    WorldPoint playerTile = client.getLocalPlayer().getWorldLocation();
    Optional<TileObject> safeRock = TileObjects.search().withAction("Leave").first();

    if (client.getGameState() != GameState.LOGGED_IN || !isInFight(client)) {
      turnOffPrayers();
      return;
    }

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
              context.setDrankSuperCombat(true);
            });
          }
        });
        return;
      } else if (vardorvis.get().getWorldLocation().getX() == safeTile.getX()) {
        System.out.println("vardorvis stuck");
        movePlayerToTile(safeTile);
      }
    }

    if (!newAxes.isEmpty()) {
      newAxes.forEach((axe) -> {
        if (axe.getWorldLocation().getX() == safeTile.getX() - 1
            && axe.getWorldLocation().getY() == safeTile.getY() - 1) {
          handleAxeMove();
        }
      });
    }

    if (!activeAxes.isEmpty()) {
      activeAxes.forEach((axe) -> {
        if (axe.getWorldLocation().getX() == safeTile.getX() + 1
            && axe.getWorldLocation().getY() == safeTile.getY() - 1) {
          axeTicks = 1;
          handleAxeMove();
        }
      });
    }

    if (safeRock.isPresent() && safeTile == null) {
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
        context.setDrankSuperCombat(drankSuperCombat);
      }
    }

    if (!client.getLocalPlayer().isInteracting()) {
      NPCs.search().nameContains(VARDORVIS).first().ifPresent(npc -> {
        NPCInteraction.interact(npc, "Attack");
      });
    }
  }

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

  private void doBloodCaptcha() {
    List<Widget> captchaBlood = Widgets.search().filter(widget -> widget.getParentId() != 9764864)
        .hiddenState(false).withAction("Destroy").result();
    if (!captchaBlood.isEmpty()) {
      captchaBlood.forEach(x -> {
        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetAction(x, "Destroy");
      });
    }
  }

  private boolean isInFight(Client client) {
    return client.isInInstancedRegion() && NPCs.search().nameContains(VARDORVIS).nearestToPlayer()
        .isPresent();
  }

  private void turnOffPrayers() {
    if (PrayerUtil.isPrayerActive(Prayer.PIETY)) {
      PrayerUtil.togglePrayer(Prayer.PIETY);
    }
    if (PrayerUtil.isPrayerActive(Prayer.PROTECT_FROM_MELEE)) {
      PrayerUtil.togglePrayer(Prayer.PROTECT_FROM_MELEE);
    }
  }

  private void eat(int at) {
    if (needsToEat(at)) {
      Inventory.search().withAction("Eat").result().stream()
          .findFirst()
          .ifPresentOrElse(food -> InventoryInteraction.useItem(food, "Eat"),
              this::teleToHouse
          );
    }
  }

  private void drinkPrayer(int at) {
    if (needsToDrinkPrayer(at)) {
      Inventory.search().nameContains("Prayer potion").result().stream()
          .findFirst()
          .ifPresentOrElse(prayerPotion -> InventoryInteraction.useItem(prayerPotion, "Drink"),
              this::teleToHouse
          );
    }
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
}

