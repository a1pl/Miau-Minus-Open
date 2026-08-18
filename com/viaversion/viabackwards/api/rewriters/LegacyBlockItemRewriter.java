package com.viaversion.viabackwards.api.rewriters;

import com.viaversion.viabackwards.api.BackwardsProtocol;
import com.viaversion.viabackwards.api.data.BackwardsMappingDataLoader;
import com.viaversion.viabackwards.api.data.MappedLegacyBlockItem;
import com.viaversion.viabackwards.protocol.protocol1_11_1to1_12.data.BlockColors;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.BlockChangeRecord;
import com.viaversion.viaversion.api.minecraft.chunks.Chunk;
import com.viaversion.viaversion.api.minecraft.chunks.ChunkSection;
import com.viaversion.viaversion.api.minecraft.chunks.DataPalette;
import com.viaversion.viaversion.api.minecraft.chunks.PaletteType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_12;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.api.protocol.packet.ServerboundPacketType;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandler;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ShortTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.IdAndData;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class LegacyBlockItemRewriter<C extends ClientboundPacketType, S extends ServerboundPacketType, T extends BackwardsProtocol<C, ?, ?, S>>
    extends BackwardsItemRewriterBase<C, S, T> {
    protected final Int2ObjectMap<MappedLegacyBlockItem> replacementData = new Int2ObjectOpenHashMap<>(8);

    protected LegacyBlockItemRewriter(
        T protocol,
        String name,
        Type<Item> itemType,
        Type<Item[]> itemArrayType,
        Type<Item> mappedItemType,
        Type<Item[]> mappedItemArrayType
    ) {
        super(protocol, itemType, itemArrayType, mappedItemType, mappedItemArrayType, false);
        JsonObject jsonObject = this.readMappingsFile("item-mappings-" + name + ".json");

        for (MappedLegacyBlockItem.Type value : MappedLegacyBlockItem.Type.values()) {
            this.addMappings(value, jsonObject, this.replacementData);
        }
    }

    protected LegacyBlockItemRewriter(T protocol, String name, Type<Item> itemType, Type<Item[]> itemArrayType) {
        this(protocol, name, itemType, itemArrayType, itemType, itemArrayType);
    }

    protected LegacyBlockItemRewriter(T protocol, String name) {
        this(protocol, name, Type.ITEM1_8, Type.ITEM1_8_SHORT_ARRAY);
    }

    private void addMappings(
        MappedLegacyBlockItem.Type type, JsonObject object, Int2ObjectMap<MappedLegacyBlockItem> mappings
    ) {
        if (object.has(type.getName())) {
            JsonObject mappingsObject = object.getAsJsonObject(type.getName());

            for (Entry<String, JsonElement> dataEntry : mappingsObject.entrySet()) {
                this.addMapping(dataEntry.getKey(), dataEntry.getValue().getAsJsonObject(), type, mappings);
            }
        }
    }

    private void addMapping(
        String key, JsonObject object, MappedLegacyBlockItem.Type type, Int2ObjectMap<MappedLegacyBlockItem> mappings
    ) {
        int id = object.getAsJsonPrimitive("id").getAsInt();
        JsonPrimitive jsonData = object.getAsJsonPrimitive("data");
        short data = jsonData != null ? jsonData.getAsShort() : 0;
        String name = type != MappedLegacyBlockItem.Type.BLOCK ? object.getAsJsonPrimitive("name").getAsString() : null;
        if (key.indexOf(45) == -1) {
            int dataSeparatorIndex = key.indexOf(58);
            int unmappedId;
            if (dataSeparatorIndex != -1) {
                short unmappedData = Short.parseShort(key.substring(dataSeparatorIndex + 1));
                unmappedId = Integer.parseInt(key.substring(0, dataSeparatorIndex));
                unmappedId = IdAndData.toRawData(unmappedId, unmappedData);
            } else {
                unmappedId = IdAndData.toRawData(Integer.parseInt(key));
            }

            mappings.put(unmappedId, new MappedLegacyBlockItem(id, data, name, type));
        } else {
            String[] split = key.split("-", 2);
            int from = Integer.parseInt(split[0]);
            int to = Integer.parseInt(split[1]);
            if (name != null && name.contains("%color%")) {
                for (int i = from; i <= to; i++) {
                    mappings.put(
                        IdAndData.toRawData(i),
                        new MappedLegacyBlockItem(id, data, name.replace("%color%", BlockColors.get(i - from)), type)
                    );
                }
            } else {
                MappedLegacyBlockItem mappedBlockItem = new MappedLegacyBlockItem(id, data, name, type);

                for (int i = from; i <= to; i++) {
                    mappings.put(IdAndData.toRawData(i), mappedBlockItem);
                }
            }
        }
    }

    public void registerBlockChange(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map((Type<T>)Type.POSITION1_8);
                this.map(Type.VAR_INT);
                this.handler(wrapper -> {
                    int idx = wrapper.get(Type.VAR_INT, 0);
                    wrapper.set(Type.VAR_INT, 0, LegacyBlockItemRewriter.this.handleBlockId(idx));
                });
            }
        });
    }

    public void registerMultiBlockChange(C packetType) {
        this.protocol.registerClientbound(packetType, new PacketHandlers() {
            @Override
            public void register() {
                this.map(Type.INT);
                this.map(Type.INT);
                this.map((Type<T>)Type.BLOCK_CHANGE_RECORD_ARRAY);
                this.handler(wrapper -> {
                    for (BlockChangeRecord record : wrapper.get(Type.BLOCK_CHANGE_RECORD_ARRAY, 0)) {
                        record.setBlockId(LegacyBlockItemRewriter.this.handleBlockId(record.getBlockId()));
                    }
                });
            }
        });
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        MappedLegacyBlockItem data = this.getMappedBlockItem(item.identifier(), item.data());
        if (data != null && data.getType() != MappedLegacyBlockItem.Type.BLOCK) {
            if (item.tag() == null) {
                item.setTag(new CompoundTag());
            }

            short originalData = item.data();
            item.tag().putInt(this.nbtTagName("id"), item.identifier());
            item.setIdentifier(data.getId());
            if (data.getData() != -1) {
                item.setData(data.getData());
                item.tag().putShort(this.nbtTagName("data"), originalData);
            }

            if (data.getName() != null) {
                CompoundTag display = item.tag().getCompoundTag("display");
                if (display == null) {
                    item.tag().put("display", display = new CompoundTag());
                }

                StringTag nameTag = display.getStringTag("Name");
                if (nameTag == null) {
                    nameTag = new StringTag(data.getName());
                    display.put("Name", nameTag);
                    display.put(this.nbtTagName("customName"), new ByteTag());
                }

                String value = nameTag.getValue();
                if (value.contains("%vb_color%")) {
                    display.putString("Name", value.replace("%vb_color%", BlockColors.get(originalData)));
                }
            }

            return item;
        } else {
            return super.handleItemToClient(connection, item);
        }
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        super.handleItemToServer(connection, item);
        if (item.tag() != null) {
            Tag originalId = item.tag().remove(this.nbtTagName("id"));
            if (originalId instanceof IntTag) {
                item.setIdentifier(((NumberTag)originalId).asInt());
            }

            Tag originalData = item.tag().remove(this.nbtTagName("data"));
            if (originalData instanceof ShortTag) {
                item.setData(((NumberTag)originalData).asShort());
            }
        }

        return item;
    }

    public PacketHandler getFallingBlockHandler() {
        return wrapper -> {
            Optional<EntityTypes1_12.ObjectType> type = EntityTypes1_12.ObjectType.findById(wrapper.get(Type.BYTE, 0));
            if (type.isPresent() && type.get() == EntityTypes1_12.ObjectType.FALLING_BLOCK) {
                int objectData = wrapper.get(Type.INT, 0);
                IdAndData block = this.handleBlock(objectData & 4095, objectData >> 12 & 15);
                if (block == null) {
                    return;
                }

                wrapper.set(Type.INT, 0, block.getId() | block.getData() << 12);
            }
        };
    }

    public @Nullable IdAndData handleBlock(int blockId, int data) {
        MappedLegacyBlockItem settings = this.getMappedBlockItem(blockId, data);
        if (settings != null && settings.getType() != MappedLegacyBlockItem.Type.ITEM) {
            IdAndData block = settings.getBlock();
            return block.getData() == -1 ? block.withData(data) : block;
        } else {
            return null;
        }
    }

    public int handleBlockId(int rawId) {
        int id = IdAndData.getId(rawId);
        int data = IdAndData.getData(rawId);
        IdAndData mappedBlock = this.handleBlock(id, data);
        return mappedBlock == null ? rawId : IdAndData.toRawData(mappedBlock.getId(), mappedBlock.getData());
    }

    public void handleChunk(Chunk chunk) {
        Map<LegacyBlockItemRewriter.Pos, CompoundTag> tags = new HashMap<>();

        for (CompoundTag tag : chunk.getBlockEntities()) {
            NumberTag xTag;
            NumberTag yTag;
            NumberTag zTag;
            if ((xTag = tag.getNumberTag("x")) != null
                && (yTag = tag.getNumberTag("y")) != null
                && (zTag = tag.getNumberTag("z")) != null) {
                LegacyBlockItemRewriter.Pos pos = new LegacyBlockItemRewriter.Pos(
                    xTag.asInt() & 15, yTag.asInt(), zTag.asInt() & 15
                );
                tags.put(pos, tag);
                if (pos.getY() >= 0 && pos.getY() <= 255) {
                    ChunkSection section = chunk.getSections()[pos.getY() >> 4];
                    if (section != null) {
                        int block = section.palette(PaletteType.BLOCKS).idAt(pos.getX(), pos.getY() & 15, pos.getZ());
                        MappedLegacyBlockItem settings = this.getMappedBlockItem(block);
                        if (settings != null && settings.hasBlockEntityHandler()) {
                            settings.getBlockEntityHandler().handleOrNewCompoundTag(block, tag);
                        }
                    }
                }
            }
        }

        for (int i = 0; i < chunk.getSections().length; i++) {
            ChunkSection section = chunk.getSections()[i];
            if (section != null) {
                boolean hasBlockEntityHandler = false;
                DataPalette palette = section.palette(PaletteType.BLOCKS);

                for (int j = 0; j < palette.size(); j++) {
                    int block = palette.idByIndex(j);
                    int btype = block >> 4;
                    int meta = block & 15;
                    IdAndData b = this.handleBlock(btype, meta);
                    if (b != null) {
                        palette.setIdByIndex(j, IdAndData.toRawData(b.getId(), b.getData()));
                    }

                    if (!hasBlockEntityHandler) {
                        MappedLegacyBlockItem settings = this.getMappedBlockItem(block);
                        if (settings != null && settings.hasBlockEntityHandler()) {
                            hasBlockEntityHandler = true;
                        }
                    }
                }

                if (hasBlockEntityHandler) {
                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                int block = palette.idAt(x, y, z);
                                MappedLegacyBlockItem settings = this.getMappedBlockItem(block);
                                if (settings != null && settings.hasBlockEntityHandler()) {
                                    LegacyBlockItemRewriter.Pos pos = new LegacyBlockItemRewriter.Pos(
                                        x, y + (i << 4), z
                                    );
                                    if (!tags.containsKey(pos)) {
                                        CompoundTag tag = new CompoundTag();
                                        tag.putInt("x", x + (chunk.getX() << 4));
                                        tag.putInt("y", y + (i << 4));
                                        tag.putInt("z", z + (chunk.getZ() << 4));
                                        settings.getBlockEntityHandler().handleOrNewCompoundTag(block, tag);
                                        chunk.getBlockEntities().add(tag);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    protected CompoundTag getNamedTag(String text) {
        CompoundTag tag = new CompoundTag();
        CompoundTag displayTag = new CompoundTag();
        tag.put("display", displayTag);
        text = "§r" + text;
        displayTag.putString("Name", this.jsonNameFormat ? ComponentUtil.legacyToJsonString(text) : text);
        return tag;
    }

    private @Nullable MappedLegacyBlockItem getMappedBlockItem(int id, int data) {
        MappedLegacyBlockItem mapping = this.replacementData.get(IdAndData.toRawData(id, data));
        return mapping == null && data != 0 ? this.replacementData.get(IdAndData.toRawData(id)) : mapping;
    }

    private @Nullable MappedLegacyBlockItem getMappedBlockItem(int rawId) {
        MappedLegacyBlockItem mapping = this.replacementData.get(rawId);
        return mapping != null ? mapping : this.replacementData.get(IdAndData.removeData(rawId));
    }

    protected JsonObject readMappingsFile(String name) {
        return BackwardsMappingDataLoader.INSTANCE.loadFromDataDir(name);
    }

    private static final class Pos {
        private final int x;
        private final short y;
        private final int z;

        private Pos(int x, int y, int z) {
            this.x = x;
            this.y = (short)y;
            this.z = z;
        }

        public int getX() {
            return this.x;
        }

        public int getY() {
            return this.y;
        }

        public int getZ() {
            return this.z;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o != null && this.getClass() == o.getClass()) {
                LegacyBlockItemRewriter.Pos pos = (LegacyBlockItemRewriter.Pos)o;
                if (this.x != pos.x) {
                    return false;
                } else {
                    return this.y != pos.y ? false : this.z == pos.z;
                }
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int result = this.x;
            result = 31 * result + this.y;
            return 31 * result + this.z;
        }

        @Override
        public String toString() {
            return "Pos{x=" + this.x + ", y=" + this.y + ", z=" + this.z + '}';
        }
    }
}
