package com.wigglydonplugins.AutoCrafting;

import com.example.EthanApiPlugin.Collections.Inventory;
import com.example.EthanApiPlugin.Collections.Widgets;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
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
  int playerIdleCounter = 0;

  @Subscribe
  private void onGameTick(GameTick event) {
    if (running) {
      if (tickDelay > 0) {
        tickDelay--;
        return;
      }

      Inventory.search().withId(config.LEATHER_TYPE().getLeatherType()).first().ifPresent((leather) -> {
        leatherItem = leather;
      });

      if (config.ARMOR_TYPE().getLeatherNeeded() <= Inventory.getItemAmount(config.LEATHER_TYPE().getLeatherType())) {

        // if idle
        if (playerIdleCounter == 0) {
          //if on craft menu
          if (Widgets.search().withTextContains("How many do you wish to make?").first().isPresent()) {
//            System.out.println("craft x screen");
            //else craft
          } else {
          InventoryInteraction.useItem("Needle", "Use");
          Inventory.search().nameContains("Needle").first().ifPresent(needle -> {
            MousePackets.queueClickPacket();
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetOnWidget(needle, leatherItem);
            System.out.println("click needle on leather");
          });
          }

          // not idle
        } else if (client.getLocalPlayer().getAnimation() == -1) {
          //wait for idle
            playerIdleCounter --;
        } else playerIdleCounter = 5;
      } else {
        //bank here
        System.out.println("not enough materials left");
//        bank();
      }

    }
  }

}
