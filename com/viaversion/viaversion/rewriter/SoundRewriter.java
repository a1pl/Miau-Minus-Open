package com.viaversion.viaversion.rewriter;

import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;

public class SoundRewriter<C extends ClientboundPacketType> {
    protected final Protocol<C, ?, ?, ?> protocol;
    protected final IdRewriteFunction idRewriter;

    public SoundRewriter(Protocol<C, ?, ?, ?> protocol) {
        this.protocol = protocol;
        this.idRewriter = id -> protocol.getMappingData().getSoundMappings().getNewId(id);
    }

    public SoundRewriter(Protocol<C, ?, ?, ?> protocol, IdRewriteFunction idRewriter) {
        this.protocol = protocol;
        this.idRewriter = idRewriter;
    }

    public void registerSound(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.handler(SoundRewriter.this.getSoundHandler());
            }
        });
    }

    public void register1_19_3Sound(C packetType) {
        this.protocol.registerClientbound(packetType, this.soundHolderHandler());
    }

    public PacketHandler soundHolderHandler() {
        return wrapper -> {
            Holder<SoundEvent> soundEventHolder = wrapper.read(Type.SOUND_EVENT);
            if (soundEventHolder.isDirect()) {
                wrapper.write(Type.SOUND_EVENT, soundEventHolder);
            } else {
                int mappedId = this.idRewriter.rewrite(soundEventHolder.id());
                if (mappedId == -1) {
                    wrapper.cancel();
                } else {
                    if (mappedId != soundEventHolder.id()) {
                        soundEventHolder = Holder.of(mappedId);
                    }

                    wrapper.write(Type.SOUND_EVENT, soundEventHolder);
                }
            }
        };
    }

    public PacketHandler getSoundHandler() {
        return wrapper -> {
            int soundId = wrapper.get(Type.VAR_INT, 0);
            int mappedId = this.idRewriter.rewrite(soundId);
            if (mappedId == -1) {
                wrapper.cancel();
            } else if (soundId != mappedId) {
                wrapper.set(Type.VAR_INT, 0, mappedId);
            }
        };
    }
}
