package com.wigglydonplugins.AutoVardorvis;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("AutoVardorvis")
public interface AutoVardorvisConfig extends Config {

    @ConfigItem(
            keyName = "eatat",
            name = "Eat at",
            description = "Eat at what health?",
            position = 0
    )
    default int EATAT() { return 75; }

    @ConfigItem(
            keyName = "drinkprayerat",
            name = "Drink prayer potion at",
            description = "Drink prayer potion when?",
            position = 1
    )
    default int DRINKPRAYERAT() { return 15; }


}
