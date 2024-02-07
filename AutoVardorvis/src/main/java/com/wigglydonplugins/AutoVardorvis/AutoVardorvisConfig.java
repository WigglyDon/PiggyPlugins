package com.wigglydonplugins.AutoVardorvis;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("AutoVardorvis")
public interface AutoVardorvisConfig extends Config {

    @ConfigItem(
            keyName = "autoPray",
            name = "Auto Prayers",
            description = ""
    )
    default boolean autoPray() {
        return false;
    }

    @ConfigItem(
            keyName = "awakened",
            name = "Awakened Vardorvis",
            description = "",
            position = 0
    )
    default boolean awakened() {
        return false;
    }
}
