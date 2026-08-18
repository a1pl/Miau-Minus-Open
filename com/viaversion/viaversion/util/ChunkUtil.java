package com.viaversion.viaversion.util;

import com.viaversion.viaversion.api.minecraft.chunks.BaseChunk;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSectionImpl;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import java.util.ArrayList;
import java.util.Arrays;

public class ChunkUtil {
    public static Chunk createEmptyChunk(int chunkX, int chunkZ) {
        return createEmptyChunk(chunkX, chunkZ, 16, 65535);
    }

    public static Chunk createEmptyChunk(int chunkX, int chunkZ, int sectionCount) {
        int sectionBitmask = 0;

        for (int i = 0; i < sectionCount; i++) {
            sectionBitmask = sectionBitmask << 1 | 1;
        }

        return createEmptyChunk(chunkX, chunkZ, sectionCount, sectionBitmask);
    }

    public static Chunk createEmptyChunk(int chunkX, int chunkZ, int sectionCount, int bitmask) {
        ChunkSection[] airSections = new ChunkSection[sectionCount];

        for (int i = 0; i < airSections.length; i++) {
            airSections[i] = new ChunkSectionImpl(true);
            airSections[i].palette(PaletteType.BLOCKS).addId(0);
        }

        return new BaseChunk(chunkX, chunkZ, true, false, bitmask, airSections, new int[256], new ArrayList<>());
    }

    public static void setDummySkylight(Chunk chunk) {
        setDummySkylight(chunk, false);
    }

    public static void setDummySkylight(Chunk chunk, boolean fullbright) {
        for (ChunkSection section : chunk.getSections()) {
            if (section != null && section.hasLight()) {
                byte[] skyLight = new byte[2048];
                if (fullbright) {
                    Arrays.fill(skyLight, (byte)-1);
                }

                section.getLight().setSkyLight(skyLight);
            }
        }
    }
}
