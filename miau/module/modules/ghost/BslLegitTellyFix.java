package miau.module.modules.ghost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import miau.Miau;
import miau.event.EventTarget;
import miau.event.impl.LeftClickMouseEvent;
import miau.event.impl.LoadWorldEvent;
import miau.event.impl.MoveInputEvent;
import miau.event.impl.PacketEvent;
import miau.event.impl.PlayerUpdateEvent;
import miau.event.impl.Render2DEvent;
import miau.event.impl.Render3DEvent;
import miau.event.impl.RightClickMouseEvent;
import miau.event.impl.UpdateEvent;
import miau.event.types.EventType;
import miau.mixin.IAccessorRenderManager;
import miau.module.Module;
import miau.module.modules.movement.SafeWalk;
import miau.property.properties.BooleanProperty;
import miau.util.client.ChatUtil;
import miau.util.client.KeyBindUtil;
import miau.util.player.RotationUtil;
import miau.util.world.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

public class BslLegitTellyFix extends Module {
    private static final Minecraft mc = Minecraft.func_71410_x();
    public final BooleanProperty autoSwap = new BooleanProperty("Auto swap", true);
    public final BooleanProperty disableSafeWalk = new BooleanProperty("Disable SafeWalk", true);
    public final BooleanProperty showActivationHitbox = new BooleanProperty("Show activation hitbox", false);
    private static final Map<String, Object> bridge = new HashMap<>();
    private boolean armed = false;
    private boolean running = false;
    private long activatePromptAt = 0L;
    private long promptBrokeAt = 0L;
    private float promptAlpha = 0.0F;
    private long promptFadeLastAt = 0L;
    private int promptFadeRgb = 16733525;
    private int[] hitboxLastPos = null;
    private int hitboxLastFace = -1;
    private boolean activationMovementHeld = false;
    private boolean antiSwayTapUsed = false;
    private final HashSet<String> cancelledGhostBlocks = new HashSet<>();
    private boolean tellyAutoPlaceWindow = false;
    private boolean autoPlaceDebugActive = false;
    private boolean safeWalkStateCaptured = false;
    private boolean safeWalkWasEnabled = false;
    private int setupTick = 0;
    private int cyclePhase = 19;
    private float baseYaw = 0.0F;
    private int travelX = 0;
    private int travelZ = 0;
    private double antiSwayLane = 0.0;
    private float antiSwayYawOffset = 0.0F;
    private int bridgeLaneBlock = 0;
    private int bridgeStartProgress = 0;
    private int[] latestStraightPlacedPos = null;
    private boolean firstTellyPlacementPending = false;
    private boolean adaptiveAimValid = false;
    private float adaptiveAimYaw = 0.0F;
    private float adaptiveAimPitch = 0.0F;
    private long adaptiveAimUpdatedAt = 0L;
    private long takeoverDetectionAt = 0L;
    private boolean takeoverCameraValid = false;
    private float takeoverCameraYaw = 0.0F;
    private float takeoverCameraPitch = 0.0F;
    private float takeoverAccumulated = 0.0F;
    private long takeoverLastFrameAt = 0L;
    private long freezeLastTickAt = 0L;
    private boolean ignoreForwardUntilRelease = false;
    private boolean ignoreBackUntilRelease = false;
    private boolean ignoreLeftUntilRelease = false;
    private boolean ignoreRightUntilRelease = false;
    private boolean ignoreJumpUntilRelease = false;
    private boolean ignoreSneakUntilRelease = false;
    private boolean ignoreSprintUntilRelease = false;
    private boolean rotationActive = false;
    private long rotationStartedAt = 0L;
    private long rotationDuration = 50L;
    private float rotationStartYaw = 0.0F;
    private float rotationStartPitch = 0.0F;
    private float rotationTargetYaw = 0.0F;
    private float rotationTargetPitch = 0.0F;
    private float scriptedRotationYaw = 0.0F;
    private float scriptedRotationPitch = 0.0F;
    private final double SENSITIVITY_QUANTUM = 0.03404715;
    private final int[] YAW_NUDGE_PATTERN = new int[]{0, 1, -1, 2, -2};
    private int rotationStepCounter = 0;
    private final double ACTIVATION_ACROSS_MIN = 0.38;
    private final double ACTIVATION_ACROSS_MAX = 0.65;
    private final double ACTIVATION_HEIGHT_MIN = 0.25;
    private final double ACTIVATION_HEIGHT_MAX = 0.75;
    private final float ACTIVATION_YAW_TOLERANCE = 2.0F;
    private final float[] yawCurve = new float[]{
        91.68F,
        98.88F,
        78.94F,
        37.45F,
        1.61F,
        -21.69F,
        -33.98F,
        -35.8F,
        -34.64F,
        -33.85F,
        -33.06F,
        -31.55F,
        -29.26F,
        -26.65F,
        -24.19F,
        -21.07F,
        -18.84F,
        -17.06F,
        -8.87F,
        2.61F,
        41.94F
    };
    private final float[] pitchCurve = new float[]{
        64.31F,
        59.95F,
        60.57F,
        61.46F,
        60.64F,
        58.89F,
        56.91F,
        56.63F,
        58.65F,
        61.63F,
        64.2F,
        66.74F,
        68.69F,
        70.64F,
        73.01F,
        75.37F,
        77.46F,
        78.56F,
        78.9F,
        77.22F,
        72.25F
    };
    private final float[] forwardCurve = new float[]{
        1.0F,
        1.0F,
        0.0F,
        0.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        1.0F
    };
    private final float[] strafeCurve = new float[]{
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        0.0F,
        0.0F,
        0.0F,
        0.0F,
        0.0F,
        0.0F,
        0.0F,
        0.0F,
        0.0F,
        0.0F,
        0.0F,
        0.0F,
        0.0F,
        -1.0F,
        -1.0F,
        -1.0F,
        -1.0F
    };
    private final double[] FACE_HIT_OFFSETS = new double[]{0.5, 0.25, 0.75, 0.15, 0.85};
    private final double[] EXTENDED_FACE_HIT_OFFSETS = new double[]{
        0.5, 0.25, 0.75, 0.15, 0.85, 0.35, 0.65, 0.05, 0.95
    };
    private final int[] ALLOWED_PLACE_FACES = new int[]{2, 3, 4, 5, 1};
    private final String[] REPLACEABLE_BLOCKS = new String[]{
        "air",
        "water",
        "flowing_water",
        "lava",
        "flowing_lava",
        "fire",
        "tallgrass",
        "deadbush",
        "snow_layer",
        "double_plant",
        "vine"
    };
    private final String[] EXPERIMENTAL_REPLACEABLE_BLOCKS = new String[]{
        "sapling",
        "yellow_flower",
        "red_flower",
        "brown_mushroom",
        "red_mushroom",
        "wheat",
        "carrots",
        "potatoes",
        "nether_wart",
        "reeds"
    };
    private final String[] UNPLACEABLE_EXACT = new String[]{
        "snow_layer",
        "web",
        "sapling",
        "daylight_detector",
        "beacon",
        "banner",
        "end_portal_frame",
        "end_portal",
        "lever",
        "stone_button",
        "wooden_button",
        "skull",
        "cactus",
        "double_plant",
        "waterlily",
        "carpet",
        "tripwire_hook",
        "tallgrass",
        "yellow_flower",
        "red_flower",
        "flower_pot",
        "sign",
        "ladder",
        "torch",
        "redstone_torch",
        "unlit_redstone_torch",
        "gravel",
        "clay",
        "sand",
        "soul_sand",
        "chest",
        "trapped_chest",
        "ender_chest",
        "furnace",
        "lit_furnace",
        "jukebox",
        "enchanting_table",
        "dropper",
        "dispenser",
        "hopper",
        "anvil",
        "noteblock",
        "crafting_table",
        "mob_spawner",
        "brewing_stand",
        "bed"
    };
    private final String[] UNPLACEABLE_CONTAINS = new String[]{
        "stairs",
        "slab",
        "fence",
        "pane",
        "rail",
        "door",
        "torch",
        "pumpkin",
        "flower",
        "sapling",
        "banner",
        "button",
        "skull",
        "web",
        "carpet",
        "cactus",
        "sign",
        "mushroom"
    };
    private final String[] INTERACTABLE_TYPES = new String[]{
        "BlockTrapDoor",
        "BlockDoor",
        "BlockContainer",
        "BlockJukebox",
        "BlockFenceGate",
        "BlockChest",
        "BlockEnderChest",
        "BlockEnchantmentTable",
        "BlockBrewingStand",
        "BlockBed",
        "BlockDropper",
        "BlockDispenser",
        "BlockHopper",
        "BlockAnvil",
        "BlockNote",
        "BlockWorkbench",
        "BlockFurnace",
        "BlockBeacon",
        "BlockMobSpawner",
        "BlockDaylightDetector",
        "BlockCommandBlock",
        "BlockStandingSign",
        "BlockWallSign",
        "BlockSkull"
    };
    private int currentClientTick = Integer.MIN_VALUE;
    private int placementEvaluationTick = Integer.MIN_VALUE;
    private int lastPlacementAttemptTick = Integer.MIN_VALUE;
    private int lastSuccessfulPlaceTick = Integer.MIN_VALUE;
    private int forceSuppressTick = Integer.MIN_VALUE;
    private long totalC08Counter = 0L;
    private long c08CounterAtTickBoundary = 0L;
    private boolean hasLastSentServerPos = false;
    private double lastSentServerPosX;
    private double lastSentServerPosY;
    private double lastSentServerPosZ;
    private Object[] cachedCandidate = null;
    private int cachedCandidateTick = Integer.MIN_VALUE;
    private float cachedCandidateYaw = Float.NaN;
    private float cachedCandidatePitch = Float.NaN;
    private boolean candidateResolvedThisTick = false;
    private int[] lastPlacedPos = null;
    private int[] lastSupportPos = null;
    private int lastSupportFace = -1;
    private List<int[]> cachedBelowTargets = null;
    private int cachedBelowTargetsTick = Integer.MIN_VALUE;
    private final Map<String, Integer> rejectedTargets = new HashMap<>();
    private int forcedModeCheck = 0;
    private boolean useSuppressed = false;
    private boolean silentPitchActive = false;
    private float silentPitch = 0.0F;
    private boolean placingViaModule = false;
    private boolean manualC08InWindow = false;
    private final Map<Integer, Boolean> lastKeyDown = new HashMap<>();
    private boolean lastRmbDown = false;

    public BslLegitTellyFix() {
        super("BslLegitTellyFix", false);
    }

    @Override
    public void onEnabled() {
        this.autoPlaceOnEnable();
        this.armAutomation();
    }

    @Override
    public void onDisabled() {
        this.stopAutomation(false);
        this.autoPlaceOnDisable();
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        if (mc.field_71439_g != null) {
            this.stopAutomation(false);
        }

        this.autoPlaceOnWorldJoin();
    }

    @EventTarget
    public void onPreUpdate(PlayerUpdateEvent event) {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            this.enforceSafeWalkDisabledForRun();
            this.pollKeyTransitions();
            this.pollMouseTransitions();
            if (this.running) {
                this.setKeyPressed("attack", false);
                this.applySmoothedRotation();
            }

            if (this.armed && !this.running) {
                this.updateActivationPrompt();
            }

            if (this.running) {
                long freezeNow = this.now();
                if (this.freezeLastTickAt != 0L && freezeNow - this.freezeLastTickAt > 300L) {
                    this.stopAutomation(true);
                } else {
                    this.freezeLastTickAt = freezeNow;
                    Entity player = mc.field_71439_g;
                    if (!player.field_70128_L && !(player.field_70143_R > 7.0F)) {
                        this.handleAutoSwap(player);
                        if (!this.isHoldingBlock(player)) {
                            this.stopAutomation(true);
                        } else {
                            if (this.firstTellyPlacementPending) {
                                this.updateAdaptivePlacementAim(player);
                            }

                            this.autoPlaceOnPreUpdate();
                            if (this.firstTellyPlacementPending) {
                                this.updateAdaptivePlacementAim(player);
                            }
                        }
                    } else {
                        this.stopAutomation(true);
                    }
                }
            }
        }
    }

    private float activationPitch() {
        return 75.0F;
    }

    private void handleAutoSwap(Entity player) {
        if (this.autoSwap.getValue()) {
            if (player instanceof EntityPlayer) {
                int threshold = 5;
                ItemStack held = ((EntityPlayer)player).func_70694_bm();
                int heldCount = held != null && this.isUsableBlockStack(held) ? held.field_77994_a : 0;
                if (heldCount <= threshold) {
                    int bestSlot = -1;
                    int bestSize = heldCount;

                    for (int slot = 0; slot <= 8; slot++) {
                        if (slot != ((EntityPlayer)player).field_71071_by.field_70461_c) {
                            ItemStack stack = ((EntityPlayer)player).field_71071_by.func_70301_a(slot);
                            if (this.isUsableBlockStack(stack) && stack.field_77994_a > bestSize) {
                                bestSize = stack.field_77994_a;
                                bestSlot = slot;
                            }
                        }
                    }

                    if (bestSlot != -1) {
                        ((EntityPlayer)player).field_71071_by.field_70461_c = bestSlot;
                    }
                }
            }
        }
    }

    private boolean activationPromptReady() {
        return this.activatePromptAt != 0L && this.now() - this.activatePromptAt >= 1000L;
    }

    private boolean activationSuppressUse() {
        return this.activatePromptAt != 0L && this.now() - this.activatePromptAt >= 850L;
    }

    private void updateActivationPrompt() {
        Entity player = mc.field_71439_g;
        if (player != null && mc.field_71462_r == null) {
            this.setActivationMovementHold(this.activationPromptReady() && Mouse.isButtonDown(1));
            boolean lookingDown = player.field_70125_A >= this.activationPitch();
            boolean atEdge = lookingDown && this.isLookingAtEdge(player);
            if (mc.field_71439_g.func_70093_af() && atEdge) {
                if (this.activatePromptAt == 0L) {
                    this.activatePromptAt = this.now();
                }

                this.promptBrokeAt = 0L;
                if (this.activationSuppressUse()) {
                    this.setKeyPressed("use", false);
                }

                if (this.activationPromptReady() && Mouse.isButtonDown(1)) {
                    this.disableSafeWalkForRun();
                    this.enforceSafeWalkDisabledForRun();
                } else if (this.safeWalkStateCaptured) {
                    this.restoreSafeWalkState();
                }
            } else if (this.activatePromptAt != 0L) {
                if (!this.activationPromptReady()) {
                    this.clearActivationPrompt();
                } else {
                    if (this.promptBrokeAt == 0L) {
                        this.rememberActivationPromptColor();
                        this.promptBrokeAt = this.now();
                    }

                    this.setKeyPressed("use", false);
                    if (!mc.field_71439_g.func_70093_af()
                        && Mouse.isButtonDown(1)
                        && this.isActivationYawAligned(player.field_70177_z)) {
                        this.rememberActivationPromptColor();
                        this.activatePromptAt = 0L;
                        this.promptBrokeAt = 0L;
                        this.beginAutomation();
                        if (!this.running) {
                            this.setKeyPressed("use", false);
                        }
                    } else {
                        if (this.now() - this.promptBrokeAt > 300L) {
                            this.clearActivationPrompt();
                        }
                    }
                }
            }
        } else {
            this.clearActivationPrompt();
        }
    }

    private void clearActivationPrompt() {
        this.rememberActivationPromptColor();
        if (this.activationSuppressUse()) {
            this.setKeyPressed("use", false);
        }

        this.activatePromptAt = 0L;
        this.promptBrokeAt = 0L;
        this.setActivationMovementHold(false);
        if (!this.running) {
            this.restoreSafeWalkState();
        }
    }

    private void rememberActivationPromptColor() {
        if (this.activatePromptAt != 0L) {
            this.promptFadeRgb = this.activationPromptReady() ? 5635925 : 16733525;
        }
    }

    private int[] travelDirectionFromYaw(float yaw) {
        double radians = Math.toRadians(yaw);
        double rawX = Math.sin(radians) - Math.cos(radians);
        double rawZ = -Math.cos(radians) - Math.sin(radians);
        return Math.abs(rawX) >= Math.abs(rawZ)
            ? new int[]{rawX >= 0.0 ? 1 : -1, 0}
            : new int[]{0, rawZ >= 0.0 ? 1 : -1};
    }

    private boolean isLookingAtEdge(Entity player) {
        if (!this.isActivationYawAligned(player.field_70177_z)) {
            return false;
        }

        Object[] hit = this.raycastBlock(4.5, player.field_70177_z, player.field_70125_A);
        if (hit != null && hit.length >= 3 && hit[0] != null && hit[1] != null && hit[2] != null) {
            int face = this.faceFromName((String)hit[2]);
            if (face < 2) {
                return false;
            }

            if (!this.isInActivationFaceCenter(face, (Vec3)hit[1])) {
                return false;
            }

            int[] travel = this.travelDirectionFromYaw(player.field_70177_z);
            int travelFace = travel[0] > 0 ? 5 : (travel[0] < 0 ? 4 : (travel[1] > 0 ? 3 : 2));
            if (face != travelFace) {
                return false;
            }

            int[] pos = this.posFromVec((Vec3)hit[0]);
            if (!this.isPlayerOnActivationBlock(player, pos)) {
                return false;
            }

            int aheadX = pos[0] + travel[0];
            int aheadZ = pos[2] + travel[1];
            if (!this.isReplaceableName(this.blockNameAt(aheadX, pos[1] + 1, aheadZ), false)) {
                return false;
            }

            Vec3 playerPos = this.playerPosition(player);
            double lipDistance;
            if (face == 5) {
                lipDistance = pos[0] + 1 - playerPos.field_72450_a;
            } else if (face == 4) {
                lipDistance = playerPos.field_72450_a - pos[0];
            } else if (face == 3) {
                lipDistance = pos[2] + 1 - playerPos.field_72449_c;
            } else {
                lipDistance = playerPos.field_72449_c - pos[2];
            }

            return lipDistance <= 0.65;
        } else {
            return false;
        }
    }

    private boolean isActivationYawAligned(float yaw) {
        float nearestDiagonal = Math.round((yaw - 45.0F) / 90.0F) * 90.0F + 45.0F;
        return Math.abs(this.tellyWrapAngle(yaw - nearestDiagonal)) <= 2.0F;
    }

    private boolean isPlayerOnActivationBlock(Entity player, int[] pos) {
        if (pos == null) {
            return false;
        }

        Vec3 playerPos = this.playerPosition(player);
        if (pos[1] != this.floor(playerPos.field_72448_b - 0.01)) {
            return false;
        }

        double centerX = pos[0] + 0.5;
        double centerZ = pos[2] + 0.5;
        return Math.abs(playerPos.field_72450_a - centerX) <= 0.85
            && Math.abs(playerPos.field_72449_c - centerZ) <= 0.85;
    }

    private boolean isInActivationFaceCenter(int face, Vec3 localHit) {
        if (localHit == null) {
            return false;
        }

        double acrossFace = face != 4 && face != 5 ? localHit.field_72450_a : localHit.field_72449_c;
        if (face == 3 || face == 4) {
            acrossFace = 1.0 - acrossFace;
        }

        return acrossFace >= 0.38
            && acrossFace <= 0.65
            && localHit.field_72448_b >= 0.25
            && localHit.field_72448_b <= 0.75;
    }

    @EventTarget
    public void onRenderWorld(Render3DEvent event) {
        if (this.showActivationHitbox.getValue()) {
            if (this.armed && !this.running) {
                if (!(this.promptAlpha < 0.05F)) {
                    if (this.activatePromptAt != 0L) {
                        Object[] hit = this.raycastBlock(
                            4.5, mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A
                        );
                        if (hit != null && hit.length >= 3 && hit[0] != null && hit[2] != null) {
                            int face = this.faceFromName((String)hit[2]);
                            if (face >= 2) {
                                this.hitboxLastPos = this.posFromVec((Vec3)hit[0]);
                                this.hitboxLastFace = face;
                            }
                        }
                    }

                    if (this.hitboxLastPos != null && this.hitboxLastFace >= 2) {
                        this.drawActivationFaceRegion(this.hitboxLastPos, this.hitboxLastFace);
                    }
                }
            }
        }
    }

    private void drawActivationFaceRegion(int[] pos, int face) {
        Vec3 cam = this.renderPosition();
        if (cam != null) {
            double yMin = pos[1] + 0.25;
            double yMax = pos[1] + 0.75;
            double x1;
            double z1;
            double x2;
            double z2;
            if (face == 5) {
                x1 = pos[0] + 1.005;
                x2 = x1;
                z1 = pos[2] + 0.38;
                z2 = pos[2] + 0.65;
            } else if (face == 4) {
                x1 = pos[0] - 0.005;
                x2 = x1;
                z1 = pos[2] + 0.35;
                z2 = pos[2] + 0.62;
            } else if (face == 3) {
                z1 = pos[2] + 1.005;
                z2 = z1;
                x1 = pos[0] + 0.35;
                x2 = pos[0] + 0.62;
            } else {
                z1 = pos[2] - 0.005;
                z2 = z1;
                x1 = pos[0] + 0.38;
                x2 = pos[0] + 0.65;
            }

            int r = this.promptFadeRgb >> 16 & 0xFF;
            int g = this.promptFadeRgb >> 8 & 0xFF;
            int b = this.promptFadeRgb & 0xFF;
            int fillAlpha = (int)(60.0F * this.promptAlpha);
            int lineAlpha = (int)(220.0F * this.promptAlpha);
            if (fillAlpha < 4) {
                fillAlpha = 4;
            }

            if (lineAlpha < 16) {
                lineAlpha = 16;
            }

            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GL11.glDisable(3553);
            GL11.glDisable(3008);
            GL11.glDisable(2884);
            GL11.glDisable(2929);
            GL11.glDepthMask(false);
            GL11.glTranslated(-cam.field_72450_a, -cam.field_72448_b, -cam.field_72449_c);
            GL11.glColor4f(r / 255.0F, g / 255.0F, b / 255.0F, fillAlpha / 255.0F);
            GL11.glBegin(7);
            GL11.glVertex3d(x1, yMin, z1);
            GL11.glVertex3d(x2, yMin, z2);
            GL11.glVertex3d(x2, yMax, z2);
            GL11.glVertex3d(x1, yMax, z1);
            GL11.glEnd();
            GL11.glLineWidth(2.0F);
            GL11.glColor4f(r / 255.0F, g / 255.0F, b / 255.0F, lineAlpha / 255.0F);
            GL11.glBegin(2);
            GL11.glVertex3d(x1, yMin, z1);
            GL11.glVertex3d(x2, yMin, z2);
            GL11.glVertex3d(x2, yMax, z2);
            GL11.glVertex3d(x1, yMax, z1);
            GL11.glEnd();
            GL11.glLineWidth(1.0F);
            GL11.glDepthMask(true);
            GL11.glEnable(2929);
            GL11.glEnable(2884);
            GL11.glEnable(3008);
            GL11.glEnable(3553);
            GL11.glDisable(3042);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glPopMatrix();
        }
    }

    @EventTarget
    public void onRenderTick(Render2DEvent event) {
        this.updateActivatePromptFade();
        this.drawActivatePrompt();
        if (this.running) {
            if (!this.detectManualCameraTakeover()) {
                this.applySmoothedRotation();
                this.autoPlaceOnRenderTick();
            }
        }
    }

    private void drawActivatePrompt() {
        if (!(this.promptAlpha < 0.05F)) {
            int[] display = this.getDisplaySize();
            if (display != null && display.length >= 2) {
                String text = "Activate?";
                int alpha = (int)(this.promptAlpha * 255.0F);
                if (alpha < 16) {
                    alpha = 16;
                }

                int color = alpha << 24 | this.promptFadeRgb;
                float x = display[0] / 2.0F - mc.field_71466_p.func_78256_a(text) / 2.0F;
                float y = display[1] / 2.0F + 10.0F;
                mc.field_71466_p.func_175063_a(text, x, y, color);
            }
        }
    }

    private void updateActivatePromptFade() {
        boolean show = this.armed && !this.running && this.activatePromptAt != 0L;
        if (show) {
            this.rememberActivationPromptColor();
        }

        long now = this.now();
        long elapsed = this.promptFadeLastAt == 0L ? 0L : Math.min(100L, now - this.promptFadeLastAt);
        this.promptFadeLastAt = now;
        float step = (float)elapsed / 200.0F;
        this.promptAlpha += show ? step : -step;
        if (this.promptAlpha < 0.0F) {
            this.promptAlpha = 0.0F;
        }

        if (this.promptAlpha > 1.0F) {
            this.promptAlpha = 1.0F;
        }
    }

    @EventTarget
    public void onLeftClick(LeftClickMouseEvent event) {
        if (this.running) {
            this.setKeyPressed("attack", false);
            event.setCancelled(true);
        } else if (!this.autoPlaceOnMouse(0, true)) {
            event.setCancelled(true);
        }
    }

    @EventTarget
    public void onRightClick(RightClickMouseEvent event) {
        if (this.running) {
            this.setKeyPressed("use", this.tellyAutoPlaceWindow);
            event.setCancelled(true);
        } else if (this.armed && this.activationSuppressUse() && Mouse.isButtonDown(1)) {
            event.setCancelled(true);
        } else if (!this.autoPlaceOnMouse(1, true)) {
            event.setCancelled(true);
        }
    }

    private void pollMouseTransitions() {
        boolean down = Mouse.isButtonDown(1);
        if (this.lastRmbDown && !down && this.armed && !this.running) {
            this.setActivationMovementHold(false);
        }

        this.lastRmbDown = down;
    }

    private void pollKeyTransitions() {
        int[] keys = new int[]{
            this.getKeyCode("drop"),
            this.getKeyCode("forward"),
            this.getKeyCode("back"),
            this.getKeyCode("left"),
            this.getKeyCode("right"),
            this.getKeyCode("jump"),
            this.getKeyCode("sneak"),
            this.getKeyCode("sprint")
        };

        for (int code : keys) {
            boolean down = KeyBindUtil.isKeyDown(code);
            Boolean was = this.lastKeyDown.get(code);
            if (was != null && !was && down) {
                this.onKeyPressed(code);
            } else if (was != null && was && !down) {
                this.onKeyReleased(code);
            }

            this.lastKeyDown.put(code, down);
        }
    }

    private void onKeyPressed(int keyCode) {
        if (this.isDropProtected() && keyCode == this.getKeyCode("drop")) {
            this.setKeyPressed("drop", false);
        } else if (this.running) {
            if (keyCode == this.getKeyCode("sneak")) {
                this.suppressSneakInput();
            } else {
                if (this.setupTick < 0
                    && this.isManualMovementKey(keyCode)
                    && !this.isInitialMovementHold(keyCode)
                    && !this.isScriptHeldKey(keyCode)) {
                    this.stopAutomation(true);
                }
            }
        }
    }

    private void onKeyReleased(int keyCode) {
        if (this.running
            || !this.activationMovementHeld
            || keyCode != this.getKeyCode("back") && keyCode != this.getKeyCode("right")) {
            if (this.running) {
                if (this.isManualMovementKey(keyCode)) {
                    this.clearInitialMovementHold(keyCode);
                }
            }
        } else {
            this.setKeyPressed("back", true);
            this.setKeyPressed("right", true);
        }
    }

    private void setActivationMovementHold(boolean hold) {
        if (hold) {
            this.activationMovementHeld = true;
            this.setKeyPressed("back", true);
            this.setKeyPressed("right", true);
        } else if (this.activationMovementHeld) {
            this.activationMovementHeld = false;
            this.setKeyPressed("back", KeyBindUtil.isKeyDown(this.getKeyCode("back")));
            this.setKeyPressed("right", KeyBindUtil.isKeyDown(this.getKeyCode("right")));
        }
    }

    private boolean isScriptHeldKey(int keyCode) {
        if (keyCode == this.getKeyCode("forward")) {
            return this.isPressed("forward");
        } else if (keyCode == this.getKeyCode("back")) {
            return this.isPressed("back");
        } else if (keyCode == this.getKeyCode("left")) {
            return this.isPressed("left");
        } else if (keyCode == this.getKeyCode("right")) {
            return this.isPressed("right");
        } else if (keyCode == this.getKeyCode("jump")) {
            return this.isPressed("jump");
        } else {
            return keyCode == this.getKeyCode("sprint") ? this.isPressed("sprint") : false;
        }
    }

    private boolean isManualMovementKey(int keyCode) {
        return keyCode == this.getKeyCode("forward")
            || keyCode == this.getKeyCode("back")
            || keyCode == this.getKeyCode("left")
            || keyCode == this.getKeyCode("right")
            || keyCode == this.getKeyCode("jump")
            || keyCode == this.getKeyCode("sneak")
            || keyCode == this.getKeyCode("sprint");
    }

    private void captureInitialMovementHolds() {
        this.ignoreForwardUntilRelease = KeyBindUtil.isKeyDown(this.getKeyCode("forward"));
        this.ignoreBackUntilRelease = KeyBindUtil.isKeyDown(this.getKeyCode("back"));
        this.ignoreLeftUntilRelease = KeyBindUtil.isKeyDown(this.getKeyCode("left"));
        this.ignoreRightUntilRelease = KeyBindUtil.isKeyDown(this.getKeyCode("right"));
        this.ignoreJumpUntilRelease = KeyBindUtil.isKeyDown(this.getKeyCode("jump"));
        this.ignoreSneakUntilRelease = KeyBindUtil.isKeyDown(this.getKeyCode("sneak"));
        this.ignoreSprintUntilRelease = KeyBindUtil.isKeyDown(this.getKeyCode("sprint"));
    }

    private boolean isInitialMovementHold(int keyCode) {
        if (keyCode == this.getKeyCode("forward")) {
            return this.ignoreForwardUntilRelease;
        } else if (keyCode == this.getKeyCode("back")) {
            return this.ignoreBackUntilRelease;
        } else if (keyCode == this.getKeyCode("left")) {
            return this.ignoreLeftUntilRelease;
        } else if (keyCode == this.getKeyCode("right")) {
            return this.ignoreRightUntilRelease;
        } else if (keyCode == this.getKeyCode("jump")) {
            return this.ignoreJumpUntilRelease;
        } else if (keyCode == this.getKeyCode("sneak")) {
            return this.ignoreSneakUntilRelease;
        } else {
            return keyCode == this.getKeyCode("sprint") ? this.ignoreSprintUntilRelease : false;
        }
    }

    private void clearInitialMovementHold(int keyCode) {
        if (keyCode == this.getKeyCode("forward")) {
            this.ignoreForwardUntilRelease = false;
        }

        if (keyCode == this.getKeyCode("back")) {
            this.ignoreBackUntilRelease = false;
        }

        if (keyCode == this.getKeyCode("left")) {
            this.ignoreLeftUntilRelease = false;
        }

        if (keyCode == this.getKeyCode("right")) {
            this.ignoreRightUntilRelease = false;
        }

        if (keyCode == this.getKeyCode("jump")) {
            this.ignoreJumpUntilRelease = false;
        }

        if (keyCode == this.getKeyCode("sneak")) {
            this.ignoreSneakUntilRelease = false;
        }

        if (keyCode == this.getKeyCode("sprint")) {
            this.ignoreSprintUntilRelease = false;
        }
    }

    private void clearInitialMovementHolds() {
        this.ignoreForwardUntilRelease = false;
        this.ignoreBackUntilRelease = false;
        this.ignoreLeftUntilRelease = false;
        this.ignoreRightUntilRelease = false;
        this.ignoreJumpUntilRelease = false;
        this.ignoreSneakUntilRelease = false;
        this.ignoreSprintUntilRelease = false;
    }

    private boolean detectManualCameraTakeover() {
        if (this.running && this.setupTick < 0 && this.now() >= this.takeoverDetectionAt) {
            Entity player = mc.field_71439_g;
            if (player == null) {
                return false;
            }

            long now = this.now();
            float expectedYaw = this.scriptedRotationYaw;
            float expectedPitch = this.scriptedRotationPitch;
            if (!this.takeoverCameraValid) {
                this.takeoverCameraValid = true;
                this.takeoverCameraYaw = player.field_70177_z;
                this.takeoverCameraPitch = player.field_70125_A;
                this.takeoverAccumulated = 0.0F;
                this.takeoverLastFrameAt = now;
                return false;
            }

            double yawInput = Math.abs(this.tellyWrapAngle(player.field_70177_z - expectedYaw));
            double pitchInput = Math.abs(player.field_70125_A - expectedPitch);
            double noiseFloor = 0.0153212175;
            long elapsed = Math.max(0L, now - this.takeoverLastFrameAt);
            this.takeoverLastFrameAt = now;
            this.takeoverAccumulated -= (float)(elapsed * 0.045);
            if (this.takeoverAccumulated < 0.0F) {
                this.takeoverAccumulated = 0.0F;
            }

            if (yawInput > noiseFloor || pitchInput > noiseFloor) {
                this.takeoverAccumulated += (float)(yawInput + pitchInput);
            }

            this.takeoverCameraYaw = player.field_70177_z;
            this.takeoverCameraPitch = player.field_70125_A;
            if (this.takeoverAccumulated >= 25.0F) {
                this.stopAutomation(true);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @EventTarget
    public void onPacketSent(PacketEvent event) {
        if (event.getType() == EventType.SEND) {
            this.joueur(event.getPacket());
        }
    }

    private void joueur(Packet<?> packet) {
        if (packet instanceof C07PacketPlayerDigging) {
            C07PacketPlayerDigging digging = (C07PacketPlayerDigging)packet;
            String status = digging.func_180762_c() == null ? "" : digging.func_180762_c().toString().toUpperCase();
            if (this.isDropProtected() && status.contains("DROP")) {
                return;
            }
        }

        if (!this.running) {
            if (!(packet instanceof C07PacketPlayerDigging)
                || !this.isDropProtected()
                || !String.valueOf(((C07PacketPlayerDigging)packet).func_180762_c()).toUpperCase().contains("DROP")) {
                ;
            }
        } else {
            if (packet instanceof C02PacketUseEntity) {
                C02PacketUseEntity interaction = (C02PacketUseEntity)packet;
                if (interaction.func_149565_c() == Action.ATTACK) {
                    return;
                }
            }

            if (packet instanceof C07PacketPlayerDigging) {
                C07PacketPlayerDigging digging = (C07PacketPlayerDigging)packet;
                String status = digging.func_180762_c() == null ? "" : digging.func_180762_c().toString().toUpperCase();
                if (status.contains("DESTROY")) {
                    return;
                }
            }

            if (packet instanceof C0BPacketEntityAction) {
                C0BPacketEntityAction action = (C0BPacketEntityAction)packet;
                if (action.func_180764_b()
                    == net.minecraft.network.play.client.C0BPacketEntityAction.Action.START_SNEAKING) {
                    return;
                }
            }

            int[] placedTarget = null;
            if (packet instanceof C08PacketPlayerBlockPlacement) {
                C08PacketPlayerBlockPlacement placement = (C08PacketPlayerBlockPlacement)packet;
                int direction = placement.func_149568_f();
                if (direction != 255) {
                    placedTarget = this.offsetPos(this.posFromPos(placement.func_179724_a()), direction);
                    if (!this.isStraightTellyTarget(placedTarget)) {
                        this.cancelledGhostBlocks.add(this.posKey(placedTarget));
                        return;
                    }
                }
            }

            boolean allowed = this.autoPlaceOnPacketSent(packet);
            if (allowed && placedTarget != null) {
                this.cancelledGhostBlocks.remove(this.posKey(placedTarget));
                this.latestStraightPlacedPos = new int[]{placedTarget[0], placedTarget[1], placedTarget[2]};
                if (this.firstTellyPlacementPending && this.setupTick < 0) {
                    this.firstTellyPlacementPending = false;
                    this.adaptiveAimValid = false;
                    this.adaptiveAimUpdatedAt = 0L;
                }
            }
        }
    }

    @EventTarget
    public void onPacketReceived(PacketEvent event) {
        if (event.getType() == EventType.RECEIVE) {
            Packet<?> packet = event.getPacket();
            if (this.running && packet instanceof S08PacketPlayerPosLook) {
                this.stopAutomation(true);
                event.setCancelled(true);
            } else {
                if (packet instanceof S23PacketBlockChange && !this.cancelledGhostBlocks.isEmpty()) {
                    S23PacketBlockChange change = (S23PacketBlockChange)packet;
                    if (change.func_179827_b() != null) {
                        this.cancelledGhostBlocks.remove(this.posKeyFromPos(change.func_179827_b()));
                    }
                }
            }
        }
    }

    private boolean isActivationInProgress() {
        return this.armed && !this.running && this.activatePromptAt != 0L;
    }

    private boolean isDropProtected() {
        return this.running || this.isActivationInProgress();
    }

    @EventTarget
    public void onPostPlayerInput(MoveInputEvent event) {
        if (this.running) {
            this.suppressSneakInput();
            this.enforceSafeWalkDisabledForRun();
            if (this.setupTick >= 0) {
                if (this.setupTick < 12) {
                    boolean setupJump = this.setupTick >= 6;
                    this.applyMovement(-1.0F, -1.0F, setupJump, false);
                    this.applyUse(true);
                    if (this.setupTick == 11) {
                        this.setRotationTarget(this.baseYaw + this.yawCurve[19], this.pitchCurve[19], 50L);
                    } else {
                        this.setRotationTarget(this.baseYaw, 74.52F, 50L);
                    }

                    this.setupTick++;
                    return;
                }

                this.setupTick = -1;
                this.takeoverDetectionAt = this.now() + 125L;
                this.takeoverCameraValid = mc.field_71439_g != null;
                this.takeoverAccumulated = 0.0F;
                this.takeoverLastFrameAt = this.now();
                if (mc.field_71439_g != null) {
                    this.takeoverCameraYaw = mc.field_71439_g.field_70177_z;
                    this.takeoverCameraPitch = mc.field_71439_g.field_70125_A;
                }

                this.captureInitialMovementHolds();
                this.cyclePhase = 19;
                this.firstTellyPlacementPending = true;
                this.adaptiveAimValid = false;
                this.clearCachedCandidate();
                this.updateAdaptivePlacementAim(mc.field_71439_g);
            }

            int phase = this.cyclePhase;
            float strafe = this.strafeCurve[phase];
            boolean sprinting = phase == 0 || phase == 1;
            boolean jumping = phase >= 1 && phase <= 19;
            boolean use = phase >= 7;
            this.applyMovement(this.forwardCurve[phase], strafe, jumping, sprinting);
            this.applyUse(use);
            int nextPhase = (phase + 1) % this.yawCurve.length;
            this.setRotationTarget(this.baseYaw + this.yawCurve[nextPhase], this.pitchCurve[nextPhase], 50L);
            this.cyclePhase = nextPhase;
        }
    }

    @EventTarget
    public void onPreMotion(UpdateEvent event) {
        if (this.running && event.getType() == EventType.PRE) {
            if (mc.field_71439_g != null) {
                float yaw = mc.field_71439_g.field_70177_z;
                float pitch = mc.field_71439_g.field_70125_A;
                this.autoPlaceOnPreMotion();
                if (this.silentPitchActive && !this.manualC08InWindow) {
                    pitch = this.sanitizePitch(this.silentPitch, pitch);
                }

                event.setRotation(yaw, pitch, 10);
                RotationUtil.serverYaw = yaw;
                RotationUtil.serverPitch = pitch;
            }
        }
    }

    private void autoPlaceOnPreMotion() {
    }

    @EventTarget
    public void onPostMotion(UpdateEvent event) {
        if (event.getType() == EventType.POST) {
            if (this.running) {
                this.c08CounterAtTickBoundary = this.totalC08Counter;
                this.manualC08InWindow = false;
            }
        }
    }

    private void armAutomation() {
        this.armed = true;
        this.running = false;
        this.activatePromptAt = 0L;
        this.promptBrokeAt = 0L;
        this.setupTick = 0;
        this.cyclePhase = 19;
        this.rotationActive = false;
        this.activationMovementHeld = false;
        this.printStatus("&eArmed. Sneak looking down, wait for green, hold rmb and release sneak");
    }

    private void beginAutomation() {
        Entity player = mc.field_71439_g;
        if (player == null || !this.isHoldingBlock(player)) {
            this.printStatus("&cHold blocks before starting");
        } else if (this.isActivationYawAligned(player.field_70177_z)) {
            this.disableSafeWalkForRun();
            this.baseYaw = player.field_70177_z;
            this.calculateTravelDirection(this.baseYaw);
            this.antiSwayLane = this.travelX != 0
                ? this.playerPosition(player).field_72449_c
                : this.playerPosition(player).field_72450_a;
            this.antiSwayYawOffset = 0.0F;
            this.antiSwayTapUsed = false;
            this.cancelledGhostBlocks.clear();
            this.initializeStraightBridgeLane(player);
            this.firstTellyPlacementPending = false;
            this.adaptiveAimValid = false;
            this.adaptiveAimUpdatedAt = 0L;
            this.setupTick = 0;
            this.cyclePhase = 19;
            this.armed = false;
            this.running = true;
            this.freezeLastTickAt = this.now();
            this.activationMovementHeld = false;
            this.tellyAutoPlaceWindow = true;
            this.scriptedRotationYaw = player.field_70177_z;
            this.scriptedRotationPitch = player.field_70125_A;
            this.takeoverDetectionAt = 0L;
            this.takeoverCameraValid = false;
            this.clearInitialMovementHolds();
            this.resetControllerState();
            this.setKeyPressed("attack", false);
            this.applyMovement(-1.0F, -1.0F, false, false);
            this.setRotationTarget(this.baseYaw, 74.52F, 50L);
            this.applyUse(true);
            this.printStatus("&aStarted");
        }
    }

    private void stopAutomation(boolean turnOffButton) {
        this.armed = false;
        this.running = false;
        this.setupTick = 0;
        this.cyclePhase = 19;
        this.rotationActive = false;
        this.activationMovementHeld = false;
        this.tellyAutoPlaceWindow = false;
        this.autoPlaceDebugActive = false;
        this.antiSwayYawOffset = 0.0F;
        this.antiSwayTapUsed = false;
        this.firstTellyPlacementPending = false;
        this.latestStraightPlacedPos = null;
        this.adaptiveAimValid = false;
        this.adaptiveAimUpdatedAt = 0L;
        this.scriptedRotationYaw = 0.0F;
        this.scriptedRotationPitch = 0.0F;
        this.takeoverDetectionAt = 0L;
        this.takeoverCameraValid = false;
        this.takeoverCameraYaw = 0.0F;
        this.takeoverCameraPitch = 0.0F;
        this.takeoverAccumulated = 0.0F;
        this.takeoverLastFrameAt = 0L;

        try {
            this.cancelledGhostBlocks.clear();
            this.clearInitialMovementHolds();
            this.resetControllerState();
            mc.field_71439_g.field_71158_b.field_78900_b = 0.0F;
            mc.field_71439_g.field_71158_b.field_78902_a = 0.0F;
            mc.field_71439_g.field_71158_b.field_78901_c = false;
            mc.field_71439_g.func_70031_b(false);
            this.releaseMovementKeys();
            this.restorePhysicalUse();
            this.setKeyPressed("attack", Mouse.isButtonDown(0));
        } catch (Exception var3) {
        }

        this.restoreSafeWalkState();
        this.freezeLastTickAt = 0L;
        this.armed = true;
        this.activatePromptAt = 0L;
        this.promptBrokeAt = 0L;
        if (turnOffButton) {
            this.printStatus("&eStopped. Sneak looking down to arm again");
        }
    }

    private void disableSafeWalkForRun() {
        if (this.safeWalkStateCaptured) {
            this.enforceSafeWalkDisabledForRun();
        } else if (this.disableSafeWalk.getValue()) {
            try {
                this.safeWalkWasEnabled = this.isSafeWalkEnabled();
                this.safeWalkStateCaptured = true;
                if (this.safeWalkWasEnabled) {
                    this.setSafeWalkEnabled(false);
                }
            } catch (Exception ignored) {
                this.safeWalkStateCaptured = false;
            }
        }
    }

    private void enforceSafeWalkDisabledForRun() {
        if (this.safeWalkStateCaptured) {
            try {
                if (this.isSafeWalkEnabled()) {
                    this.setSafeWalkEnabled(false);
                }
            } catch (Exception var2) {
            }
        }
    }

    private void restoreSafeWalkState() {
        if (this.safeWalkStateCaptured) {
            boolean restoreEnabled = this.safeWalkWasEnabled;
            this.safeWalkStateCaptured = false;

            try {
                boolean currentlyEnabled = this.isSafeWalkEnabled();
                if (restoreEnabled && !currentlyEnabled) {
                    this.setSafeWalkEnabled(true);
                }

                if (!restoreEnabled && currentlyEnabled) {
                    this.setSafeWalkEnabled(false);
                }
            } catch (Exception var3) {
            }
        }
    }

    private SafeWalk safeWalk() {
        return (SafeWalk)Miau.moduleManager.modules.get(SafeWalk.class);
    }

    private boolean isSafeWalkEnabled() {
        SafeWalk s = this.safeWalk();
        return s != null && s.isEnabled();
    }

    private void setSafeWalkEnabled(boolean on) {
        SafeWalk s = this.safeWalk();
        if (s != null) {
            s.setEnabled(on);
        }
    }

    private void printStatus(String message) {
        ChatUtil.display("§bTelly §7| " + message);
    }

    private void setRotationTarget(float targetYaw, float targetPitch, long duration) {
        Entity player = mc.field_71439_g;
        if (player != null) {
            this.applySmoothedRotation();
            this.rotationStartYaw = player.field_70177_z;
            this.rotationStartPitch = player.field_70125_A;
            float correctedTargetYaw = targetYaw;
            boolean adaptivePlacementTarget = this.running
                && this.tellyAutoPlaceWindow
                && this.firstTellyPlacementPending
                && this.adaptiveAimValid
                && this.now() - this.adaptiveAimUpdatedAt <= 125L;
            if (adaptivePlacementTarget) {
                correctedTargetYaw = this.adaptiveAimYaw;
                targetPitch = this.adaptiveAimPitch;
            } else if (this.running) {
                correctedTargetYaw += this.antiSwayYawOffset;
            }

            this.rotationStepCounter++;
            correctedTargetYaw += (float)(0.03404715 * this.YAW_NUDGE_PATTERN[this.rotationStepCounter % 5]);
            this.rotationTargetYaw = this.rotationStartYaw
                + this.tellyWrapAngle(correctedTargetYaw - this.rotationStartYaw);
            this.rotationTargetPitch = this.clamp(targetPitch, -90.0F, 90.0F);
            this.rotationStartedAt = this.now();
            this.rotationDuration = Math.max(1L, duration);
            this.rotationActive = true;
        }
    }

    private void applySmoothedRotation() {
        if (this.rotationActive) {
            Entity player = mc.field_71439_g;
            if (player != null) {
                double progress = (double)(this.now() - this.rotationStartedAt) / this.rotationDuration;
                if (progress < 0.0) {
                    progress = 0.0;
                }

                if (progress > 1.0) {
                    progress = 1.0;
                }

                float desiredYaw = this.rotationStartYaw
                    + (this.rotationTargetYaw - this.rotationStartYaw) * (float)progress;
                float desiredPitch = this.rotationStartPitch
                    + (this.rotationTargetPitch - this.rotationStartPitch) * (float)progress;
                float quantizedYaw = this.quantizeFrom(this.rotationStartYaw, desiredYaw);
                float quantizedPitch = this.quantizeFrom(this.rotationStartPitch, desiredPitch);
                this.scriptedRotationYaw = quantizedYaw;
                this.scriptedRotationPitch = this.clamp(quantizedPitch, -90.0F, 90.0F);
                player.field_70177_z = this.scriptedRotationYaw;
                player.field_70125_A = this.scriptedRotationPitch;
                if (progress >= 1.0) {
                    this.rotationActive = false;
                }
            }
        }
    }

    private float quantizeFrom(float origin, float value) {
        double steps = Math.round((value - origin) / 0.03404715);
        return (float)(origin + steps * 0.03404715);
    }

    private void applyMovement(float forward, float strafe, boolean jumping, boolean sprinting) {
        float controlledForward = forward;
        boolean controlledSprint = sprinting;
        float correctedStrafe = strafe;
        boolean antiSway = this.running;
        if (antiSway) {
            correctedStrafe = this.applyAntiSwayCorrection(controlledForward, strafe);
        } else {
            this.antiSwayYawOffset = 0.0F;
        }

        this.setKeyPressed("forward", controlledForward > 0.03F);
        this.setKeyPressed("back", controlledForward < -0.03F);
        this.setKeyPressed("left", correctedStrafe > 0.5F);
        this.setKeyPressed("right", correctedStrafe < -0.5F);
        this.setKeyPressed("jump", jumping);
        this.setKeyPressed("sprint", controlledSprint);
        mc.field_71439_g.field_71158_b.field_78900_b = controlledForward;
        mc.field_71439_g.field_71158_b.field_78902_a = correctedStrafe;
        mc.field_71439_g.field_71158_b.field_78901_c = jumping;
        mc.field_71439_g.field_71158_b.field_78899_d = false;
        mc.field_71439_g.func_70031_b(controlledSprint);
    }

    private void suppressSneakInput() {
        this.setKeyPressed("sneak", false);
        mc.field_71439_g.field_71158_b.field_78899_d = false;
    }

    private void calculateTravelDirection(float yaw) {
        double radians = Math.toRadians(yaw);
        double rawX = Math.sin(radians) - Math.cos(radians);
        double rawZ = -Math.cos(radians) - Math.sin(radians);
        if (Math.abs(rawX) >= Math.abs(rawZ)) {
            this.travelX = rawX >= 0.0 ? 1 : -1;
            this.travelZ = 0;
        } else {
            this.travelX = 0;
            this.travelZ = rawZ >= 0.0 ? 1 : -1;
        }
    }

    private void initializeStraightBridgeLane(Entity player) {
        Vec3 position = this.playerPosition(player);
        int startX = this.floor(position.field_72450_a);
        int startY = this.floor(position.field_72448_b) - 1;
        int startZ = this.floor(position.field_72449_c);
        this.bridgeLaneBlock = this.travelX != 0 ? startZ : startX;
        this.bridgeStartProgress = startX * this.travelX + startZ * this.travelZ;
        Object[] hit = this.raycastBlock(4.5, player.field_70177_z, player.field_70125_A);
        if (hit != null && hit.length > 0 && hit[0] instanceof Vec3) {
            int[] hitPos = this.posFromVec((Vec3)hit[0]);
            int hitLane = this.travelX != 0 ? hitPos[2] : hitPos[0];
            int hitProgress = this.straightProgress(hitPos);
            if (hitLane == this.bridgeLaneBlock
                && Math.abs(hitPos[0] - startX) <= 2
                && Math.abs(hitPos[2] - startZ) <= 2
                && hitProgress < this.bridgeStartProgress) {
                this.bridgeStartProgress = hitProgress;
            }
        }

        this.latestStraightPlacedPos = new int[]{startX, startY, startZ};
    }

    private int straightProgress(int[] position) {
        return position == null ? Integer.MIN_VALUE : position[0] * this.travelX + position[2] * this.travelZ;
    }

    private boolean isStraightTellyTarget(int[] position) {
        if (this.running && position != null) {
            int lane = this.travelX != 0 ? position[2] : position[0];
            return lane != this.bridgeLaneBlock ? false : this.straightProgress(position) >= this.bridgeStartProgress;
        } else {
            return true;
        }
    }

    private void updateAdaptivePlacementAim(Entity player) {
        if (this.firstTellyPlacementPending) {
            Object[] candidate = this.cachedCandidate;
            if (candidate != null) {
                int[] target = this.candidatePlacedPos(candidate);
                Vec3 hitVec = this.candidateHitVec(candidate);
                if (this.isStraightTellyTarget(target) && hitVec != null) {
                    this.setAdaptiveAimToPoint(player, hitVec);
                    return;
                }
            }

            int[] support = this.latestStraightPlacedPos != null ? this.latestStraightPlacedPos : this.lastPlacedPos;
            if (support != null && this.isStraightTellyTarget(support)) {
                int face = this.travelX > 0 ? 5 : (this.travelX < 0 ? 4 : (this.travelZ > 0 ? 3 : 2));
                int[] nextTarget = this.offsetPos(support, face);
                if (this.isStraightTellyTarget(nextTarget)
                    && this.isReplaceable(nextTarget[0], nextTarget[1], nextTarget[2])) {
                    Vec3 fallbackHit = this.getSupportFaceHitVec(support, face, 0.5, 0.5);
                    this.setAdaptiveAimToPoint(player, fallbackHit);
                }
            }
        }
    }

    private void setAdaptiveAimToPoint(Entity player, Vec3 point) {
        if (player != null && point != null) {
            Vec3 position = this.playerPosition(player);
            double eyeX = position.field_72450_a;
            double eyeY = position.field_72448_b + player.func_70047_e();
            double eyeZ = position.field_72449_c;
            double dx = point.field_72450_a - eyeX;
            double dy = point.field_72448_b - eyeY;
            double dz = point.field_72449_c - eyeZ;
            double horizontal = Math.sqrt(dx * dx + dz * dz);
            if (!(horizontal < 1.0E-5) || !(Math.abs(dy) < 1.0E-5)) {
                this.adaptiveAimYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
                this.adaptiveAimPitch = this.clamp((float)(-Math.toDegrees(Math.atan2(dy, horizontal))), -89.0F, 89.0F);
                this.adaptiveAimUpdatedAt = this.now();
                this.adaptiveAimValid = true;
            }
        }
    }

    private float applyAntiSwayCorrection(float forward, float recordedStrafe) {
        Entity player = mc.field_71439_g;
        if (player == null) {
            return recordedStrafe;
        }

        Vec3 position = this.playerPosition(player);
        Vec3 motion = this.playerMotion();
        double lanePosition = this.travelX != 0 ? position.field_72449_c : position.field_72450_a;
        double laneVelocity = motion == null ? 0.0 : (this.travelX != 0 ? motion.field_72449_c : motion.field_72450_a);
        double error = this.antiSwayLane - lanePosition;
        if (Math.abs(error) < 0.015 && Math.abs(laneVelocity) < 0.008) {
            this.antiSwayTapUsed = false;
            this.antiSwayYawOffset *= 0.65F;
            if (Math.abs(this.antiSwayYawOffset) < 0.03F) {
                this.antiSwayYawOffset = 0.0F;
            }

            return recordedStrafe;
        } else {
            double desiredLaneVelocity = error * 0.42 - laneVelocity * 0.78;
            if (desiredLaneVelocity > 0.16) {
                desiredLaneVelocity = 0.16;
            }

            if (desiredLaneVelocity < -0.16) {
                desiredLaneVelocity = -0.16;
            }

            double velocityCorrection = desiredLaneVelocity - laneVelocity;
            double radians = Math.toRadians(player.field_70177_z);
            double sin = Math.sin(radians);
            double cos = Math.cos(radians);
            double yawLaneDerivative = this.travelX != 0
                ? -forward * sin + recordedStrafe * cos
                : -forward * cos - recordedStrafe * sin;
            double desiredYawOffset = 0.0;
            if (Math.abs(yawLaneDerivative) >= 0.12) {
                desiredYawOffset = Math.toDegrees(velocityCorrection * 0.55 / yawLaneDerivative);
            }

            if (desiredYawOffset > 2.25) {
                desiredYawOffset = 2.25;
            }

            if (desiredYawOffset < -2.25) {
                desiredYawOffset = -2.25;
            }

            this.antiSwayYawOffset = this.antiSwayYawOffset * 0.6F + (float)desiredYawOffset * 0.4F;
            double strafeLaneAxis = this.travelX != 0 ? sin : cos;
            boolean tapHelps = Math.abs(strafeLaneAxis) >= 0.2 && velocityCorrection * strafeLaneAxis > 0.0;
            if (tapHelps && !this.antiSwayTapUsed && Math.abs(velocityCorrection) >= 0.03 && recordedStrafe < 0.5F) {
                this.antiSwayTapUsed = true;
                return recordedStrafe + 1.0F;
            } else {
                return recordedStrafe;
            }
        }
    }

    private void applyUse(boolean pressed) {
        if (pressed && !this.autoPlaceDebugActive) {
            this.printStatus("&aAutoPlace activated");
        }

        this.autoPlaceDebugActive = pressed;
        this.tellyAutoPlaceWindow = pressed;
        this.setKeyPressed("use", pressed);
    }

    private void restorePhysicalUse() {
        this.tellyAutoPlaceWindow = false;
        this.autoPlaceDebugActive = false;
        this.setKeyPressed("use", Mouse.isButtonDown(1));
    }

    private void releaseMovementKeys() {
        this.restorePhysicalKey("forward");
        this.restorePhysicalKey("back");
        this.restorePhysicalKey("left");
        this.restorePhysicalKey("right");
        this.restorePhysicalKey("jump");
        this.restorePhysicalKey("sneak");
        this.restorePhysicalKey("sprint");
    }

    private void restorePhysicalKey(String key) {
        int code = this.getKeyCode(key);
        this.setKeyPressed(key, code >= 0 && KeyBindUtil.isKeyDown(code));
    }

    private float tellyWrapAngle(float angle) {
        while (angle <= -180.0F) {
            angle += 360.0F;
        }

        while (angle > 180.0F) {
            angle -= 360.0F;
        }

        return angle;
    }

    private float clamp(float value, float minimum, float maximum) {
        if (value < minimum) {
            return minimum;
        } else {
            return value > maximum ? maximum : value;
        }
    }

    private void autoPlaceOnEnable() {
        this.setKeyPressed("attack", false);
        this.resetControllerState();
    }

    private void autoPlaceOnDisable() {
        this.resetControllerState();
        this.restoreUseToPhysicalState();
        this.setKeyPressed("attack", false);
        bridge.remove("AutoPlacePlacing");
        this.releaseExperimentalPlacementClaim();
    }

    private void resetControllerState() {
        this.currentClientTick = Integer.MIN_VALUE;
        this.placementEvaluationTick = Integer.MIN_VALUE;
        this.lastPlacementAttemptTick = Integer.MIN_VALUE;
        this.lastSuccessfulPlaceTick = Integer.MIN_VALUE;
        this.forceSuppressTick = Integer.MIN_VALUE;
        this.totalC08Counter = 0L;
        this.c08CounterAtTickBoundary = 0L;
        this.hasLastSentServerPos = false;
        this.clearCachedCandidate();
        this.lastPlacedPos = null;
        this.lastSupportPos = null;
        this.lastSupportFace = -1;
        this.cachedBelowTargets = null;
        this.cachedBelowTargetsTick = Integer.MIN_VALUE;
        this.rejectedTargets.clear();
        this.forcedModeCheck = 0;
        this.useSuppressed = false;
        this.silentPitchActive = false;
        this.placingViaModule = false;
        this.manualC08InWindow = false;
    }

    private void autoPlaceOnWorldJoin() {
        this.resetControllerState();
    }

    private void autoPlaceOnPreUpdate() {
        Entity player = mc.field_71439_g;
        if (player != null) {
            this.syncPlacementTick(player);
            if (this.placementEvaluationTick != this.currentClientTick) {
                this.placementEvaluationTick = this.currentClientTick;
                this.processAutoPlaceTick(player);
            }
        }
    }

    private void syncPlacementTick(Entity player) {
        int tick = this.placementTick(player);
        if (tick != this.currentClientTick) {
            this.currentClientTick = tick;
            this.candidateResolvedThisTick = false;
            this.silentPitchActive = false;
        }
    }

    private boolean useExtendedSearch() {
        return true;
    }

    private void autoPlaceOnPostMotion() {
        this.c08CounterAtTickBoundary = this.totalC08Counter;
        this.manualC08InWindow = false;
    }

    private void autoPlaceOnRenderTick() {
        Entity player = mc.field_71439_g;
        if (player != null) {
            if (this.isAutoPlaceActiveWindow(player)) {
                ItemStack heldStack = this.heldItem(player);
                if (this.isUsableBlockStack(heldStack)) {
                    float basePitch = this.sanitizePitch(player.field_70125_A, player.field_70125_A);
                    Object[] candidate = this.resolveCandidateWithOffCursorSilentPitch(
                        player, player.field_70177_z, basePitch, heldStack
                    );
                    if (candidate != null) {
                        this.silentPitch = this.sanitizePitch(this.candidatePitch(candidate), basePitch);
                        this.silentPitchActive = true;
                        this.suppressUse();
                    }
                }
            }
        }
    }

    private boolean autoPlaceOnMouse(int button, boolean state) {
        if (state && (button == 0 || button == 1)) {
            if (button == 1 && this.shouldCancelAutoPlaceUseItem()) {
                this.suppressUse();
                return false;
            }

            if (!this.shouldSuppressManualClicksThisTick()) {
                return true;
            }

            this.setKeyPressed("attack", false);
            return false;
        } else {
            return true;
        }
    }

    private boolean shouldSuppressManualClicksThisTick() {
        return !this.isInGameContext()
            ? false
            : this.lastSuccessfulPlaceTick == this.currentClientTick
                || this.forceSuppressTick == this.currentClientTick;
    }

    private boolean shouldCancelAutoPlaceUseItem() {
        if (!this.isInGameContext()) {
            return false;
        } else {
            return this.shouldSuppressManualClicksThisTick() ? true : this.useSuppressed && this.silentPitchActive;
        }
    }

    private void suppressUse() {
        this.setKeyPressed("use", false);
        this.useSuppressed = true;
    }

    private void restoreUseToPhysicalState() {
        this.setKeyPressed("use", this.running ? this.tellyAutoPlaceWindow : Mouse.isButtonDown(1));
        this.useSuppressed = false;
    }

    private boolean isInGameContext() {
        return mc.field_71439_g != null && mc.field_71462_r == null;
    }

    private boolean areAutoPlaceConditionsMet(Entity player) {
        return !this.tellyAutoPlaceWindow ? false : this.isUsableBlockStack(this.heldItem(player));
    }

    private boolean isAutoPlaceActiveWindow(Entity player) {
        if (!this.isInGameContext()) {
            return false;
        } else if (bridge.containsKey("ScaffoldRunning")) {
            return false;
        } else {
            return !this.areAutoPlaceConditionsMet(player) ? false : this.isUsableBlockStack(this.heldItem(player));
        }
    }

    private boolean isUsableBlockStack(ItemStack stack) {
        if (stack != null && this.isBlockStack(stack) && stack.func_77973_b() != null && stack.field_77994_a > 0) {
            String name = this.stackName(stack).toLowerCase();

            for (String bad : this.UNPLACEABLE_EXACT) {
                if (name.equals(bad)) {
                    return false;
                }
            }

            for (String bad : this.UNPLACEABLE_CONTAINS) {
                if (name.contains(bad)) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean isBlockBelowPlayerReplaceable(Entity player) {
        Vec3 pos = this.playerPosition(player);
        return this.isReplaceable(
            this.floor(pos.field_72450_a), this.floor(pos.field_72448_b) - 1, this.floor(pos.field_72449_c)
        );
    }

    private boolean placedInCurrentWindow() {
        return this.totalC08Counter > this.c08CounterAtTickBoundary;
    }

    private boolean claimExperimentalPlacementTick() {
        Object tickValue = bridge.get("PlacementArbiterTick");
        Object ownerValue = bridge.get("PlacementArbiterOwner");
        if (tickValue instanceof Number
            && ((Number)tickValue).intValue() == this.currentClientTick
            && ownerValue != null
            && !"BslLegitTellyFix".equals(String.valueOf(ownerValue))) {
            return false;
        }

        bridge.put("PlacementArbiterTick", this.currentClientTick);
        bridge.put("PlacementArbiterOwner", "BslLegitTellyFix");
        return true;
    }

    private void releaseExperimentalPlacementClaim() {
        Object ownerValue = bridge.get("PlacementArbiterOwner");
        if (ownerValue != null && "BslLegitTellyFix".equals(String.valueOf(ownerValue))) {
            bridge.remove("PlacementArbiterTick");
            bridge.remove("PlacementArbiterOwner");
        }
    }

    private void processAutoPlaceTick(Entity player) {
        this.pruneRejectedTargets();
        if (this.lastPlacedPos != null
            && !this.isSupportAvailable(this.lastPlacedPos[0], this.lastPlacedPos[1], this.lastPlacedPos[2])) {
            this.lastPlacedPos = null;
            this.lastSupportPos = null;
            this.lastSupportFace = -1;
        }

        if (!this.isAutoPlaceActiveWindow(player)) {
            this.clearCachedCandidate();
            bridge.remove("AutoPlacePlacing");
            if (this.useSuppressed) {
                this.restoreUseToPhysicalState();
            }
        } else {
            ItemStack heldStack = this.heldItem(player);
            if (!this.isUsableBlockStack(heldStack)) {
                this.clearCachedCandidate();
                if (this.useSuppressed) {
                    this.restoreUseToPhysicalState();
                }
            } else if (!this.isBlockBelowPlayerReplaceable(player)) {
                this.clearCachedCandidate();
                if (this.useSuppressed) {
                    this.restoreUseToPhysicalState();
                }
            } else {
                float yaw = player.field_70177_z;
                float basePitch = this.sanitizePitch(player.field_70125_A, player.field_70125_A);
                Object[] candidate = this.resolveCandidateWithOffCursorSilentPitch(player, yaw, basePitch, heldStack);
                if (candidate != null) {
                    this.silentPitch = this.sanitizePitch(this.candidatePitch(candidate), basePitch);
                    this.silentPitchActive = true;
                    this.suppressUse();
                } else if (this.useSuppressed
                    && !this.placedInCurrentWindow()
                    && this.lastPlacementAttemptTick != this.currentClientTick) {
                    this.restoreUseToPhysicalState();
                }

                if (this.placedInCurrentWindow() || this.lastPlacementAttemptTick == this.currentClientTick) {
                    this.suppressUse();
                } else if (candidate == null) {
                    this.clearCachedCandidate();
                } else if (!this.claimExperimentalPlacementTick()) {
                    this.clearCachedCandidate();
                } else {
                    bridge.put("AutoPlacePlacing", Boolean.TRUE);
                    this.lastPlacementAttemptTick = this.currentClientTick;
                    if (!this.attemptPlacement(player, candidate, heldStack)) {
                        if (!this.placedInCurrentWindow()) {
                            float retryYaw = player.field_70177_z;
                            float retryPitch = player.field_70125_A;
                            this.clearCachedCandidate();
                            Object[] retryCandidate = this.findBelowPlacement(
                                player,
                                retryYaw,
                                retryPitch,
                                heldStack,
                                this.now() + (this.useExtendedSearch() ? 4L : 2L)
                            );
                            this.cacheCandidate(retryCandidate, retryYaw, retryPitch);
                            if (retryCandidate != null) {
                                this.silentPitch = this.sanitizePitch(this.candidatePitch(retryCandidate), retryPitch);
                                this.silentPitchActive = true;
                                if (this.attemptPlacement(player, retryCandidate, heldStack)) {
                                    return;
                                }
                            }

                            this.releaseExperimentalPlacementClaim();
                        }
                    }
                }
            }
        }
    }

    private boolean attemptPlacement(Entity player, Object[] candidate, ItemStack heldStack) {
        if (candidate == null) {
            return false;
        } else {
            int[] placedPos = this.candidatePlacedPos(candidate);
            int[] supportPos = this.candidateSupportPos(candidate);
            int face = this.candidateFace(candidate);
            if (placedPos == null || supportPos == null || face <= 0) {
                return false;
            } else if (!this.isStraightTellyTarget(placedPos)) {
                return false;
            } else if (!this.isBlockBelowPlayerReplaceable(player)) {
                return false;
            } else if (!this.isUsableBlockStack(this.heldItem(player))) {
                return false;
            } else if (this.placedInCurrentWindow()) {
                return false;
            } else {
                float placementPitch = this.sanitizePitch(this.candidatePitch(candidate), player.field_70125_A);
                Object[] prePlaceHit = this.resolveVerifiedHit(
                    player.field_70177_z, placementPitch, supportPos, face, placedPos
                );
                if (prePlaceHit == null) {
                    return false;
                } else if (this.cancelledGhostBlocks.contains(this.posKey(supportPos))) {
                    return false;
                } else if (!this.isReplaceable(placedPos[0], placedPos[1], placedPos[2])) {
                    return false;
                } else if (!this.isSupportAvailable(supportPos[0], supportPos[1], supportPos[2])) {
                    return false;
                } else if (this.doesPlacementIntersectPlayer(player, placedPos)) {
                    return false;
                } else {
                    long counterBefore = this.totalC08Counter;
                    Vec3 hitAbs = (Vec3)prePlaceHit[2];
                    this.placingViaModule = true;
                    boolean placed = this.placeBlock(
                        supportPos[0], supportPos[1], supportPos[2], this.faceName(face), hitAbs
                    );
                    this.placingViaModule = false;
                    boolean packetSent = this.totalC08Counter > counterBefore;
                    if (!placed && !packetSent) {
                        return false;
                    } else if (!packetSent) {
                        this.markRejectedTarget(placedPos);
                        return false;
                    } else {
                        this.lastPlacedPos = placedPos;
                        this.lastSupportPos = supportPos;
                        this.lastSupportFace = face;
                        this.lastSuccessfulPlaceTick = this.currentClientTick;
                        this.forceSuppressTick = this.currentClientTick;
                        mc.field_71439_g.func_71038_i();
                        return true;
                    }
                }
            }
        }
    }

    private Object[] resolveVerifiedHit(
        float yaw, float pitch, int[] expectedSupport, int expectedFace, int[] expectedPlaced
    ) {
        Object[] traced = this.rayCast(yaw, pitch);
        if (traced == null) {
            return null;
        } else {
            int[] tracedSupport = (int[])traced[0];
            int tracedFace = (Integer)traced[1];
            if (this.posEquals(tracedSupport, expectedSupport) && tracedFace == expectedFace) {
                int[] tracedPlaced = this.offsetPos(tracedSupport, tracedFace);
                return !this.posEquals(tracedPlaced, expectedPlaced) ? null : traced;
            } else {
                return null;
            }
        }
    }

    private Object[] resolveCandidateWithOffCursorSilentPitch(
        Entity player, float yaw, float basePitch, ItemStack heldStack
    ) {
        float safeBasePitch = this.sanitizePitch(basePitch, player.field_70125_A);
        Object[] previousCandidate = this.cachedCandidate;
        Object[] baseCandidate = this.resolveCandidateForCurrentTick(player, yaw, safeBasePitch, heldStack);
        if (baseCandidate == null) {
            if (previousCandidate != null) {
                float previousBlockPitch = this.getBlockDerivedSilentPitch(player, previousCandidate, safeBasePitch);
                Object[] recovered = this.resolveCandidateForCurrentTick(player, yaw, previousBlockPitch, heldStack);
                if (recovered != null) {
                    return recovered;
                }

                this.cacheCandidate(previousCandidate, yaw, safeBasePitch);
                return previousCandidate;
            } else {
                return null;
            }
        } else {
            if (this.isPlacementLookAligned(
                yaw,
                safeBasePitch,
                this.candidateSupportPos(baseCandidate),
                this.candidateFace(baseCandidate),
                this.candidatePlacedPos(baseCandidate)
            )) {
                return baseCandidate;
            }

            float blockPitch = this.getBlockDerivedSilentPitch(player, baseCandidate, safeBasePitch);
            if (this.isPlacementLookAligned(
                yaw,
                blockPitch,
                this.candidateSupportPos(baseCandidate),
                this.candidateFace(baseCandidate),
                this.candidatePlacedPos(baseCandidate)
            )) {
                return new Object[]{
                    blockPitch,
                    this.candidateSupportPos(baseCandidate),
                    this.candidateFace(baseCandidate),
                    this.candidateHitVec(baseCandidate),
                    this.candidatePlacedPos(baseCandidate)
                };
            }

            Object[] corrected = this.resolveCandidateForCurrentTick(player, yaw, blockPitch, heldStack);
            if (corrected != null
                && this.posEquals(this.candidatePlacedPos(baseCandidate), this.candidatePlacedPos(corrected))) {
                return corrected;
            }

            this.cacheCandidate(baseCandidate, yaw, safeBasePitch);
            return baseCandidate;
        }
    }

    private Object[] resolveCandidateForCurrentTick(Entity player, float yaw, float pitch, ItemStack heldStack) {
        float safePitch = this.sanitizePitch(pitch, player.field_70125_A);
        if (this.hasCachedCandidateForCurrentTick(yaw, safePitch)) {
            return this.cachedCandidate;
        }

        Object[] candidate = this.findBelowPlacement(
            player, yaw, safePitch, heldStack, this.now() + (this.useExtendedSearch() ? 8L : 4L)
        );
        this.cacheCandidate(candidate, yaw, safePitch);
        return candidate;
    }

    private float getBlockDerivedSilentPitch(Entity player, Object[] candidate, float fallbackPitch) {
        if (candidate == null) {
            return this.sanitizePitch(fallbackPitch, fallbackPitch);
        }

        Vec3 hitVec = this.candidateHitVec(candidate);
        if (hitVec != null) {
            Float derived = this.computePitchToHitVec(player, hitVec);
            if (derived != null) {
                return this.sanitizePitch(derived, fallbackPitch);
            }
        }

        return this.sanitizePitch(this.candidatePitch(candidate), fallbackPitch);
    }

    private void cacheCandidate(Object[] candidate, float yaw, float pitch) {
        this.cachedCandidate = candidate;
        this.cachedCandidateTick = this.currentClientTick;
        this.cachedCandidateYaw = yaw;
        this.cachedCandidatePitch = pitch;
        this.candidateResolvedThisTick = candidate != null;
    }

    private boolean hasCachedCandidateForCurrentTick(float yaw, float pitch) {
        if (this.cachedCandidateTick != this.currentClientTick
            || !this.candidateResolvedThisTick
            || this.cachedCandidate == null) {
            return false;
        } else {
            return !Float.isNaN(this.cachedCandidateYaw) && !Float.isNaN(this.cachedCandidatePitch)
                ? Math.abs(this.wrapAngle(yaw - this.cachedCandidateYaw)) <= 0.75F
                    && Math.abs(pitch - this.cachedCandidatePitch) <= 0.75F
                : false;
        }
    }

    private void clearCachedCandidate() {
        this.cachedCandidate = null;
        this.cachedCandidateTick = Integer.MIN_VALUE;
        this.cachedCandidateYaw = Float.NaN;
        this.cachedCandidatePitch = Float.NaN;
        this.candidateResolvedThisTick = false;
    }

    private Object[] findBelowPlacement(
        Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs
    ) {
        if (this.now() >= deadlineMs) {
            return null;
        }

        Object[] cursorRayCandidate = this.findDirectCursorRayPlacement(player, yaw, currentPitch, heldStack);
        if (cursorRayCandidate != null) {
            return cursorRayCandidate;
        }

        int currentY = this.getCurrentBelowTargetY(player);
        int strictY = this.getStrictBelowTargetY(player);
        int previousY = this.getPreviousBelowTargetY(player);
        int[] feetPos = this.getFeetBelowTargetAtY(player, currentY);
        List<int[]> targets = new ArrayList<>();
        this.addBelowTarget(player, targets, feetPos);
        this.addBelowTarget(player, targets, this.offsetPos(feetPos, this.facingFromYaw(yaw)));

        for (int dy = 0; dy <= 2; dy++) {
            int targetY = dy == 0 ? currentY : (dy == 1 ? strictY : previousY);
            if (targetY != Integer.MIN_VALUE
                && (dy != 1 || targetY != currentY)
                && (dy != 2 || targetY != currentY && targetY != strictY)) {
                this.addBelowTarget(player, targets, new int[]{feetPos[0], targetY, feetPos[2]});
            }
        }

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                this.addBelowTarget(player, targets, new int[]{feetPos[0] + dx, currentY, feetPos[2] + dz});
                if (strictY != currentY) {
                    this.addBelowTarget(player, targets, new int[]{feetPos[0] + dx, strictY, feetPos[2] + dz});
                }

                if (previousY != currentY && previousY != strictY) {
                    this.addBelowTarget(player, targets, new int[]{feetPos[0] + dx, previousY, feetPos[2] + dz});
                }
            }
        }

        if (!player.field_70122_E) {
            this.addBelowTarget(player, targets, this.getMotionBelowTargetAtY(player, currentY, 1.0));
            if (previousY != currentY && previousY != strictY) {
                this.addBelowTarget(player, targets, this.getMotionBelowTargetAtY(player, previousY, 1.0));
            }
        }

        Object[] bestCandidate = null;
        double bestScore = Double.POSITIVE_INFINITY;

        for (int[] targetPos : targets) {
            if (this.now() >= deadlineMs) {
                return null;
            }

            if (this.isPlacementTargetAvailable(player, targetPos)) {
                Object[] candidate = this.findPitchPlacementForTarget(
                    player, yaw, currentPitch, targetPos, heldStack, null, -1, deadlineMs, false, true
                );
                if (candidate != null) {
                    double score = this.scorePlacementCandidate(
                        player, currentPitch, this.candidatePitch(candidate), this.candidateFace(candidate), 0.5, 0.5
                    );
                    if (score < bestScore) {
                        bestScore = score;
                        bestCandidate = candidate;
                    }
                }
            }
        }

        return bestCandidate;
    }

    private Object[] findDirectCursorRayPlacement(Entity player, float yaw, float pitch, ItemStack heldStack) {
        if (!this.isUsableBlockStack(heldStack)) {
            return null;
        }

        Object[] traced = this.rayCast(yaw, pitch);
        if (traced == null) {
            return null;
        }

        int[] supportPos = (int[])traced[0];
        int face = (Integer)traced[1];
        if (face == 0) {
            return null;
        }

        int[] targetPos = this.offsetPos(supportPos, face);
        if (!this.isPlacementTargetAvailable(player, targetPos)) {
            return null;
        }

        if (!this.isSupportAvailable(supportPos[0], supportPos[1], supportPos[2])) {
            return null;
        }

        if (this.shouldRejectStraightSideSwitch(player, targetPos, face)) {
            return null;
        }

        float tracedPitch = this.clampFloat(pitch, -89.0F, 89.0F);
        return new Object[]{tracedPitch, supportPos, face, (Vec3)traced[2], targetPos};
    }

    private boolean isCursorDirectedAtBlock(float yaw, float pitch) {
        return this.rayCast(yaw, pitch) != null;
    }

    private boolean isStraightCenterBelowAir(Entity player) {
        Vec3 pos = this.playerPosition(player);
        return this.isReplaceableName(
            this.blockNameAt(
                this.floor(pos.field_72450_a), this.getCurrentBelowTargetY(player), this.floor(pos.field_72449_c)
            ),
            true
        );
    }

    private boolean isStraightPreviousTickCenterOnGroundSupport(Entity player) {
        Vec3 last = this.playerPositionLast(player);
        return !this.isReplaceableName(
            this.blockNameAt(
                this.floor(last.field_72450_a), this.floor(last.field_72448_b) - 1, this.floor(last.field_72449_c)
            ),
            true
        );
    }

    private boolean isNearStraightSupportEdge(Entity player) {
        if (this.lastSupportPos != null && this.lastSupportFace >= 2) {
            Vec3 pos = this.playerPosition(player);
            double localX = pos.field_72450_a - this.lastSupportPos[0];
            double localZ = pos.field_72449_c - this.lastSupportPos[2];
            if (this.isPastStraightSupportEdgeThreshold(this.lastSupportFace, localX, localZ)) {
                return true;
            } else {
                Vec3 motion = this.playerMotion();
                if (motion.field_72450_a * motion.field_72450_a + motion.field_72449_c * motion.field_72449_c < 1.0E-4) {
                    return false;
                } else {
                    return !this.isMovingTowardStraightSupportEdge(
                            this.lastSupportFace, motion.field_72450_a, motion.field_72449_c
                        )
                        ? false
                        : this.isPastStraightSupportEdgeThreshold(
                            this.lastSupportFace,
                            localX + motion.field_72450_a * 1.45,
                            localZ + motion.field_72449_c * 1.45
                        );
                }
            }
        } else {
            return false;
        }
    }

    private boolean isPastStraightSupportEdgeThreshold(int supportFace, double localX, double localZ) {
        if (supportFace == 5) {
            return localX >= 0.52;
        } else if (supportFace == 4) {
            return localX <= 0.48;
        } else if (supportFace == 3) {
            return localZ >= 0.52;
        } else {
            return supportFace == 2 ? localZ <= 0.48 : false;
        }
    }

    private boolean isMovingTowardStraightSupportEdge(int supportFace, double motionX, double motionZ) {
        if (supportFace == 5) {
            return motionX > 0.0;
        } else if (supportFace == 4) {
            return motionX < 0.0;
        } else if (supportFace == 3) {
            return motionZ > 0.0;
        } else {
            return supportFace == 2 ? motionZ < 0.0 : false;
        }
    }

    private List<int[]> getBelowPlayerFallbackEndpoints(Entity player, float yaw, float pitch, int targetY) {
        List<int[]> endpoints = new ArrayList<>();
        if (!this.isDiagonalMovementContext(player)) {
            if (!player.field_70122_E) {
                this.addBelowTargetIfUnique(player, endpoints, this.getFeetBelowTargetAtY(player, targetY));
                this.addBelowTargetIfUnique(player, endpoints, this.getMotionBelowTargetAtY(player, targetY, 1.0));
                this.addBelowTargetIfUnique(player, endpoints, this.getMotionBelowTargetAtY(player, targetY, 1.7));
            }

            this.addBelowTargetIfUnique(player, endpoints, this.getCursorStartTargetAtY(player, yaw, pitch, targetY));
            this.addBelowTargetIfUnique(player, endpoints, this.getCursorPlacedTargetFromRay(yaw, pitch, targetY));
            this.addBelowTargetIfUnique(player, endpoints, this.getCursorTargetAtY(player, yaw, pitch, targetY));
            return endpoints;
        } else {
            this.addBelowTargetIfUnique(player, endpoints, this.getMotionBelowTargetAtY(player, targetY, 1.0));
            this.addBelowTargetIfUnique(player, endpoints, this.getMotionBelowTargetAtY(player, targetY, 1.7));
            return endpoints;
        }
    }

    private Object[] findBelowPlayerAirborneFallback(
        Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs
    ) {
        if (this.now() >= deadlineMs) {
            return null;
        }

        int playerBelowY = this.getCurrentBelowTargetY(player);
        boolean diagonal = this.isDiagonalMovementContext(player);
        boolean allowNonCursorTarget = diagonal || !player.field_70122_E;
        List<int[]> fallbackTargets = new ArrayList<>();

        for (int[] endpoint : this.getBelowPlayerFallbackEndpoints(player, yaw, currentPitch, playerBelowY)) {
            this.addBelowTarget(player, fallbackTargets, endpoint);
        }

        for (int[] targetPos : fallbackTargets) {
            if (this.now() >= deadlineMs) {
                return null;
            }

            if (this.isPlacementTargetAvailable(player, targetPos)) {
                Object[] candidate = this.findPitchPlacementForTarget(
                    player, yaw, currentPitch, targetPos, heldStack, null, -1, deadlineMs, false, allowNonCursorTarget
                );
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private Object[] findNearestSupportToBelowPlayerFallback(
        Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs
    ) {
        if (this.now() >= deadlineMs) {
            return null;
        }

        int targetY = this.getCurrentBelowTargetY(player);
        int[] belowPlayer = this.getFeetBelowTargetAtY(player, targetY);
        if (belowPlayer != null && !this.hasDirectSupportNeighbor(belowPlayer)) {
            int[] searchOrigin = this.getPathStartTowardBelowPlayer(player, targetY, belowPlayer);
            int[] nearestStart = this.findNearestSupportedReplaceableTarget(
                player, searchOrigin, belowPlayer, targetY, deadlineMs
            );
            if (nearestStart == null) {
                return null;
            }

            List<int[]> requiredPath = this.rasterizeHorizontalLineAtY(nearestStart, belowPlayer, targetY, 64);

            for (int i = requiredPath.size() - 1; i >= 0; i--) {
                if (this.now() >= deadlineMs) {
                    return null;
                }

                int[] pathPos = requiredPath.get(i);
                if (this.isPlacementTargetAvailable(player, pathPos)) {
                    Object[] candidate = this.findPitchPlacementForTarget(
                        player, yaw, currentPitch, pathPos, heldStack, null, -1, deadlineMs, false, true
                    );
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private int[] findNearestSupportedReplaceableTarget(
        Entity player, int[] origin, int[] belowPlayer, int targetY, long deadlineMs
    ) {
        if (origin != null && belowPlayer != null && this.now() < deadlineMs) {
            for (int radius = 0; radius <= 3; radius++) {
                int[] bestAtRadius = null;
                double bestScore = Double.POSITIVE_INFINITY;

                for (int dx = -radius; dx <= radius; dx++) {
                    int dzAbs = radius - Math.abs(dx);
                    int[] positive = new int[]{origin[0] + dx, targetY, origin[2] + dzAbs};
                    if (this.isPlacementTargetAvailable(player, positive) && this.hasDirectSupportNeighbor(positive)) {
                        double score = this.scoreAirPathStartCandidate(positive, belowPlayer, origin);
                        if (score < bestScore) {
                            bestScore = score;
                            bestAtRadius = positive;
                        }
                    }

                    if (dzAbs != 0) {
                        int[] negative = new int[]{origin[0] + dx, targetY, origin[2] - dzAbs};
                        if (this.isPlacementTargetAvailable(player, negative)
                            && this.hasDirectSupportNeighbor(negative)) {
                            double score = this.scoreAirPathStartCandidate(negative, belowPlayer, origin);
                            if (score < bestScore) {
                                bestScore = score;
                                bestAtRadius = negative;
                            }
                        }
                    }
                }

                if (bestAtRadius != null) {
                    return bestAtRadius;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    private double scoreAirPathStartCandidate(int[] candidate, int[] belowPlayer, int[] origin) {
        double sampleY = candidate[1] + 0.5;
        double goalDistSq = this.distSq(
            candidate[0] + 0.5, sampleY, candidate[2] + 0.5, belowPlayer[0] + 0.5, sampleY, belowPlayer[2] + 0.5
        );
        double originDistSq = this.distSq(
            candidate[0] + 0.5, sampleY, candidate[2] + 0.5, origin[0] + 0.5, sampleY, origin[2] + 0.5
        );
        return goalDistSq * 4.0 + originDistSq;
    }

    private int[] getPathStartTowardBelowPlayer(Entity player, int targetY, int[] fallback) {
        int[] pathStart = null;
        if (this.lastPlacedPos != null && this.lastPlacedPos[1] == targetY) {
            pathStart = this.lastPlacedPos;
        }

        if (pathStart == null) {
            pathStart = this.getMotionBelowTargetAtY(player, targetY, 1.7);
        }

        if (pathStart == null) {
            pathStart = this.getMotionBelowTargetAtY(player, targetY, 1.0);
        }

        return pathStart != null ? pathStart : fallback;
    }

    private boolean hasValidLastPlacedPos(Entity player) {
        return this.lastPlacedPos == null
            ? false
            : this.isWithinReach(player, this.lastPlacedPos)
                && this.isSupportAvailable(this.lastPlacedPos[0], this.lastPlacedPos[1], this.lastPlacedPos[2])
                && !this.isInteractable(this.lastPlacedPos[0], this.lastPlacedPos[1], this.lastPlacedPos[2]);
    }

    private boolean hasValidLastSupportFace(Entity player) {
        return this.lastSupportPos != null && this.lastSupportFace >= 0
            ? this.isWithinReach(player, this.lastSupportPos)
                && this.isSupportAvailable(this.lastSupportPos[0], this.lastSupportPos[1], this.lastSupportPos[2])
                && !this.isInteractable(this.lastSupportPos[0], this.lastSupportPos[1], this.lastSupportPos[2])
            : false;
    }

    private Object[] findLegacyBelowPlacement(
        Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs
    ) {
        if (this.now() < deadlineMs && this.isUsableBlockStack(heldStack)) {
            if (this.isDiagonalMovementContext(player)) {
                Object[] diagonalCandidate = this.findLegacyDiagonalPlacement(
                    player, yaw, currentPitch, heldStack, deadlineMs
                );
                if (diagonalCandidate != null) {
                    return diagonalCandidate;
                }
            }

            if (this.hasValidLastPlacedPos(player)) {
                Object[] preferred = this.findLegacyBelowPlacementForSupport(
                    player, yaw, currentPitch, heldStack, this.lastPlacedPos, deadlineMs
                );
                if (preferred != null) {
                    return preferred;
                }
            }

            return this.findLegacyBelowPlacementForSupport(player, yaw, currentPitch, heldStack, null, deadlineMs);
        } else {
            return null;
        }
    }

    private Object[] findLegacyDiagonalPlacement(
        Entity player, float yaw, float currentPitch, ItemStack heldStack, long deadlineMs
    ) {
        if (this.now() >= deadlineMs) {
            return null;
        }

        List<int[]> diagonalTargets = new ArrayList<>();
        int currentY = this.getCurrentBelowTargetY(player);
        int strictY = this.getStrictBelowTargetY(player);

        for (int[] endpoint : this.getBelowPlayerFallbackEndpoints(player, yaw, currentPitch, currentY)) {
            this.addBelowTarget(player, diagonalTargets, endpoint);
        }

        if (strictY != currentY) {
            for (int[] endpoint : this.getBelowPlayerFallbackEndpoints(player, yaw, currentPitch, strictY)) {
                this.addBelowTarget(player, diagonalTargets, endpoint);
            }
        }

        if (diagonalTargets.isEmpty()) {
            return null;
        }

        int[] preferredSupportPos = this.hasValidLastPlacedPos(player) ? this.lastPlacedPos : null;

        for (int[] targetPos : diagonalTargets) {
            if (this.now() >= deadlineMs) {
                return null;
            }

            if (this.isPlacementTargetAvailable(player, targetPos)) {
                Object[] candidate = this.findLegacyPitchPlacementForTarget(
                    player, yaw, currentPitch, targetPos, heldStack, preferredSupportPos, deadlineMs
                );
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        if (preferredSupportPos == null) {
            return null;
        }

        for (int[] targetPos : diagonalTargets) {
            if (this.now() >= deadlineMs) {
                return null;
            }

            if (this.isPlacementTargetAvailable(player, targetPos)) {
                Object[] candidate = this.findLegacyPitchPlacementForTarget(
                    player, yaw, currentPitch, targetPos, heldStack, null, deadlineMs
                );
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private Object[] findLegacyBelowPlacementForSupport(
        Entity player, float yaw, float currentPitch, ItemStack heldStack, int[] preferredSupportPos, long deadlineMs
    ) {
        for (int[] targetPos : this.getMessageStyleBelowTargets(player)) {
            if (this.now() >= deadlineMs) {
                return null;
            }

            if (this.isPlacementTargetAvailable(player, targetPos)) {
                Object[] candidate = this.findLegacyPitchPlacementForTarget(
                    player, yaw, currentPitch, targetPos, heldStack, preferredSupportPos, deadlineMs
                );
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private Object[] findLegacyPitchPlacementForTarget(
        Entity player,
        float yaw,
        float currentPitch,
        int[] targetPos,
        ItemStack heldStack,
        int[] preferredSupportPos,
        long deadlineMs
    ) {
        float clampedBasePitch = this.clampFloat(currentPitch, 40.0F, 89.0F);
        Object[] direct = this.tryLegacyPitch(yaw, clampedBasePitch, targetPos, preferredSupportPos, deadlineMs);
        if (direct != null) {
            return direct;
        }

        for (int offset = 1; offset <= 49; offset++) {
            if (this.now() >= deadlineMs) {
                return null;
            }

            float up = clampedBasePitch + offset;
            if (up <= 89.0F) {
                Object[] candidate = this.tryLegacyPitch(yaw, up, targetPos, preferredSupportPos, deadlineMs);
                if (candidate != null) {
                    return candidate;
                }
            }

            float down = clampedBasePitch - offset;
            if (down >= 40.0F) {
                Object[] candidate = this.tryLegacyPitch(yaw, down, targetPos, preferredSupportPos, deadlineMs);
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private Object[] tryLegacyPitch(float yaw, float pitch, int[] targetPos, int[] preferredSupportPos, long deadlineMs) {
        if (this.now() >= deadlineMs) {
            return null;
        } else {
            Object[] traced = this.rayCast(yaw, pitch);
            if (traced == null) {
                return null;
            } else {
                int[] supportPos = (int[])traced[0];
                int face = (Integer)traced[1];
                if (preferredSupportPos != null && !this.posEquals(supportPos, preferredSupportPos)) {
                    return null;
                } else if (face == 0) {
                    return null;
                } else if (!this.isReplaceable(supportPos[0], supportPos[1], supportPos[2])
                    && !this.isInteractable(supportPos[0], supportPos[1], supportPos[2])) {
                    int[] placedPos = this.offsetPos(supportPos, face);
                    return !this.posEquals(placedPos, targetPos)
                        ? null
                        : new Object[]{Math.min(pitch, 89.0F), supportPos, face, (Vec3)traced[2], placedPos};
                } else {
                    return null;
                }
            }
        }
    }

    private List<int[]> getMessageStyleBelowTargets(Entity player) {
        double[] offsets = new double[]{0.0, 0.29, -0.29};
        Vec3 pos = this.playerPosition(player);
        int maxY = this.floor(pos.field_72448_b) - 1;
        int minY = this.floor(pos.field_72448_b) - 2;
        List<int[]> targets = new ArrayList<>();

        for (int targetY = maxY; targetY >= minY; targetY--) {
            for (double xOffset : offsets) {
                for (double zOffset : offsets) {
                    targets.add(
                        new int[]{
                            this.floor(pos.field_72450_a + xOffset), targetY, this.floor(pos.field_72449_c + zOffset)
                        }
                    );
                }
            }
        }

        return targets;
    }

    private Object[] findBelowPlacementForSupport(
        Entity player,
        float yaw,
        float currentPitch,
        ItemStack heldStack,
        int[] preferredSupportPos,
        int preferredSupportFace,
        long deadlineMs
    ) {
        boolean diagonal = this.isDiagonalMovementContext(player);

        for (int[] targetPos : this.getBelowTargets(player, yaw, currentPitch)) {
            if (this.now() >= deadlineMs) {
                return null;
            }

            if (this.isPlacementTargetAvailable(player, targetPos) && this.isStrictOneBelowPlayer(player, targetPos)) {
                Object[] candidate = this.findPitchPlacementForTarget(
                    player,
                    yaw,
                    currentPitch,
                    targetPos,
                    heldStack,
                    preferredSupportPos,
                    preferredSupportFace,
                    deadlineMs,
                    false,
                    diagonal
                );
                if (candidate != null) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private boolean isWithinReach(Entity player, int[] pos) {
        if (pos == null) {
            return false;
        }

        Vec3 eyes = this.getEyes(player);
        double cx = Math.max(pos[0], Math.min(eyes.field_72450_a, pos[0] + 1.0));
        double cy = Math.max(pos[1], Math.min(eyes.field_72448_b, pos[1] + 1.0));
        double cz = Math.max(pos[2], Math.min(eyes.field_72449_c, pos[2] + 1.0));
        double dx = eyes.field_72450_a - cx;
        double dy = eyes.field_72448_b - cy;
        double dz = eyes.field_72449_c - cz;
        return dx * dx + dy * dy + dz * dz <= this.reach() * this.reach();
    }

    private Object[] findPitchPlacementForTarget(
        Entity player,
        float yaw,
        float currentPitch,
        int[] targetPos,
        ItemStack heldStack,
        int[] preferredSupportPos,
        int preferredSupportFace,
        long deadlineMs,
        boolean requireLookAlignment,
        boolean allowNonCursorTarget
    ) {
        if (this.now() < deadlineMs && targetPos != null) {
            boolean effectiveAllowNonCursorTarget = allowNonCursorTarget
                || this.shouldAllowPlayerOneNonCursorTarget(player, targetPos);
            if (!effectiveAllowNonCursorTarget
                && !this.isCursorOrBelowPlayerTarget(player, targetPos, yaw, currentPitch)) {
                return null;
            }

            if (!this.isPlacementTargetAvailable(player, targetPos)) {
                return null;
            }

            Object[] bestCandidate = null;
            double bestScore = Double.POSITIVE_INFINITY;

            for (int placeFace : this.getAllowedPlaceFacesForContext(player, yaw)) {
                if (this.now() >= deadlineMs) {
                    break;
                }

                if (!this.shouldRejectStraightSideSwitch(player, targetPos, placeFace)) {
                    int[] supportPos = this.offsetPos(targetPos, this.opposite(placeFace));
                    if ((preferredSupportPos == null || this.posEquals(supportPos, preferredSupportPos))
                        && (preferredSupportFace < 0 || placeFace == preferredSupportFace)
                        && this.isSupportAvailable(supportPos[0], supportPos[1], supportPos[2])
                        && this.isWithinReach(player, supportPos)) {
                        double[] hitOffsets = this.useExtendedSearch()
                            ? this.EXTENDED_FACE_HIT_OFFSETS
                            : this.FACE_HIT_OFFSETS;

                        for (double primaryOffset : hitOffsets) {
                            for (double secondaryOffset : hitOffsets) {
                                if (this.now() >= deadlineMs) {
                                    break;
                                }

                                Vec3 hitVec = this.getSupportFaceHitVec(
                                    supportPos, placeFace, primaryOffset, secondaryOffset
                                );
                                Object[] candidate = this.buildPlacementCandidateForHitVec(
                                    player,
                                    yaw,
                                    targetPos,
                                    supportPos,
                                    placeFace,
                                    hitVec,
                                    requireLookAlignment,
                                    effectiveAllowNonCursorTarget
                                );
                                if (candidate != null) {
                                    double candidateScore = this.scorePlacementCandidate(
                                        player,
                                        currentPitch,
                                        this.candidatePitch(candidate),
                                        placeFace,
                                        primaryOffset,
                                        secondaryOffset
                                    );
                                    if (candidateScore < bestScore) {
                                        bestScore = candidateScore;
                                        bestCandidate = candidate;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return bestCandidate == null && preferredSupportPos != null && preferredSupportFace >= 0
                ? this.findRayAlignedPitchCandidate(
                    yaw, currentPitch, targetPos, preferredSupportPos, preferredSupportFace, deadlineMs
                )
                : bestCandidate;
        } else {
            return null;
        }
    }

    private Object[] findRayAlignedPitchCandidate(
        float yaw, float currentPitch, int[] targetPos, int[] supportPos, int placeFace, long deadlineMs
    ) {
        float clampedBasePitch = this.clampFloat(currentPitch, 40.0F, 89.0F);

        for (int offset = 0; offset <= 49; offset++) {
            if (this.now() >= deadlineMs) {
                return null;
            }

            float upPitch = clampedBasePitch + offset;
            if (upPitch <= 89.0F) {
                Object[] candidate = this.tryRayAlignedPitch(yaw, upPitch, targetPos, supportPos, placeFace);
                if (candidate != null) {
                    return candidate;
                }
            }

            if (offset != 0) {
                float downPitch = clampedBasePitch - offset;
                if (downPitch >= 40.0F) {
                    Object[] candidate = this.tryRayAlignedPitch(yaw, downPitch, targetPos, supportPos, placeFace);
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }

    private Object[] tryRayAlignedPitch(float yaw, float pitch, int[] targetPos, int[] supportPos, int placeFace) {
        Object[] traced = this.rayCast(yaw, pitch);
        if (traced == null) {
            return null;
        } else {
            int[] tracedSupport = (int[])traced[0];
            int tracedFace = (Integer)traced[1];
            if (this.posEquals(tracedSupport, supportPos) && tracedFace == placeFace) {
                int[] tracedPlaced = this.offsetPos(tracedSupport, tracedFace);
                return !this.posEquals(tracedPlaced, targetPos)
                    ? null
                    : new Object[]{pitch, tracedSupport, tracedFace, (Vec3)traced[2], tracedPlaced};
            } else {
                return null;
            }
        }
    }

    private double scorePlacementCandidate(
        Entity player,
        float currentPitch,
        float candidatePitchValue,
        int placeFace,
        double primaryOffset,
        double secondaryOffset
    ) {
        double pitchPenalty = Math.abs(this.wrapAngle(candidatePitchValue - currentPitch));
        double centerPenalty = Math.abs(primaryOffset - 0.5) + Math.abs(secondaryOffset - 0.5);
        double facePenalty = placeFace == 1 ? 0.0 : 0.35;
        double straightSidePenalty = this.getStraightSideSwitchPenalty(player, placeFace);
        return pitchPenalty + centerPenalty * 2.0 + facePenalty + straightSidePenalty;
    }

    private double getStraightSideSwitchPenalty(Entity player, int placeFace) {
        if (this.getConditionModeCheck(player) != 1) {
            return 0.0;
        } else if (this.lastSupportFace < 2) {
            return 0.0;
        } else {
            return placeFace == this.lastSupportFace ? 0.0 : 0.8;
        }
    }

    private boolean shouldRejectStraightSideSwitch(Entity player, int[] targetPos, int placeFace) {
        if (targetPos == null || this.getConditionModeCheck(player) != 1) {
            return false;
        }

        if (placeFace < 2) {
            return false;
        }

        if (this.lastSupportFace < 2) {
            return false;
        }

        if (placeFace == this.lastSupportFace) {
            return false;
        }

        if (this.isNearStraightSupportEdge(player)) {
            return false;
        }

        int[] laneSupportPos = this.offsetPos(targetPos, this.opposite(this.lastSupportFace));
        return this.isSupportAvailable(laneSupportPos[0], laneSupportPos[1], laneSupportPos[2])
            && this.isWithinReach(player, laneSupportPos);
    }

    private Object[] buildPlacementCandidateForHitVec(
        Entity player,
        float yaw,
        int[] targetPos,
        int[] supportPos,
        int placeFace,
        Vec3 hitVec,
        boolean requireLookAlignment,
        boolean allowNonCursorTarget
    ) {
        if (hitVec == null) {
            return null;
        } else {
            int[] offsetTarget = this.offsetPos(supportPos, placeFace);
            if (!this.posEquals(offsetTarget, targetPos)) {
                return null;
            } else if (!this.isStrictOneBelowPlayer(player, offsetTarget)) {
                return null;
            } else {
                Float pitch = this.computePitchToHitVec(player, hitVec);
                if (pitch == null) {
                    return null;
                } else if (!this.isPlacementLookAligned(yaw, pitch, supportPos, placeFace, targetPos)) {
                    return null;
                } else {
                    return !allowNonCursorTarget
                            && !this.isDiagonalMovementContext(player)
                            && !this.isSupportFaceVisible(player, supportPos, placeFace, hitVec)
                        ? null
                        : new Object[]{pitch, supportPos, placeFace, hitVec, offsetTarget};
                }
            }
        }
    }

    private int[] getAllowedPlaceFacesForContext(Entity player, float yaw) {
        if (this.getConditionModeCheck(player) != 1) {
            return this.ALLOWED_PLACE_FACES;
        }

        int forward = this.getStraightForwardFacing(player, yaw);
        return this.useExtendedSearch()
            ? new int[]{this.rotateY(forward), this.rotateYCCW(forward), forward, this.opposite(forward), 1}
            : new int[]{this.rotateY(forward), this.rotateYCCW(forward), forward, this.opposite(forward)};
    }

    private boolean isPlacementLookAligned(float yaw, float pitch, int[] supportPos, int placeFace, int[] targetPos) {
        if (supportPos != null && placeFace >= 0 && targetPos != null) {
            Object[] traced = this.rayCast(yaw, pitch);
            if (traced == null) {
                return false;
            } else if (this.posEquals((int[])traced[0], supportPos) && (Integer)traced[1] == placeFace) {
                int[] tracedOffset = this.offsetPos((int[])traced[0], (Integer)traced[1]);
                return this.posEquals(tracedOffset, targetPos);
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isSupportFaceVisible(Entity player, int[] supportPos, int placeFace, Vec3 hitVec) {
        Vec3 eyes = this.getEyes(player);
        double dx = hitVec.field_72450_a - eyes.field_72450_a;
        double dy = hitVec.field_72448_b - eyes.field_72448_b;
        double dz = hitVec.field_72449_c - eyes.field_72449_c;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 1.0E-4) {
            return false;
        }

        float traceYaw = (float)(Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        float tracePitch = (float)(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
        Object[] traced = this.raycastBlock(distance + 0.5, traceYaw, tracePitch);
        if (traced == null) {
            return false;
        }

        int[] tracedPos = this.posFromVec((Vec3)traced[0]);
        int tracedFace = this.faceFromName((String)traced[2]);
        return this.posEquals(tracedPos, supportPos) && tracedFace == placeFace;
    }

    private Vec3 getSupportFaceHitVec(int[] supportPos, int placeFace, double primaryOffset, double secondaryOffset) {
        double primary = Math.max(0.001, Math.min(0.999, primaryOffset));
        double secondary = Math.max(0.001, Math.min(0.999, secondaryOffset));
        if (placeFace == 2) {
            return new Vec3(supportPos[0] + primary, supportPos[1] + secondary, supportPos[2] + 0.001);
        } else if (placeFace == 3) {
            return new Vec3(supportPos[0] + primary, supportPos[1] + secondary, supportPos[2] + 0.999);
        } else if (placeFace == 5) {
            return new Vec3(supportPos[0] + 0.999, supportPos[1] + primary, supportPos[2] + secondary);
        } else if (placeFace == 4) {
            return new Vec3(supportPos[0] + 0.001, supportPos[1] + primary, supportPos[2] + secondary);
        } else {
            return placeFace == 0
                ? new Vec3(supportPos[0] + primary, supportPos[1] + 0.001, supportPos[2] + secondary)
                : new Vec3(supportPos[0] + primary, supportPos[1] + 0.999, supportPos[2] + secondary);
        }
    }

    private Float computePitchToHitVec(Entity player, Vec3 hitVec) {
        Vec3 eyes = this.getEyes(player);
        double dx = hitVec.field_72450_a - eyes.field_72450_a;
        double dz = hitVec.field_72449_c - eyes.field_72449_c;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double dy = hitVec.field_72448_b - eyes.field_72448_b;
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, horizontal)));
        return Math.max(-89.0F, Math.min(89.0F, pitch));
    }

    private List<int[]> getBelowTargets(Entity player, float yaw, float pitch) {
        if (this.cachedBelowTargetsTick == this.currentClientTick && this.cachedBelowTargets != null) {
            return this.cachedBelowTargets;
        }

        List<int[]> belowTargets = new ArrayList<>();
        boolean diagonal = this.isDiagonalMovementContext(player);
        if (!diagonal) {
            int currentY = this.getCurrentBelowTargetY(player);
            this.addBelowTarget(player, belowTargets, this.getCursorStartTargetAtY(player, yaw, pitch, currentY));
            if (belowTargets.isEmpty()) {
                int strictY = this.getStrictBelowTargetY(player);
                if (strictY != currentY) {
                    this.addBelowTarget(player, belowTargets, this.getCursorStartTargetAtY(player, yaw, pitch, strictY));
                }
            }

            if (belowTargets.isEmpty()) {
                this.addBelowTarget(player, belowTargets, this.getCursorPlacedTargetFromRay(yaw, pitch, currentY));
            }

            if (belowTargets.isEmpty()) {
                int strictY = this.getStrictBelowTargetY(player);
                if (strictY != currentY) {
                    this.addBelowTarget(player, belowTargets, this.getCursorPlacedTargetFromRay(yaw, pitch, strictY));
                }
            }

            if (belowTargets.isEmpty()) {
                this.addBelowTarget(player, belowTargets, this.getCursorTargetAtY(player, yaw, pitch, currentY));
            }
        } else {
            int currentY = this.getCurrentBelowTargetY(player);
            this.addBelowTarget(player, belowTargets, this.getMotionBelowTargetAtY(player, currentY, 1.0));
            this.addBelowTarget(player, belowTargets, this.getMotionBelowTargetAtY(player, currentY, 1.7));

            for (int[] endpoint : this.getBelowPlayerFallbackEndpoints(player, yaw, pitch, currentY)) {
                this.addBelowTarget(player, belowTargets, endpoint);
            }
        }

        this.cachedBelowTargets = belowTargets;
        this.cachedBelowTargetsTick = this.currentClientTick;
        return belowTargets;
    }

    private boolean isCursorOrBelowPlayerTarget(Entity player, int[] targetPos, float yaw, float pitch) {
        if (targetPos == null) {
            return false;
        }

        if (!this.isDiagonalMovementContext(player)) {
            int currentY = this.getCurrentBelowTargetY(player);
            if (this.posEquals(this.getCursorStartTargetAtY(player, yaw, pitch, currentY), targetPos)) {
                return true;
            }

            if (this.posEquals(this.getCursorPlacedTargetFromRay(yaw, pitch, currentY), targetPos)) {
                return true;
            }

            int strictY = this.getStrictBelowTargetY(player);
            if (strictY != currentY) {
                if (this.posEquals(this.getCursorStartTargetAtY(player, yaw, pitch, strictY), targetPos)) {
                    return true;
                }

                if (this.posEquals(this.getCursorPlacedTargetFromRay(yaw, pitch, strictY), targetPos)) {
                    return true;
                }
            }

            return this.isCursorInsideTargetAtY(player, targetPos, yaw, pitch, currentY)
                ? true
                : this.posEquals(this.getCursorTargetAtY(player, yaw, pitch, currentY), targetPos);
        } else {
            int strictY = this.getStrictBelowTargetY(player);
            return this.isBelowPlayerTargetAtY(player, targetPos, strictY, yaw, pitch)
                ? true
                : this.isBelowPlayerTargetAtY(player, targetPos, this.getCurrentBelowTargetY(player), yaw, pitch);
        }
    }

    private boolean isBelowPlayerTargetAtY(Entity player, int[] targetPos, int targetY, float yaw, float pitch) {
        for (int[] candidate : this.getBelowPlayerFallbackEndpoints(player, yaw, pitch, targetY)) {
            if (this.posEquals(targetPos, candidate)) {
                return true;
            }
        }

        return false;
    }

    private int[] getFeetBelowTargetAtY(Entity player, int targetY) {
        Vec3 pos = this.playerPosition(player);
        return new int[]{this.floor(pos.field_72450_a), targetY, this.floor(pos.field_72449_c)};
    }

    private boolean shouldAllowPlayerOneNonCursorTarget(Entity player, int[] targetPos) {
        if (targetPos == null) {
            return false;
        }

        if (this.isDiagonalMovementContext(player) || player.field_70122_E) {
            return false;
        }

        if (!this.isPlayerHitboxFullyInsideSingleBlockColumn(player)) {
            return false;
        }

        if (this.hasValidLastSupportFace(player) && this.lastSupportFace != 0) {
            int[] continuationTarget = this.offsetPos(this.lastSupportPos, this.lastSupportFace);
            if (!this.posEquals(targetPos, continuationTarget)) {
                return false;
            }

            int targetY = targetPos[1];
            int currentY = this.getCurrentBelowTargetY(player);
            int strictY = this.getStrictBelowTargetY(player);
            if (targetY != currentY && targetY != strictY) {
                return false;
            }

            int[] feetBelow = this.getFeetBelowTargetAtY(player, targetY);
            int horizontalDistance = Math.abs(targetPos[0] - feetBelow[0]) + Math.abs(targetPos[2] - feetBelow[2]);
            return horizontalDistance <= 1;
        } else {
            return false;
        }
    }

    private boolean isPlayerHitboxFullyInsideSingleBlockColumn(Entity player) {
        Vec3 pos = this.playerPosition(player);
        double half = player.field_70130_N / 2.0;
        int minX = this.floor(pos.field_72450_a - half + 1.0E-4);
        int maxX = this.floor(pos.field_72450_a + half - 1.0E-4);
        if (minX != maxX) {
            return false;
        }

        int minZ = this.floor(pos.field_72449_c - half + 1.0E-4);
        int maxZ = this.floor(pos.field_72449_c + half - 1.0E-4);
        return minZ == maxZ;
    }

    private int[] getMotionBelowTargetAtY(Entity player, int targetY, double multiplier) {
        Vec3 pos = this.playerPosition(player);
        Vec3 motion = this.playerMotion();
        return new int[]{
            this.floor(pos.field_72450_a + motion.field_72450_a * multiplier),
            targetY,
            this.floor(pos.field_72449_c + motion.field_72449_c * multiplier)
        };
    }

    private boolean hasDirectSupportNeighbor(int[] targetPos) {
        for (int placeFace : this.ALLOWED_PLACE_FACES) {
            int[] supportPos = this.offsetPos(targetPos, this.opposite(placeFace));
            if (this.isSupportAvailable(supportPos[0], supportPos[1], supportPos[2])) {
                return true;
            }
        }

        return false;
    }

    private void addBelowTargetIfUnique(Entity player, List<int[]> targets, int[] candidate) {
        if (candidate != null) {
            if (this.isStrictOneBelowPlayer(player, candidate)) {
                for (int[] existing : targets) {
                    if (this.posEquals(existing, candidate)) {
                        return;
                    }
                }

                targets.add(candidate);
            }
        }
    }

    private void addBelowTarget(Entity player, List<int[]> targets, int[] candidate) {
        this.addBelowTargetIfUnique(player, targets, candidate);
    }

    private List<int[]> rasterizeHorizontalLineAtY(int[] start, int[] end, int y, int maxSteps) {
        List<int[]> line = new ArrayList<>();
        int x0 = start[0];
        int z0 = start[2];
        int x1 = end[0];
        int z1 = end[2];
        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = Integer.compare(x1, x0);
        int sz = Integer.compare(z1, z0);
        int movedX = 0;
        int movedZ = 0;

        for (int steps = 0; steps < maxSteps; steps++) {
            line.add(new int[]{x0, y, z0});
            if (x0 == x1 && z0 == z1 || movedX >= dx && movedZ >= dz) {
                break;
            }

            if (movedX >= dx) {
                z0 += sz;
                movedZ++;
            } else if (movedZ >= dz) {
                x0 += sx;
                movedX++;
            } else if ((1 + 2 * movedX) * dz < (1 + 2 * movedZ) * dx) {
                x0 += sx;
                movedX++;
            } else {
                z0 += sz;
                movedZ++;
            }
        }

        return line;
    }

    private int getDetectedModeCheck(Entity player) {
        float forwardInput = Math.abs(mc.field_71439_g.field_71158_b.field_78900_b);
        float strafeInput = Math.abs(mc.field_71439_g.field_71158_b.field_78902_a);
        if (!(forwardInput >= 0.08F) && !(strafeInput >= 0.08F)) {
            double[] direction = this.getMotionDirectionComponents(player);
            if (direction == null) {
                return 1;
            }

            double angleDeg = Math.toDegrees(Math.atan2(direction[1], direction[0]));
            double norm90 = (angleDeg % 90.0 + 90.0) % 90.0;
            return Math.abs(norm90 - 45.0) <= 18.0 ? 2 : 1;
        } else {
            return forwardInput >= 0.08F && strafeInput >= 0.08F ? 1 : 2;
        }
    }

    private double[] getMotionDirectionComponents(Entity player) {
        Vec3 pos = this.playerPosition(player);
        Vec3 last = this.playerPositionLast(player);
        double dirX = pos.field_72450_a - last.field_72450_a;
        double dirZ = pos.field_72449_c - last.field_72449_c;
        double speedSq = dirX * dirX + dirZ * dirZ;
        if (speedSq < 1.0E-4) {
            Vec3 motion = this.playerMotion();
            dirX = motion.field_72450_a;
            dirZ = motion.field_72449_c;
            speedSq = dirX * dirX + dirZ * dirZ;
        }

        return speedSq < 1.0E-4 ? null : new double[]{dirX, dirZ};
    }

    private double[] getInputDirectionComponents(float referenceYaw) {
        float forwardInput = mc.field_71439_g.field_71158_b.field_78900_b;
        float strafeInput = mc.field_71439_g.field_71158_b.field_78902_a;
        if (Math.abs(forwardInput) < 0.08F && Math.abs(strafeInput) < 0.08F) {
            return null;
        }

        double yawRadians = Math.toRadians(referenceYaw);
        double sinYaw = Math.sin(yawRadians);
        double cosYaw = Math.cos(yawRadians);
        double dirX = forwardInput * -sinYaw + strafeInput * cosYaw;
        double dirZ = forwardInput * cosYaw - strafeInput * sinYaw;
        return dirX * dirX + dirZ * dirZ < 1.0E-4 ? null : new double[]{dirX, dirZ};
    }

    private int getStraightForwardFacing(Entity player, float fallbackYaw) {
        double[] direction = this.getInputDirectionComponents(fallbackYaw);
        if (direction == null) {
            direction = this.getMotionDirectionComponents(player);
        }

        if (direction == null) {
            return this.facingFromYaw(fallbackYaw);
        }

        float directionYaw = (float)(Math.toDegrees(Math.atan2(direction[1], direction[0])) - 90.0);
        return this.facingFromYaw(directionYaw);
    }

    private int getConditionModeCheck(Entity player) {
        return this.forcedModeCheck != 0 ? this.forcedModeCheck : this.getDetectedModeCheck(player);
    }

    private boolean isDiagonalMovementContext(Entity player) {
        return this.getConditionModeCheck(player) == 2;
    }

    private int[] getCursorPlacedTargetFromRay(float yaw, float pitch, int targetY) {
        Object[] traced = this.rayCast(yaw, pitch);
        if (traced == null) {
            return null;
        }

        int[] offsetTarget = this.offsetPos((int[])traced[0], (Integer)traced[1]);
        return offsetTarget[1] != targetY ? null : offsetTarget;
    }

    private int[] getCursorStartTargetAtY(Entity player, float fallbackYaw, float fallbackPitch, int targetY) {
        Vec3 cursorPoint = this.getCursorIntersectionAtY(player, targetY);
        Vec3 lookVec = this.getCursorLookVec(player);
        if (cursorPoint != null && lookVec != null) {
            double startX = cursorPoint.field_72450_a - lookVec.field_72450_a * 0.03;
            double startZ = cursorPoint.field_72449_c - lookVec.field_72449_c * 0.03;
            return new int[]{this.floor(startX), targetY, this.floor(startZ)};
        } else {
            return null;
        }
    }

    private int[] getCursorTargetAtY(Entity player, float fallbackYaw, float fallbackPitch, int targetY) {
        Vec3 cursorPoint = this.getCursorIntersectionAtY(player, targetY);
        return cursorPoint == null
            ? null
            : new int[]{this.floor(cursorPoint.field_72450_a), targetY, this.floor(cursorPoint.field_72449_c)};
    }

    private Vec3 getCursorIntersectionAtY(Entity player, int targetY) {
        Vec3 eyes = this.getEyes(player);
        Vec3 lookVec = this.getCursorLookVec(player);
        if (lookVec != null && !(Math.abs(lookVec.field_72448_b) < 1.0E-4)) {
            double t = (targetY - eyes.field_72448_b) / lookVec.field_72448_b;
            return t <= 0.0
                ? null
                : new Vec3(
                    eyes.field_72450_a + lookVec.field_72450_a * t,
                    targetY + 0.5,
                    eyes.field_72449_c + lookVec.field_72449_c * t
                );
        } else {
            return null;
        }
    }

    private Vec3 getCursorLookVec(Entity player) {
        double[] cameraRotations = this.renderRotations();
        return cameraRotations != null && cameraRotations.length >= 2
            ? this.getLookVec((float)cameraRotations[0], (float)cameraRotations[1])
            : this.getLookVec(player.field_70177_z, player.field_70125_A);
    }

    private Vec3 getLookVec(float yaw, float pitch) {
        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);
        double cosPitch = Math.cos(pitchRad);
        return new Vec3(-Math.sin(yawRad) * cosPitch, -Math.sin(pitchRad), Math.cos(yawRad) * cosPitch);
    }

    private boolean isCursorInsideTargetAtY(Entity player, int[] targetPos, float yaw, float pitch, int targetY) {
        if (targetPos != null && targetPos[1] == targetY) {
            Vec3 cursorPoint = this.getCursorIntersectionAtY(player, targetY);
            if (cursorPoint == null) {
                return false;
            }

            double x = cursorPoint.field_72450_a;
            double z = cursorPoint.field_72449_c;
            return x >= targetPos[0] - 1.0E-6
                && x <= targetPos[0] + 1.0 + 1.0E-6
                && z >= targetPos[2] - 1.0E-6
                && z <= targetPos[2] + 1.0 + 1.0E-6;
        } else {
            return false;
        }
    }

    private boolean isPlacementTargetAvailable(Entity player, int[] pos) {
        return this.isBasePlacementTargetAvailable(player, pos) && this.isStrictOneBelowPlayer(player, pos);
    }

    private boolean isBasePlacementTargetAvailable(Entity player, int[] pos) {
        return pos != null
            && this.isStraightTellyTarget(pos)
            && !this.isRejectedTarget(pos)
            && !this.doesPlacementIntersectPlayer(player, pos)
            && this.isReplaceable(pos[0], pos[1], pos[2]);
    }

    private boolean doesPlacementIntersectPlayer(Entity player, int[] placePos) {
        if (placePos == null) {
            return false;
        }

        if (this.isInsideAnyPlayerPositionCell(player, placePos)) {
            return true;
        }

        Vec3 pos = this.playerPosition(player);
        double half = player.field_70130_N / 2.0;
        double height = player.field_70131_O;
        if (this.boxIntersectsBlock(
            pos.field_72450_a - half,
            pos.field_72448_b,
            pos.field_72449_c - half,
            pos.field_72450_a + half,
            pos.field_72448_b + height,
            pos.field_72449_c + half,
            placePos
        )) {
            return true;
        }

        if (this.isBlockPosInsideBounds(
            placePos,
            pos.field_72450_a - half,
            pos.field_72448_b,
            pos.field_72449_c - half,
            pos.field_72450_a + half,
            pos.field_72448_b + height,
            pos.field_72449_c + half
        )) {
            return true;
        }

        if (!this.shouldUseHistoricalPlayerCollisionChecks(player, placePos)) {
            return false;
        }

        Vec3 last = this.playerPositionLast(player);
        if (last.field_72450_a != pos.field_72450_a
            || last.field_72448_b != pos.field_72448_b
            || last.field_72449_c != pos.field_72449_c) {
            if (this.boxIntersectsBlock(
                last.field_72450_a - half,
                last.field_72448_b,
                last.field_72449_c - half,
                last.field_72450_a + half,
                last.field_72448_b + height,
                last.field_72449_c + half,
                placePos
            )) {
                return true;
            }

            if (this.isBlockPosInsideBounds(
                placePos,
                last.field_72450_a - half,
                last.field_72448_b,
                last.field_72449_c - half,
                last.field_72450_a + half,
                last.field_72448_b + height,
                last.field_72449_c + half
            )) {
                return true;
            }
        }

        if (this.hasLastSentServerPos
            && (
                this.lastSentServerPosX != pos.field_72450_a
                    || this.lastSentServerPosY != pos.field_72448_b
                    || this.lastSentServerPosZ != pos.field_72449_c
            )) {
            double sx = this.lastSentServerPosX;
            double sy = this.lastSentServerPosY;
            double sz = this.lastSentServerPosZ;
            if (this.boxIntersectsBlock(sx - half, sy, sz - half, sx + half, sy + height, sz + half, placePos)) {
                return true;
            }

            if (this.isBlockPosInsideBounds(placePos, sx - half, sy, sz - half, sx + half, sy + height, sz + half)) {
                return true;
            }
        }

        return false;
    }

    private boolean boxIntersectsBlock(
        double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int[] pos
    ) {
        return maxX > pos[0]
            && minX < pos[0] + 1.0
            && maxY > pos[1]
            && minY < pos[1] + 1.0
            && maxZ > pos[2]
            && minZ < pos[2] + 1.0;
    }

    private boolean isBlockPosInsideBounds(
        int[] pos, double minX, double minY, double minZ, double maxX, double maxY, double maxZ
    ) {
        int bMinX = this.floor(minX + 1.0E-4);
        int bMaxX = this.floor(maxX - 1.0E-4);
        if (pos[0] >= bMinX && pos[0] <= bMaxX) {
            int bMinZ = this.floor(minZ + 1.0E-4);
            int bMaxZ = this.floor(maxZ - 1.0E-4);
            if (pos[2] >= bMinZ && pos[2] <= bMaxZ) {
                int bMinY = this.floor(minY + 1.0E-4);
                int bMaxY = this.floor(maxY - 1.0E-4);
                return pos[1] >= bMinY && pos[1] <= bMaxY;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean isInsideAnyPlayerPositionCell(Entity player, int[] placePos) {
        Vec3 pos = this.playerPosition(player);
        if (this.isInsidePlayerPositionCell(placePos, pos.field_72450_a, pos.field_72448_b, pos.field_72449_c)) {
            return true;
        }

        if (!this.shouldUseHistoricalPlayerCollisionChecks(player, placePos)) {
            return false;
        }

        Vec3 last = this.playerPositionLast(player);
        return this.isInsidePlayerPositionCell(placePos, last.field_72450_a, last.field_72448_b, last.field_72449_c)
            ? true
            : this.hasLastSentServerPos
                && this.isInsidePlayerPositionCell(
                    placePos, this.lastSentServerPosX, this.lastSentServerPosY, this.lastSentServerPosZ
                );
    }

    private boolean shouldUseHistoricalPlayerCollisionChecks(Entity player, int[] placePos) {
        if (!player.field_70122_E) {
            return false;
        } else {
            return placePos == null ? true : placePos[1] > this.getCurrentBelowTargetY(player);
        }
    }

    private boolean isInsidePlayerPositionCell(int[] placePos, double x, double y, double z) {
        int playerX = this.floor(x);
        int playerY = this.floor(y);
        int playerZ = this.floor(z);
        return placePos[0] == playerX
            && placePos[2] == playerZ
            && (placePos[1] == playerY || placePos[1] == playerY + 1);
    }

    private boolean isStrictOneBelowPlayer(Entity player, int[] pos) {
        if (pos == null) {
            return false;
        }

        int targetY = pos[1];
        int currentY = this.getCurrentBelowTargetY(player);
        if (targetY == currentY) {
            return true;
        }

        if (targetY == this.getStrictBelowTargetY(player)) {
            return true;
        }

        int previousY = this.getPreviousBelowTargetY(player);
        return previousY != Integer.MIN_VALUE && targetY == previousY
            ? true
            : this.isStraightAscendingContext(player) && targetY == currentY + 1;
    }

    private double getStableBelowReferenceY(Entity player) {
        Vec3 pos = this.playerPosition(player);
        double referenceY = pos.field_72448_b;
        Vec3 motion = this.playerMotion();
        if (!player.field_70122_E && motion.field_72448_b > -0.12 && motion.field_72448_b <= 0.0) {
            referenceY = Math.max(referenceY, this.playerPositionLast(player).field_72448_b);
        }

        return referenceY;
    }

    private int getStrictBelowTargetY(Entity player) {
        if (this.isDiagonalMovementContext(player)) {
            return this.getCurrentBelowTargetY(player);
        }

        double projectedY = this.getStableBelowReferenceY(player);
        Vec3 motion = this.playerMotion();
        if (!player.field_70122_E && motion.field_72448_b < -0.12) {
            projectedY = this.playerPosition(player).field_72448_b + motion.field_72448_b * 0.75;
        }

        return this.floor(projectedY) - 1;
    }

    private int getCurrentBelowTargetY(Entity player) {
        return this.floor(this.getStableBelowReferenceY(player)) - 1;
    }

    private int getPreviousBelowTargetY(Entity player) {
        return this.floor(this.playerPositionLast(player).field_72448_b) - 1;
    }

    private boolean isStraightAscendingContext(Entity player) {
        if (this.getConditionModeCheck(player) != 1) {
            return false;
        }

        Vec3 motion = this.playerMotion();
        return motion.field_72448_b > 0.0
            || this.playerPosition(player).field_72448_b > this.playerPositionLast(player).field_72448_b + 1.0E-4;
    }

    private boolean isSupportAvailable(int x, int y, int z) {
        return this.isInteractable(x, y, z) ? false : !this.isReplaceable(x, y, z);
    }

    private boolean isRejectedTarget(int[] pos) {
        Integer rejectedAtTick = this.rejectedTargets.get(this.posKey(pos));
        return rejectedAtTick == null ? false : this.currentClientTick - rejectedAtTick <= 4;
    }

    private void markRejectedTarget(int[] pos) {
        if (pos != null) {
            this.rejectedTargets.put(this.posKey(pos), this.currentClientTick);
        }
    }

    private void pruneRejectedTargets() {
        if (!this.rejectedTargets.isEmpty()) {
            Iterator<Entry<String, Integer>> iterator = this.rejectedTargets.entrySet().iterator();

            while (iterator.hasNext()) {
                Entry<String, Integer> entry = iterator.next();
                if (this.currentClientTick - entry.getValue() > 4) {
                    iterator.remove();
                } else {
                    String[] parts = entry.getKey().split(",");
                    if (!this.isReplaceable(
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])
                    )) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    private Object[] rayCast(float yaw, float pitch) {
        Object[] hit = this.raycastBlock(this.reach(), yaw, pitch);
        if (hit != null && hit[0] != null && hit[2] != null) {
            int face = this.faceFromName((String)hit[2]);
            if (face >= 0 && face != 0) {
                int[] supportPos = this.posFromVec((Vec3)hit[0]);
                Vec3 offset = (Vec3)hit[1];
                Vec3 hitAbs = new Vec3(
                    supportPos[0] + offset.field_72450_a,
                    supportPos[1] + offset.field_72448_b,
                    supportPos[2] + offset.field_72449_c
                );
                return new Object[]{supportPos, face, hitAbs};
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private Vec3 getEyes(Entity player) {
        Vec3 pos = this.playerPosition(player);
        return new Vec3(pos.field_72450_a, pos.field_72448_b + player.func_70047_e(), pos.field_72449_c);
    }

    private String blockNameAt(int x, int y, int z) {
        Block block = BlockUtil.getBlock(new BlockPos(x, y, z));
        return block == null ? "air" : this.blockName(block);
    }

    private String blockName(Block block) {
        try {
            ResourceLocation name = (ResourceLocation)Block.field_149771_c.func_177774_c(block);
            return name == null ? "air" : name.func_110623_a();
        } catch (Exception e) {
            return "air";
        }
    }

    private boolean isReplaceable(int x, int y, int z) {
        return this.isReplaceableName(this.blockNameAt(x, y, z), false);
    }

    private boolean isReplaceableName(String name, boolean airOnly) {
        if (name == null) {
            return false;
        }

        if (airOnly) {
            return name.equals("air");
        }

        for (String replaceable : this.REPLACEABLE_BLOCKS) {
            if (name.equals(replaceable)) {
                return true;
            }
        }

        for (String replaceable : this.EXPERIMENTAL_REPLACEABLE_BLOCKS) {
            if (name.equals(replaceable)) {
                return true;
            }
        }

        return false;
    }

    private boolean isInteractable(int x, int y, int z) {
        Block block = BlockUtil.getBlock(new BlockPos(x, y, z));
        if (block == null) {
            return false;
        }

        if (BlockUtil.isInteractable(block)) {
            return true;
        }

        String type = block.getClass().getSimpleName();
        if (type == null) {
            return false;
        }

        for (String interactableType : this.INTERACTABLE_TYPES) {
            if (type.equals(interactableType)) {
                return true;
            }
        }

        return false;
    }

    private double reach() {
        return mc.field_71442_b.func_78758_h() ? 5.0 : 4.5;
    }

    private int placementTick(Entity player) {
        return this.isRavenTimerActive() ? (int)(this.now() / 50L) : player.field_70173_aa;
    }

    private boolean isRavenTimerActive() {
        try {
            for (Module m : Miau.moduleManager.modules.values()) {
                if (m.getName().equalsIgnoreCase("Timer") && m.isEnabled()) {
                    return true;
                }
            }

            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private float candidatePitch(Object[] candidate) {
        return this.clampFloat((Float)candidate[0], -90.0F, 90.0F);
    }

    private int[] candidateSupportPos(Object[] candidate) {
        return (int[])candidate[1];
    }

    private int candidateFace(Object[] candidate) {
        return (Integer)candidate[2];
    }

    private Vec3 candidateHitVec(Object[] candidate) {
        return (Vec3)candidate[3];
    }

    private int[] candidatePlacedPos(Object[] candidate) {
        return (int[])candidate[4];
    }

    private float sanitizePitch(float pitch, float fallbackPitch) {
        float safeFallback = this.clampFloat(Float.isNaN(fallbackPitch) ? 0.0F : fallbackPitch, -90.0F, 90.0F);
        return !Float.isNaN(pitch) && !Float.isInfinite(pitch) ? this.clampFloat(pitch, -90.0F, 90.0F) : safeFallback;
    }

    private int floor(double value) {
        int i = (int)value;
        return value < i ? i - 1 : i;
    }

    private float clampFloat(float value, float min, float max) {
        return value < min ? min : (value > max ? max : value);
    }

    private float wrapAngle(float angle) {
        angle %= 360.0F;
        if (angle >= 180.0F) {
            angle -= 360.0F;
        }

        if (angle < -180.0F) {
            angle += 360.0F;
        }

        return angle;
    }

    private double distSq(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    private boolean posEquals(int[] a, int[] b) {
        return a != null && b != null && a[0] == b[0] && a[1] == b[1] && a[2] == b[2];
    }

    private String posKey(int[] pos) {
        return pos[0] + "," + pos[1] + "," + pos[2];
    }

    private int[] posFromVec(Vec3 vec) {
        return new int[]{this.floor(vec.field_72450_a), this.floor(vec.field_72448_b), this.floor(vec.field_72449_c)};
    }

    private int[] posFromPos(BlockPos pos) {
        return new int[]{pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p()};
    }

    private String posKeyFromPos(BlockPos pos) {
        return pos.func_177958_n() + "," + pos.func_177956_o() + "," + pos.func_177952_p();
    }

    private long now() {
        return System.currentTimeMillis();
    }

    private ItemStack heldItem(Entity player) {
        return player instanceof EntityLivingBase ? ((EntityLivingBase)player).func_70694_bm() : null;
    }

    private boolean isHoldingBlock(Entity player) {
        return this.isUsableBlockStack(this.heldItem(player));
    }

    private boolean isBlockStack(ItemStack stack) {
        return stack != null && stack.func_77973_b() instanceof ItemBlock;
    }

    private String stackName(ItemStack stack) {
        if (stack != null && this.isBlockStack(stack)) {
            Block block = ((ItemBlock)stack.func_77973_b()).func_179223_d();
            return this.blockName(block);
        } else {
            return "";
        }
    }

    private String faceName(int face) {
        if (face == 0) {
            return "DOWN";
        } else if (face == 1) {
            return "UP";
        } else if (face == 2) {
            return "NORTH";
        } else if (face == 3) {
            return "SOUTH";
        } else {
            return face == 4 ? "WEST" : "EAST";
        }
    }

    private int faceFromName(String name) {
        if (name == null) {
            return -1;
        } else {
            String upper = name.toUpperCase();
            if (upper.equals("DOWN")) {
                return 0;
            } else if (upper.equals("UP")) {
                return 1;
            } else if (upper.equals("NORTH")) {
                return 2;
            } else if (upper.equals("SOUTH")) {
                return 3;
            } else if (upper.equals("WEST")) {
                return 4;
            } else {
                return upper.equals("EAST") ? 5 : -1;
            }
        }
    }

    private int[] offsetPos(int[] pos, int face) {
        if (face == 0) {
            return new int[]{pos[0], pos[1] - 1, pos[2]};
        } else if (face == 1) {
            return new int[]{pos[0], pos[1] + 1, pos[2]};
        } else if (face == 2) {
            return new int[]{pos[0], pos[1], pos[2] - 1};
        } else if (face == 3) {
            return new int[]{pos[0], pos[1], pos[2] + 1};
        } else {
            return face == 4 ? new int[]{pos[0] - 1, pos[1], pos[2]} : new int[]{pos[0] + 1, pos[1], pos[2]};
        }
    }

    private Vec3 playerPosition(Entity player) {
        return new Vec3(player.field_70165_t, player.field_70163_u, player.field_70161_v);
    }

    private Vec3 renderPosition() {
        RenderManager renderManager = mc.func_175598_ae();
        return new Vec3(
            ((IAccessorRenderManager)renderManager).getRenderPosX(),
            ((IAccessorRenderManager)renderManager).getRenderPosY(),
            ((IAccessorRenderManager)renderManager).getRenderPosZ()
        );
    }

    private int[] getDisplaySize() {
        ScaledResolution resolution = new ScaledResolution(mc);
        return new int[]{resolution.func_78326_a(), resolution.func_78328_b()};
    }

    private Object[] raycastBlock(double distance, float yaw, float pitch) {
        if (mc.field_71439_g != null && mc.field_71441_e != null) {
            Vec3 eyePos = mc.field_71439_g.func_174824_e(1.0F);
            Vec3 lookVec = this.getLookVec(yaw, pitch);
            Vec3 targetPos = eyePos.func_72441_c(
                lookVec.field_72450_a * distance, lookVec.field_72448_b * distance, lookVec.field_72449_c * distance
            );
            MovingObjectPosition mop = mc.field_71441_e.func_147447_a(eyePos, targetPos, false, false, true);
            if (mop != null && mop.func_178782_a() != null && mop.field_72307_f != null) {
                BlockPos blockPos = mop.func_178782_a();
                Vec3 blockVec = new Vec3(blockPos.func_177958_n(), blockPos.func_177956_o(), blockPos.func_177952_p());
                Vec3 localHit = new Vec3(
                    mop.field_72307_f.field_72450_a - blockPos.func_177958_n(),
                    mop.field_72307_f.field_72448_b - blockPos.func_177956_o(),
                    mop.field_72307_f.field_72449_c - blockPos.func_177952_p()
                );
                String face = mop.field_178784_b == null ? "" : mop.field_178784_b.func_176610_l().toUpperCase();
                return new Object[]{blockVec, localHit, face};
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private boolean placeBlock(int x, int y, int z, String faceName, Vec3 hitAbs) {
        if (mc.field_71439_g == null) {
            return false;
        }

        int direction = this.faceFromName(faceName);
        if (direction < 0) {
            return false;
        }

        ItemStack stack = this.heldItem(mc.field_71439_g);
        if (!this.isUsableBlockStack(stack)) {
            return false;
        }

        BlockPos pos = new BlockPos(x, y, z);
        float fx = (float)Math.max(0.0, Math.min(1.0, hitAbs.field_72450_a - x));
        float fy = (float)Math.max(0.0, Math.min(1.0, hitAbs.field_72448_b - y));
        float fz = (float)Math.max(0.0, Math.min(1.0, hitAbs.field_72449_c - z));
        mc.field_71439_g
            .field_71174_a
            .func_147297_a(new C08PacketPlayerBlockPlacement(pos, direction, stack, fx, fy, fz));
        return true;
    }

    private boolean autoPlaceOnPacketSent(Packet<?> packet) {
        if (!(packet instanceof C03PacketPlayer)) {
            if (packet instanceof C08PacketPlayerBlockPlacement) {
                C08PacketPlayerBlockPlacement c08 = (C08PacketPlayerBlockPlacement)packet;
                if (c08.func_149568_f() == 255) {
                    if (this.shouldCancelAutoPlaceUseItem()) {
                        this.suppressUse();
                        return false;
                    }
                } else {
                    ItemStack stack = c08.func_149574_g();
                    if (stack != null && this.isBlockStack(stack)) {
                        this.totalC08Counter++;
                        if (!this.placingViaModule) {
                            this.manualC08InWindow = true;
                        }
                    }
                }
            }

            return true;
        } else {
            if (packet instanceof C04PacketPlayerPosition || packet instanceof C06PacketPlayerPosLook) {
                this.hasLastSentServerPos = true;
                this.lastSentServerPosX = mc.field_71439_g.field_70165_t;
                this.lastSentServerPosY = mc.field_71439_g.field_70163_u;
                this.lastSentServerPosZ = mc.field_71439_g.field_70161_v;
            }

            return true;
        }
    }

    private int getKeyCode(String name) {
        if (name == null) {
            return -1;
        }

        KeyBinding binding;
        switch (name) {
            case "forward":
                binding = mc.field_71474_y.field_74351_w;
                break;
            case "back":
                binding = mc.field_71474_y.field_74368_y;
                break;
            case "left":
                binding = mc.field_71474_y.field_74370_x;
                break;
            case "right":
                binding = mc.field_71474_y.field_74366_z;
                break;
            case "jump":
                binding = mc.field_71474_y.field_74314_A;
                break;
            case "sneak":
                binding = mc.field_71474_y.field_74311_E;
                break;
            case "sprint":
                binding = mc.field_71474_y.field_151444_V;
                break;
            case "drop":
                binding = mc.field_71474_y.field_74316_C;
                break;
            case "use":
                binding = mc.field_71474_y.field_74313_G;
                break;
            case "attack":
                binding = mc.field_71474_y.field_74312_F;
                break;
            default:
                return -1;
        }

        return binding == null ? -1 : binding.func_151463_i();
    }

    private void setKeyPressed(String name, boolean pressed) {
        int code = this.getKeyCode(name);
        if (code >= 0) {
            KeyBindUtil.setKeyBindState(code, pressed);
        }
    }

    private boolean isPressed(String name) {
        int code = this.getKeyCode(name);
        return code >= 0 && KeyBindUtil.isKeyDown(code);
    }

    private Vec3 playerMotion() {
        return mc.field_71439_g == null
            ? null
            : new Vec3(mc.field_71439_g.field_70159_w, mc.field_71439_g.field_70181_x, mc.field_71439_g.field_70179_y);
    }

    private Vec3 playerPositionLast(Entity player) {
        return new Vec3(player.field_70142_S, player.field_70137_T, player.field_70136_U);
    }

    private double[] renderRotations() {
        return mc.field_71439_g == null
            ? null
            : new double[]{mc.field_71439_g.field_70177_z, mc.field_71439_g.field_70125_A};
    }

    private int opposite(int face) {
        if (face == 0) {
            return 1;
        } else if (face == 1) {
            return 0;
        } else if (face == 2) {
            return 3;
        } else if (face == 3) {
            return 2;
        } else {
            return face == 4 ? 5 : 4;
        }
    }

    private int rotateY(int face) {
        if (face == 2) {
            return 5;
        } else if (face == 5) {
            return 3;
        } else if (face == 3) {
            return 4;
        } else {
            return face == 4 ? 2 : face;
        }
    }

    private int rotateYCCW(int face) {
        if (face == 2) {
            return 4;
        } else if (face == 4) {
            return 3;
        } else if (face == 3) {
            return 5;
        } else {
            return face == 5 ? 2 : face;
        }
    }

    private int facingFromYaw(float yaw) {
        int index = this.floor(yaw / 90.0 + 0.5) & 3;
        if (index == 0) {
            return 3;
        } else if (index == 1) {
            return 4;
        } else {
            return index == 2 ? 2 : 5;
        }
    }
}
