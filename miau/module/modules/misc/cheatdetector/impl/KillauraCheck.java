package miau.module.modules.misc.cheatdetector.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import miau.module.modules.misc.cheatdetector.Check;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;

public class KillauraCheck extends Check {
    private static final long COMBAT_WINDOW_TICKS = 70L;
    private static final long SESSION_RESET_TICKS = 140L;
    private static final int WINDOW_SIZE = 10;
    private static final float QUANTUM = 1.40625F;
    private static final float VL_LIMIT = 400.0F;
    private static final float VL_FADE_PER_TICK = 0.5F;
    private static final int EAT_TIMEOUT = 33;
    private static final int MIN_USE_TIME = 6;
    private static final int CONSUME_FAIL_VL = 8;
    private static final float BURST_STEP_MIN = 7.0F;
    private static final float BURST_QUIET = 2.5F;
    private static final int BURST_MAX_TICKS = 7;
    private static final float BURST_SUM_MIN = 20.0F;
    private static final float SNAP_PRE_ERROR_MIN = 20.0F;
    private static final int SNAP_MIN_HITS = 3;
    private static final float SNAP_VL = 90.0F;
    private static final float RETURN_VL = 55.0F;
    private static final long RETURN_PAIR_TICKS = 8L;
    private static final int TRACK_WINDOW = 24;
    private static final float TRACK_RATIO = 0.85F;
    private static final float TRACK_LOS_MIN = 2.5F;
    private static final float TRACK_LOS_MAX = 45.0F;
    private static final double TRACK_MIN_DIST = 2.2;
    private static final float TRACK_VL = 80.0F;
    private static final double MOVE_MIN_SPEED = 0.15;
    private static final double MOVE_MAX_SPEED = 0.45;
    private static final double MOVE_FLAT_DY = 0.001;
    private static final double MOVE_SMOOTH_ACCEL = 0.022;
    private static final int MOVE_WINDOW = 12;
    private static final float MOVE_MEAN_LIMIT = 7.5F;
    private static final float MOVE_DESYNC_RESIDUAL = 8.0F;
    private static final float MOVE_VL = 70.0F;
    private static final float LOCK_RESIDUAL = 13.0F;
    private static final int LOCK_HITS = 3;
    private static final float MOVE_LOCK_VL = 85.0F;
    private static final double SPRINT_ACCEL = 0.08;
    private static final double SPRINT_MIN_SPEED = 0.25;
    private static final float SPRINT_OFFSET = 62.0F;
    private static final int SPRINT_HITS = 4;
    private static final float MOVE_SPRINT_VL = 85.0F;
    private static final int MOVE_DECAY_TICKS = 40;
    private static final double TARGET_RANGE_SQ = 36.0;
    private static final double HITBOX_HALF_WIDTH = 0.4;
    private static final int TRAIL_LEN = 5;
    private float aimVl;
    private boolean failedKillaura;
    private long lastSwingTick = Long.MIN_VALUE;
    private float lastYaw;
    private float lastPitch;
    private boolean hasRotation;
    private final List<Float> yawChangeWindow = new ArrayList<>();
    private int snapStreak;
    private int burstTicks;
    private float burstSum;
    private float burstDir;
    private float preBurstYaw;
    private int quietTicks;
    private int snapHits;
    private int snapMisses;
    private long lastSnapHitTick = Long.MIN_VALUE;
    private UUID lastTargetId;
    private float lastBearing = Float.NaN;
    private int trackSamples;
    private int trackTicks;
    private double lastVelX;
    private double lastVelZ;
    private double lastMoveY;
    private boolean hasVel;
    private int moveSamples;
    private int moveDesyncTicks;
    private float residualSum;
    private int lockDesync;
    private int sprintDesync;
    private int moveTickCounter;
    private int useItemTicks;
    private long lastEatTick;
    private int consumeVl;
    private static final Map<UUID, KillauraCheck.Trail> TRAILS = new HashMap<>();

    @Override
    public String getName() {
        return "Killaura";
    }

    @Override
    public void onUpdate(EntityPlayer player) {
        if (player.field_70154_o == null) {
            UUID uuid = player.func_110124_au();
            long tick = mc.field_71441_e.func_82737_E();
            trail(uuid).push(player.field_70165_t, player.field_70161_v, tick);
            trail(mc.field_71439_g.func_110124_au())
                .push(mc.field_71439_g.field_70165_t, mc.field_71439_g.field_70161_v, tick);
            this.consumeComponent(player, tick);
            if (player.field_82175_bq) {
                this.lastSwingTick = tick;
            }

            float yaw = player.field_70177_z;
            float pitch = player.field_70125_A;
            if (!this.hasRotation) {
                this.lastYaw = yaw;
                this.lastPitch = pitch;
                this.hasRotation = true;
            } else {
                float prevYaw = this.lastYaw;
                float yawChange = wrapDegrees(yaw - this.lastYaw);
                float pitchChange = wrapDegrees(pitch - this.lastPitch);
                this.lastYaw = yaw;
                this.lastPitch = pitch;
                double moveX = player.field_70165_t - player.field_70142_S;
                double moveY = player.field_70163_u - player.field_70137_T;
                double moveZ = player.field_70161_v - player.field_70136_U;
                if (moveX * moveX + moveZ * moveZ > 25.0) {
                    this.yawChangeWindow.clear();
                    this.resetBurst();
                    this.lastBearing = Float.NaN;
                    this.lastTargetId = null;
                    this.hasVel = false;
                    this.moveSamples = 0;
                    this.moveDesyncTicks = 0;
                    this.residualSum = 0.0F;
                } else if (this.lastSwingTick != Long.MIN_VALUE
                    && tick >= this.lastSwingTick
                    && tick - this.lastSwingTick <= 70L) {
                    float absYawChange = Math.abs(yawChange);
                    float absPitchChange = Math.abs(pitchChange);
                    if (absYawChange != 0.0F || absPitchChange != 0.0F) {
                        this.yawChangeWindow.add(absYawChange);
                        if (this.yawChangeWindow.size() >= 10) {
                            this.analyzeWindow(player, this.yawChangeWindow);
                            this.yawChangeWindow.clear();
                        }
                    }

                    List<EntityPlayer> targets = this.targetsNear(player, tick);
                    this.burstMachine(player, tick, yawChange, prevYaw, targets);
                    this.trackComponent(player, yaw, targets);
                    this.movementComponent(player, moveX, moveY, moveZ, yaw, targets);
                    if (this.aimVl > 400.0F) {
                        this.flag(player, "vl: " + this.aimVl);
                        this.aimVl = 360.0F;
                    }

                    if (this.aimVl > 0.0F) {
                        this.aimVl = Math.max(0.0F, this.aimVl - 0.5F);
                    }
                } else {
                    if (this.lastSwingTick != Long.MIN_VALUE && tick - this.lastSwingTick > 140L) {
                        this.resetSession();
                    }
                }
            }
        }
    }

    private void analyzeWindow(EntityPlayer player, List<Float> window) {
        float first = window.get(0);
        float old = first;
        int machineKnown = 0;
        int constant = 0;
        int robotized = 0;
        int bigUp = 0;
        int bigDown = 0;

        for (float change : window) {
            float r = Math.abs(change - first);
            float diff = change - old;
            if (r < 2.109375F && change > 2.8125F) {
                robotized++;
            }

            if (r < 1.40625F && change > 4.21875F) {
                machineKnown++;
            }

            if (r < 0.703125F && change > 3.515625F) {
                constant++;
            }

            if (diff > 12.0F) {
                bigUp++;
            }

            if (diff < -12.0F) {
                bigDown++;
            }

            old = change;
        }

        if (machineKnown > 8) {
            this.addVl(100.0F, "heuristic(aim)");
        }

        if (constant > 6) {
            this.addVl(65.0F, "heuristic(constant)");
        }

        if (robotized > 8) {
            this.addVl(50.0F, "heuristic(sync)");
        }

        if (bigUp > 1 && bigDown > 1 && bigUp + bigDown > 4) {
            this.snapStreak++;
            if (this.snapStreak > 2) {
                this.addVl(55.0F, "pattern(snap)");
            }
        } else {
            this.snapStreak = 0;
        }
    }

    private void burstMachine(
        EntityPlayer player, long tick, float yawChange, float prevYaw, List<EntityPlayer> targets
    ) {
        float absYaw = Math.abs(yawChange);
        if (this.burstTicks > 0) {
            boolean sameDir = yawChange * this.burstDir >= 0.0F;
            if (absYaw < 2.5F) {
                if (this.burstSum >= 20.0F) {
                    this.evaluateBurst(player, tick, targets);
                }

                this.resetBurst();
                this.quietTicks = 1;
            } else if (sameDir) {
                this.burstTicks++;
                this.burstSum += absYaw;
                if (this.burstTicks > 7) {
                    this.burstTicks = -1;
                }
            } else if (absYaw > 7.0F) {
                this.burstTicks = 1;
                this.burstSum = absYaw;
                this.burstDir = yawChange;
                this.preBurstYaw = prevYaw;
                this.quietTicks = 0;
            } else {
                this.resetBurst();
                this.quietTicks = 0;
            }
        } else if (this.burstTicks == -1) {
            if (absYaw < 2.5F) {
                this.resetBurst();
                this.quietTicks = 1;
            }
        } else if (absYaw > 7.0F && this.quietTicks >= 2) {
            this.burstTicks = 1;
            this.burstSum = absYaw;
            this.burstDir = yawChange;
            this.preBurstYaw = prevYaw;
            this.quietTicks = 0;
        } else if (absYaw < 2.5F) {
            this.quietTicks++;
        } else {
            this.quietTicks = 0;
        }
    }

    private void evaluateBurst(EntityPlayer player, long tick, List<EntityPlayer> targets) {
        if (!targets.isEmpty()) {
            float bestErr = Float.MAX_VALUE;
            float bestPre = 0.0F;
            float bestPreInside = Float.MAX_VALUE;

            for (EntityPlayer target : targets) {
                KillauraCheck.Trail trail = trail(target.func_110124_au());
                float err = this.minInsideError(player, trail, this.lastYaw);
                if (err < bestErr) {
                    bestErr = err;
                    float bearingNow = bearingTo(player, trail.x[0], trail.z[0]);
                    bestPre = Math.abs(wrapDegrees(this.preBurstYaw - bearingNow));
                }

                bestPreInside = Math.min(bestPreInside, this.minInsideError(player, trail, this.preBurstYaw));
            }

            if (bestErr <= 1.40625F && bestPre > 20.0F) {
                this.snapHits++;
                this.lastSnapHitTick = tick;
                if (this.snapHits >= 3 && this.snapHits > this.snapMisses) {
                    this.addVl(90.0F, "silent(snap)");
                    this.flag(player, "silent(snap)");
                }
            } else if (bestPreInside <= 1.40625F && bestErr > 15.0F) {
                if (this.lastSnapHitTick != Long.MIN_VALUE && tick - this.lastSnapHitTick <= 8L) {
                    this.addVl(55.0F, "silent(return)");
                }
            } else if (bestPre > 20.0F && bestErr > 2.8125F) {
                this.snapMisses++;
            }
        }
    }

    private void trackComponent(EntityPlayer player, float yaw, List<EntityPlayer> targets) {
        EntityPlayer target = null;
        double bestDistSq = Double.MAX_VALUE;

        for (EntityPlayer candidate : targets) {
            double dx = candidate.field_70165_t - player.field_70165_t;
            double dy = candidate.field_70163_u - player.field_70163_u;
            double dz = candidate.field_70161_v - player.field_70161_v;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                target = candidate;
            }
        }

        if (target == null) {
            this.lastTargetId = null;
            this.lastBearing = Float.NaN;
        } else {
            UUID tid = target.func_110124_au();
            KillauraCheck.Trail trail = trail(tid);
            float bearingNow = bearingTo(player, trail.x[0], trail.z[0]);
            if (tid.equals(this.lastTargetId) && !Float.isNaN(this.lastBearing)) {
                float losDelta = Math.abs(wrapDegrees(bearingNow - this.lastBearing));
                double dx = target.field_70165_t - player.field_70165_t;
                double dz = target.field_70161_v - player.field_70161_v;
                double horizDist = Math.sqrt(dx * dx + dz * dz);
                if (losDelta > 2.5F && losDelta < 45.0F && horizDist >= 2.2) {
                    this.trackSamples++;
                    if (this.minInsideError(player, trail, yaw) <= 0.703125F) {
                        this.trackTicks++;
                    }

                    if (this.trackSamples >= 24) {
                        if (this.trackTicks >= 0.85F * this.trackSamples) {
                            this.addVl(80.0F, "silent(track) " + this.trackTicks + "/" + this.trackSamples);
                        }

                        this.trackSamples = 0;
                        this.trackTicks = 0;
                    }
                }
            }

            this.lastTargetId = tid;
            this.lastBearing = bearingNow;
        }
    }

    private void movementComponent(
        EntityPlayer player, double moveX, double moveY, double moveZ, float yaw, List<EntityPlayer> targets
    ) {
        if (++this.moveTickCounter >= 40) {
            this.moveTickCounter = 0;
            this.lockDesync = Math.max(0, this.lockDesync - 1);
            this.sprintDesync = Math.max(0, this.sprintDesync - 1);
        }

        boolean flat = this.hasVel && Math.abs(moveY) < 0.001 && Math.abs(this.lastMoveY) < 0.001;
        boolean haveAccel = this.hasVel;
        double ax = moveX - this.lastVelX;
        double az = moveZ - this.lastVelZ;
        this.lastVelX = moveX;
        this.lastVelZ = moveZ;
        this.lastMoveY = moveY;
        this.hasVel = true;
        if (haveAccel) {
            double accel = Math.sqrt(ax * ax + az * az);
            double speed = Math.sqrt(moveX * moveX + moveZ * moveZ);
            if (flat && player.field_70737_aN <= 0 && !(speed < 0.15) && !(speed > 0.45)) {
                Block ground = mc.field_71441_e
                    .func_180495_p(new BlockPos(player.field_70165_t, player.field_70163_u - 0.5, player.field_70161_v))
                    .func_177230_c();
                if (ground != Blocks.field_150432_aD && ground != Blocks.field_150403_cj) {
                    float moveBearing = (float)Math.toDegrees(Math.atan2(-moveX, moveZ));
                    float offset = wrapDegrees(moveBearing - yaw);
                    float residual = bucketResidual(offset);
                    if (player.func_70051_ag() && speed > 0.25 && accel < 0.08 && Math.abs(offset) > 62.0F) {
                        this.sprintDesync++;
                        if (this.sprintDesync >= 4) {
                            this.addVl(85.0F, "movement(sprint) " + (int)offset + '°');
                            this.sprintDesync -= 4;
                        }
                    }

                    if (!(accel > 0.022)) {
                        if (residual > 13.0F) {
                            for (EntityPlayer target : targets) {
                                if (this.minInsideError(player, trail(target.func_110124_au()), yaw) <= 1.40625F) {
                                    this.lockDesync++;
                                    if (this.lockDesync >= 3) {
                                        this.addVl(85.0F, "movement(lock) " + (int)residual + '°');
                                        this.lockDesync -= 3;
                                    }
                                    break;
                                }
                            }
                        }

                        this.moveSamples++;
                        this.residualSum += residual;
                        if (residual > 8.0F) {
                            this.moveDesyncTicks++;
                        }

                        if (this.moveSamples >= 12) {
                            float mean = this.residualSum / this.moveSamples;
                            if (mean > 7.5F) {
                                this.addVl(
                                    70.0F,
                                    "movement(fix) mean="
                                        + String.format("%.1f", mean)
                                        + '°'
                                        + " hard="
                                        + this.moveDesyncTicks
                                        + "/"
                                        + this.moveSamples
                                );
                            }

                            this.moveSamples = 0;
                            this.moveDesyncTicks = 0;
                            this.residualSum = 0.0F;
                        }
                    }
                }
            }
        }
    }

    private static float bucketResidual(float offset) {
        float nearest = 45.0F * Math.round(offset / 45.0F);
        return Math.abs(wrapDegrees(offset - nearest));
    }

    private void consumeComponent(EntityPlayer player, long tick) {
        ItemStack heldItem = player.func_70694_bm();
        boolean isUsingItem = player.func_71039_bw();
        boolean isConsumable = heldItem != null && this.isConsumable(heldItem.func_77973_b());
        boolean isAttacking = player.field_110158_av > 0;
        if (isUsingItem && isConsumable) {
            this.useItemTicks++;
        } else {
            if (this.useItemTicks > 0) {
                this.lastEatTick = tick;
            }

            this.useItemTicks = 0;
        }

        long sinceLastEat = tick - this.lastEatTick;
        if (isAttacking && this.useItemTicks > 6 && sinceLastEat < 33L && isConsumable) {
            this.consumeVl++;
            if (this.consumeVl >= 8) {
                this.flag(player, "vl: " + this.consumeVl);
                this.consumeVl = 0;
            }
        } else if (this.consumeVl > 0) {
            this.consumeVl--;
        }
    }

    private List<EntityPlayer> targetsNear(EntityPlayer attacker, long tick) {
        List<EntityPlayer> out = new ArrayList<>();

        for (EntityPlayer p : mc.field_71441_e.field_73010_i) {
            if (p != attacker && p != mc.field_71439_g && !p.field_70128_L && !p.func_175149_v()) {
                double dx = p.field_70165_t - attacker.field_70165_t;
                double dy = p.field_70163_u - attacker.field_70163_u;
                double dz = p.field_70161_v - attacker.field_70161_v;
                if (!(dx * dx + dy * dy + dz * dz > 36.0)) {
                    trail(p.func_110124_au()).push(p.field_70165_t, p.field_70161_v, tick);
                    out.add(p);
                }
            }
        }

        return out;
    }

    private float minInsideError(EntityPlayer attacker, KillauraCheck.Trail trail, float yaw) {
        float best = Float.MAX_VALUE;

        for (int i = 0; i < trail.size; i++) {
            double dx = trail.x[i] - attacker.field_70165_t;
            double dz = trail.z[i] - attacker.field_70161_v;
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            if (!(horizDist < 0.5)) {
                float bearing = (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
                float err = Math.abs(wrapDegrees(yaw - bearing));
                float halfWidth = (float)Math.toDegrees(Math.atan2(0.4, horizDist));
                best = Math.min(best, Math.max(0.0F, err - halfWidth));
            }
        }

        return best;
    }

    private static float bearingTo(EntityPlayer attacker, double x, double z) {
        double dx = x - attacker.field_70165_t;
        double dz = z - attacker.field_70161_v;
        return (float)(Math.atan2(dz, dx) * 180.0 / Math.PI) - 90.0F;
    }

    private void addVl(float vl, String reason) {
        this.aimVl += vl;
    }

    private void resetBurst() {
        this.burstTicks = 0;
        this.burstSum = 0.0F;
        this.burstDir = 0.0F;
    }

    private void resetSession() {
        this.resetBurst();
        this.quietTicks = 0;
        this.snapHits = 0;
        this.snapMisses = 0;
        this.lastSnapHitTick = Long.MIN_VALUE;
        this.trackSamples = 0;
        this.trackTicks = 0;
        this.lastTargetId = null;
        this.lastBearing = Float.NaN;
        this.hasVel = false;
        this.moveSamples = 0;
        this.moveDesyncTicks = 0;
        this.residualSum = 0.0F;
        this.lockDesync = 0;
        this.sprintDesync = 0;
        this.moveTickCounter = 0;
    }

    private static KillauraCheck.Trail trail(UUID uuid) {
        return TRAILS.computeIfAbsent(uuid, k -> new KillauraCheck.Trail());
    }

    private boolean isConsumable(Item item) {
        return item instanceof ItemFood || item instanceof ItemPotion || item instanceof ItemBucketMilk;
    }

    private static float wrapDegrees(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F) {
            angle -= 360.0F;
        }

        if (angle < -180.0F) {
            angle += 360.0F;
        }

        return angle;
    }

    private static final class Trail {
        final double[] x = new double[5];
        final double[] z = new double[5];
        long lastTick = Long.MIN_VALUE;
        int size;

        private Trail() {
        }

        void push(double px, double pz, long tick) {
            if (tick != this.lastTick || this.size <= 0) {
                System.arraycopy(this.x, 0, this.x, 1, 4);
                System.arraycopy(this.z, 0, this.z, 1, 4);
                this.x[0] = px;
                this.z[0] = pz;
                this.lastTick = tick;
                if (this.size < 5) {
                    this.size++;
                }
            }
        }
    }
}
