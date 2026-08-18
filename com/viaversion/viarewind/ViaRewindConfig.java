package com.viaversion.viarewind;

import com.viaversion.viaversion.util.Config;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ViaRewindConfig extends Config implements com.viaversion.viarewind.api.ViaRewindConfig {
    private com.viaversion.viarewind.api.ViaRewindConfig.CooldownIndicator cooldownIndicator;
    private boolean replaceAdventureMode;
    private boolean replaceParticles;
    private int maxBookPages;
    private int maxBookPageSize;
    private boolean emulateWorldBorder;
    private boolean alwaysShowOriginalMobName;
    private String worldBorderParticle;
    private boolean enableOffhand;
    private String offhandCommand;

    public ViaRewindConfig(File configFile) {
        super(configFile);
    }

    @Override
    public void reload() {
        super.reload();
        this.loadFields();
    }

    private void loadFields() {
        this.cooldownIndicator = com.viaversion.viarewind.api.ViaRewindConfig.CooldownIndicator.valueOf(
            this.getString("cooldown-indicator", "TITLE").toUpperCase()
        );
        this.replaceAdventureMode = this.getBoolean("replace-adventure", false);
        this.replaceParticles = this.getBoolean("replace-particles", false);
        this.maxBookPages = this.getInt("max-book-pages", 100);
        this.maxBookPageSize = this.getInt("max-book-page-length", 5000);
        this.emulateWorldBorder = this.getBoolean("emulate-world-border", true);
        this.alwaysShowOriginalMobName = this.getBoolean("always-show-original-mob-name", true);
        this.worldBorderParticle = this.getString("world-border-particle", "fireworksSpark");
        this.enableOffhand = this.getBoolean("enable-offhand", true);
        this.offhandCommand = this.getString("offhand-command", "/offhand");
    }

    @Override
    public com.viaversion.viarewind.api.ViaRewindConfig.CooldownIndicator getCooldownIndicator() {
        return this.cooldownIndicator;
    }

    @Override
    public boolean isReplaceAdventureMode() {
        return this.replaceAdventureMode;
    }

    @Override
    public boolean isReplaceParticles() {
        return this.replaceParticles;
    }

    @Override
    public int getMaxBookPages() {
        return this.maxBookPages;
    }

    @Override
    public int getMaxBookPageSize() {
        return this.maxBookPageSize;
    }

    @Override
    public boolean isEmulateWorldBorder() {
        return this.emulateWorldBorder;
    }

    @Override
    public boolean alwaysShowOriginalMobName() {
        return this.alwaysShowOriginalMobName;
    }

    @Override
    public String getWorldBorderParticle() {
        return this.worldBorderParticle;
    }

    @Override
    public boolean isEnableOffhand() {
        return this.enableOffhand;
    }

    @Override
    public String getOffhandCommand() {
        return this.offhandCommand;
    }

    @Override
    public URL getDefaultConfigURL() {
        return this.getClass().getClassLoader().getResource("assets/viarewind/config.yml");
    }

    @Override
    public InputStream getDefaultConfigInputStream() {
        return this.getClass().getClassLoader().getResourceAsStream("assets/viarewind/config.yml");
    }

    @Override
    protected void handleConfig(Map<String, Object> map) {
    }

    @Override
    public List<String> getUnsupportedOptions() {
        return Collections.emptyList();
    }
}
