package com.viaversion.viarewind.protocol.protocol1_8to1_9.storage;

import com.viaversion.viarewind.protocol.protocol1_8to1_9.emulation.WitherBossBar;
import com.viaversion.viaversion.api.connection.StoredObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarStorage extends StoredObject {
    private final Map<UUID, WitherBossBar> bossBars = new HashMap<>();

    public BossBarStorage(UserConnection user) {
        super(user);
    }

    public void reset() {
        this.updateLocation();
        this.changeWorld();
    }

    public void add(UUID uuid, String title, float health) throws Exception {
        WitherBossBar bossBar = new WitherBossBar(this.getUser(), uuid, title, health);
        PlayerPositionTracker playerPositionTracker = this.getUser().get(PlayerPositionTracker.class);
        bossBar.setPlayerLocation(
            playerPositionTracker.getPosX(),
            playerPositionTracker.getPosY(),
            playerPositionTracker.getPosZ(),
            playerPositionTracker.getYaw(),
            playerPositionTracker.getPitch()
        );
        bossBar.show();
        this.bossBars.put(uuid, bossBar);
    }

    public void remove(UUID uuid) {
        WitherBossBar bossBar = this.bossBars.remove(uuid);
        if (bossBar != null) {
            bossBar.hide();
        }
    }

    public void updateLocation() {
        PlayerPositionTracker playerPositionTracker = this.getUser().get(PlayerPositionTracker.class);
        this.bossBars
            .values()
            .forEach(
                bossBar -> {
                    try {
                        bossBar.setPlayerLocation(
                            playerPositionTracker.getPosX(),
                            playerPositionTracker.getPosY(),
                            playerPositionTracker.getPosZ(),
                            playerPositionTracker.getYaw(),
                            playerPositionTracker.getPitch()
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            );
    }

    public void changeWorld() {
        this.bossBars.values().forEach(bossBar -> {
            bossBar.hide();
            bossBar.show();
        });
    }

    public void updateHealth(UUID uuid, float health) {
        WitherBossBar bossBar = this.bossBars.get(uuid);
        if (bossBar != null) {
            bossBar.setHealth(health);
        }
    }

    public void updateTitle(UUID uuid, String title) {
        WitherBossBar bossBar = this.bossBars.get(uuid);
        if (bossBar != null) {
            bossBar.setTitle(title);
        }
    }
}
