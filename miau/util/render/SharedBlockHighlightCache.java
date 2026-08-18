package miau.util.render;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import miau.event.impl.PacketEvent;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.network.play.server.S22PacketMultiBlockChange;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.network.play.server.S22PacketMultiBlockChange.BlockUpdateData;
import net.minecraft.util.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public final class SharedBlockHighlightCache {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static final SharedBlockHighlightCache INSTANCE = new SharedBlockHighlightCache();
    private final Map<Long, Set<BlockPos>> bedFootByChunk = new ConcurrentHashMap<>();
    private final Set<SharedBlockHighlightCache.UpdateListener> updateListeners = ConcurrentHashMap.newKeySet();
    private final Deque<long[]> scanQueue = new ArrayDeque<>();
    private boolean bedAttached;
    private static final BedFootHighlightMatcher BED_MATCHER = new BedFootHighlightMatcher();

    private SharedBlockHighlightCache() {
    }

    public static SharedBlockHighlightCache get() {
        return INSTANCE;
    }

    public void attachBed() {
        this.bedAttached = true;
    }

    public void detachBed() {
        this.bedAttached = false;
        this.bedFootByChunk.clear();
    }

    private boolean isBedActive() {
        return this.bedAttached;
    }

    public boolean anyConsumerActive() {
        return this.isBedActive();
    }

    public void clear() {
        this.bedFootByChunk.clear();
        this.scanQueue.clear();

        for (SharedBlockHighlightCache.UpdateListener listener : this.updateListeners) {
            listener.onCacheCleared();
        }
    }

    public void addUpdateListener(SharedBlockHighlightCache.UpdateListener listener) {
        if (listener != null) {
            this.updateListeners.add(listener);
        }
    }

    public void removeUpdateListener(SharedBlockHighlightCache.UpdateListener listener) {
        if (listener != null) {
            this.updateListeners.remove(listener);
        }
    }

    public void enqueueChunk(int chunkX, int chunkZ) {
        if (this.anyConsumerActive()) {
            this.scanQueue.addLast(new long[]{chunkX, chunkZ});

            for (SharedBlockHighlightCache.UpdateListener listener : this.updateListeners) {
                listener.onChunkQueued(chunkX, chunkZ);
            }
        }
    }

    public void removeChunk(int chunkX, int chunkZ) {
        long k = key(chunkX, chunkZ);
        this.bedFootByChunk.remove(k);

        for (SharedBlockHighlightCache.UpdateListener listener : this.updateListeners) {
            listener.onChunkRemoved(chunkX, chunkZ);
        }
    }

    public void enqueueLoadedChunks() {
        if (this.anyConsumerActive()) {
            this.scanQueue.clear();
            if (mc.field_71441_e != null && mc.field_71439_g != null) {
                int rd = mc.field_71474_y.field_151451_c;
                int pcx = (int)mc.field_71439_g.field_70165_t >> 4;
                int pcz = (int)mc.field_71439_g.field_70161_v >> 4;

                for (int cx = pcx - rd; cx <= pcx + rd; cx++) {
                    for (int cz = pcz - rd; cz <= pcz + rd; cz++) {
                        Chunk chunk = mc.field_71441_e.func_72964_e(cx, cz);
                        if (chunk != null && !(chunk instanceof EmptyChunk)) {
                            this.enqueueChunk(cx, cz);
                        }
                    }
                }
            }
        }
    }

    public void tickScan(int maxSections) {
        if (mc.field_71441_e != null && this.anyConsumerActive()) {
            int remaining = maxSections;

            while (remaining > 0 && !this.scanQueue.isEmpty()) {
                long[] cpos = this.scanQueue.pollFirst();
                int cx = (int)cpos[0];
                int cz = (int)cpos[1];
                Chunk chunk = mc.field_71441_e.func_72964_e(cx, cz);
                if (chunk != null && !(chunk instanceof EmptyChunk)) {
                    remaining -= this.scanChunk(chunk);
                }
            }
        }
    }

    public void onBlockChange(BlockPos pos, IBlockState newState) {
        long ck = key(pos.func_177958_n() >> 4, pos.func_177952_p() >> 4);
        BlockPos immutablePos = new BlockPos(pos.func_177958_n(), pos.func_177956_o(), pos.func_177952_p());
        if (this.isBedActive()) {
            if (BED_MATCHER.matchesBlock(newState) && BED_MATCHER.shouldIndexAt(pos, newState)) {
                this.bedFootByChunk.computeIfAbsent(ck, k -> ConcurrentHashMap.newKeySet()).add(immutablePos);
            } else {
                Set<BlockPos> set = this.bedFootByChunk.get(ck);
                if (set != null) {
                    set.remove(pos);
                }
            }
        }

        for (SharedBlockHighlightCache.UpdateListener listener : this.updateListeners) {
            listener.onBlockChanged(immutablePos, newState);
        }
    }

    public Iterable<Entry<Long, Set<BlockPos>>> entriesBedFeet() {
        return this.bedFootByChunk.entrySet();
    }

    public int totalBedFeet() {
        int n = 0;

        for (Set<BlockPos> s : this.bedFootByChunk.values()) {
            n += s.size();
        }

        return n;
    }

    public boolean containsBedFoot(BlockPos pos) {
        if (pos == null) {
            return false;
        }

        long ck = key(pos.func_177958_n() >> 4, pos.func_177952_p() >> 4);
        Set<BlockPos> set = this.bedFootByChunk.get(ck);
        return set != null && set.contains(pos);
    }

    public void handlePacket(PacketEvent e) {
        if (this.anyConsumerActive()) {
            if (e.getPacket() instanceof S23PacketBlockChange) {
                S23PacketBlockChange pkt = (S23PacketBlockChange)e.getPacket();
                this.onBlockChange(pkt.func_179827_b(), pkt.func_180728_a());
            } else if (e.getPacket() instanceof S22PacketMultiBlockChange) {
                S22PacketMultiBlockChange pkt = (S22PacketMultiBlockChange)e.getPacket();

                for (BlockUpdateData data : pkt.func_179844_a()) {
                    this.onBlockChange(data.func_180090_a(), data.func_180088_c());
                }
            } else if (e.getPacket() instanceof S21PacketChunkData) {
                S21PacketChunkData pkt = (S21PacketChunkData)e.getPacket();
                if (pkt.func_149276_g() == 0) {
                    this.removeChunk(pkt.func_149273_e(), pkt.func_149271_f());
                } else {
                    this.enqueueChunk(pkt.func_149273_e(), pkt.func_149271_f());
                }
            } else if (e.getPacket() instanceof S26PacketMapChunkBulk) {
                S26PacketMapChunkBulk pkt = (S26PacketMapChunkBulk)e.getPacket();

                for (int i = 0; i < pkt.func_149254_d(); i++) {
                    this.enqueueChunk(pkt.func_149255_a(i), pkt.func_149253_b(i));
                }
            }
        }
    }

    private int scanChunk(Chunk chunk) {
        int scanned = 0;
        long ck = key(chunk.field_76635_g, chunk.field_76647_h);
        Set<BlockPos> bedFound = ConcurrentHashMap.newKeySet();
        ExtendedBlockStorage[] sections = chunk.func_76587_i();
        int baseX = chunk.field_76635_g << 4;
        int baseZ = chunk.field_76647_h << 4;

        for (int si = 0; si < sections.length; si++) {
            ExtendedBlockStorage section = sections[si];
            if (section != null) {
                scanned++;
                int baseY = si << 4;

                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            BlockPos pos = new BlockPos(baseX + x, baseY + y, baseZ + z);
                            IBlockState state = section.func_177485_a(x, y, z);
                            if (state != null
                                && this.isBedActive()
                                && BED_MATCHER.matchesBlock(state)
                                && BED_MATCHER.shouldIndexAt(pos, state)) {
                                bedFound.add(pos);
                            }
                        }
                    }
                }
            }
        }

        if (this.isBedActive()) {
            if (!bedFound.isEmpty()) {
                this.bedFootByChunk.put(ck, bedFound);
            } else {
                this.bedFootByChunk.remove(ck);
            }
        }

        return Math.max(scanned, 1);
    }

    private static long key(int cx, int cz) {
        return (long)cx << 32 | cz & 4294967295L;
    }

    public interface UpdateListener {
        void onBlockChanged(BlockPos var1, IBlockState var2);

        void onChunkQueued(int var1, int var2);

        void onChunkRemoved(int var1, int var2);

        void onCacheCleared();
    }
}
