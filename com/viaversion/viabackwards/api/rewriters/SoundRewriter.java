package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;

public class SoundRewriter<C extends ClientboundPacketType> extends com.viaversion.viaversion.rewriter.SoundRewriter<C> {
    private final BackwardsProtocol<C, ?, ?, ?> protocol;

    public SoundRewriter(BackwardsProtocol<C, ?, ?, ?> protocol) {
        super(protocol);
        this.protocol = protocol;
    }

    public void registerNamedSound(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.STRING);
                this.handler(SoundRewriter.this.getNamedSoundHandler());
            }
        });
    }

    public void registerStopSound(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.handler(SoundRewriter.this.getStopSoundHandler());
            }
        });
    }

    public PacketHandler getNamedSoundHandler() {
        return wrapper -> {
            String soundId = wrapper.get(Type.STRING, 0);
            String mappedId = this.protocol.getMappingData().getMappedNamedSound(soundId);
            if (mappedId != null) {
                if (!mappedId.isEmpty()) {
                    wrapper.set(Type.STRING, 0, mappedId);
                } else {
                    wrapper.cancel();
                }
            }
        };
    }

    public PacketHandler getStopSoundHandler() {
        return wrapper -> {
            byte flags = wrapper.passthrough(Type.BYTE);
            if ((flags & 2) != 0) {
                if ((flags & 1) != 0) {
                    wrapper.passthrough(Type.VAR_INT);
                }

                String soundId = wrapper.read(Type.STRING);
                String mappedId = this.protocol.getMappingData().getMappedNamedSound(soundId);
                if (mappedId == null) {
                    wrapper.write(Type.STRING, soundId);
                } else {
                    if (!mappedId.isEmpty()) {
                        wrapper.write(Type.STRING, mappedId);
                    } else {
                        wrapper.cancel();
                    }
                }
            }
        };
    }

    @Override
    public void register1_19_3Sound(C packetType) {
        this.protocol.registerClientbound(packetType, this.get1_19_3SoundHandler());
    }

    public PacketHandler get1_19_3SoundHandler() {
        return wrapper -> {
            Holder<SoundEvent> soundEventHolder = wrapper.read(Type.SOUND_EVENT);
            if (soundEventHolder.isDirect()) {
                wrapper.write(Type.SOUND_EVENT, this.rewriteSoundEvent(wrapper, soundEventHolder));
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

    public Holder<SoundEvent> rewriteSoundEvent(PacketWrapper wrapper, Holder<SoundEvent> soundEventHolder) {
        SoundEvent soundEvent = soundEventHolder.value();
        String mappedIdentifier = this.protocol.getMappingData().getMappedNamedSound(soundEvent.identifier());
        if (mappedIdentifier != null) {
            if (!mappedIdentifier.isEmpty()) {
                return Holder.of(soundEvent.withIdentifier(mappedIdentifier));
            }

            wrapper.cancel();
        }

        return soundEventHolder;
    }
}
