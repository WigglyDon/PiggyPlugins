package java.com.wigglydonplugins.AutoCrafting;

import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameTick;
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
  private int tickDelay = 0;
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
    if (running) {
      if (tickDelay > 0) {
        tickDelay--;
        return;
      }
    }
  }

}
