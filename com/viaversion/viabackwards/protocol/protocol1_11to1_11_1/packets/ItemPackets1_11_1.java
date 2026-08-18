package com.viaversion.viabackwards.protocol.protocol1_11to1_11_1.packets;

import com.viaversion.viabackwards.api.rewriters.LegacyBlockItemRewriter;
import com.viaversion.viabackwards.api.rewriters.LegacyEnchantmentRewriter;
import com.viaversion.viabackwards.protocol.protocol1_11to1_11_1.Protocol1_11To1_11_1;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ClientboundPackets1_9_3;
import com.viaversion.viaversion.protocols.protocol1_9_3to1_9_1_2.ServerboundPackets1_9_3;

public class ItemPackets1_11_1
    extends LegacyBlockItemRewriter<ClientboundPackets1_9_3, ServerboundPackets1_9_3, Protocol1_11To1_11_1> {
    private LegacyEnchantmentRewriter enchantmentRewriter;

    public ItemPackets1_11_1(Protocol1_11To1_11_1 protocol) {
        super(protocol, "1.11.1");
    }

    @Override
    protected void registerPackets() {
        this.registerSetSlot(ClientboundPackets1_9_3.SET_SLOT);
        this.registerWindowItems(ClientboundPackets1_9_3.WINDOW_ITEMS);
        this.registerEntityEquipment(ClientboundPackets1_9_3.ENTITY_EQUIPMENT);
        this.protocol
            .registerClientbound(
                ClientboundPackets1_9_3.PLUGIN_MESSAGE,
                new PacketHandlers() {
                    @Override
                    public void register() {
                        this.map(Type.STRING);
                        this.handler(
                            wrapper -> {
                                if (wrapper.get(Type.STRING, 0).equals("MC|TrList")) {
                                    wrapper.passthrough(Type.INT);
                                    int size = wrapper.passthrough(Type.UNSIGNED_BYTE);

                                    for (int i = 0; i < size; i++) {
                                        wrapper.write(
                                            Type.ITEM1_8,
                                            ItemPackets1_11_1.this.handleItemToClient(
                                                wrapper.user(), wrapper.read(Type.ITEM1_8)
                                            )
                                        );
                                        wrapper.write(
                                            Type.ITEM1_8,
                                            ItemPackets1_11_1.this.handleItemToClient(
                                                wrapper.user(), wrapper.read(Type.ITEM1_8)
                                            )
                                        );
                                        boolean secondItem = wrapper.passthrough(Type.BOOLEAN);
                                        if (secondItem) {
                                            wrapper.write(
                                                Type.ITEM1_8,
                                                ItemPackets1_11_1.this.handleItemToClient(
                                                    wrapper.user(), wrapper.read(Type.ITEM1_8)
                                                )
                                            );
                                        }

                                        wrapper.passthrough(Type.BOOLEAN);
                                        wrapper.passthrough(Type.INT);
                                        wrapper.passthrough(Type.INT);
                                    }
                                }
                            }
                        );
                    }
                }
            );
        this.registerClickWindow(ServerboundPackets1_9_3.CLICK_WINDOW);
        this.registerCreativeInvAction(ServerboundPackets1_9_3.CREATIVE_INVENTORY_ACTION);
        this.protocol.getEntityRewriter().filter().handler((event, meta) -> {
            if (meta.metaType().type().equals(Type.ITEM1_8)) {
                meta.setValue(this.handleItemToClient(event.user(), (Item)meta.getValue()));
            }
        });
    }

    @Override
    protected void registerRewrites() {
        this.enchantmentRewriter = new LegacyEnchantmentRewriter(this.nbtTagName());
        this.enchantmentRewriter.registerEnchantment(22, "§7Sweeping Edge");
    }

    @Override
    public Item handleItemToClient(UserConnection connection, Item item) {
        if (item == null) {
            return null;
        }

        super.handleItemToClient(connection, item);
        this.enchantmentRewriter.handleToClient(item);
        return item;
    }

    @Override
    public Item handleItemToServer(UserConnection connection, Item item) {
        if (item == null) {
            return null;
        }

        super.handleItemToServer(connection, item);
        this.enchantmentRewriter.handleToServer(item);
        return item;
    }
}
