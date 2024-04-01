package com.wigglydonplugins.AutoCrafting;

import com.example.EthanApiPlugin.Collections.Bank;
import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.NPCs;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.InteractionApi.BankInteraction;
import com.example.InteractionApi.BankInventoryInteraction;
import com.example.InteractionApi.NPCInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Inject;
import com.google.inject.Provides;
import java.awt.event.KeyEvent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
    name = "<html><font color=\"#FF0000\">[WD]</font>Auto Crafting</html>",
    description = "Automated crafting plugin"
)
public class AutoCraftingPlugin extends Plugin {

  @Inject
  private Client client;
  @Inject
  private OverlayManager overlayManager;
  @Inject
  private AutoCraftingOverlay overlay;
  @Inject
  private AutoCraftingConfig config;
  boolean running = false;
  int tickDelay = 0;
  int lastCrafted = 0;
  private Widget leatherItem = null;

  @Provides
  private AutoCraftingConfig getConfig(ConfigManager configManager) {
    return configManager.getConfig(AutoCraftingConfig.class);
  }

  @Override
  protected void startUp() throws Exception {
    overlayManager.add(overlay);
    running = client.getGameState() == GameState.LOGGED_IN;
  }

  @Override
  protected void shutDown() throws Exception {
    overlayManager.remove(overlay);
    running = false;
  }

  @Subscribe
  private void onGameTick(GameTick event) {
    if (!running || tickDelay > 0) {
      tickDelay--;
      return;
    }
    if (lastCrafted > 0) {
      lastCrafted--;
    }

    //leather crafting animation
    if (client.getLocalPlayer().getAnimation() == 1249 && !Inventory.full()) {
      lastCrafted = 3;
    }

    Inventory.search().withId(config.LEATHER_TYPE().getLeatherType()).first()
        .ifPresent((leather) -> {
          leatherItem = leather;
        });

    if (config.ARMOR_TYPE().getLeatherNeeded() <= Inventory.getItemAmount(
        config.LEATHER_TYPE().getLeatherType())) {

      if (Inventory.search().withId(config.LEATHER_TYPE().getLeatherType()).result().size()
          >= config.ARMOR_TYPE().getLeatherNeeded()) {
        if (lastCrafted == 0) {
          Widgets.search().withAction("Make")
              .first()
              .ifPresentOrElse((w) -> {

                int remainingLeather = Inventory.getItemAmount(
                    config.LEATHER_TYPE().getLeatherType());
                WidgetPackets.queueResumePause(w.getId(), remainingLeather);
                lastCrafted = 3;

              }, () -> {
                Inventory.search().nameContains("Needle").first().ifPresent(needle -> {
                  MousePackets.queueClickPacket();
                  MousePackets.queueClickPacket();
                  WidgetPackets.queueWidgetOnWidget(needle, leatherItem);
                });
              });
        }
      }


    } else if (!Bank.isOpen()) {
      NPCs.search().nameContains("Banker").nearestToPlayer().ifPresent((banker) -> {
        NPCInteraction.interact(banker, "Bank");
      });
    }
    bank();
  }

  private void bank() {
    if (Bank.isOpen() && Inventory.search().nameContains(config.ARMOR_TYPE().getArmorType()).first()
        .isPresent()) {
      String armorName = Inventory.search().nameContains(config.ARMOR_TYPE().getArmorType()).first()
          .get().getName();
      BankInventoryInteraction.useItem(armorName, "Deposit-All");
      withdraw(config.LEATHER_TYPE().getLeatherType(), 100);
      sendKey(KeyEvent.VK_ESCAPE);
      lastCrafted = 0;

    }
  }

  private void withdraw(int id, int amount) {
    Bank.search().withId(id).first().ifPresent(item ->
        BankInteraction.withdrawX(item, amount)
    );
  }

  private void sendKey(int key) {
    keyEvent(KeyEvent.KEY_PRESSED, key);
    keyEvent(KeyEvent.KEY_RELEASED, key);
  }

  private void keyEvent(int id, int key) {
    KeyEvent e = new KeyEvent(
        client.getCanvas(),
        id,
        System.currentTimeMillis(),
        0,
        key,
        KeyEvent.CHAR_UNDEFINED
    );
    client.getCanvas().dispatchEvent(e);
  }


}
