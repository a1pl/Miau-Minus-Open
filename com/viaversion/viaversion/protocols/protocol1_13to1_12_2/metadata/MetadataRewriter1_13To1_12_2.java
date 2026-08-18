package com.viaversion.viaversion.protocols.protocol1_13to1_12_2.metadata;

import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_13;
import com.viaversion.viaversion.api.minecraft.metadata.Metadata;
import com.viaversion.viaversion.api.type.types.version.Types1_13;
import com.viaversion.viaversion.protocols.protocol1_12_1to1_12.ClientboundPackets1_12_1;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.data.EntityTypeRewriter;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.data.ParticleRewriter;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.packets.WorldPackets;
import com.viaversion.viaversion.rewriter.EntityRewriter;
import com.viaversion.viaversion.util.ComponentUtil;

public class MetadataRewriter1_13To1_12_2 extends EntityRewriter<ClientboundPackets1_12_1, Protocol1_13To1_12_2> {
    public MetadataRewriter1_13To1_12_2(Protocol1_13To1_12_2 protocol) {
        super(protocol);
    }

    @Override
    protected void registerRewrites() {
        this.filter().mapMetaType(typeId -> Types1_13.META_TYPES.byId(typeId > 4 ? typeId + 1 : typeId));
        this.filter()
            .metaType(Types1_13.META_TYPES.itemType)
            .handler((event, meta) -> this.protocol.getItemRewriter().handleItemToClient(event.user(), meta.value()));
        this.filter().metaType(Types1_13.META_TYPES.blockStateType).handler((event, meta) -> {
            int oldId = meta.<Integer>value();
            if (oldId != 0) {
                int combined = (oldId & 4095) << 4 | oldId >> 12 & 15;
                int newId = WorldPackets.toNewId(combined);
                meta.setValue(newId);
            }
        });
        this.filter().index(0).handler((event, meta) -> meta.setValue((byte)((Byte)meta.getValue() & -17)));
        this.filter()
            .index(2)
            .handler(
                (event, meta) -> {
                    if (meta.getValue() != null && !((String)meta.getValue()).isEmpty()) {
                        meta.setTypeAndValue(
                            Types1_13.META_TYPES.optionalComponentType,
                            ComponentUtil.legacyToJson((String)meta.getValue())
                        );
                    } else {
                        meta.setTypeAndValue(Types1_13.META_TYPES.optionalComponentType, null);
                    }
                }
            );
        this.filter()
            .type(EntityTypes1_13.EntityType.WOLF)
            .index(17)
            .handler((event, meta) -> meta.setValue(15 - (Integer)meta.getValue()));
        this.filter().type(EntityTypes1_13.EntityType.ZOMBIE).addIndex(15);
        this.filter().type(EntityTypes1_13.EntityType.MINECART_ABSTRACT).index(9).handler((event, meta) -> {
            int oldId = meta.<Integer>value();
            int combined = (oldId & 4095) << 4 | oldId >> 12 & 15;
            int newId = WorldPackets.toNewId(combined);
            meta.setValue(newId);
        });
        this.filter().type(EntityTypes1_13.EntityType.AREA_EFFECT_CLOUD).handler((event, meta) -> {
            if (meta.id() == 9) {
                int particleId = meta.<Integer>value();
                Metadata parameter1Meta = event.metaAtIndex(10);
                Metadata parameter2Meta = event.metaAtIndex(11);
                int parameter1 = parameter1Meta != null ? parameter1Meta.<Integer>value() : 0;
                int parameter2 = parameter2Meta != null ? parameter2Meta.<Integer>value() : 0;
                Particle particle = ParticleRewriter.rewriteParticle(particleId, new Integer[]{parameter1, parameter2});
                if (particle != null && particle.getId() != -1) {
                    event.createExtraMeta(new Metadata(9, Types1_13.META_TYPES.particleType, particle));
                }
            }

            if (meta.id() >= 9) {
                event.cancel();
            }
        });
    }

    @Override
    public int newEntityId(int id) {
        return EntityTypeRewriter.getNewId(id);
    }

    @Override
    public EntityType typeFromId(int type) {
        return EntityTypes1_13.getTypeFromId(type, false);
    }

    @Override
    public EntityType objectTypeFromId(int type) {
        return EntityTypes1_13.getTypeFromId(type, true);
    }
}
