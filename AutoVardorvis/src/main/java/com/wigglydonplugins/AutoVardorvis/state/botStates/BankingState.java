package com.wigglydonplugins.AutoVardorvis.state.botStates;

import com.example.EthanApiPlugin.Collections.Bank;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.InteractionApi.NPCInteraction;
import com.wigglydonplugins.AutoVardorvis.AutoVardorvisPlugin.MainClassContext;

public class BankingState {

  public void execute(MainClassContext context) {
    if (!Bank.isOpen()) {
      System.out.println("trying to open bank");
      NPCs.search().nameContains("Jack").nearestToPlayer().ifPresent(banker ->
          NPCInteraction.interact(banker, "Bank")
      );
    } else if (Bank.isOpen()) {
      System.out.println("bank open, trying to withdraw");
      if (Inventory.getItemAmount("Ring of shadows") == 0) {
        System.out.println("try to withdraw ring of shadow");
      }
    }
  }

}
