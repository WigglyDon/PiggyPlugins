package com.wigglydonplugins.AutoVardorvis.state.botStates;

import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.InteractionApi.InventoryInteraction;
import com.example.InteractionApi.TileObjectInteraction;
import com.wigglydonplugins.AutoVardorvis.AutoVardorvisPlugin.MainClassContext;
import java.util.Optional;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;

public class GoToBankState {

  public MainClassContext context;
  public Client client;

  public void execute(MainClassContext context) {
    this.context = context;
    this.client = context.getClient();
    if (!inHouse()) {
      teleToHouse();
      return;
    }
    if (client.getBoostedSkillLevel(Skill.HITPOINTS) < 99
        || client.getBoostedSkillLevel(Skill.PRAYER) < 99) {
      Optional<TileObject> pool = TileObjects.search().nameContains("pool of").withAction("Drink")
          .first();
      pool.ifPresent(poolObject -> {
        TileObjectInteraction.interact(poolObject, "Drink");
      });
      return;
    }
    if (inHouse()) {
      Optional<TileObject> portal = TileObjects.search().nameContains("Portal Nexus")
          .first();
      portal.ifPresent(portalObject -> {
        TileObjectInteraction.interact(portalObject, "Lunar Isle");
      });
    }
  }

  private void teleToHouse() {
    InventoryInteraction.useItem("Teleport to house", "Break");
  }

  private boolean inHouse() {
    return !TileObjects.search().nameContains("pool of").result().isEmpty();
  }

}
