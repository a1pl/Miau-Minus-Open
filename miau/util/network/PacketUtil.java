package miau.util.network;

import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S03PacketTimeUpdate;
import net.minecraft.network.play.server.S04PacketEntityEquipment;
import net.minecraft.network.play.server.S05PacketSpawnPosition;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0APacketUseBed;
import net.minecraft.network.play.server.S0BPacketAnimation;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S0DPacketCollectItem;
import net.minecraft.network.play.server.S0EPacketSpawnObject;
import net.minecraft.network.play.server.S0FPacketSpawnMob;
import net.minecraft.network.play.server.S10PacketSpawnPainting;
import net.minecraft.network.play.server.S11PacketSpawnExperienceOrb;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S1BPacketEntityAttach;
import net.minecraft.network.play.server.S1CPacketEntityMetadata;
import net.minecraft.network.play.server.S1DPacketEntityEffect;
import net.minecraft.network.play.server.S1EPacketRemoveEntityEffect;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S20PacketEntityProperties;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S24PacketBlockAction;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.network.play.server.S28PacketEffect;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.network.play.server.S2APacketParticles;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S31PacketWindowProperty;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S33PacketUpdateSign;
import net.minecraft.network.play.server.S34PacketMaps;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.network.play.server.S36PacketSignEditorOpen;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S3APacketTabComplete;
import net.minecraft.network.play.server.S3BPacketScoreboardObjective;
import net.minecraft.network.play.server.S3CPacketUpdateScore;
import net.minecraft.network.play.server.S3DPacketDisplayScoreboard;
import net.minecraft.network.play.server.S3EPacketTeams;
import net.minecraft.network.play.server.S3FPacketCustomPayload;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.network.play.server.S41PacketServerDifficulty;
import net.minecraft.network.play.server.S42PacketCombatEvent;
import net.minecraft.network.play.server.S43PacketCamera;
import net.minecraft.network.play.server.S44PacketWorldBorder;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.network.play.server.S46PacketSetCompressionLevel;
import net.minecraft.network.play.server.S47PacketPlayerListHeaderFooter;
import net.minecraft.network.play.server.S48PacketResourcePackSend;
import net.minecraft.network.play.server.S49PacketUpdateEntityNBT;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PacketUtil {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final Logger LOGGER = LogManager.getLogger("PacketUtil");
    private static int chunkErrorCount = 0;
    private static long lastChunkErrorTime = 0L;
    public static boolean sendingNoEvent = false;

    public static void sendPacket(Packet<?> packet) {
        mc.func_147114_u().func_147298_b().func_179290_a(packet);
    }

    public static void sendPacketNoEvent(Packet<?> packet) {
        sendingNoEvent = true;

        try {
            mc.func_147114_u().func_147298_b().func_179288_a(packet, null, new GenericFutureListener[0]);
        } finally {
            sendingNoEvent = false;
        }
    }

    public static void handlePacket(Packet<INetHandlerPlayClient> packet) {
        if (packet != null) {
            if (mc.func_147114_u() != null && mc.field_71439_g != null && mc.field_71441_e != null) {
                if (packet instanceof S00PacketKeepAlive) {
                    mc.func_147114_u().func_147272_a((S00PacketKeepAlive)packet);
                } else if (packet instanceof S01PacketJoinGame) {
                    mc.func_147114_u().func_147282_a((S01PacketJoinGame)packet);
                } else if (packet instanceof S02PacketChat) {
                    mc.func_147114_u().func_147251_a((S02PacketChat)packet);
                } else if (packet instanceof S03PacketTimeUpdate) {
                    mc.func_147114_u().func_147285_a((S03PacketTimeUpdate)packet);
                } else if (packet instanceof S04PacketEntityEquipment) {
                    mc.func_147114_u().func_147242_a((S04PacketEntityEquipment)packet);
                } else if (packet instanceof S05PacketSpawnPosition) {
                    mc.func_147114_u().func_147271_a((S05PacketSpawnPosition)packet);
                } else if (packet instanceof S06PacketUpdateHealth) {
                    mc.func_147114_u().func_147249_a((S06PacketUpdateHealth)packet);
                } else if (packet instanceof S07PacketRespawn) {
                    mc.func_147114_u().func_147280_a((S07PacketRespawn)packet);
                } else if (packet instanceof S08PacketPlayerPosLook) {
                    mc.func_147114_u().func_147258_a((S08PacketPlayerPosLook)packet);
                } else if (packet instanceof S09PacketHeldItemChange) {
                    mc.func_147114_u().func_147257_a((S09PacketHeldItemChange)packet);
                } else if (packet instanceof S10PacketSpawnPainting) {
                    mc.func_147114_u().func_147288_a((S10PacketSpawnPainting)packet);
                } else if (packet instanceof S0APacketUseBed) {
                    mc.func_147114_u().func_147278_a((S0APacketUseBed)packet);
                } else if (packet instanceof S0BPacketAnimation) {
                    mc.func_147114_u().func_147279_a((S0BPacketAnimation)packet);
                } else if (packet instanceof S0CPacketSpawnPlayer) {
                    mc.func_147114_u().func_147237_a((S0CPacketSpawnPlayer)packet);
                } else if (packet instanceof S0DPacketCollectItem) {
                    mc.func_147114_u().func_147246_a((S0DPacketCollectItem)packet);
                } else if (packet instanceof S0EPacketSpawnObject) {
                    mc.func_147114_u().func_147235_a((S0EPacketSpawnObject)packet);
                } else if (packet instanceof S0FPacketSpawnMob) {
                    mc.func_147114_u().func_147281_a((S0FPacketSpawnMob)packet);
                } else if (packet instanceof S11PacketSpawnExperienceOrb) {
                    mc.func_147114_u().func_147286_a((S11PacketSpawnExperienceOrb)packet);
                } else if (packet instanceof S12PacketEntityVelocity) {
                    mc.func_147114_u().func_147244_a((S12PacketEntityVelocity)packet);
                } else if (packet instanceof S13PacketDestroyEntities) {
                    mc.func_147114_u().func_147238_a((S13PacketDestroyEntities)packet);
                } else if (packet instanceof S14PacketEntity) {
                    mc.func_147114_u().func_147259_a((S14PacketEntity)packet);
                } else if (packet instanceof S18PacketEntityTeleport) {
                    mc.func_147114_u().func_147275_a((S18PacketEntityTeleport)packet);
                } else if (packet instanceof S19PacketEntityStatus) {
                    mc.func_147114_u().func_147236_a((S19PacketEntityStatus)packet);
                } else if (packet instanceof S19PacketEntityHeadLook) {
                    mc.func_147114_u().func_147267_a((S19PacketEntityHeadLook)packet);
                } else if (packet instanceof S1BPacketEntityAttach) {
                    mc.func_147114_u().func_147243_a((S1BPacketEntityAttach)packet);
                } else if (packet instanceof S1CPacketEntityMetadata) {
                    mc.func_147114_u().func_147284_a((S1CPacketEntityMetadata)packet);
                } else if (packet instanceof S1DPacketEntityEffect) {
                    mc.func_147114_u().func_147260_a((S1DPacketEntityEffect)packet);
                } else if (packet instanceof S1EPacketRemoveEntityEffect) {
                    mc.func_147114_u().func_147262_a((S1EPacketRemoveEntityEffect)packet);
                } else if (packet instanceof S1FPacketSetExperience) {
                    mc.func_147114_u().func_147295_a((S1FPacketSetExperience)packet);
                } else if (packet instanceof S20PacketEntityProperties) {
                    mc.func_147114_u().func_147290_a((S20PacketEntityProperties)packet);
                } else if (packet instanceof S21PacketChunkData) {
                    handleChunkDataSafe((S21PacketChunkData)packet);
                } else if (packet instanceof S22PacketMultiBlockChange) {
                    mc.func_147114_u().func_147287_a((S22PacketMultiBlockChange)packet);
                } else if (packet instanceof S23PacketBlockChange) {
                    mc.func_147114_u().func_147234_a((S23PacketBlockChange)packet);
                } else if (packet instanceof S24PacketBlockAction) {
                    mc.func_147114_u().func_147261_a((S24PacketBlockAction)packet);
                } else if (packet instanceof S25PacketBlockBreakAnim) {
                    mc.func_147114_u().func_147294_a((S25PacketBlockBreakAnim)packet);
                } else if (packet instanceof S26PacketMapChunkBulk) {
                    handleMapChunkBulkSafe((S26PacketMapChunkBulk)packet);
                } else if (packet instanceof S27PacketExplosion) {
                    mc.func_147114_u().func_147283_a((S27PacketExplosion)packet);
                } else if (packet instanceof S28PacketEffect) {
                    mc.func_147114_u().func_147277_a((S28PacketEffect)packet);
                } else if (packet instanceof S29PacketSoundEffect) {
                    mc.func_147114_u().func_147255_a((S29PacketSoundEffect)packet);
                } else if (packet instanceof S2APacketParticles) {
                    mc.func_147114_u().func_147289_a((S2APacketParticles)packet);
                } else if (packet instanceof S2BPacketChangeGameState) {
                    mc.func_147114_u().func_147252_a((S2BPacketChangeGameState)packet);
                } else if (packet instanceof S2CPacketSpawnGlobalEntity) {
                    mc.func_147114_u().func_147292_a((S2CPacketSpawnGlobalEntity)packet);
                } else if (packet instanceof S2DPacketOpenWindow) {
                    mc.func_147114_u().func_147265_a((S2DPacketOpenWindow)packet);
                } else if (packet instanceof S2EPacketCloseWindow) {
                    mc.func_147114_u().func_147276_a((S2EPacketCloseWindow)packet);
                } else if (packet instanceof S2FPacketSetSlot) {
                    mc.func_147114_u().func_147266_a((S2FPacketSetSlot)packet);
                } else if (packet instanceof S30PacketWindowItems) {
                    mc.func_147114_u().func_147241_a((S30PacketWindowItems)packet);
                } else if (packet instanceof S31PacketWindowProperty) {
                    mc.func_147114_u().func_147245_a((S31PacketWindowProperty)packet);
                } else if (packet instanceof S32PacketConfirmTransaction) {
                    mc.func_147114_u().func_147239_a((S32PacketConfirmTransaction)packet);
                } else if (packet instanceof S33PacketUpdateSign) {
                    mc.func_147114_u().func_147248_a((S33PacketUpdateSign)packet);
                } else if (packet instanceof S34PacketMaps) {
                    mc.func_147114_u().func_147264_a((S34PacketMaps)packet);
                } else if (packet instanceof S35PacketUpdateTileEntity) {
                    mc.func_147114_u().func_147273_a((S35PacketUpdateTileEntity)packet);
                } else if (packet instanceof S36PacketSignEditorOpen) {
                    mc.func_147114_u().func_147268_a((S36PacketSignEditorOpen)packet);
                } else if (packet instanceof S37PacketStatistics) {
                    mc.func_147114_u().func_147293_a((S37PacketStatistics)packet);
                } else if (packet instanceof S38PacketPlayerListItem) {
                    mc.func_147114_u().func_147256_a((S38PacketPlayerListItem)packet);
                } else if (packet instanceof S39PacketPlayerAbilities) {
                    mc.func_147114_u().func_147270_a((S39PacketPlayerAbilities)packet);
                } else if (packet instanceof S3APacketTabComplete) {
                    mc.func_147114_u().func_147274_a((S3APacketTabComplete)packet);
                } else if (packet instanceof S3BPacketScoreboardObjective) {
                    mc.func_147114_u().func_147291_a((S3BPacketScoreboardObjective)packet);
                } else if (packet instanceof S3CPacketUpdateScore) {
                    mc.func_147114_u().func_147250_a((S3CPacketUpdateScore)packet);
                } else if (packet instanceof S3DPacketDisplayScoreboard) {
                    mc.func_147114_u().func_147254_a((S3DPacketDisplayScoreboard)packet);
                } else if (packet instanceof S3EPacketTeams) {
                    mc.func_147114_u().func_147247_a((S3EPacketTeams)packet);
                } else if (packet instanceof S3FPacketCustomPayload) {
                    mc.func_147114_u().func_147240_a((S3FPacketCustomPayload)packet);
                } else if (packet instanceof S40PacketDisconnect) {
                    mc.func_147114_u().func_147253_a((S40PacketDisconnect)packet);
                } else if (packet instanceof S41PacketServerDifficulty) {
                    mc.func_147114_u().func_175101_a((S41PacketServerDifficulty)packet);
                } else if (packet instanceof S42PacketCombatEvent) {
                    mc.func_147114_u().func_175098_a((S42PacketCombatEvent)packet);
                } else if (packet instanceof S43PacketCamera) {
                    mc.func_147114_u().func_175094_a((S43PacketCamera)packet);
                } else if (packet instanceof S44PacketWorldBorder) {
                    mc.func_147114_u().func_175093_a((S44PacketWorldBorder)packet);
                } else if (packet instanceof S45PacketTitle) {
                    mc.func_147114_u().func_175099_a((S45PacketTitle)packet);
                } else if (packet instanceof S46PacketSetCompressionLevel) {
                    mc.func_147114_u().func_175100_a((S46PacketSetCompressionLevel)packet);
                } else if (packet instanceof S47PacketPlayerListHeaderFooter) {
                    mc.func_147114_u().func_175096_a((S47PacketPlayerListHeaderFooter)packet);
                } else if (packet instanceof S48PacketResourcePackSend) {
                    mc.func_147114_u().func_175095_a((S48PacketResourcePackSend)packet);
                } else if (packet instanceof S49PacketUpdateEntityNBT) {
                    mc.func_147114_u().func_175097_a((S49PacketUpdateEntityNBT)packet);
                } else {
                    LOGGER.warn(
                        "Unable to match packet type, processing via processPacket: {}",
                        new Object[]{packet.getClass().getSimpleName()}
                    );
                    packet.func_148833_a(mc.func_147114_u());
                }
            } else {
                LOGGER.warn(
                    "Dropped packet {} because netHandler/thePlayer/theWorld is null",
                    new Object[]{packet.getClass().getSimpleName()}
                );
            }
        }
    }

    private static boolean validateChunkData(S21PacketChunkData packet) {
        try {
            if (packet == null) {
                return false;
            }

            int chunkX = packet.func_149273_e();
            int chunkZ = packet.func_149271_f();
            if (Math.abs(chunkX) <= 30000000 && Math.abs(chunkZ) <= 30000000) {
                int extractedSize = packet.func_149276_g();
                if (extractedSize >= 0 && extractedSize <= 2097152) {
                    return true;
                }

                LOGGER.warn(
                    "Invalid chunk data size from ViaVersion: {} bytes at x={}, z={}",
                    new Object[]{extractedSize, chunkX, chunkZ}
                );
                return false;
            } else {
                LOGGER.warn("Invalid chunk coordinates from ViaVersion: x={}, z={}", new Object[]{chunkX, chunkZ});
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("Chunk validation failed due to ViaVersion packet corruption", e);
            return false;
        }
    }

    private static boolean validateMapChunkBulk(S26PacketMapChunkBulk packet) {
        try {
            if (packet == null) {
                return false;
            }

            int chunkCount = packet.func_149254_d();
            if (chunkCount > 0 && chunkCount <= 1024) {
                return true;
            }

            LOGGER.warn("Invalid bulk chunk count from ViaVersion: {}", new Object[]{chunkCount});
            return false;
        } catch (Exception e) {
            LOGGER.error("Bulk chunk validation failed due to ViaVersion packet corruption", e);
            return false;
        }
    }

    private static void logChunkError(String packetType, Exception e, Object... details) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastChunkErrorTime > 5000L) {
            chunkErrorCount = 0;
        }

        chunkErrorCount++;
        lastChunkErrorTime = currentTime;
        if (chunkErrorCount <= 3) {
            LOGGER.error(
                "ViaVersion chunk packet error #{} [{}]: {}",
                new Object[]{chunkErrorCount, packetType, details.length > 0 ? details[0] : "", e}
            );
        } else if (chunkErrorCount == 4) {
            LOGGER.error("ViaVersion chunk errors continuing (suppressing further detailed logs)");
        } else if (chunkErrorCount % 50 == 0) {
            LOGGER.error("ViaVersion chunk error count: {} total", new Object[]{chunkErrorCount});
        }
    }

    private static void handleChunkDataSafe(S21PacketChunkData packet) {
        try {
            if (!validateChunkData(packet)) {
                LOGGER.warn(
                    "Skipping invalid chunk data packet at x={}, z={}",
                    new Object[]{packet.func_149273_e(), packet.func_149271_f()}
                );
                return;
            }

            mc.func_147114_u().func_147263_a(packet);
        } catch (Exception e) {
            logChunkError("S21PacketChunkData", e, "x=" + packet.func_149273_e() + ", z=" + packet.func_149271_f());
        }
    }

    private static void handleMapChunkBulkSafe(S26PacketMapChunkBulk packet) {
        try {
            if (!validateMapChunkBulk(packet)) {
                LOGGER.warn("Skipping invalid bulk chunk packet with {} chunks", new Object[]{packet.func_149254_d()});
                return;
            }

            mc.func_147114_u().func_147269_a(packet);
        } catch (Exception e) {
            logChunkError("S26PacketMapChunkBulk", e, "chunkCount=" + packet.func_149254_d());
        }
    }
}
