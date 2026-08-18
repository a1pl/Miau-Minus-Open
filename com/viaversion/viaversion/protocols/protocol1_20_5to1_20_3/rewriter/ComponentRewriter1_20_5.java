package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.rewriter;

import com.google.common.base.Preconditions;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.GameProfile;
import com.viaversion.viaversion.api.minecraft.GlobalPosition;
import com.viaversion.viaversion.api.minecraft.Holder;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
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
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.FloatTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ClientboundPacket1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.ArmorMaterials1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Attributes1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.BannerPatterns1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.DyeColors;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Enchantments1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.EquipmentSlots1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Instruments1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.PotionEffects1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Potions1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.TrimMaterials1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.TrimPatterns1_20_3;
import com.viaversion.viaversion.rewriter.ComponentRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.Either;
import com.viaversion.viaversion.util.Key;
import com.viaversion.viaversion.util.SerializerVersion;
import com.viaversion.viaversion.util.UUIDUtil;
import com.viaversion.viaversion.util.Unit;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

public class ComponentRewriter1_20_5 extends ComponentRewriter<ClientboundPacket1_20_3> {
    protected final Map<StructuredDataKey, ComponentRewriter1_20_5.DataConverter> rewriters = new HashMap<>();

    public ComponentRewriter1_20_5(Protocol<ClientboundPacket1_20_3, ?, ?, ?> protocol) {
        super(protocol, ComponentRewriter.ReadType.NBT);
        this.register(StructuredDataKey.CUSTOM_DATA, this::convertCustomData);
        this.register(StructuredDataKey.MAX_STACK_SIZE, this::convertMaxStackSize);
        this.register(StructuredDataKey.MAX_DAMAGE, this::convertMaxDamage);
        this.register(StructuredDataKey.DAMAGE, this::convertDamage);
        this.register(StructuredDataKey.UNBREAKABLE, this::convertUnbreakable);
        this.register(StructuredDataKey.CUSTOM_NAME, this::convertCustomName);
        this.register(StructuredDataKey.ITEM_NAME, this::convertItemName);
        this.register(StructuredDataKey.LORE, this::convertLore);
        this.register(StructuredDataKey.RARITY, this::convertRarity);
        this.register(StructuredDataKey.ENCHANTMENTS, this::convertEnchantments);
        this.register(StructuredDataKey.CAN_PLACE_ON, this::convertCanPlaceOn);
        this.register(StructuredDataKey.CAN_BREAK, this::convertCanBreak);
        this.register(StructuredDataKey.ATTRIBUTE_MODIFIERS, this::convertAttributeModifiers);
        this.register(StructuredDataKey.CUSTOM_MODEL_DATA, this::convertCustomModelData);
        this.register(StructuredDataKey.HIDE_ADDITIONAL_TOOLTIP, this::convertHideAdditionalTooltip);
        this.register(StructuredDataKey.HIDE_TOOLTIP, this::convertHideTooltip);
        this.register(StructuredDataKey.REPAIR_COST, this::convertRepairCost);
        this.register(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE, this::convertEnchantmentGlintOverride);
        this.register(StructuredDataKey.CREATIVE_SLOT_LOCK, null);
        this.register(StructuredDataKey.INTANGIBLE_PROJECTILE, this::convertIntangibleProjectile);
        this.register(StructuredDataKey.FOOD, this::convertFood);
        this.register(StructuredDataKey.FIRE_RESISTANT, this::convertFireResistant);
        this.register(StructuredDataKey.TOOL, this::convertTool);
        this.register(StructuredDataKey.STORED_ENCHANTMENTS, this::convertStoredEnchantments);
        this.register(StructuredDataKey.DYED_COLOR, this::convertDyedColor);
        this.register(StructuredDataKey.MAP_COLOR, this::convertMapColor);
        this.register(StructuredDataKey.MAP_ID, this::convertMapId);
        this.register(StructuredDataKey.MAP_DECORATIONS, this::convertMapDecorations);
        this.register(StructuredDataKey.MAP_POST_PROCESSING, null);
        this.register(StructuredDataKey.CHARGED_PROJECTILES, this::convertChargedProjectiles);
        this.register(StructuredDataKey.BUNDLE_CONTENTS, this::convertBundleContents);
        this.register(StructuredDataKey.POTION_CONTENTS, this::convertPotionContents);
        this.register(StructuredDataKey.SUSPICIOUS_STEW_EFFECTS, this::convertSuspiciousStewEffects);
        this.register(StructuredDataKey.WRITABLE_BOOK_CONTENT, this::convertWritableBookContent);
        this.register(StructuredDataKey.WRITTEN_BOOK_CONTENT, this::convertWrittenBookContent);
        this.register(StructuredDataKey.TRIM, this::convertTrim);
        this.register(StructuredDataKey.DEBUG_STICK_STATE, this::convertDebugStickRate);
        this.register(StructuredDataKey.ENTITY_DATA, this::convertEntityData);
        this.register(StructuredDataKey.BUCKET_ENTITY_DATA, this::convertBucketEntityData);
        this.register(StructuredDataKey.BLOCK_ENTITY_DATA, this::convertBlockEntityData);
        this.register(StructuredDataKey.INSTRUMENT, this::convertInstrument);
        this.register(StructuredDataKey.OMINOUS_BOTTLE_AMPLIFIER, this::convertOminousBottleAmplifier);
        this.register(StructuredDataKey.RECIPES, this::convertRecipes);
        this.register(StructuredDataKey.LODESTONE_TRACKER, this::convertLodestoneTracker);
        this.register(StructuredDataKey.FIREWORK_EXPLOSION, this::convertFireworkExplosion);
        this.register(StructuredDataKey.FIREWORKS, this::convertFireworks);
        this.register(StructuredDataKey.PROFILE, this::convertProfile);
        this.register(StructuredDataKey.NOTE_BLOCK_SOUND, this::convertNoteBlockSound);
        this.register(StructuredDataKey.BANNER_PATTERNS, this::convertBannerPatterns);
        this.register(StructuredDataKey.BASE_COLOR, this::convertBaseColor);
        this.register(StructuredDataKey.POT_DECORATIONS, this::convertPotDecorations);
        this.register(StructuredDataKey.CONTAINER, this::convertContainer);
        this.register(StructuredDataKey.BLOCK_STATE, this::convertBlockState);
        this.register(StructuredDataKey.BEES, this::convertBees);
        this.register(StructuredDataKey.LOCK, this::convertLock);
        this.register(StructuredDataKey.CONTAINER_LOOT, this::convertContainerLoot);
    }

    @Override
    protected void handleHoverEvent(UserConnection connection, CompoundTag hoverEventTag) {
        StringTag actionTag = hoverEventTag.getStringTag("action");
        if (actionTag != null) {
            if (actionTag.getValue().equals("show_item")) {
                Tag valueTag = hoverEventTag.remove("value");
                if (valueTag != null) {
                    CompoundTag tag = ComponentUtil.deserializeShowItem(valueTag, SerializerVersion.V1_20_3);
                    CompoundTag contentsTag = new CompoundTag();
                    contentsTag.put("id", tag.getStringTag("id"));
                    contentsTag.put("count", new IntTag(tag.getByte("Count")));
                    if (tag.get("tag") instanceof CompoundTag) {
                        contentsTag.put(
                            "tag", new StringTag(SerializerVersion.V1_20_3.toSNBT(tag.getCompoundTag("tag")))
                        );
                    }

                    hoverEventTag.put("contents", contentsTag);
                }

                CompoundTag contentsTag = hoverEventTag.getCompoundTag("contents");
                if (contentsTag == null) {
                    return;
                }

                StringTag idTag = contentsTag.getStringTag("id");
                if (idTag == null) {
                    return;
                }

                int itemId = Protocol1_20_5To1_20_3.MAPPINGS.getFullItemMappings().id(idTag.getValue());
                if (itemId == -1) {
                    return;
                }

                StringTag tag = contentsTag.remove("tag");
                if (tag == null) {
                    return;
                }

                CompoundTag tagTag;
                try {
                    tagTag = (CompoundTag)SerializerVersion.V1_20_3.toTag(tag.getValue());
                } catch (Exception e) {
                    if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                        Via.getPlatform()
                            .getLogger()
                            .log(Level.WARNING, "Error reading 1.20.3 NBT in show_item: " + contentsTag, e);
                    }

                    return;
                }

                Item oldItem = new DataItem();
                oldItem.setIdentifier(itemId);
                if (tagTag != null) {
                    oldItem.setTag(tagTag);
                }

                Item newItem = this.protocol.getItemRewriter().handleItemToClient(connection, oldItem);
                if (newItem == null) {
                    return;
                }

                if (newItem.identifier() != 0) {
                    String itemName = Protocol1_20_5To1_20_3.MAPPINGS
                        .getFullItemMappings()
                        .mappedIdentifier(newItem.identifier());
                    if (itemName != null) {
                        contentsTag.putString("id", itemName);
                    }
                } else {
                    contentsTag.putString("id", "minecraft:stone");
                }

                Map<StructuredDataKey<?>, StructuredData<?>> data = newItem.structuredData().data();
                if (!data.isEmpty()) {
                    CompoundTag components;
                    try {
                        components = this.toTag(data, false);
                    } catch (Exception e) {
                        if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                            Via.getPlatform()
                                .getLogger()
                                .log(Level.WARNING, "Error writing 1.20.5 components in show_item!", e);
                        }

                        return;
                    }

                    contentsTag.put("components", components);
                }
            } else if (actionTag.getValue().equals("show_entity")) {
                Tag valueTag = hoverEventTag.remove("value");
                if (valueTag != null) {
                    CompoundTag tag = ComponentUtil.deserializeShowItem(valueTag, SerializerVersion.V1_20_3);
                    CompoundTag contentsTag = new CompoundTag();
                    contentsTag.put("type", tag.getStringTag("type"));
                    contentsTag.put("id", tag.getStringTag("id"));
                    contentsTag.put(
                        "name",
                        SerializerVersion.V1_20_3.toTag(SerializerVersion.V1_20_3.toComponent(tag.getString("name")))
                    );
                    hoverEventTag.put("contents", contentsTag);
                }

                CompoundTag contentsTag = hoverEventTag.getCompoundTag("contents");
                if (contentsTag == null) {
                    return;
                }

                if (this.protocol.getMappingData().getEntityMappings().mappedId(contentsTag.getString("type")) == -1) {
                    contentsTag.put("type", new StringTag("pig"));
                }
            }
        }
    }

    protected CompoundTag toTag(Map<StructuredDataKey<?>, StructuredData<?>> data, boolean empty) {
        CompoundTag tag = new CompoundTag();

        for (Entry<StructuredDataKey<?>, StructuredData<?>> entry : data.entrySet()) {
            StructuredDataKey<?> key = entry.getKey();
            if (!this.rewriters.containsKey(key)) {
                Via.getPlatform().getLogger().severe("No converter for " + key.identifier() + " found!");
            } else {
                StructuredData<?> value = entry.getValue();
                if (value.isEmpty()) {
                    if (!empty) {
                        throw new IllegalArgumentException("Empty structured data: " + key.identifier());
                    }

                    tag.put("!" + key.identifier(), new CompoundTag());
                } else {
                    Tag valueTag = this.rewriters.get(key).convert(value.value());
                    if (valueTag != null) {
                        tag.put(key.identifier(), valueTag);
                    }
                }
            }
        }

        return tag;
    }

    protected CompoundTag convertCustomData(CompoundTag value) {
        return value;
    }

    protected IntTag convertMaxStackSize(Integer value) {
        return this.convertIntRange(value, 1, 99);
    }

    protected IntTag convertMaxDamage(Integer value) {
        return this.convertPositiveInt(value);
    }

    protected IntTag convertDamage(Integer value) {
        return this.convertNonNegativeInt(value);
    }

    protected CompoundTag convertUnbreakable(Unbreakable value) {
        CompoundTag tag = new CompoundTag();
        if (!value.showInTooltip()) {
            tag.putBoolean("show_in_tooltip", false);
        }

        return tag;
    }

    protected StringTag convertCustomName(Tag value) {
        return this.convertComponent(value);
    }

    protected StringTag convertItemName(Tag value) {
        return this.convertComponent(value);
    }

    protected ListTag<StringTag> convertLore(Tag[] value) {
        return this.convertComponents(value, 256);
    }

    protected StringTag convertRarity(Integer value) {
        return this.convertEnumEntry(value, "common", "uncommon", "rare", "epic");
    }

    protected CompoundTag convertEnchantments(Enchantments value) {
        CompoundTag tag = new CompoundTag();
        CompoundTag levels = new CompoundTag();

        for (Int2IntMap.Entry entry : value.enchantments().int2IntEntrySet()) {
            int level = this.checkIntRange(0, 255, entry.getIntValue());
            levels.putInt(Enchantments1_20_5.idToKey(entry.getIntKey()), level);
        }

        tag.put("levels", levels);
        if (!value.showInTooltip()) {
            tag.putBoolean("show_in_tooltip", false);
        }

        return tag;
    }

    protected CompoundTag convertCanPlaceOn(AdventureModePredicate value) {
        CompoundTag tag = new CompoundTag();
        ListTag<CompoundTag> predicates = new ListTag<>(CompoundTag.class);

        for (BlockPredicate predicate : value.predicates()) {
            CompoundTag predicateTag = new CompoundTag();
            if (predicate.holderSet() != null) {
                this.convertHolderSet(predicateTag, "blocks", predicate.holderSet());
            }

            if (predicate.propertyMatchers() != null) {
                CompoundTag state = new CompoundTag();

                for (StatePropertyMatcher matcher : predicate.propertyMatchers()) {
                    Either<String, StatePropertyMatcher.RangedMatcher> match = matcher.matcher();
                    if (match.isLeft()) {
                        state.putString(matcher.name(), match.left());
                    } else {
                        StatePropertyMatcher.RangedMatcher range = match.right();
                        CompoundTag rangeTag = new CompoundTag();
                        if (range.minValue() != null) {
                            rangeTag.putString("min", range.minValue());
                        }

                        if (range.maxValue() != null) {
                            rangeTag.putString("max", range.maxValue());
                        }

                        state.put(matcher.name(), rangeTag);
                    }
                }

                predicateTag.put("state", state);
            }

            if (predicate.tag() != null) {
                predicateTag.putString("nbt", this.serializerVersion().toSNBT(predicate.tag()));
            }

            predicates.add(predicateTag);
        }

        tag.put("predicates", predicates);
        if (!value.showInTooltip()) {
            tag.putBoolean("show_in_tooltip", false);
        }

        return tag;
    }

    protected CompoundTag convertCanBreak(AdventureModePredicate value) {
        return this.convertCanPlaceOn(value);
    }

    protected CompoundTag convertAttributeModifiers(AttributeModifiers value) {
        CompoundTag tag = new CompoundTag();
        ListTag<CompoundTag> modifiers = new ListTag<>(CompoundTag.class);

        for (AttributeModifier modifier : value.modifiers()) {
            CompoundTag modifierTag = new CompoundTag();
            String type = Attributes1_20_5.idToKey(modifier.attribute());
            if (type == null) {
                throw new IllegalArgumentException("Unknown attribute type: " + modifier.attribute());
            }

            modifierTag.putString("type", type);
            this.convertModifierData(modifierTag, modifier.modifier());
            if (modifier.slotType() != 0) {
                String slotType = EquipmentSlots1_20_5.idToKey(modifier.slotType());
                Preconditions.checkNotNull(slotType, "Unknown slot type %s", new Object[]{modifier.slotType()});
                modifierTag.putString("slot", slotType);
            }

            modifiers.add(modifierTag);
        }

        tag.put("modifiers", modifiers);
        if (!value.showInTooltip()) {
            tag.putBoolean("show_in_tooltip", false);
        }

        return tag;
    }

    protected IntTag convertCustomModelData(Integer value) {
        return new IntTag(value);
    }

    protected CompoundTag convertHideAdditionalTooltip(Unit value) {
        return this.convertUnit();
    }

    protected CompoundTag convertHideTooltip(Unit value) {
        return this.convertUnit();
    }

    protected IntTag convertRepairCost(Integer value) {
        return this.convertIntRange(value, 0, Integer.MAX_VALUE);
    }

    protected ByteTag convertEnchantmentGlintOverride(Boolean value) {
        return new ByteTag(value);
    }

    protected CompoundTag convertIntangibleProjectile(Tag value) {
        return this.convertUnit();
    }

    protected CompoundTag convertFood(FoodProperties value) {
        CompoundTag tag = new CompoundTag();
        tag.put("nutrition", this.convertNonNegativeInt(value.nutrition()));
        tag.putFloat("saturation_modifier", value.saturationModifier());
        if (value.canAlwaysEat()) {
            tag.putBoolean("can_always_eat", true);
        }

        if (value.eatSeconds() != 1.6F) {
            tag.put("eat_seconds", this.convertPositiveFloat(value.eatSeconds()));
        }

        if (value.possibleEffects().length > 0) {
            ListTag<CompoundTag> effects = new ListTag<>(CompoundTag.class);

            for (FoodEffect foodEffect : value.possibleEffects()) {
                CompoundTag effectTag = new CompoundTag();
                CompoundTag potionEffectTag = new CompoundTag();
                this.convertPotionEffect(potionEffectTag, foodEffect.effect());
                effectTag.put("effect", potionEffectTag);
                if (foodEffect.probability() != 1.0F) {
                    effectTag.putFloat("probability", foodEffect.probability());
                }
            }

            tag.put("effects", effects);
        }

        return tag;
    }

    protected CompoundTag convertFireResistant(Unit value) {
        return this.convertUnit();
    }

    protected CompoundTag convertTool(ToolProperties value) {
        CompoundTag tag = new CompoundTag();
        ListTag<CompoundTag> rules = new ListTag<>(CompoundTag.class);

        for (ToolRule rule : value.rules()) {
            CompoundTag ruleTag = new CompoundTag();
            this.convertHolderSet(ruleTag, "blocks", rule.blocks());
            if (rule.speed() != null) {
                ruleTag.putFloat("speed", rule.speed());
            }

            if (rule.correctForDrops() != null) {
                ruleTag.putBoolean("correct_for_drops", rule.correctForDrops());
            }

            rules.add(ruleTag);
        }

        tag.put("rules", rules);
        if (value.defaultMiningSpeed() != 1.0F) {
            tag.putFloat("default_mining_speed", value.defaultMiningSpeed());
        }

        if (value.damagePerBlock() != 1) {
            tag.put("damage_per_block", this.convertNonNegativeInt(value.damagePerBlock()));
        }

        return tag;
    }

    protected CompoundTag convertStoredEnchantments(Enchantments value) {
        return this.convertEnchantments(value);
    }

    protected CompoundTag convertDyedColor(DyedColor value) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("rgb", value.rgb());
        if (!value.showInTooltip()) {
            tag.putBoolean("show_in_tooltip", false);
        }

        return tag;
    }

    protected IntTag convertMapColor(Integer value) {
        return new IntTag(value);
    }

    protected IntTag convertMapId(Integer value) {
        return new IntTag(value);
    }

    protected CompoundTag convertMapDecorations(CompoundTag value) {
        return value;
    }

    protected ListTag<CompoundTag> convertChargedProjectiles(Item[] value) {
        return this.convertItemArray(value);
    }

    protected ListTag<CompoundTag> convertBundleContents(Item[] value) {
        return this.convertItemArray(value);
    }

    protected CompoundTag convertPotionContents(PotionContents value) {
        CompoundTag tag = new CompoundTag();
        if (value.potion() != null) {
            String potion = Potions1_20_5.idToKey(value.potion());
            if (potion != null) {
                tag.putString("potion", potion);
            }
        }

        if (value.customColor() != null) {
            tag.putInt("custom_color", value.customColor());
        }

        for (PotionEffect effect : value.customEffects()) {
            this.convertPotionEffect(tag, effect);
        }

        return tag;
    }

    protected ListTag<CompoundTag> convertSuspiciousStewEffects(SuspiciousStewEffect[] value) {
        ListTag<CompoundTag> tag = new ListTag<>(CompoundTag.class);

        for (SuspiciousStewEffect effect : value) {
            CompoundTag effectTag = new CompoundTag();
            String id = PotionEffects1_20_5.idToKey(effect.mobEffect());
            if (id != null) {
                effectTag.putString("id", id);
            }

            if (effect.duration() != 160) {
                effectTag.putInt("duration", effect.duration());
            }

            tag.add(effectTag);
        }

        return tag;
    }

    protected CompoundTag convertWritableBookContent(FilterableString[] value) {
        CompoundTag tag = new CompoundTag();
        if (value == null) {
            return tag;
        }

        if (value.length > 100) {
            throw new IllegalArgumentException("Too many pages: " + value.length);
        }

        ListTag<CompoundTag> pagesTag = new ListTag<>(CompoundTag.class);

        for (FilterableString page : value) {
            CompoundTag pageTag = new CompoundTag();
            this.convertFilterableString(pageTag, page, 0, 1024);
            pagesTag.add(pageTag);
        }

        tag.put("pages", pagesTag);
        return tag;
    }

    protected CompoundTag convertWrittenBookContent(WrittenBook value) {
        CompoundTag tag = new CompoundTag();
        this.convertFilterableString(tag, value.title(), 0, 32);
        tag.putString("author", value.author());
        if (value.generation() != 0) {
            tag.put("generation", this.convertIntRange(value.generation(), 0, 3));
        }

        CompoundTag title = new CompoundTag();
        this.convertFilterableString(title, value.title(), 0, 32);
        tag.put("title", title);
        ListTag<CompoundTag> pagesTag = new ListTag<>(CompoundTag.class);

        for (FilterableComponent page : value.pages()) {
            CompoundTag pageTag = new CompoundTag();
            this.convertFilterableComponent(pageTag, page);
            pagesTag.add(pageTag);
        }

        if (!pagesTag.isEmpty()) {
            tag.put("pages", pagesTag);
        }

        if (value.resolved()) {
            tag.putBoolean("resolved", true);
        }

        return tag;
    }

    protected CompoundTag convertTrim(ArmorTrim value) {
        CompoundTag tag = new CompoundTag();
        Holder<ArmorTrimMaterial> material = value.material();
        if (material.hasId()) {
            String trimMaterial = TrimMaterials1_20_3.idToKey(material.id());
            tag.putString("material", trimMaterial);
        } else {
            ArmorTrimMaterial armorTrimMaterial = material.value();
            CompoundTag materialTag = new CompoundTag();
            String ingredient = Protocol1_20_5To1_20_3.MAPPINGS
                .getFullItemMappings()
                .identifier(armorTrimMaterial.itemId());
            if (ingredient == null) {
                throw new IllegalArgumentException("Unknown item: " + armorTrimMaterial.itemId());
            }

            CompoundTag overrideArmorMaterialsTag = new CompoundTag();

            for (Int2ObjectMap.Entry<String> entry : armorTrimMaterial.overrideArmorMaterials().int2ObjectEntrySet()) {
                String materialKey = ArmorMaterials1_20_5.idToKey(entry.getIntKey());
                if (materialKey != null) {
                    overrideArmorMaterialsTag.putString(materialKey, entry.getValue());
                }
            }

            materialTag.putString("asset_name", armorTrimMaterial.assetName());
            materialTag.putString("ingredient", ingredient);
            materialTag.putFloat("item_model_index", armorTrimMaterial.itemModelIndex());
            materialTag.put("override_armor_materials", overrideArmorMaterialsTag);
            materialTag.put("description", armorTrimMaterial.description());
            tag.put("material", materialTag);
        }

        Holder<ArmorTrimPattern> pattern = value.pattern();
        if (pattern.hasId()) {
            tag.putString("pattern", TrimPatterns1_20_3.idToKey(pattern.id()));
        } else {
            ArmorTrimPattern armorTrimPattern = pattern.value();
            CompoundTag patternTag = new CompoundTag();
            String templateItem = Protocol1_20_5To1_20_3.MAPPINGS
                .getFullItemMappings()
                .identifier(armorTrimPattern.itemId());
            if (templateItem == null) {
                throw new IllegalArgumentException("Unknown item: " + armorTrimPattern.itemId());
            }

            patternTag.put("asset_id", this.convertIdentifier(armorTrimPattern.assetName()));
            patternTag.putString("template_item", templateItem);
            patternTag.put("description", armorTrimPattern.description());
            if (armorTrimPattern.decal()) {
                patternTag.putBoolean("decal", true);
            }

            tag.put("pattern", patternTag);
        }

        if (!value.showInTooltip()) {
            tag.putBoolean("show_in_tooltip", false);
        }

        return tag;
    }

    protected CompoundTag convertDebugStickRate(CompoundTag value) {
        return value;
    }

    protected CompoundTag convertEntityData(CompoundTag value) {
        return this.convertNbtWithId(value);
    }

    protected CompoundTag convertBucketEntityData(CompoundTag value) {
        return this.convertNbt(value);
    }

    protected CompoundTag convertBlockEntityData(CompoundTag value) {
        return this.convertNbtWithId(value);
    }

    protected Tag convertInstrument(Holder<Instrument> value) {
        if (value.hasId()) {
            return new StringTag(Instruments1_20_3.idToKey(value.id()));
        }

        Instrument instrument = value.value();
        CompoundTag tag = new CompoundTag();
        Holder<SoundEvent> sound = instrument.soundEvent();
        if (sound.hasId()) {
            tag.putString("sound_event", Protocol1_20_5To1_20_3.MAPPINGS.soundName(sound.id()));
        } else {
            SoundEvent soundEvent = sound.value();
            CompoundTag soundEventTag = new CompoundTag();
            soundEventTag.put("sound_id", this.convertIdentifier(soundEvent.identifier()));
            if (soundEvent.fixedRange() != null) {
                soundEventTag.putFloat("range", soundEvent.fixedRange());
            }
        }

        tag.put("use_duration", this.convertPositiveInt(instrument.useDuration()));
        tag.put("range", this.convertPositiveFloat(instrument.range()));
        return tag;
    }

    protected IntTag convertOminousBottleAmplifier(Integer value) {
        return this.convertIntRange(value, 0, 4);
    }

    protected Tag convertRecipes(Tag value) {
        return value;
    }

    protected CompoundTag convertLodestoneTracker(LodestoneTracker value) {
        CompoundTag tag = new CompoundTag();
        if (value.pos() != null) {
            this.convertGlobalPos(tag, "target", value.pos());
        }

        if (!value.tracked()) {
            tag.putBoolean("tracked", false);
        }

        return tag;
    }

    protected CompoundTag convertFireworkExplosion(FireworkExplosion value) {
        CompoundTag tag = new CompoundTag();
        tag.put("shape", this.convertEnumEntry(value.shape(), "small_ball", "large_ball", "star", "creeper", "burst"));
        if (value.colors().length > 0) {
            tag.put("colors", new IntArrayTag(value.colors()));
        }

        if (value.fadeColors().length > 0) {
            tag.put("fade_colors", new IntArrayTag(value.fadeColors()));
        }

        if (value.hasTrail()) {
            tag.putBoolean("trail", true);
        }

        if (value.hasTwinkle()) {
            tag.putBoolean("twinkle", true);
        }

        return tag;
    }

    protected CompoundTag convertFireworks(Fireworks value) {
        CompoundTag tag = new CompoundTag();
        if (value.flightDuration() != 0) {
            tag.put("flight_duration", this.convertUnsignedByte((byte)value.flightDuration()));
        }

        ListTag<CompoundTag> explosions = new ListTag<>(CompoundTag.class);
        if (value.explosions().length > 256) {
            throw new IllegalArgumentException("Too many explosions: " + value.explosions().length);
        }

        for (FireworkExplosion explosion : value.explosions()) {
            explosions.add(this.convertFireworkExplosion(explosion));
        }

        tag.put("explosions", explosions);
        return tag;
    }

    protected CompoundTag convertProfile(GameProfile value) {
        CompoundTag tag = new CompoundTag();
        if (value.name() != null) {
            tag.putString("name", value.name());
        }

        if (value.id() != null) {
            tag.put("id", new IntArrayTag(UUIDUtil.toIntArray(value.id())));
        }

        if (value.properties().length > 0) {
            this.convertProperties(tag, "properties", value.properties());
        }

        return tag;
    }

    protected StringTag convertNoteBlockSound(String value) {
        return this.convertIdentifier(value);
    }

    protected ListTag<CompoundTag> convertBannerPatterns(BannerPatternLayer[] value) {
        ListTag<CompoundTag> tag = new ListTag<>(CompoundTag.class);

        for (BannerPatternLayer layer : value) {
            CompoundTag layerTag = new CompoundTag();
            this.convertBannerPattern(layerTag, "pattern", layer.pattern());
            layerTag.put("color", this.convertDyeColor(layer.dyeColor()));
            tag.add(layerTag);
        }

        return tag;
    }

    protected StringTag convertBaseColor(Integer value) {
        return this.convertDyeColor(value);
    }

    protected ListTag<StringTag> convertPotDecorations(PotDecorations value) {
        ListTag<StringTag> tag = new ListTag<>(StringTag.class);

        for (int decoration : value.itemIds()) {
            String item = Protocol1_20_5To1_20_3.MAPPINGS.getFullItemMappings().identifier(decoration);
            if (item == null) {
                throw new IllegalArgumentException("Unknown item: " + decoration);
            }

            tag.add(new StringTag(item));
        }

        return tag;
    }

    protected ListTag<CompoundTag> convertContainer(Item[] value) {
        ListTag<CompoundTag> tag = new ListTag<>(CompoundTag.class);
        ListTag<CompoundTag> items = this.convertItemArray(value);

        for (int i = 0; i < items.size(); i++) {
            CompoundTag itemTag = new CompoundTag();
            itemTag.putInt("slot", i);
            itemTag.put("item", items.get(i));
            tag.add(itemTag);
        }

        return tag;
    }

    protected CompoundTag convertBlockState(BlockStateProperties value) {
        CompoundTag tag = new CompoundTag();

        for (Entry<String, String> entry : value.properties().entrySet()) {
            tag.putString(entry.getKey(), entry.getValue());
        }

        return tag;
    }

    protected ListTag<CompoundTag> convertBees(Bee[] value) {
        ListTag<CompoundTag> tag = new ListTag<>(CompoundTag.class);

        for (Bee bee : value) {
            CompoundTag beeTag = new CompoundTag();
            if (!bee.entityData().isEmpty()) {
                beeTag.put("entity_data", this.convertNbt(bee.entityData()));
            }

            beeTag.putInt("ticks_in_hive", bee.ticksInHive());
            beeTag.putInt("min_ticks_in_hive", bee.minTicksInHive());
        }

        return tag;
    }

    protected StringTag convertLock(Tag value) {
        return (StringTag)value;
    }

    protected CompoundTag convertContainerLoot(CompoundTag value) {
        return value;
    }

    protected void convertModifierData(CompoundTag tag, ModifierData data) {
        tag.put("uuid", new IntArrayTag(UUIDUtil.toIntArray(data.uuid())));
        tag.putString("name", data.name());
        tag.putDouble("amount", data.amount());
        tag.putString("operation", BlockItemPacketRewriter1_20_5.ATTRIBUTE_OPERATIONS[data.operation()]);
    }

    protected void convertPotionEffect(CompoundTag tag, PotionEffect effect) {
        String id = PotionEffects1_20_5.idToKey(effect.effect());
        if (id == null) {
            throw new IllegalArgumentException("Unknown potion effect: " + effect.effect());
        }

        tag.putString("id", id);
        this.convertPotionEffectData(tag, effect.effectData());
    }

    protected void convertPotionEffectData(CompoundTag tag, PotionEffectData data) {
        if (data.amplifier() != 0) {
            tag.putInt("amplifier", data.amplifier());
        }

        if (data.duration() != 0) {
            tag.putInt("duration", data.duration());
        }

        if (data.ambient()) {
            tag.putBoolean("ambient", true);
        }

        if (!data.showParticles()) {
            tag.putBoolean("show_particles", false);
        }

        tag.putBoolean("show_icon", data.showIcon());
        if (data.hiddenEffect() != null) {
            CompoundTag hiddenEffect = new CompoundTag();
            this.convertPotionEffectData(hiddenEffect, data.hiddenEffect());
            tag.put("hidden_effect", hiddenEffect);
        }
    }

    protected void convertHolderSet(CompoundTag tag, String name, HolderSet set) {
        if (set.hasTagKey()) {
            tag.putString(name, set.tagKey());
        } else {
            tag.put(name, new IntArrayTag(set.ids()));
        }
    }

    protected ListTag<CompoundTag> convertItemArray(Item[] value) {
        ListTag<CompoundTag> tag = new ListTag<>(CompoundTag.class);

        for (Item item : value) {
            CompoundTag itemTag = new CompoundTag();
            this.convertItem(itemTag, item);
            tag.add(itemTag);
        }

        return tag;
    }

    protected void convertItem(CompoundTag tag, Item item) {
        String name = Protocol1_20_5To1_20_3.MAPPINGS.getFullItemMappings().identifier(item.identifier());
        if (name == null) {
            throw new IllegalArgumentException("Unknown item: " + item.identifier());
        }

        tag.putString("id", name);

        try {
            tag.put("count", this.convertPositiveInt(item.amount()));
        } catch (IllegalArgumentException ignored) {
            tag.putInt("count", 1);
        }

        Map<StructuredDataKey<?>, StructuredData<?>> components = item.structuredData().data();
        tag.put("components", this.toTag(components, true));
    }

    protected void convertFilterableString(CompoundTag tag, FilterableString string, int min, int max) {
        tag.put("raw", this.convertString(string.raw(), min, max));
        if (string.filtered() != null) {
            tag.put("filtered", this.convertString(string.filtered(), min, max));
        }
    }

    protected void convertFilterableComponent(CompoundTag tag, FilterableComponent component) {
        tag.put("raw", this.convertComponent(component.raw()));
        if (component.filtered() != null) {
            tag.put("filtered", this.convertComponent(component.filtered()));
        }
    }

    protected void convertGlobalPos(CompoundTag tag, String name, GlobalPosition position) {
        CompoundTag posTag = new CompoundTag();
        posTag.putString("dimension", position.dimension());
        posTag.put("pos", new IntArrayTag(new int[]{position.x(), position.y(), position.z()}));
        tag.put(name, posTag);
    }

    protected void convertProperties(CompoundTag tag, String name, GameProfile.Property[] properties) {
        ListTag<CompoundTag> propertiesTag = new ListTag<>(CompoundTag.class);

        for (GameProfile.Property property : properties) {
            CompoundTag propertyTag = new CompoundTag();
            propertyTag.putString("name", property.name());
            propertyTag.putString("value", property.value());
            if (property.signature() != null) {
                propertyTag.putString("signature", property.signature());
            }

            propertiesTag.add(propertyTag);
        }

        tag.put(name, propertiesTag);
    }

    protected void convertBannerPattern(CompoundTag tag, String name, Holder<BannerPattern> pattern) {
        if (pattern.hasId()) {
            tag.putString(name, BannerPatterns1_20_5.idToKey(pattern.id()));
        } else {
            BannerPattern bannerPattern = pattern.value();
            CompoundTag patternTag = new CompoundTag();
            patternTag.put("asset_id", this.convertIdentifier(bannerPattern.assetId()));
            patternTag.putString("translation_key", bannerPattern.translationKey());
            tag.put(name, patternTag);
        }
    }

    protected IntTag convertPositiveInt(Integer value) {
        return this.convertIntRange(value, 1, Integer.MAX_VALUE);
    }

    protected IntTag convertNonNegativeInt(Integer value) {
        return this.convertIntRange(value, 0, Integer.MAX_VALUE);
    }

    protected IntTag convertIntRange(Integer value, int min, int max) {
        return new IntTag(this.checkIntRange(min, max, value));
    }

    protected FloatTag convertPositiveFloat(Float value) {
        return this.convertFloatRange(value, 0.0F, Float.MAX_VALUE);
    }

    protected FloatTag convertFloatRange(Float value, float min, float max) {
        return new FloatTag(this.checkFloatRange(min, max, value));
    }

    protected StringTag convertString(String value, int min, int max) {
        return new StringTag(this.checkStringRange(min, max, value));
    }

    protected ByteTag convertUnsignedByte(byte value) {
        if (value > -1) {
            throw new IllegalArgumentException("Value out of range: " + value);
        } else {
            return new ByteTag(value);
        }
    }

    protected StringTag convertComponent(Tag value) {
        return this.convertComponent(value, 0, Integer.MAX_VALUE);
    }

    protected StringTag convertComponent(Tag value, int min, int max) {
        String json = this.serializerVersion().toString(this.serializerVersion().toComponent(value));
        return new StringTag(this.checkStringRange(min, max, json));
    }

    protected ListTag<StringTag> convertComponents(Tag[] value, int maxLength) {
        this.checkIntRange(0, maxLength, value.length);
        ListTag<StringTag> listTag = new ListTag<>(StringTag.class);

        for (Tag tag : value) {
            String json = this.serializerVersion().toString(this.serializerVersion().toComponent(tag));
            listTag.add(new StringTag(json));
        }

        return listTag;
    }

    protected StringTag convertEnumEntry(Integer value, String... values) {
        Preconditions.checkArgument(value >= 0 && value < values.length, "Enum value out of range: " + value);
        return new StringTag(values[value]);
    }

    protected CompoundTag convertUnit() {
        return new CompoundTag();
    }

    protected CompoundTag convertNbt(CompoundTag tag) {
        return tag;
    }

    protected CompoundTag convertNbtWithId(CompoundTag tag) {
        if (tag.getStringTag("id") == null) {
            throw new IllegalArgumentException("Missing id tag in nbt: " + tag);
        } else {
            return tag;
        }
    }

    protected StringTag convertIdentifier(String value) {
        if (!Key.isValid(value)) {
            throw new IllegalArgumentException("Invalid identifier: " + value);
        } else {
            return new StringTag(value);
        }
    }

    protected StringTag convertDyeColor(Integer value) {
        return new StringTag(DyeColors.colorById(value));
    }

    private int checkIntRange(int min, int max, int value) {
        Preconditions.checkArgument(value >= min && value <= max, "Value out of range: " + value);
        return value;
    }

    private float checkFloatRange(float min, float max, float value) {
        Preconditions.checkArgument(value >= min && value <= max, "Value out of range: " + value);
        return value;
    }

    private String checkStringRange(int min, int max, String value) {
        int length = value.length();
        Preconditions.checkArgument(length >= min && length <= max, "Value out of range: " + value);
        return value;
    }

    private <T> void register(StructuredDataKey<T> key, ComponentRewriter1_20_5.DataConverter<T> converter) {
        this.rewriters.put(key, converter);
    }

    public SerializerVersion serializerVersion() {
        return SerializerVersion.V1_20_5;
    }

    @FunctionalInterface
    protected interface DataConverter<T> {
        Tag convert(T var1);
    }
}
