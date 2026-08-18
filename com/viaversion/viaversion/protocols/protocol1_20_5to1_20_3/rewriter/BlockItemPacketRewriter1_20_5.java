package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.rewriter;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.ParticleMappings;
import com.viaversion.viaversion.api.minecraft.GameProfile;
import com.viaversion.viaversion.api.minecraft.GlobalPosition;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.minecraft.Particle;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.StructuredItem;
import com.viaversion.viaversion.api.minecraft.item.data.AdventureModePredicate;
import com.viaversion.viaversion.api.minecraft.item.data.ArmorTrim;
import com.viaversion.viaversion.api.minecraft.item.data.ArmorTrimMaterial;
import com.viaversion.viaversion.api.minecraft.item.data.ArmorTrimPattern;
import com.viaversion.viaversion.api.minecraft.item.data.AttributeModifier;
import com.viaversion.viaversion.api.minecraft.item.data.AttributeModifiers;
import com.viaversion.viaversion.api.minecraft.item.data.BannerPattern;
import com.viaversion.viaversion.api.minecraft.item.data.BannerPatternLayer;
import com.viaversion.viaversion.api.minecraft.item.data.Bee;
import com.viaversion.viaversion.api.minecraft.item.data.BlockPredicate;
import com.viaversion.viaversion.api.minecraft.item.data.BlockStateProperties;
import com.viaversion.viaversion.api.minecraft.item.data.DyedColor;
import com.viaversion.viaversion.api.minecraft.item.data.Enchantments;
import com.viaversion.viaversion.api.minecraft.item.data.FilterableComponent;
import com.viaversion.viaversion.api.minecraft.item.data.FilterableString;
import com.viaversion.viaversion.api.minecraft.item.data.FireworkExplosion;
import com.viaversion.viaversion.api.minecraft.item.data.Fireworks;
import com.viaversion.viaversion.api.minecraft.item.data.FoodEffect;
import com.viaversion.viaversion.api.minecraft.item.data.FoodProperties;
import com.viaversion.viaversion.api.minecraft.item.data.Instrument;
import com.viaversion.viaversion.api.minecraft.item.data.LodestoneTracker;
import com.viaversion.viaversion.api.minecraft.item.data.ModifierData;
import com.viaversion.viaversion.api.minecraft.item.data.PotDecorations;
import com.viaversion.viaversion.api.minecraft.item.data.PotionContents;
import com.viaversion.viaversion.api.minecraft.item.data.PotionEffect;
import com.viaversion.viaversion.api.minecraft.item.data.PotionEffectData;
import com.viaversion.viaversion.api.minecraft.item.data.StatePropertyMatcher;
import com.viaversion.viaversion.api.minecraft.item.data.SuspiciousStewEffect;
import com.viaversion.viaversion.api.minecraft.item.data.ToolProperties;
import com.viaversion.viaversion.api.minecraft.item.data.ToolRule;
import com.viaversion.viaversion.api.minecraft.item.data.Unbreakable;
import com.viaversion.viaversion.api.minecraft.item.data.WrittenBook;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.api.type.types.chunk.ChunkType1_20_2;
import com.viaversion.viaversion.api.type.types.version.Types1_20_3;
import com.viaversion.viaversion.api.type.types.version.Types1_20_5;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntOpenHashMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.opennbt.stringified.SNBT;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPacket1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.rewriter.RecipeRewriter1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Attributes1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.BannerPatterns1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.DyeColors;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Enchantments1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.EquipmentSlots1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Instruments1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.MapDecorations1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.PotionEffects1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Potions1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.TrimMaterials1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.TrimPatterns1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundPacket1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.rewriter.BlockRewriter;
import com.viaversion.viaversion.rewriter.ItemRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.Either;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.MathUtil;
import com.viaversion.viaversion.util.SerializerVersion;
import com.viaversion.viaversion.util.UUIDUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class BlockItemPacketRewriter1_20_5
    extends ItemRewriter<ClientboundPacket1_20_3, ServerboundPacket1_20_5, Protocol1_20_5To1_20_3> {
    public static final String[] MOB_TAGS = new String[]{
        "NoAI",
        "Silent",
        "NoGravity",
        "Glowing",
        "Invulnerable",
        "Health",
        "Age",
        "Variant",
        "HuntingCooldown",
        "BucketVariantTag"
    };
    public static final String[] ATTRIBUTE_OPERATIONS = new String[]{
        "add_value", "add_multiplied_base", "add_multiplied_total"
    };
    private static final StructuredDataConverter DATA_CONVERTER = new StructuredDataConverter(false);
    private static final GameProfile.Property[] EMPTY_PROPERTIES = new GameProfile.Property[0];
    private static final StatePropertyMatcher[] EMPTY_PROPERTY_MATCHERS = new StatePropertyMatcher[0];

    public BlockItemPacketRewriter1_20_5(Protocol1_20_5To1_20_3 protocol) {
        super(protocol, Type.ITEM1_20_2, Type.ITEM1_20_2_ARRAY, Types1_20_5.ITEM, Types1_20_5.ITEM_ARRAY);
    }

    @Override
    public void registerPackets() {
        BlockRewriter<ClientboundPacket1_20_3> blockRewriter = BlockRewriter.for1_20_2(this.protocol);
        blockRewriter.registerBlockAction(ClientboundPackets1_20_3.BLOCK_ACTION);
        blockRewriter.registerBlockChange(ClientboundPackets1_20_3.BLOCK_CHANGE);
        blockRewriter.registerVarLongMultiBlockChange1_20(ClientboundPackets1_20_3.MULTI_BLOCK_CHANGE);
        blockRewriter.registerEffect(ClientboundPackets1_20_3.EFFECT, 1010, 2001);
        blockRewriter.registerChunkData1_19(
            ClientboundPackets1_20_3.CHUNK_DATA,
            ChunkType1_20_2::new,
            (user, blockEntity) -> this.updateBlockEntityTag(user, null, blockEntity.tag())
        );
        this.protocol.registerClientbound(ClientboundPackets1_20_3.BLOCK_ENTITY_DATA, wrapper -> {
            wrapper.passthrough(Type.POSITION1_14);
            wrapper.passthrough(Type.VAR_INT);
            CompoundTag tag = wrapper.read(Type.COMPOUND_TAG);
            if (tag != null) {
                this.updateBlockEntityTag(wrapper.user(), null, tag);
            } else {
                tag = new CompoundTag();
            }

            wrapper.write(Type.COMPOUND_TAG, tag);
        });
        this.registerSetCooldown(ClientboundPackets1_20_3.COOLDOWN);
        this.registerWindowItems1_17_1(ClientboundPackets1_20_3.WINDOW_ITEMS);
        this.registerSetSlot1_17_1(ClientboundPackets1_20_3.SET_SLOT);
        this.registerEntityEquipmentArray(ClientboundPackets1_20_3.ENTITY_EQUIPMENT);
        this.registerClickWindow1_17_1(ServerboundPackets1_20_5.CLICK_WINDOW);
        this.registerWindowPropertyEnchantmentHandler(ClientboundPackets1_20_3.WINDOW_PROPERTY);
        this.registerCreativeInvAction(ServerboundPackets1_20_5.CREATIVE_INVENTORY_ACTION);
        this.protocol.registerServerbound(ServerboundPackets1_20_5.CLICK_WINDOW_BUTTON, wrapper -> {
            byte containerId = wrapper.read(Type.BYTE);
            byte buttonId = wrapper.read(Type.BYTE);
            wrapper.write(Type.VAR_INT, Integer.valueOf(containerId));
            wrapper.write(Type.VAR_INT, Integer.valueOf(buttonId));
        });
        this.protocol.registerClientbound(ClientboundPackets1_20_3.ADVANCEMENTS, wrapper -> {
            wrapper.passthrough(Type.BOOLEAN);
            int size = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < size; i++) {
                wrapper.passthrough(Type.STRING);
                wrapper.passthrough(Type.OPTIONAL_STRING);
                if (wrapper.passthrough(Type.BOOLEAN)) {
                    wrapper.passthrough(Type.TAG);
                    wrapper.passthrough(Type.TAG);
                    Item item = this.handleNonNullItemToClient(wrapper.user(), wrapper.read(this.itemType()));
                    wrapper.write(this.mappedItemType(), item);
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
        this.protocol
            .registerClientbound(
                ClientboundPackets1_20_3.SPAWN_PARTICLE,
                wrapper -> {
                    int particleId = wrapper.read(Type.VAR_INT);
                    wrapper.passthrough(Type.BOOLEAN);
                    wrapper.passthrough(Type.DOUBLE);
                    wrapper.passthrough(Type.DOUBLE);
                    wrapper.passthrough(Type.DOUBLE);
                    wrapper.passthrough(Type.FLOAT);
                    wrapper.passthrough(Type.FLOAT);
                    wrapper.passthrough(Type.FLOAT);
                    float data = wrapper.passthrough(Type.FLOAT);
                    wrapper.passthrough(Type.INT);
                    ParticleMappings mappings = this.protocol.getMappingData().getParticleMappings();
                    int mappedId = mappings.getNewId(particleId);
                    Particle particle = new Particle(mappedId);
                    if (mappedId == mappings.mappedId("entity_effect")) {
                        particle.add(Type.INT, data != 0.0F ? ThreadLocalRandom.current().nextInt() : 0);
                    } else if (particleId == mappings.id("dust_color_transition")) {
                        for (int i = 0; i < 7; i++) {
                            particle.add(Type.FLOAT, wrapper.read(Type.FLOAT));
                        }

                        particle.add(Type.FLOAT, particle.<Float>removeArgument(3).getValue());
                    } else if (mappings.isBlockParticle(particleId)) {
                        int blockStateId = wrapper.read(Type.VAR_INT);
                        particle.add(Type.VAR_INT, this.protocol.getMappingData().getNewBlockStateId(blockStateId));
                    } else if (mappings.isItemParticle(particleId)) {
                        Item item = this.handleNonNullItemToClient(wrapper.user(), wrapper.read(Type.ITEM1_20_2));
                        particle.add(Types1_20_5.ITEM, item);
                    } else if (particleId == mappings.id("dust")) {
                        for (int i = 0; i < 4; i++) {
                            particle.add(Type.FLOAT, wrapper.read(Type.FLOAT));
                        }
                    } else if (particleId == mappings.id("vibration")) {
                        int sourceTypeId = wrapper.read(Type.VAR_INT);
                        particle.add(Type.VAR_INT, sourceTypeId);
                        if (sourceTypeId == 0) {
                            particle.add(Type.POSITION1_14, wrapper.read(Type.POSITION1_14));
                        } else if (sourceTypeId == 1) {
                            particle.add(Type.VAR_INT, wrapper.read(Type.VAR_INT));
                            particle.add(Type.FLOAT, wrapper.read(Type.FLOAT));
                        } else {
                            Via.getPlatform()
                                .getLogger()
                                .warning("Unknown vibration path position source type: " + sourceTypeId);
                        }

                        particle.add(Type.VAR_INT, wrapper.read(Type.VAR_INT));
                    } else if (particleId == mappings.id("sculk_charge")) {
                        particle.add(Type.FLOAT, wrapper.read(Type.FLOAT));
                    } else if (particleId == mappings.id("shriek")) {
                        particle.add(Type.VAR_INT, wrapper.read(Type.VAR_INT));
                    }

                    wrapper.write(Types1_20_5.PARTICLE, particle);
                }
            );
        this.protocol.registerClientbound(ClientboundPackets1_20_3.EXPLOSION, wrapper -> {
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
            this.protocol.getEntityRewriter().rewriteParticle(wrapper, Types1_20_3.PARTICLE, Types1_20_5.PARTICLE);
            this.protocol.getEntityRewriter().rewriteParticle(wrapper, Types1_20_3.PARTICLE, Types1_20_5.PARTICLE);
            wrapper.write(Type.VAR_INT, 0);
        });
        this.protocol.registerClientbound(ClientboundPackets1_20_3.TRADE_LIST, wrapper -> {
            wrapper.passthrough(Type.VAR_INT);
            int size = wrapper.passthrough(Type.VAR_INT);

            for (int i = 0; i < size; i++) {
                Item input = this.handleItemToClient(wrapper.user(), wrapper.read(Type.ITEM1_20_2));
                wrapper.write(Types1_20_5.ITEM_COST, input);
                Item output = this.handleNonNullItemToClient(wrapper.user(), wrapper.read(Type.ITEM1_20_2));
                wrapper.write(Types1_20_5.ITEM, output);
                Item secondInput = this.handleItemToClient(wrapper.user(), wrapper.read(Type.ITEM1_20_2));
                wrapper.write(Types1_20_5.OPTIONAL_ITEM_COST, secondInput);
                wrapper.passthrough(Type.BOOLEAN);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.INT);
                wrapper.passthrough(Type.FLOAT);
                wrapper.passthrough(Type.INT);
            }
        });
        RecipeRewriter1_20_3<ClientboundPacket1_20_3> recipeRewriter = new RecipeRewriter1_20_3<ClientboundPacket1_20_3>(
            this.protocol
        ) {
            @Override
            protected Item rewrite(UserConnection connection, @Nullable Item item) {
                item = super.rewrite(connection, item);
                return item != null && !item.isEmpty() ? item : new StructuredItem(1, 1);
            }
        };
        this.protocol
            .registerClientbound(
                ClientboundPackets1_20_3.DECLARE_RECIPES,
                wrapper -> {
                    int size = wrapper.passthrough(Type.VAR_INT);

                    for (int i = 0; i < size; i++) {
                        String type = wrapper.read(Type.STRING);
                        wrapper.passthrough(Type.STRING);
                        wrapper.write(
                            Type.VAR_INT, this.protocol.getMappingData().getRecipeSerializerMappings().mappedId(type)
                        );
                        recipeRewriter.handleRecipeType(wrapper, Key.stripMinecraftNamespace(type));
                    }
                }
            );
    }

    public Item handleNonNullItemToClient(UserConnection connection, @Nullable Item item) {
        item = this.handleItemToClient(connection, item);
        return item != null && !item.isEmpty() ? item : new StructuredItem(1, 1);
    }

    @Override
    public @Nullable Item handleItemToClient(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        CompoundTag tag = item.tag();
        if (tag != null) {
            tag.putBoolean(this.nbtTagName(), true);
        }

        Item structuredItem = this.toStructuredItem(connection, item);
        return super.handleItemToClient(connection, structuredItem);
    }

    @Override
    public @Nullable Item handleItemToServer(UserConnection connection, @Nullable Item item) {
        if (item == null) {
            return null;
        }

        super.handleItemToServer(connection, item);
        return this.toOldItem(item, DATA_CONVERTER);
    }

    public Item toOldItem(Item item, StructuredDataConverter dataConverter) {
        StructuredDataContainer data = item.structuredData();
        data.setIdLookup(this.protocol, true);
        StructuredData<CompoundTag> customData = data.getNonEmpty(StructuredDataKey.CUSTOM_DATA);
        CompoundTag tag = customData != null ? customData.value() : new CompoundTag();
        DataItem dataItem = new DataItem(item.identifier(), (byte)item.amount(), (short)0, tag);
        if (customData != null && tag.remove(this.nbtTagName()) != null) {
            return dataItem;
        }

        for (StructuredData<?> structuredData : data.data().values()) {
            dataConverter.writeToTag(structuredData, tag);
        }

        return dataItem;
    }

    public Item toStructuredItem(UserConnection connection, Item old) {
        CompoundTag tag = old.tag();
        StructuredItem item = new StructuredItem(old.identifier(), (byte)old.amount(), new StructuredDataContainer());
        StructuredDataContainer data = item.structuredData();
        data.setIdLookup(this.protocol, true);
        if (tag == null) {
            return item;
        }

        int hideFlagsValue = tag.getInt("HideFlags");
        if ((hideFlagsValue & 32) != 0) {
            data.set(StructuredDataKey.HIDE_ADDITIONAL_TOOLTIP);
        }

        this.updateDisplay(connection, data, tag.getCompoundTag("display"), hideFlagsValue);
        NumberTag damage = tag.getNumberTag("Damage");
        if (damage != null && damage.asInt() != 0) {
            data.set(StructuredDataKey.DAMAGE, damage.asInt());
        }

        NumberTag repairCost = tag.getNumberTag("RepairCost");
        if (repairCost != null && repairCost.asInt() != 0) {
            data.set(StructuredDataKey.REPAIR_COST, repairCost.asInt());
        }

        NumberTag customModelData = tag.getNumberTag("CustomModelData");
        if (customModelData != null) {
            data.set(StructuredDataKey.CUSTOM_MODEL_DATA, customModelData.asInt());
        }

        CompoundTag blockState = tag.getCompoundTag("BlockStateTag");
        if (blockState != null) {
            this.updateBlockState(data, blockState);
        }

        CompoundTag entityTag = tag.getCompoundTag("EntityTag");
        if (entityTag != null) {
            data.set(StructuredDataKey.ENTITY_DATA, entityTag.copy());
        }

        CompoundTag blockEntityTag = tag.getCompoundTag("BlockEntityTag");
        if (blockEntityTag != null) {
            CompoundTag clonedTag = blockEntityTag.copy();
            this.updateBlockEntityTag(connection, data, clonedTag);
            item.structuredData().set(StructuredDataKey.BLOCK_ENTITY_DATA, clonedTag);
        }

        CompoundTag debugProperty = tag.getCompoundTag("DebugProperty");
        if (debugProperty != null) {
            data.set(StructuredDataKey.DEBUG_STICK_STATE, debugProperty.copy());
        }

        NumberTag unbreakable = tag.getNumberTag("Unbreakable");
        if (unbreakable != null && unbreakable.asBoolean()) {
            data.set(StructuredDataKey.UNBREAKABLE, new Unbreakable((hideFlagsValue & 4) == 0));
        }

        CompoundTag trimTag = tag.getCompoundTag("Trim");
        if (trimTag != null) {
            this.updateArmorTrim(data, trimTag, (hideFlagsValue & 128) == 0);
        }

        CompoundTag explosionTag = tag.getCompoundTag("Explosion");
        if (explosionTag != null) {
            data.set(StructuredDataKey.FIREWORK_EXPLOSION, this.readExplosion(explosionTag));
        }

        ListTag<StringTag> recipesTag = tag.getListTag("Recipes", StringTag.class);
        if (recipesTag != null) {
            data.set(StructuredDataKey.RECIPES, recipesTag);
        }

        CompoundTag lodestonePosTag = tag.getCompoundTag("LodestonePos");
        String lodestoneDimension = tag.getString("LodestoneDimension");
        if (lodestonePosTag != null && lodestoneDimension != null) {
            this.updateLodestoneTracker(tag, lodestonePosTag, lodestoneDimension, data);
        }

        ListTag<CompoundTag> effectsTag = tag.getListTag("effects", CompoundTag.class);
        if (effectsTag != null) {
            this.updateEffects(effectsTag, data);
        }

        String instrument = tag.getString("instrument");
        if (instrument != null) {
            int id = Instruments1_20_3.keyToId(instrument);
            if (id != -1) {
                data.set(StructuredDataKey.INSTRUMENT, Holder.of(id));
            }
        }

        ListTag<CompoundTag> attributeModifiersTag = tag.getListTag("AttributeModifiers", CompoundTag.class);
        boolean showAttributes = (hideFlagsValue & 2) == 0;
        if (attributeModifiersTag != null) {
            this.updateAttributes(data, attributeModifiersTag, showAttributes);
        } else if (!showAttributes) {
            data.set(StructuredDataKey.ATTRIBUTE_MODIFIERS, new AttributeModifiers(new AttributeModifier[0], false));
        }

        CompoundTag fireworksTag = tag.getCompoundTag("Fireworks");
        if (fireworksTag != null) {
            ListTag<CompoundTag> explosionsTag = fireworksTag.getListTag("Explosions", CompoundTag.class);
            if (explosionsTag != null) {
                this.updateFireworks(data, fireworksTag, explosionsTag);
            }
        }

        if (old.identifier() == 1085) {
            this.updateWritableBookPages(data, tag);
        } else if (old.identifier() == 1086) {
            this.updateWrittenBookPages(connection, data, tag);
        }

        this.updatePotionTags(data, tag);
        this.updateMobTags(data, tag);
        this.updateItemList(connection, data, tag, "ChargedProjectiles", StructuredDataKey.CHARGED_PROJECTILES, false);
        if (old.identifier() == 927) {
            this.updateItemList(connection, data, tag, "Items", StructuredDataKey.BUNDLE_CONTENTS, false);
        }

        this.updateEnchantments(data, tag, "Enchantments", StructuredDataKey.ENCHANTMENTS, (hideFlagsValue & 1) == 0);
        this.updateEnchantments(
            data, tag, "StoredEnchantments", StructuredDataKey.STORED_ENCHANTMENTS, (hideFlagsValue & 32) == 0
        );
        NumberTag mapId = tag.getNumberTag("map");
        if (mapId != null) {
            data.set(StructuredDataKey.MAP_ID, mapId.asInt());
        }

        ListTag<CompoundTag> decorationsTag = tag.getListTag("Decorations", CompoundTag.class);
        if (decorationsTag != null) {
            this.updateMapDecorations(data, decorationsTag);
        }

        this.updateProfile(data, tag.get("SkullOwner"));
        CompoundTag customCreativeLock = tag.getCompoundTag("CustomCreativeLock");
        if (customCreativeLock != null) {
            data.set(StructuredDataKey.CREATIVE_SLOT_LOCK);
        }

        ListTag<StringTag> canPlaceOnTag = tag.getListTag("CanPlaceOn", StringTag.class);
        if (canPlaceOnTag != null) {
            data.set(
                StructuredDataKey.CAN_PLACE_ON, this.updateBlockPredicates(canPlaceOnTag, (hideFlagsValue & 16) == 0)
            );
        }

        ListTag<StringTag> canDestroyTag = tag.getListTag("CanDestroy", StringTag.class);
        if (canDestroyTag != null) {
            data.set(StructuredDataKey.CAN_BREAK, this.updateBlockPredicates(canDestroyTag, (hideFlagsValue & 8) == 0));
        }

        IntTag mapScaleDirectionTag = tag.getIntTag("map_scale_direction");
        if (mapScaleDirectionTag != null) {
            data.set(StructuredDataKey.MAP_POST_PROCESSING, 1);
        } else {
            NumberTag mapToLockTag = tag.getNumberTag("map_to_lock");
            if (mapToLockTag != null) {
                data.set(StructuredDataKey.MAP_POST_PROCESSING, 0);
            }
        }

        CompoundTag backupTag = StructuredDataConverter.removeBackupTag(tag);
        if (backupTag != null) {
            this.restoreFromBackupTag(backupTag, data);
        }

        data.set(StructuredDataKey.CUSTOM_DATA, tag);
        return item;
    }

    private int unmappedItemId(String name) {
        return this.protocol.getMappingData().getFullItemMappings().id(name);
    }

    private int toMappedItemId(String name) {
        int unmappedId = this.unmappedItemId(name);
        return unmappedId != -1 ? this.protocol.getMappingData().getNewItemId(unmappedId) : -1;
    }

    private void restoreFromBackupTag(CompoundTag backupTag, StructuredDataContainer data) {
        CompoundTag instrument = backupTag.getCompoundTag("instrument");
        if (instrument != null) {
            this.restoreInstrumentFromBackup(instrument, data);
        }

        IntArrayTag potDecorationsTag = backupTag.getIntArrayTag("pot_decorations");
        if (potDecorationsTag != null && potDecorationsTag.getValue().length == 4) {
            data.set(StructuredDataKey.POT_DECORATIONS, new PotDecorations(potDecorationsTag.getValue()));
        }

        ByteTag enchantmentGlintOverride = backupTag.getByteTag("enchantment_glint_override");
        if (enchantmentGlintOverride != null) {
            data.set(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE, enchantmentGlintOverride.asBoolean());
        }

        if (backupTag.contains("hide_tooltip")) {
            data.set(StructuredDataKey.HIDE_TOOLTIP);
        }

        Tag intangibleProjectile = backupTag.get("intangible_projectile");
        if (intangibleProjectile != null) {
            data.set(StructuredDataKey.INTANGIBLE_PROJECTILE, intangibleProjectile);
        }

        IntTag maxStackSize = backupTag.getIntTag("max_stack_size");
        if (maxStackSize != null) {
            data.set(StructuredDataKey.MAX_STACK_SIZE, MathUtil.clamp(maxStackSize.asInt(), 1, 99));
        }

        IntTag maxDamage = backupTag.getIntTag("max_damage");
        if (maxDamage != null) {
            data.set(StructuredDataKey.MAX_DAMAGE, Math.max(maxDamage.asInt(), 1));
        }

        IntTag rarity = backupTag.getIntTag("rarity");
        if (rarity != null) {
            data.set(StructuredDataKey.RARITY, rarity.asInt());
        }

        CompoundTag food = backupTag.getCompoundTag("food");
        if (food != null) {
            this.restoreFoodFromBackup(food, data);
        }

        if (backupTag.contains("fire_resistant")) {
            data.set(StructuredDataKey.FIRE_RESISTANT);
        }

        CompoundTag tool = backupTag.getCompoundTag("tool");
        if (tool != null) {
            this.restoreToolFromBackup(tool, data);
        }

        IntTag ominousBottleAmplifier = backupTag.getIntTag("ominous_bottle_amplifier");
        if (ominousBottleAmplifier != null) {
            data.set(StructuredDataKey.OMINOUS_BOTTLE_AMPLIFIER, MathUtil.clamp(ominousBottleAmplifier.asInt(), 0, 4));
        }

        ListTag<CompoundTag> bannerPatterns = backupTag.getListTag("banner_patterns", CompoundTag.class);
        if (bannerPatterns != null) {
            this.restoreBannerPatternsFromBackup(bannerPatterns, data);
        }
    }

    private void restoreInstrumentFromBackup(CompoundTag instrument, StructuredDataContainer data) {
        int useDuration = instrument.getInt("use_duration");
        float range = instrument.getFloat("range");
        Tag soundEventTag = instrument.get("sound_event");
        Holder<SoundEvent> soundEvent;
        if (soundEventTag instanceof IntTag) {
            soundEvent = Holder.of(((IntTag)soundEventTag).asInt());
        } else {
            if (!(soundEventTag instanceof CompoundTag)) {
                return;
            }

            CompoundTag soundEventCompound = (CompoundTag)soundEventTag;
            StringTag identifier = soundEventCompound.getStringTag("identifier");
            if (identifier == null) {
                return;
            }

            soundEvent = Holder.of(
                new SoundEvent(
                    identifier.getValue(),
                    soundEventCompound.contains("fixed_range") ? soundEventCompound.getFloat("fixed_range") : null
                )
            );
        }

        data.set(StructuredDataKey.INSTRUMENT, Holder.of(new Instrument(soundEvent, useDuration, range)));
    }

    private void restoreFoodFromBackup(CompoundTag food, StructuredDataContainer data) {
        int nutrition = food.getInt("nutrition");
        float saturation = food.getFloat("saturation");
        boolean canAlwaysEat = food.getBoolean("can_always_eat");
        float eatSeconds = food.getFloat("eat_seconds");
        ListTag<CompoundTag> possibleEffectsTag = food.getListTag("possible_effects", CompoundTag.class);
        if (possibleEffectsTag != null) {
            List<FoodEffect> possibleEffects = new ArrayList<>();

            for (CompoundTag effect : possibleEffectsTag) {
                CompoundTag potionEffectTag = effect.getCompoundTag("effect");
                if (potionEffectTag != null) {
                    possibleEffects.add(
                        new FoodEffect(
                            new PotionEffect(
                                potionEffectTag.getInt("effect"), this.readPotionEffectData(potionEffectTag)
                            ),
                            effect.getFloat("probability")
                        )
                    );
                }
            }

            data.set(
                StructuredDataKey.FOOD,
                new FoodProperties(
                    nutrition, saturation, canAlwaysEat, eatSeconds, possibleEffects.toArray(new FoodEffect[0])
                )
            );
        }
    }

    private void restoreToolFromBackup(CompoundTag tool, StructuredDataContainer data) {
        ListTag<CompoundTag> rulesTag = tool.getListTag("rules", CompoundTag.class);
        if (rulesTag != null) {
            List<ToolRule> rules = new ArrayList<>();

            for (CompoundTag tag : rulesTag) {
                HolderSet blocks = null;
                if (tag.get("blocks") instanceof StringTag) {
                    blocks = HolderSet.of(tag.getString("blocks"));
                } else {
                    IntArrayTag blockIds = tag.getIntArrayTag("blocks");
                    if (blockIds != null) {
                        blocks = HolderSet.of(blockIds.getValue());
                    }
                }

                if (blocks != null) {
                    rules.add(
                        new ToolRule(
                            blocks,
                            tag.contains("speed") ? tag.getFloat("speed") : null,
                            tag.contains("correct_for_drops") ? tag.getBoolean("correct_for_drops") : null
                        )
                    );
                }
            }

            data.set(
                StructuredDataKey.TOOL,
                new ToolProperties(
                    rules.toArray(new ToolRule[0]),
                    tool.getFloat("default_mining_speed"),
                    tool.getInt("damage_per_block")
                )
            );
        }
    }

    private void restoreBannerPatternsFromBackup(ListTag<CompoundTag> bannerPatterns, StructuredDataContainer data) {
        List<BannerPatternLayer> patternLayer = new ArrayList<>();

        for (CompoundTag tag : bannerPatterns) {
            CompoundTag patternTag = tag.getCompoundTag("pattern");
            if (patternTag != null) {
                String assetId = patternTag.getString("asset_id");
                String translationKey = patternTag.getString("translation_key");
                int dyeColor = tag.getInt("dye_color");
                patternLayer.add(
                    new BannerPatternLayer(Holder.of(new BannerPattern(assetId, translationKey)), dyeColor)
                );
            }
        }

        data.set(StructuredDataKey.BANNER_PATTERNS, patternLayer.toArray(new BannerPatternLayer[0]));
    }

    private AdventureModePredicate updateBlockPredicates(ListTag<StringTag> tag, boolean showInTooltip) {
        BlockPredicate[] predicates = tag.stream()
            .map(StringTag::getValue)
            .map(this::deserializeBlockPredicate)
            .filter(Objects::nonNull)
            .toArray(BlockPredicate[]::new);
        return new AdventureModePredicate(predicates, showInTooltip);
    }

    private @Nullable BlockPredicate deserializeBlockPredicate(String rawPredicate) {
        int propertiesStartIndex = rawPredicate.indexOf(91);
        int tagStartIndex = rawPredicate.indexOf(123);
        int idLength = rawPredicate.length();
        if (propertiesStartIndex != -1) {
            idLength = propertiesStartIndex;
        }

        if (tagStartIndex != -1) {
            idLength = Math.min(propertiesStartIndex, tagStartIndex);
        }

        String identifier = rawPredicate.substring(0, idLength);
        HolderSet holders;
        if (!identifier.startsWith("#")) {
            int id = Protocol1_20_5To1_20_3.MAPPINGS.blockId(identifier);
            if (id == -1) {
                return null;
            }

            holders = HolderSet.of(new int[]{id});
        } else {
            holders = HolderSet.of(identifier.substring(1));
        }

        int propertiesEndIndex = rawPredicate.indexOf(93);
        List<StatePropertyMatcher> propertyMatchers = new ArrayList<>();
        if (propertiesStartIndex != -1 && propertiesEndIndex != -1) {
            for (String property : rawPredicate.substring(propertiesStartIndex + 1, propertiesEndIndex).split(",")) {
                int propertySplitIndex = property.indexOf(61);
                if (propertySplitIndex != -1) {
                    String propertyId = property.substring(0, propertySplitIndex).trim();
                    String propertyValue = property.substring(propertySplitIndex + 1).trim();
                    propertyMatchers.add(new StatePropertyMatcher(propertyId, Either.left(propertyValue)));
                }
            }
        }

        int tagEndIndex = rawPredicate.indexOf(125);
        CompoundTag tag = null;
        if (tagStartIndex != -1 && tagEndIndex != -1) {
            try {
                tag = SNBT.deserializeCompoundTag(rawPredicate.substring(tagStartIndex, tagEndIndex + 1));
            } catch (Exception e) {
                if (Via.getManager().isDebug()) {
                    Via.getPlatform()
                        .getLogger()
                        .log(
                            Level.SEVERE,
                            "Failed to parse block predicate tag: "
                                + rawPredicate.substring(tagStartIndex, tagEndIndex + 1),
                            e
                        );
                }
            }
        }

        return new BlockPredicate(
            holders, propertyMatchers.isEmpty() ? null : propertyMatchers.toArray(EMPTY_PROPERTY_MATCHERS), tag
        );
    }

    private void updateAttributes(
        StructuredDataContainer data, ListTag<CompoundTag> attributeModifiersTag, boolean showInTooltip
    ) {
        List<AttributeModifier> modifiers = new ArrayList<>();

        for (int i = 0; i < attributeModifiersTag.size(); i++) {
            CompoundTag modifierTag = attributeModifiersTag.get(i);
            String attributeName = modifierTag.getString("AttributeName");
            String name = modifierTag.getString("Name");
            NumberTag amountTag = modifierTag.getNumberTag("Amount");
            IntArrayTag uuidTag = modifierTag.getIntArrayTag("UUID");
            String slotType = modifierTag.getString("Slot", "any");
            if (name != null && attributeName != null && amountTag != null && uuidTag != null) {
                int slotTypeId = EquipmentSlots1_20_5.keyToId(slotType);
                if (slotTypeId != -1) {
                    int operationId = modifierTag.getInt("Operation");
                    if (operationId >= 0 && operationId <= 2) {
                        int attributeId = Attributes1_20_5.keyToId(attributeName);
                        if (attributeId != -1) {
                            modifiers.add(
                                new AttributeModifier(
                                    attributeId,
                                    new ModifierData(
                                        UUIDUtil.fromIntArray(uuidTag.getValue()),
                                        name,
                                        amountTag.asDouble(),
                                        operationId
                                    ),
                                    slotTypeId
                                )
                            );
                        }
                    }
                }
            }
        }

        data.set(
            StructuredDataKey.ATTRIBUTE_MODIFIERS,
            new AttributeModifiers(modifiers.toArray(new AttributeModifier[0]), showInTooltip)
        );
    }

    private PotionEffectData readPotionEffectData(CompoundTag tag) {
        byte amplifier = tag.getByte("amplifier");
        int duration = tag.getInt("duration");
        boolean ambient = tag.getBoolean("ambient");
        boolean showParticles = tag.getBoolean("show_particles");
        boolean showIcon = tag.getBoolean("show_icon");
        PotionEffectData hiddenEffect = null;
        CompoundTag hiddenEffectTag = tag.getCompoundTag("hidden_effect");
        if (hiddenEffectTag != null) {
            hiddenEffect = this.readPotionEffectData(hiddenEffectTag);
        }

        return new PotionEffectData(amplifier, duration, ambient, showParticles, showIcon, hiddenEffect);
    }

    private void updatePotionTags(StructuredDataContainer data, CompoundTag tag) {
        String potion = tag.getString("Potion");
        Integer potionId = null;
        if (potion != null) {
            int id = Potions1_20_5.keyToId(potion);
            if (id != -1) {
                potionId = id;
            }
        }

        NumberTag customPotionColorTag = tag.getNumberTag("CustomPotionColor");
        ListTag<CompoundTag> customPotionEffectsTag = tag.getListTag("custom_potion_effects", CompoundTag.class);
        PotionEffect[] potionEffects = null;
        if (customPotionEffectsTag != null) {
            potionEffects = customPotionEffectsTag.stream().map(effectTag -> {
                String identifier = effectTag.getString("id");
                if (identifier == null) {
                    return null;
                }

                int id = PotionEffects1_20_5.keyToId(identifier);
                return id == -1 ? null : new PotionEffect(id, this.readPotionEffectData(effectTag));
            }).filter(Objects::nonNull).toArray(PotionEffect[]::new);
        }

        if (potionId != null || customPotionColorTag != null || potionEffects != null) {
            data.set(
                StructuredDataKey.POTION_CONTENTS,
                new PotionContents(
                    potionId,
                    customPotionColorTag != null ? customPotionColorTag.asInt() : null,
                    potionEffects != null ? potionEffects : new PotionEffect[0]
                )
            );
        }
    }

    private void updateArmorTrim(StructuredDataContainer data, CompoundTag trimTag, boolean showInTooltip) {
        Tag materialTag = trimTag.get("material");
        Holder<ArmorTrimMaterial> materialHolder;
        if (materialTag instanceof StringTag) {
            int id = TrimMaterials1_20_3.keyToId(((StringTag)materialTag).getValue());
            if (id == -1) {
                return;
            }

            materialHolder = Holder.of(id);
        } else {
            if (!(materialTag instanceof CompoundTag)) {
                return;
            }

            CompoundTag materialCompoundTag = (CompoundTag)materialTag;
            StringTag assetNameTag = materialCompoundTag.getStringTag("asset_name");
            StringTag ingredientTag = materialCompoundTag.getStringTag("ingredient");
            if (assetNameTag == null || ingredientTag == null) {
                return;
            }

            int ingredientId = StructuredDataConverter.removeItemBackupTag(
                materialCompoundTag, this.toMappedItemId(ingredientTag.getValue())
            );
            if (ingredientId == -1) {
                return;
            }

            NumberTag itemModelIndexTag = materialCompoundTag.getNumberTag("item_model_index");
            CompoundTag overrideArmorMaterialsTag = materialCompoundTag.get("override_armor_materials");
            Tag descriptionTag = materialCompoundTag.get("description");
            Int2ObjectMap<String> overrideArmorMaterials = new Int2ObjectOpenHashMap<>();
            if (overrideArmorMaterialsTag != null) {
                for (Entry<String, Tag> entry : overrideArmorMaterialsTag.entrySet()) {
                    if (entry.getValue() instanceof StringTag) {
                        try {
                            int id = Integer.parseInt(entry.getKey());
                            overrideArmorMaterials.put(id, ((StringTag)entry.getValue()).getValue());
                        } catch (NumberFormatException var17) {
                        }
                    }
                }
            }

            materialHolder = Holder.of(
                new ArmorTrimMaterial(
                    assetNameTag.getValue(),
                    ingredientId,
                    itemModelIndexTag != null ? itemModelIndexTag.asFloat() : 0.0F,
                    overrideArmorMaterials,
                    descriptionTag
                )
            );
        }

        Tag patternTag = trimTag.get("pattern");
        Holder<ArmorTrimPattern> patternHolder;
        if (patternTag instanceof StringTag) {
            int id = TrimPatterns1_20_3.keyToId(((StringTag)patternTag).getValue());
            if (id == -1) {
                return;
            }

            patternHolder = Holder.of(id);
        } else {
            if (!(patternTag instanceof CompoundTag)) {
                return;
            }

            CompoundTag patternCompoundTag = (CompoundTag)patternTag;
            String assetId = patternCompoundTag.getString("assetId");
            String templateItem = patternCompoundTag.getString("templateItem");
            if (assetId == null || templateItem == null) {
                return;
            }

            int templateItemId = StructuredDataConverter.removeItemBackupTag(
                patternCompoundTag, this.toMappedItemId(templateItem)
            );
            if (templateItemId == -1) {
                return;
            }

            Tag descriptionTag = patternCompoundTag.get("description");
            boolean decal = patternCompoundTag.getBoolean("decal");
            patternHolder = Holder.of(new ArmorTrimPattern(assetId, templateItemId, descriptionTag, decal));
        }

        data.set(StructuredDataKey.TRIM, new ArmorTrim(materialHolder, patternHolder, showInTooltip));
    }

    private void updateMobTags(StructuredDataContainer data, CompoundTag tag) {
        CompoundTag bucketEntityData = new CompoundTag();

        for (String mobTagKey : MOB_TAGS) {
            Tag mobTag = tag.get(mobTagKey);
            if (mobTag != null) {
                bucketEntityData.put(mobTagKey, mobTag);
            }
        }

        if (!bucketEntityData.isEmpty()) {
            data.set(StructuredDataKey.BUCKET_ENTITY_DATA, bucketEntityData);
        }
    }

    private void updateBlockState(StructuredDataContainer data, CompoundTag blockState) {
        Map<String, String> properties = new HashMap<>();

        for (Entry<String, Tag> entry : blockState.entrySet()) {
            Tag value = entry.getValue();
            if (value instanceof StringTag) {
                properties.put(entry.getKey(), ((StringTag)value).getValue());
            } else if (value instanceof IntTag) {
                properties.put(entry.getKey(), Integer.toString(((NumberTag)value).asInt()));
            }
        }

        data.set(StructuredDataKey.BLOCK_STATE, new BlockStateProperties(properties));
    }

    private void updateFireworks(
        StructuredDataContainer data, CompoundTag fireworksTag, ListTag<CompoundTag> explosionsTag
    ) {
        int flightDuration = fireworksTag.getInt("Flight");
        Fireworks fireworks = new Fireworks(
            flightDuration,
            explosionsTag.stream().limit(256L).map(this::readExplosion).toArray(FireworkExplosion[]::new)
        );
        data.set(StructuredDataKey.FIREWORKS, fireworks);
    }

    private void updateEffects(ListTag<CompoundTag> effects, StructuredDataContainer data) {
        SuspiciousStewEffect[] suspiciousStewEffects = new SuspiciousStewEffect[effects.size()];

        for (int i = 0; i < effects.size(); i++) {
            CompoundTag effect = effects.get(i);
            String effectIdString = effect.getString("id", "luck");
            int duration = effect.getInt("duration");
            int effectId = PotionEffects1_20_5.keyToId(effectIdString);
            if (effectId != -1) {
                SuspiciousStewEffect stewEffect = new SuspiciousStewEffect(effectId, duration);
                suspiciousStewEffects[i] = stewEffect;
            }
        }

        data.set(StructuredDataKey.SUSPICIOUS_STEW_EFFECTS, suspiciousStewEffects);
    }

    private void updateLodestoneTracker(
        CompoundTag tag, CompoundTag lodestonePosTag, String lodestoneDimensionTag, StructuredDataContainer data
    ) {
        boolean tracked = tag.getBoolean("LodestoneTracked");
        int x = lodestonePosTag.getInt("X");
        int y = lodestonePosTag.getInt("Y");
        int z = lodestonePosTag.getInt("Z");
        GlobalPosition position = new GlobalPosition(lodestoneDimensionTag, x, y, z);
        data.set(StructuredDataKey.LODESTONE_TRACKER, new LodestoneTracker(position, tracked));
    }

    private FireworkExplosion readExplosion(CompoundTag tag) {
        int shape = tag.getInt("Type");
        IntArrayTag colors = tag.getIntArrayTag("Colors");
        IntArrayTag fadeColors = tag.getIntArrayTag("FadeColors");
        boolean trail = tag.getBoolean("Trail");
        boolean flicker = tag.getBoolean("Flicker");
        return new FireworkExplosion(
            shape,
            colors != null ? colors.getValue() : new int[0],
            fadeColors != null ? fadeColors.getValue() : new int[0],
            trail,
            flicker
        );
    }

    private void updateWritableBookPages(StructuredDataContainer data, CompoundTag tag) {
        ListTag<StringTag> pagesTag = tag.getListTag("pages", StringTag.class);
        CompoundTag filteredPagesTag = tag.getCompoundTag("filtered_pages");
        if (pagesTag != null) {
            List<FilterableString> pages = new ArrayList<>();

            for (int i = 0; i < pagesTag.size(); i++) {
                StringTag page = pagesTag.get(i);
                String filtered = null;
                if (filteredPagesTag != null) {
                    StringTag filteredPage = filteredPagesTag.getStringTag(String.valueOf(i));
                    if (filteredPage != null) {
                        filtered = this.limit(filteredPage.getValue(), 1024);
                    }
                }

                pages.add(new FilterableString(this.limit(page.getValue(), 1024), filtered));
                if (pages.size() == 100) {
                    break;
                }
            }

            data.set(StructuredDataKey.WRITABLE_BOOK_CONTENT, pages.toArray(new FilterableString[0]));
        }
    }

    private void updateWrittenBookPages(UserConnection connection, StructuredDataContainer data, CompoundTag tag) {
        ListTag<StringTag> pagesTag = tag.getListTag("pages", StringTag.class);
        CompoundTag filteredPagesTag = tag.getCompoundTag("filtered_pages");
        if (pagesTag != null) {
            List<FilterableComponent> pages = new ArrayList<>();

            for (int i = 0; i < pagesTag.size(); i++) {
                StringTag page = pagesTag.get(i);
                Tag filtered = null;
                if (filteredPagesTag != null) {
                    StringTag filteredPage = filteredPagesTag.getStringTag(String.valueOf(i));
                    if (filteredPage != null) {
                        try {
                            filtered = this.jsonToTag(connection, filteredPage);
                        } catch (Exception e) {
                            continue;
                        }
                    }
                }

                Tag parsedPage;
                try {
                    parsedPage = this.jsonToTag(connection, page);
                } catch (Exception e) {
                    continue;
                }

                pages.add(new FilterableComponent(parsedPage, filtered));
            }

            String title = tag.getString("title", "");
            String filteredTitle = tag.getString("filtered_title");
            String author = tag.getString("author", "");
            int generation = tag.getInt("generation");
            boolean resolved = tag.getBoolean("resolved");
            WrittenBook writtenBook = new WrittenBook(
                new FilterableString(this.limit(title, 32), this.limit(filteredTitle, 32)),
                author,
                MathUtil.clamp(generation, 0, 3),
                pages.toArray(new FilterableComponent[0]),
                resolved
            );
            data.set(StructuredDataKey.WRITTEN_BOOK_CONTENT, writtenBook);
        }
    }

    private Tag jsonToTag(UserConnection connection, StringTag stringTag) {
        Tag tag = ComponentUtil.jsonStringToTag(
            stringTag.getValue(), SerializerVersion.V1_20_3, SerializerVersion.V1_20_3
        );
        this.protocol.getComponentRewriter().processTag(connection, tag);
        return tag;
    }

    private void updateItemList(
        UserConnection connection,
        StructuredDataContainer data,
        CompoundTag tag,
        String key,
        StructuredDataKey<Item[]> dataKey,
        boolean allowEmpty
    ) {
        ListTag<CompoundTag> itemsTag = tag.getListTag(key, CompoundTag.class);
        if (itemsTag != null) {
            Item[] items = itemsTag.stream()
                .limit(256L)
                .map(item -> this.itemFromTag(connection, item))
                .filter(Objects::nonNull)
                .filter(item -> allowEmpty || !item.isEmpty())
                .toArray(Item[]::new);
            data.set(dataKey, items);
        }
    }

    private @Nullable Item itemFromTag(UserConnection connection, CompoundTag item) {
        String id = item.getString("id");
        if (id == null) {
            return null;
        }

        int itemId = StructuredDataConverter.removeItemBackupTag(item, this.unmappedItemId(id));
        if (itemId == -1) {
            return null;
        }

        byte count = item.getByte("Count", (byte)1);
        CompoundTag tag = item.getCompoundTag("tag");
        return this.handleItemToClient(connection, new DataItem(itemId, count, (short)0, tag));
    }

    private void updateEnchantments(
        StructuredDataContainer data, CompoundTag tag, String key, StructuredDataKey<Enchantments> newKey, boolean show
    ) {
        ListTag<CompoundTag> enchantmentsTag = tag.getListTag(key, CompoundTag.class);
        if (enchantmentsTag != null) {
            Enchantments enchantments = new Enchantments(new Int2IntOpenHashMap(), show);

            for (CompoundTag enchantment : enchantmentsTag) {
                String id = enchantment.getString("id");
                NumberTag lvl = enchantment.getNumberTag("lvl");
                if (id != null && lvl != null) {
                    if (Key.stripMinecraftNamespace(id).equals("sweeping")) {
                        id = "minecraft:sweeping_edge";
                    }

                    int intId = Enchantments1_20_5.keyToId(id);
                    if (intId != -1) {
                        enchantments.enchantments().put(intId, MathUtil.clamp(lvl.asInt(), 0, 255));
                    }
                }
            }

            data.set(newKey, enchantments);
            if (!enchantmentsTag.isEmpty() && enchantments.size() == 0) {
                data.set(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE, true);
            }
        }
    }

    private void updateProfile(StructuredDataContainer data, Tag skullOwnerTag) {
        if (skullOwnerTag instanceof StringTag) {
            String name = ((StringTag)skullOwnerTag).getValue();
            if (this.isValidName(name)) {
                data.set(StructuredDataKey.PROFILE, new GameProfile(name, null, EMPTY_PROPERTIES));
            }
        } else if (skullOwnerTag instanceof CompoundTag) {
            CompoundTag skullOwner = (CompoundTag)skullOwnerTag;
            String name = skullOwner.getString("Name", "");
            if (!this.isValidName(name)) {
                name = null;
            }

            IntArrayTag idTag = skullOwner.getIntArrayTag("Id");
            UUID uuid = null;
            if (idTag != null) {
                uuid = UUIDUtil.fromIntArray(idTag.getValue());
            }

            List<GameProfile.Property> properties = new ArrayList<>(1);
            CompoundTag propertiesTag = skullOwner.getCompoundTag("Properties");
            if (propertiesTag != null) {
                this.updateProperties(propertiesTag, properties);
            }

            data.set(StructuredDataKey.PROFILE, new GameProfile(name, uuid, properties.toArray(EMPTY_PROPERTIES)));
        }
    }

    private @Nullable String limit(@Nullable String s, int length) {
        if (s == null) {
            return null;
        } else {
            return s.length() > length ? s.substring(0, length) : s;
        }
    }

    private void updateBees(StructuredDataContainer data, ListTag<CompoundTag> beesTag) {
        Bee[] bees = beesTag.stream().map(bee -> {
            CompoundTag entityData = bee.getCompoundTag("EntityData");
            if (entityData == null) {
                return null;
            }

            int ticksInHive = bee.getInt("TicksInHive");
            int minOccupationTicks = bee.getInt("MinOccupationTicks");
            return new Bee(entityData, ticksInHive, minOccupationTicks);
        }).filter(Objects::nonNull).toArray(Bee[]::new);
        data.set(StructuredDataKey.BEES, bees);
    }

    private void updateProperties(CompoundTag propertiesTag, List<GameProfile.Property> properties) {
        for (Entry<String, Tag> entry : propertiesTag.entrySet()) {
            if (entry.getValue() instanceof ListTag) {
                for (Tag propertyTag : (ListTag)entry.getValue()) {
                    if (propertyTag instanceof CompoundTag) {
                        CompoundTag compoundTag = (CompoundTag)propertyTag;
                        String value = compoundTag.getString("Value", "");
                        String signature = compoundTag.getString("Signature");
                        properties.add(
                            new GameProfile.Property(this.limit(entry.getKey(), 64), value, this.limit(signature, 1024))
                        );
                        if (properties.size() == 16) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private void updateMapDecorations(StructuredDataContainer data, ListTag<CompoundTag> decorationsTag) {
        CompoundTag updatedDecorationsTag = new CompoundTag();

        for (CompoundTag decorationTag : decorationsTag) {
            String id = decorationTag.getString("id", "");
            int type = decorationTag.getInt("type");
            double x = decorationTag.getDouble("x");
            double z = decorationTag.getDouble("z");
            float rotation = decorationTag.getFloat("rot");
            CompoundTag updatedDecorationTag = new CompoundTag();
            updatedDecorationTag.putString("type", MapDecorations1_20_5.idToKey(type));
            updatedDecorationTag.putDouble("x", x);
            updatedDecorationTag.putDouble("z", z);
            updatedDecorationTag.putFloat("rotation", rotation);
            updatedDecorationsTag.put(id, updatedDecorationTag);
        }

        data.set(StructuredDataKey.MAP_DECORATIONS, updatedDecorationsTag);
    }

    private void updateDisplay(
        UserConnection connection, StructuredDataContainer data, CompoundTag displayTag, int hideFlags
    ) {
        if (displayTag != null) {
            NumberTag mapColorTag = displayTag.getNumberTag("MapColor");
            if (mapColorTag != null) {
                data.set(StructuredDataKey.MAP_COLOR, mapColorTag.asInt());
            }

            StringTag nameTag = displayTag.getStringTag("Name");
            if (nameTag != null) {
                try {
                    Tag convertedName = this.jsonToTag(connection, nameTag);
                    data.set(StructuredDataKey.CUSTOM_NAME, convertedName);
                } catch (Exception var10) {
                }
            }

            ListTag<StringTag> loreTag = displayTag.getListTag("Lore", StringTag.class);
            if (loreTag != null) {
                try {
                    data.set(
                        StructuredDataKey.LORE,
                        loreTag.stream().limit(256L).map(t -> this.jsonToTag(connection, t)).toArray(Tag[]::new)
                    );
                } catch (Exception var9) {
                }
            }

            NumberTag colorTag = displayTag.getNumberTag("color");
            if (colorTag != null) {
                data.set(StructuredDataKey.DYED_COLOR, new DyedColor(colorTag.asInt(), (hideFlags & 64) == 0));
            }
        }
    }

    private void updateBlockEntityTag(
        UserConnection connection, @Nullable StructuredDataContainer data, CompoundTag tag
    ) {
        if (tag != null) {
            if (data != null) {
                StringTag lockTag = tag.getStringTag("Lock");
                if (lockTag != null) {
                    data.set(StructuredDataKey.LOCK, lockTag);
                }

                ListTag<CompoundTag> beesTag = tag.getListTag("Bees", CompoundTag.class);
                if (beesTag != null) {
                    this.updateBees(data, beesTag);
                }

                ListTag<StringTag> sherdsTag = tag.getListTag("sherds", StringTag.class);
                if (sherdsTag != null && sherdsTag.size() == 4) {
                    String backSherd = sherdsTag.get(0).getValue();
                    String leftSherd = sherdsTag.get(1).getValue();
                    String rightSherd = sherdsTag.get(2).getValue();
                    String frontSherd = sherdsTag.get(3).getValue();
                    data.set(
                        StructuredDataKey.POT_DECORATIONS,
                        new PotDecorations(
                            this.toMappedItemId(backSherd),
                            this.toMappedItemId(leftSherd),
                            this.toMappedItemId(rightSherd),
                            this.toMappedItemId(frontSherd)
                        )
                    );
                }

                StringTag noteBlockSoundTag = tag.getStringTag("note_block_sound");
                if (noteBlockSoundTag != null) {
                    data.set(StructuredDataKey.NOTE_BLOCK_SOUND, noteBlockSoundTag.getValue());
                }

                StringTag lootTableTag = tag.getStringTag("LootTable");
                if (lootTableTag != null) {
                    long lootTableSeed = tag.getLong("LootTableSeed");
                    CompoundTag containerLoot = new CompoundTag();
                    containerLoot.putString("loot_table", lootTableTag.getValue());
                    containerLoot.putLong("loot_table_seed", lootTableSeed);
                    data.set(StructuredDataKey.CONTAINER_LOOT, containerLoot);
                }

                Tag baseColorTag = tag.remove("Base");
                if (baseColorTag instanceof NumberTag) {
                    data.set(StructuredDataKey.BASE_COLOR, ((NumberTag)baseColorTag).asInt());
                }

                this.updateItemList(connection, data, tag, "Items", StructuredDataKey.CONTAINER, true);
            }

            Tag skullOwnerTag = tag.remove("SkullOwner");
            if (skullOwnerTag instanceof StringTag) {
                CompoundTag profileTag = new CompoundTag();
                profileTag.putString("name", ((StringTag)skullOwnerTag).getValue());
                tag.put("profile", profileTag);
            } else if (skullOwnerTag instanceof CompoundTag) {
                this.updateSkullOwnerTag(tag, (CompoundTag)skullOwnerTag);
            }

            ListTag<CompoundTag> patternsTag = tag.getListTag("Patterns", CompoundTag.class);
            if (patternsTag != null) {
                BannerPatternLayer[] layers = patternsTag.stream().map(patternTag -> {
                    String pattern = patternTag.getString("Pattern", "");
                    int color = patternTag.getInt("Color", -1);
                    String fullPatternIdentifier = BannerPatterns1_20_5.compactToFullId(pattern);
                    if (fullPatternIdentifier != null && color != -1) {
                        patternTag.remove("Pattern");
                        patternTag.remove("Color");
                        patternTag.putString("pattern", fullPatternIdentifier);
                        patternTag.putString("color", DyeColors.colorById(color));
                        int id = BannerPatterns1_20_5.keyToId(fullPatternIdentifier);
                        return new BannerPatternLayer(Holder.of(id), color);
                    } else {
                        return null;
                    }
                }).filter(Objects::nonNull).toArray(BannerPatternLayer[]::new);
                tag.remove("Patterns");
                tag.put("patterns", patternsTag);
                if (data != null) {
                    data.set(StructuredDataKey.BANNER_PATTERNS, layers);
                }
            }

            this.removeEmptyItem(tag, "item");
            this.removeEmptyItem(tag, "RecordItem");
            this.removeEmptyItem(tag, "Book");
        }
    }

    private void removeEmptyItem(CompoundTag tag, String key) {
        CompoundTag itemTag = tag.getCompoundTag(key);
        if (itemTag != null) {
            int id = itemTag.getInt("id");
            if (id == 0) {
                tag.remove(key);
            }
        }
    }

    private void updateSkullOwnerTag(CompoundTag tag, CompoundTag skullOwnerTag) {
        CompoundTag profileTag = new CompoundTag();
        tag.put("profile", profileTag);
        String name = skullOwnerTag.getString("Name");
        if (name != null && this.isValidName(name)) {
            profileTag.putString("name", name);
        }

        IntArrayTag idTag = skullOwnerTag.getIntArrayTag("Id");
        if (idTag != null) {
            profileTag.put("id", idTag);
        }

        Tag propertiesTag = skullOwnerTag.remove("Properties");
        if (propertiesTag instanceof CompoundTag) {
            ListTag<CompoundTag> propertiesListTag = new ListTag<>(CompoundTag.class);

            for (Entry<String, Tag> entry : ((CompoundTag)propertiesTag).entrySet()) {
                if (entry.getValue() instanceof ListTag) {
                    for (Tag propertyTag : (ListTag)entry.getValue()) {
                        if (propertyTag instanceof CompoundTag) {
                            CompoundTag updatedPropertyTag = new CompoundTag();
                            CompoundTag propertyCompoundTag = (CompoundTag)propertyTag;
                            String value = propertyCompoundTag.getString("Value", "");
                            String signature = propertyCompoundTag.getString("Signature");
                            updatedPropertyTag.putString("name", entry.getKey());
                            updatedPropertyTag.putString("value", value);
                            if (signature != null) {
                                updatedPropertyTag.putString("signature", signature);
                            }

                            propertiesListTag.add(updatedPropertyTag);
                        }
                    }
                }
            }

            profileTag.put("properties", propertiesListTag);
        }
    }

    private boolean isValidName(String name) {
        if (name.length() > 16) {
            return false;
        }

        int i = 0;

        for (int len = name.length(); i < len; i++) {
            char c = name.charAt(i);
            if (c < '!' || c > '~') {
                return false;
            }
        }

        return true;
    }
}
