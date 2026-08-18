package com.viaversion.viaversion.rewriter;

import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.type.Type;

public class AttributeRewriter<C extends ClientboundPacketType> {
    private final Protocol<C, ?, ?, ?> protocol;

    public AttributeRewriter(Protocol<C, ?, ?, ?> protocol) {
        this.protocol = protocol;
    }

    public void register1_20_5(C packetType) {
        this.protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Type.VAR_INT);
            int size = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < size; i++) {
                int attributeId = wrapper.read(Type.VAR_INT);
                wrapper.write(Type.VAR_INT, this.protocol.getMappingData().getNewAttributeId(attributeId));
                wrapper.passthrough(Type.DOUBLE);
                int modifierSize = wrapper.passthrough(Type.VAR_INT);

                for (int j = 0; j < modifierSize; j++) {
                    wrapper.passthrough(Type.UUID);
                    wrapper.passthrough(Type.DOUBLE);
                    wrapper.passthrough(Type.BYTE);
                }
            }
        });
    }
}
