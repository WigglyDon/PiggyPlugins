package com.wigglydonplugins.AutoVardorvis.state.botStates;

import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.TileObjectInteraction;
import com.wigglydonplugins.AutoVardorvis.AutoVardorvisPlugin.MainClassContext;
import com.wigglydonplugins.AutoVardorvis.state.StateHandler.State;
import java.util.Optional;
import net.runelite.api.Client;
import net.runelite.api.TileObject;

public class GoToVardorvisState {

  MainClassContext context;

  public void execute(MainClassContext context) {
    this.context = context;
    boolean inStrangleWood = TileObjects.search().withId(48723).first().isPresent();

    if (isInFight(context.getClient())) {
      System.out.println("changing to FightingState");
      context.setContextBotState(State.FIGHTING);
    }

    Optional<TileObject> vardorvisRock = TileObjects.search().withId(49495)
        .withinDistance(37).first();
    Optional<TileObject> tunnel1 = TileObjects.search().withId(48745).first();
    Optional<TileObject> tunnel2 = TileObjects.search().withId(48746).first();

    if (inStrangleWood && !isMoving()) {
      vardorvisRock.ifPresentOrElse((rock) -> {
        System.out.println("click climb over");
        TileObjectInteraction.interact(rock, "Climb-over");
      }, () -> {
        tunnel1.ifPresent((t) -> {
          System.out.println("click tunnel");
          TileObjectInteraction.interact(t, "Enter");
        });
      });
    }
  }

  private boolean isMoving() {
    return EthanApiPlugin.isMoving() || context.getClient().getLocalPlayer().getAnimation() != -1;
  }

  private boolean isInFight(Client client) {
    return client.isInInstancedRegion() && NPCs.search().nameContains("Vardorvis").nearestToPlayer()
        .isPresent();
  }

}
