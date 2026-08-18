package com.viaversion.viarewind.api;

import com.viaversion.viaversion.api.configuration.Config;

public interface ViaRewindConfig extends Config {
    ViaRewindConfig.CooldownIndicator getCooldownIndicator();

    boolean isReplaceAdventureMode();

    boolean isReplaceParticles();

    int getMaxBookPages();

    int getMaxBookPageSize();

    boolean isEmulateWorldBorder();

    boolean alwaysShowOriginalMobName();

    String getWorldBorderParticle();

    boolean isEnableOffhand();

    String getOffhandCommand();

    enum CooldownIndicator {
        TITLE,
        ACTION_BAR,
        BOSS_BAR,
        DISABLED;
    }
}
