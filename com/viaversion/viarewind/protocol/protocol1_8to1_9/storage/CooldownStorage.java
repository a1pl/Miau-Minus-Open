package com.viaversion.viarewind.protocol.protocol1_8to1_9.storage;

import com.viaversion.viarewind.ViaRewind;
import com.viaversion.viarewind.protocol.protocol1_8to1_9.emulation.CooldownVisualization;
import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.util.Pair;
import java.util.List;
import java.util.logging.Level;

public class CooldownStorage implements StorableObject {
    private double attackSpeed = 4.0;
    private long lastHit = 0L;
    private CooldownVisualization.Factory visualizationFactory = CooldownVisualization.Factory.fromConfiguration();
    private CooldownVisualization current;

    public void tick(UserConnection connection) {
        if (!this.hasCooldown()) {
            this.endCurrentVisualization();
        } else {
            BlockPlaceDestroyTracker tracker = connection.get(BlockPlaceDestroyTracker.class);
            if (tracker.isMining()) {
                this.lastHit = 0L;
                this.endCurrentVisualization();
            } else {
                if (this.current == null) {
                    this.current = this.visualizationFactory.create(connection);
                }

                try {
                    this.current.show(this.getCooldown());
                } catch (Exception exception) {
                    ViaRewind.getPlatform()
                        .getLogger()
                        .log(Level.WARNING, "Unable to show cooldown visualization", exception);
                }
            }
        }
    }

    private void endCurrentVisualization() {
        if (this.current != null) {
            try {
                this.current.hide();
            } catch (Exception exception) {
                ViaRewind.getPlatform()
                    .getLogger()
                    .log(Level.WARNING, "Unable to hide cooldown visualization", exception);
            }

            this.current = null;
        }
    }

    public boolean hasCooldown() {
        long time = System.currentTimeMillis() - this.lastHit;
        double cooldown = this.restrain(time * this.attackSpeed / 1000.0, 1.5);
        return cooldown > 0.1 && cooldown < 1.1;
    }

    public double getCooldown() {
        long time = System.currentTimeMillis() - this.lastHit;
        return this.restrain(time * this.attackSpeed / 1000.0, 1.0);
    }

    private double restrain(double x, double b) {
        return x < 0.0 ? 0.0 : Math.min(x, b);
    }

    public void setAttackSpeed(double base, List<Pair<Byte, Double>> modifiers) {
        this.attackSpeed = base;

        for (int j = 0; j < modifiers.size(); j++) {
            if (modifiers.get(j).key() == 0) {
                this.attackSpeed = this.attackSpeed + modifiers.get(j).value();
                modifiers.remove(j--);
            }
        }

        for (int j = 0; j < modifiers.size(); j++) {
            if (modifiers.get(j).key() == 1) {
                this.attackSpeed = this.attackSpeed + base * modifiers.get(j).value();
                modifiers.remove(j--);
            }
        }

        for (int j = 0; j < modifiers.size(); j++) {
            if (modifiers.get(j).key() == 2) {
                this.attackSpeed = this.attackSpeed * (1.0 + modifiers.get(j).value());
                modifiers.remove(j--);
            }
        }
    }

    public void hit() {
        this.lastHit = System.currentTimeMillis();
    }

    public void setLastHit(long lastHit) {
        this.lastHit = lastHit;
    }
}
