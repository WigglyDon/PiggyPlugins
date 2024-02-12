package com.wigglydonplugins.AutoVardorvis.state.botStates;

import com.wigglydonplugins.AutoVardorvis.AutoVardorvisPlugin.MainClassContext;

public class GoToBankState {

  public void execute(MainClassContext context) {
    System.out.println("bank state");
  }
//              if (client.getBoostedSkillLevel(Skill.HITPOINTS) < config.POOLDRINK().width || client.getBoostedSkillLevel(
//  Skill.PRAYER
//                ) < config.POOLDRINK().height
//            ) {
//    TileObjects.search().nameContains("pool of").withAction("Drink").first().ifPresent { pool ->
//        TileObjectInteraction.interact(pool, "Drink")
//    }
//    return
//  }
//            if (inHouse()) {
//    TileObjects.search().nameContains(config.PORTAL().toString()).first().ifPresent { portal ->
//        TileObjectInteraction.interact(portal, config.PORTAL().action())
//    }
//    return
//  }

}
