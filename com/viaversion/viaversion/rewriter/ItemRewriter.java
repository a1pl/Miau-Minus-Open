package com.viaversion.viaversion.rewriter;

import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.rewriter.RewriterBase;
import com.viaversion.viaversion.api.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType, T extends Protocol<C, ?, ?, S>>
    extends RewriterBase<T>
    implements com.viaversion.viaversion.api.rewriter.ItemRewriter<T> {
    private final Type<Item> itemType;
    private final Type<Item> mappedItemType;
    private final Type<Item[]> itemArrayType;
    private final Type<Item[]> mappedItemArrayType;

    public ItemRewriter(
        T protocol,
        Type<Item> itemType,
        Type<Item[]> itemArrayType,
        Type<Item> mappedItemType,
        Type<Item[]> mappedItemArrayType
    ) {
        super(protocol);
        this.itemType = itemType;
        this.itemArrayType = itemArrayType;
        this.mappedItemType = mappedItemType;
        this.mappedItemArrayType = mappedItemArrayType;
    }

    public ItemRewriter(T protocol, Type<Item> itemType, Type<Item[]> itemArrayType) {
        this(protocol, itemType, itemArrayType, itemType, itemArrayType);
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        if (this.protocol.getMappingData() != null && this.protocol.getMappingData().getItemMappings() != null) {
            item.setIdentifier(this.protocol.getMappingData().getNewItemId(item.identifier()));
        }

        return item;
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        if (this.protocol.getMappingData() != null && this.protocol.getMappingData().getItemMappings() != null) {
            item.setIdentifier(this.protocol.getMappingData().getOldItemId(item.identifier()));
        }

        return item;
    }

    public void registerWindowItems(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.UNSIGNED_BYTE);
                this.handler(wrapper -> {
                    Item[] items = wrapper.read(ItemRewriter.this.itemArrayType);
                    wrapper.write(ItemRewriter.this.mappedItemArrayType, items);

                    for (int i = 0; i < items.length; i++) {
                        items[i] = ItemRewriter.this.handleItemToClient(wrapper.user(), items[i]);
                    }
                });
            }
        });
    }

    public void registerWindowItems1_17_1(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.VAR_INT);
                this.handler(wrapper -> {
                    Item[] items = wrapper.read(ItemRewriter.this.itemArrayType);
                    wrapper.write(ItemRewriter.this.mappedItemArrayType, items);

                    for (int i = 0; i < items.length; i++) {
                        items[i] = ItemRewriter.this.handleItemToClient(wrapper.user(), items[i]);
                    }

                    ItemRewriter.this.handleClientboundItem(wrapper);
                });
            }
        });
    }

    public void registerOpenWindow(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.handler(wrapper -> {
                    int windowType = wrapper.read(Type.VAR_INT);
                    int mappedId = ItemRewriter.this.protocol.getMappingData().getMenuMappings().getNewId(windowType);
                    if (mappedId == -1) {
                        wrapper.cancel();
                    } else {
                        wrapper.write(Type.VAR_INT, mappedId);
                    }
                });
            }
        });
    }

    public void registerSetSlot(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.SHORT);
                this.handler(wrapper -> ItemRewriter.this.handleClientboundItem(wrapper));
            }
        });
    }

    public void registerSetSlot1_17_1(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.VAR_INT);
                this.map(Type.SHORT);
                this.handler(wrapper -> ItemRewriter.this.handleClientboundItem(wrapper));
            }
        });
    }

    public void registerEntityEquipment(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.map(Type.VAR_INT);
                this.handler(wrapper -> ItemRewriter.this.handleClientboundItem(wrapper));
            }
        });
    }

    public void registerEntityEquipmentArray(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.handler(wrapper -> {
                    byte slot;
                    do {
                        slot = wrapper.passthrough(Type.BYTE);
                        ItemRewriter.this.handleClientboundItem(wrapper);
                    } while (slot < 0);
                });
            }
        });
    }

    public void registerCreativeInvAction(S packetType) {
        this.protocol.registerServerbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.SHORT);
                this.handler(wrapper -> ItemRewriter.this.handleServerboundItem(wrapper));
            }
        });
    }

    public void registerClickWindow(S packetType) {
        this.protocol.registerServerbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.SHORT);
                this.map(Type.BYTE);
                this.map(Type.SHORT);
                this.map(Type.VAR_INT);
                this.handler(wrapper -> ItemRewriter.this.handleServerboundItem(wrapper));
            }
        });
    }

    public void registerClickWindow1_17_1(S packetType) {
        this.protocol.registerServerbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.UNSIGNED_BYTE);
                this.map(Type.VAR_INT);
                this.map(Type.SHORT);
                this.map(Type.BYTE);
                this.map(Type.VAR_INT);
                this.handler(wrapper -> {
                    int length = wrapper.passthrough(Type.VAR_INT);

                    for (int i = 0; i < length; i++) {
                        wrapper.passthrough(Type.SHORT);
                        ItemRewriter.this.handleServerboundItem(wrapper);
                    }

                    ItemRewriter.this.handleServerboundItem(wrapper);
                });
            }
        });
    }

    public void registerSetCooldown(C packetType) {
        this.protocol.registerClientbound(packetType, wrapper -> {
            int itemId = wrapper.read(Type.VAR_INT);
            wrapper.write(Type.VAR_INT, this.protocol.getMappingData().getNewItemId(itemId));
        });
    }

    public void registerTradeList(C packetType) {
        this.protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Type.VAR_INT);
            int size = wrapper.passthrough(Type.UNSIGNED_BYTE);

            for (int i = 0; i < size; i++) {
                this.handleClientboundItem(wrapper);
                this.handleClientboundItem(wrapper);
                if (wrapper.passthrough(Type.BOOLEAN)) {
                    this.handleClientboundItem(wrapper);
                }

                wrapper.passthrough(Type.BOOLEAN);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.FLOAT);
                wrapper.passthrough(Type.INT);
            }
        });
    }

    public void registerTradeList1_19(C packetType) {
        this.protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Type.VAR_INT);
            int size = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < size; i++) {
                this.handleClientboundItem(wrapper);
                this.handleClientboundItem(wrapper);
                this.handleClientboundItem(wrapper);
                wrapper.passthrough(Type.BOOLEAN);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.FLOAT);
                wrapper.passthrough(Type.INT);
            }
        });
    }

    public void registerTradeList1_20_5(
        C packetType,
        Type<Item> costType,
        Type<Item> mappedCostType,
        Type<Item> optionalCostType,
        Type<Item> mappedOptionalCostType
    ) {
        this.protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Type.VAR_INT);
            int size = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < size; i++) {
                Item input = wrapper.read(costType);
                wrapper.write(mappedCostType, this.handleItemToClient(wrapper.user(), input));
                this.handleClientboundItem(wrapper);
                Item secondInput = wrapper.read(optionalCostType);
                wrapper.write(mappedOptionalCostType, this.handleItemToClient(wrapper.user(), secondInput));
                wrapper.passthrough(Type.BOOLEAN);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.FLOAT);
                wrapper.passthrough(Type.INT);
            }
        });
    }

    public void registerAdvancements(C packetType) {
        this.protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Type.BOOLEAN);
            int size = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Type.STRING);
                wrapper.passthrough(Type.OPTIONAL_STRING);
                if (wrapper.passthrough(Type.BOOLEAN)) {
                    wrapper.passthrough(Type.COMPONENT);
                    wrapper.passthrough(Type.COMPONENT);
                    this.handleClientboundItem(wrapper);
                    wrapper.passthrough(Type.VAR_INT);
                    int flags = wrapper.passthrough(Type.INT);
                    if ((flags & 1) != 0) {
                        wrapper.passthrough(Type.STRING);
                    }

                    wrapper.passthrough(Type.FLOAT);
                    wrapper.passthrough(Type.FLOAT);
                }

                wrapper.passthrough(Type.STRING_ARRAY);
                int arrayLength = wrapper.passthrough(Type.VAR_INT);

                for (int array = 0; array < arrayLength; array++) {
                    wrapper.passthrough(Type.STRING_ARRAY);
                }
            }
        });
    }

    public void registerAdvancements1_20_2(C packetType) {
        this.registerAdvancements1_20_2(packetType, Type.COMPONENT);
    }

    public void registerAdvancements1_20_3(C packetType) {
        this.registerAdvancements1_20_2(packetType, Type.TAG);
    }

    private void registerAdvancements1_20_2(C packetType, Type<?> componentType) {
        this.protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Type.BOOLEAN);
            int size = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Type.STRING);
                wrapper.passthrough(Type.OPTIONAL_STRING);
                if (wrapper.passthrough(Type.BOOLEAN)) {
                    wrapper.passthrough(componentType);
                    wrapper.passthrough(componentType);
                    this.handleClientboundItem(wrapper);
                    wrapper.passthrough(Type.VAR_INT);
                    int flags = wrapper.passthrough(Type.INT);
                    if ((flags & 1) != 0) {
                        wrapper.passthrough(Type.STRING);
                    }

                    wrapper.passthrough(Type.FLOAT);
                    wrapper.passthrough(Type.FLOAT);
                }

                int requirements = wrapper.passthrough(Type.VAR_INT);

                for (int array = 0; array < requirements; array++) {
                    wrapper.passthrough(Type.STRING_ARRAY);
                }

                wrapper.passthrough(Type.BOOLEAN);
            }
        });
    }

    public void registerWindowPropertyEnchantmentHandler(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.UNSIGNED_BYTE);
                this.handler(wrapper -> {
                    Mappings mappings = ItemRewriter.this.protocol.getMappingData().getEnchantmentMappings();
                    if (mappings != null) {
                        short property = wrapper.passthrough(Type.SHORT);
                        if (property >= 4 && property <= 6) {
                            short enchantmentId = (short)mappings.getNewId(wrapper.read(Type.SHORT));
                            wrapper.write(Type.SHORT, enchantmentId);
                        }
                    }
                });
            }
        });
    }

    public void registerSpawnParticle(C packetType, Type<?> coordType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.INT);
                this.map(Type.BOOLEAN);
                this.map((Type<T>)coordType);
                this.map((Type<T>)coordType);
                this.map((Type<T>)coordType);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.INT);
                this.handler(ItemRewriter.this.getSpawnParticleHandler());
            }
        });
    }

    public void registerSpawnParticle1_19(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.VAR_INT);
                this.map(Type.BOOLEAN);
                this.map(Type.DOUBLE);
                this.map(Type.DOUBLE);
                this.map(Type.DOUBLE);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.INT);
                this.handler(ItemRewriter.this.getSpawnParticleHandler(Type.VAR_INT));
            }
        });
    }

    public void registerSpawnParticle1_20_5(
        C packetType, Type<Particle> unmappedParticleType, Type<Particle> mappedParticleType
    ) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.BOOLEAN);
                this.map(Type.DOUBLE);
                this.map(Type.DOUBLE);
                this.map(Type.DOUBLE);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.FLOAT);
                this.map(Type.INT);
                this.handler(wrapper -> {
                    Particle particle = wrapper.read(unmappedParticleType);
                    ItemRewriter.this.rewriteParticle(wrapper.user(), particle);
                    wrapper.write(mappedParticleType, particle);
                });
            }
        });
    }

    public void registerExplosion(C packetType, Type<Particle> unmappedParticleType, Type<Particle> mappedParticleType) {
        SoundRewriter<C> cSoundRewriter = new SoundRewriter<>(this.protocol);
        this.protocol.registerClientbound(packetType, wrapper -> {
            wrapper.passthrough(Type.DOUBLE);
            wrapper.passthrough(Type.DOUBLE);
            wrapper.passthrough(Type.DOUBLE);
            wrapper.passthrough(Type.FLOAT);
            int blocks = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < blocks; i++) {
                wrapper.passthrough(Type.BYTE);
                wrapper.passthrough(Type.BYTE);
                wrapper.passthrough(Type.BYTE);
            }

            wrapper.passthrough(Type.FLOAT);
            wrapper.passthrough(Type.FLOAT);
            wrapper.passthrough(Type.FLOAT);
            wrapper.passthrough(Type.VAR_INT);
            Particle smallExplosionParticle = wrapper.read(unmappedParticleType);
            Particle largeExplosionParticle = wrapper.read(unmappedParticleType);
            wrapper.write(mappedParticleType, smallExplosionParticle);
            wrapper.write(mappedParticleType, largeExplosionParticle);
            this.rewriteParticle(wrapper.user(), smallExplosionParticle);
            this.rewriteParticle(wrapper.user(), largeExplosionParticle);
            cSoundRewriter.soundHolderHandler().handle(wrapper);
        });
    }

    public PacketHandler getSpawnParticleHandler() {
        return this.getSpawnParticleHandler(Type.INT);
    }

    public PacketHandler getSpawnParticleHandler(Type<Integer> idType) {
        return wrapper -> {
            int id = wrapper.get(idType, 0);
            if (id != -1) {
                ParticleMappings mappings = this.protocol.getMappingData().getParticleMappings();
                if (mappings.isBlockParticle(id)) {
                    int data = wrapper.read(Type.VAR_INT);
                    wrapper.write(Type.VAR_INT, this.protocol.getMappingData().getNewBlockStateId(data));
                } else if (mappings.isItemParticle(id)) {
                    this.handleClientboundItem(wrapper);
                }

                int mappedId = this.protocol.getMappingData().getNewParticleId(id);
                if (mappedId != id) {
                    wrapper.set(idType, 0, mappedId);
                }
            }
        };
    }

    private void handleClientboundItem(PacketWrapper wrapper) throws Exception {
        Item item = this.handleItemToClient(wrapper.user(), wrapper.read(this.itemType));
        wrapper.write(this.mappedItemType, item);
    }

    private void handleServerboundItem(PacketWrapper wrapper) throws Exception {
        Item item = this.handleItemToServer(wrapper.user(), wrapper.read(this.mappedItemType));
        wrapper.write(this.itemType, item);
    }

    protected void rewriteParticle(UserConnection connection, Particle particle) {
        ParticleMappings mappings = this.protocol.getMappingData().getParticleMappings();
        int id = particle.id();
        if (mappings.isBlockParticle(id)) {
            Particle.ParticleData<Integer> data = particle.getArgument(0);
            data.setValue(this.protocol.getMappingData().getNewBlockStateId(data.getValue()));
        } else if (mappings.isItemParticle(id)) {
            Particle.ParticleData<Item> data = particle.getArgument(0);
            Item item = this.handleItemToClient(connection, data.getValue());
            if (this.mappedItemType() != null && this.itemType() != this.mappedItemType()) {
                particle.set(0, this.mappedItemType(), item);
            } else {
                data.setValue(item);
            }
        }

        particle.setId(this.protocol.getMappingData().getNewParticleId(id));
    }

    @Override
    public Type<Item> itemType() {
        return this.itemType;
    }

    @Override
    public Type<Item[]> itemArrayType() {
        return this.itemArrayType;
    }

    @Override
    public Type<Item> mappedItemType() {
        return this.mappedItemType;
    }

    @Override
    public Type<Item[]> mappedItemArrayType() {
        return this.mappedItemArrayType;
    }
}
