package com.wigglydonplugins.AutoVardorvis.state.botStates;

import com.example.EthanApiPlugin.Collections.Bank;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.wigglydonplugins.AutoVardorvis.AutoVardorvisPlugin.MainClassContext;
import java.util.List;
import net.runelite.api.widgets.Widget;

public class BankingState {

  public void execute(MainClassContext context) {
    int tickDelay = context.getContextTickDelay();
    if (!Bank.isOpen()) {
      NPCs.search().nameContains("Jack").nearestToPlayer().ifPresent(banker ->
          NPCInteraction.interact(banker, "Bank")
      );
      context.setContextTickDelay(3);
    } else if (Bank.isOpen()) {
      bank();
    }
  }

  private void bank() {
    if (!Inventory.search().empty()) {
      System.out.println("try to deposit all");

      List<Widget> depositButton = Widgets.search()
          .filter(widget -> widget.getParentId() != 786474)
          .hiddenState(false).withAction("Deposit inventory").result();

      if (!depositButton.isEmpty()) {
        depositButton.forEach(x -> {
          MousePackets.queueClickPacket();
          WidgetPackets.queueWidgetAction(x, "Deposit inventory");
        });
      }
      return;
    }

    if (!hasItem("Ring of shadows")) {
      withdraw("Ring of shadows", 1);
    }
  }

  private void withdraw(String name, int amount) {
    Bank.search().nameContains(name).first().ifPresent(item ->
        BankInteraction.withdrawX(item, amount)
    );
  }

  private boolean hasItem(String name) {
    return !Inventory.search().nameContains(name).result().isEmpty();
  }

}

