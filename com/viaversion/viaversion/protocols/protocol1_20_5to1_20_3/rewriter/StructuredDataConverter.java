package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.rewriter;

import com.google.common.base.Preconditions;
import com.viaversion.viaversion.api.minecraft.GameProfile;
import com.viaversion.viaversion.api.minecraft.HolderSet;
import com.viaversion.viaversion.api.minecraft.SoundEvent;
import com.viaversion.viaversion.api.minecraft.data.StructuredData;
import com.viaversion.viaversion.api.minecraft.data.StructuredDataKey;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.minecraft.item.data.AdventureModePredicate;
import com.viaversion.viaversion.api.minecraft.item.data.ArmorTrimMaterial;
import com.viaversion.viaversion.api.minecraft.item.data.ArmorTrimPattern;
import com.viaversion.viaversion.api.minecraft.item.data.AttributeModifier;
import com.viaversion.viaversion.api.minecraft.item.data.BannerPattern;
import com.viaversion.viaversion.api.minecraft.item.data.BannerPatternLayer;
import com.viaversion.viaversion.api.minecraft.item.data.Bee;
import com.viaversion.viaversion.api.minecraft.item.data.BlockPredicate;
import com.viaversion.viaversion.api.minecraft.item.data.Enchantments;
import com.viaversion.viaversion.api.minecraft.item.data.FilterableComponent;
import com.viaversion.viaversion.api.minecraft.item.data.FilterableString;
import com.viaversion.viaversion.api.minecraft.item.data.FireworkExplosion;
import com.viaversion.viaversion.api.minecraft.item.data.FoodEffect;
import com.viaversion.viaversion.api.minecraft.item.data.Instrument;
import com.viaversion.viaversion.api.minecraft.item.data.PotionEffect;
import com.viaversion.viaversion.api.minecraft.item.data.PotionEffectData;
import com.viaversion.viaversion.api.minecraft.item.data.StatePropertyMatcher;
import com.viaversion.viaversion.api.minecraft.item.data.SuspiciousStewEffect;
import com.viaversion.viaversion.api.minecraft.item.data.ToolRule;
import com.viaversion.viaversion.libs.fastutil.ints.Int2IntMap;
import com.viaversion.viaversion.libs.fastutil.ints.Int2ObjectMap;
import com.viaversion.viaversion.libs.fastutil.objects.Reference2ObjectOpenHashMap;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.FloatTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Attributes1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.BannerPatterns1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Enchantments1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.EquipmentSlots1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Instruments1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.MapDecorations1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.PotionEffects1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.Potions1_20_5;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.data.TrimMaterials1_20_3;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.UUIDUtil;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class StructuredDataConverter {
    static final int HIDE_ENCHANTMENTS = 1;
    static final int HIDE_ATTRIBUTES = 2;
    static final int HIDE_UNBREAKABLE = 4;
    static final int HIDE_CAN_DESTROY = 8;
    static final int HIDE_CAN_PLACE_ON = 16;
    static final int HIDE_ADDITIONAL = 32;
    static final int HIDE_DYE_COLOR = 64;
    static final int HIDE_ARMOR_TRIM = 128;
    private static final String BACKUP_TAG_KEY = "VV|DataComponents";
    private static final String ITEM_BACKUP_TAG_KEY = "VV|Id";
    private final Map<StructuredDataKey<?>, StructuredDataConverter.DataConverter<?>> rewriters = new Reference2ObjectOpenHashMap<>();
    private final boolean backupInconvertibleData;

    public StructuredDataConverter(boolean backupInconvertibleData) {
        this.backupInconvertibleData = backupInconvertibleData;
        this.register(StructuredDataKey.CUSTOM_DATA, (data, tag) -> {});
        this.register(StructuredDataKey.DAMAGE, (data, tag) -> tag.putInt("Damage", data));
        this.register(StructuredDataKey.UNBREAKABLE, (data, tag) -> {
            tag.putBoolean("Unbreakable", true);
            if (!data.showInTooltip()) {
                this.putHideFlag(tag, 4);
            }
        });
        this.register(
            StructuredDataKey.CUSTOM_NAME,
            (data, tag) -> getDisplayTag(tag).putString("Name", ComponentUtil.tagToJsonString(data))
        );
        this.register(StructuredDataKey.ITEM_NAME, (data, tag) -> {
            CompoundTag displayTag = getDisplayTag(tag);
            if (!displayTag.contains("Name")) {
                CompoundTag name = new CompoundTag();
                name.putBoolean("italic", false);
                name.putString("text", "");
                name.put("extra", new ListTag<>(Collections.singletonList(data)));
                displayTag.putString("Name", ComponentUtil.tagToJsonString(name));
            }
        });
        this.register(StructuredDataKey.LORE, (data, tag) -> {
            ListTag<StringTag> lore = new ListTag<>(StringTag.class);

            for (Tag loreEntry : data) {
                lore.add(new StringTag(ComponentUtil.tagToJsonString(loreEntry)));
            }

            getDisplayTag(tag).put("Lore", lore);
        });
        this.register(StructuredDataKey.ENCHANTMENTS, (data, tag) -> this.convertEnchantments(data, tag, false));
        this.register(StructuredDataKey.STORED_ENCHANTMENTS, (data, tag) -> this.convertEnchantments(data, tag, true));
        this.register(
            StructuredDataKey.ATTRIBUTE_MODIFIERS,
            (data, tag) -> {
                ListTag<CompoundTag> modifiers = new ListTag<>(CompoundTag.class);

                for (int i = 0; i < data.modifiers().length; i++) {
                    AttributeModifier modifier = data.modifiers()[i];
                    String identifier = Attributes1_20_5.idToKey(modifier.attribute());
                    if (identifier != null) {
                        CompoundTag modifierTag = new CompoundTag();
                        modifierTag.putString(
                            "AttributeName",
                            identifier.equals("generic.jump_strength") ? "horse.jump_strength" : identifier
                        );
                        modifierTag.putString("Name", modifier.modifier().name());
                        modifierTag.putDouble("Amount", modifier.modifier().amount());
                        if (modifier.slotType() != 0) {
                            modifierTag.putString("Slot", EquipmentSlots1_20_5.idToKey(modifier.slotType()));
                        }

                        modifierTag.putInt("Operation", modifier.modifier().operation());
                        modifiers.add(modifierTag);
                    }
                }

                tag.put("AttributeModifiers", modifiers);
                if (!data.showInTooltip()) {
                    this.putHideFlag(tag, 2);
                }
            }
        );
        this.register(StructuredDataKey.CUSTOM_MODEL_DATA, (data, tag) -> tag.putInt("CustomModelData", data));
        this.register(StructuredDataKey.HIDE_ADDITIONAL_TOOLTIP, (data, tag) -> this.putHideFlag(tag, 32));
        this.register(StructuredDataKey.REPAIR_COST, (data, tag) -> tag.putInt("RepairCost", data));
        this.register(StructuredDataKey.DYED_COLOR, (data, tag) -> {
            getDisplayTag(tag).putInt("color", data.rgb());
            if (!data.showInTooltip()) {
                this.putHideFlag(tag, 64);
            }
        });
        this.register(StructuredDataKey.MAP_COLOR, (data, tag) -> getDisplayTag(tag).putInt("MapColor", data));
        this.register(StructuredDataKey.MAP_ID, (data, tag) -> tag.putInt("map", data));
        this.register(StructuredDataKey.MAP_DECORATIONS, (data, tag) -> {
            ListTag<CompoundTag> decorations = new ListTag<>(CompoundTag.class);

            for (Entry<String, Tag> entry : data.entrySet()) {
                CompoundTag decorationTag = (CompoundTag)entry.getValue();
                int id = MapDecorations1_20_5.keyToId(decorationTag.getString("type"));
                if (id != -1) {
                    CompoundTag convertedDecoration = new CompoundTag();
                    convertedDecoration.putString("id", entry.getKey());
                    convertedDecoration.putInt("type", id);
                    convertedDecoration.putDouble("x", decorationTag.getDouble("x"));
                    convertedDecoration.putDouble("z", decorationTag.getDouble("z"));
                    convertedDecoration.putFloat("rot", decorationTag.getFloat("rotation"));
                    decorations.add(convertedDecoration);
                }
            }

            tag.put("Decorations", decorations);
        });
        this.register(StructuredDataKey.WRITABLE_BOOK_CONTENT, (data, tag) -> {
            ListTag<StringTag> pages = new ListTag<>(StringTag.class);
            CompoundTag filteredPages = new CompoundTag();

            for (int i = 0; i < data.length; i++) {
                FilterableString page = data[i];
                pages.add(new StringTag(page.raw()));
                if (page.filtered() != null) {
                    filteredPages.putString(Integer.toString(i), page.filtered());
                }
            }

            tag.put("pages", pages);
            tag.put("filtered_pages", filteredPages);
        });
        this.register(StructuredDataKey.WRITTEN_BOOK_CONTENT, (data, tag) -> {
            ListTag<StringTag> pages = new ListTag<>(StringTag.class);
            CompoundTag filteredPages = new CompoundTag();

            for (int i = 0; i < data.pages().length; i++) {
                FilterableComponent page = data.pages()[i];
                pages.add(new StringTag(ComponentUtil.tagToJsonString(page.raw())));
                if (page.filtered() != null) {
                    filteredPages.putString(Integer.toString(i), ComponentUtil.tagToJsonString(page.filtered()));
                }
            }

            tag.put("pages", pages);
            tag.put("filtered_pages", filteredPages);
            tag.putString("author", data.author());
            tag.putInt("generation", data.generation());
            tag.putBoolean("resolved", data.resolved());
            tag.putString("title", data.title().raw());
            if (data.title().filtered() != null) {
                tag.putString("filtered_title", data.title().filtered());
            }
        });
        this.register(StructuredDataKey.BASE_COLOR, (data, tag) -> tag.putInt("Base", data));
        this.register(
            StructuredDataKey.CHARGED_PROJECTILES, (data, tag) -> this.convertItemList(data, tag, "ChargedProjectiles")
        );
        this.register(StructuredDataKey.BUNDLE_CONTENTS, (data, tag) -> this.convertItemList(data, tag, "Items"));
        this.register(StructuredDataKey.LODESTONE_TRACKER, (data, tag) -> {
            CompoundTag positionTag = new CompoundTag();
            tag.put("LodestonePos", positionTag);
            tag.putBoolean("LodestoneTracked", data.tracked());
            tag.putString("LodestoneDimension", data.pos().dimension());
            positionTag.putInt("X", data.pos().x());
            positionTag.putInt("Y", data.pos().y());
            positionTag.putInt("Z", data.pos().z());
        });
        this.register(StructuredDataKey.FIREWORKS, (data, tag) -> {
            CompoundTag fireworksTag = new CompoundTag();
            fireworksTag.putInt("Flight", data.flightDuration());
            tag.put("Fireworks", fireworksTag);
            ListTag<CompoundTag> explosionsTag = new ListTag<>(CompoundTag.class);

            for (FireworkExplosion explosion : data.explosions()) {
                explosionsTag.add(this.convertExplosion(explosion));
            }

            fireworksTag.put("Explosions", explosionsTag);
        });
        this.register(StructuredDataKey.FIREWORK_EXPLOSION, (data, tag) -> {
            CompoundTag var10000 = tag.put("Explosion", this.convertExplosion(data));
        });
        this.register(StructuredDataKey.PROFILE, (data, tag) -> {
            if (data.name() != null && data.id() == null && data.properties().length == 0) {
                tag.putString("SkullOwner", data.name());
            } else {
                CompoundTag profileTag = new CompoundTag();
                tag.put("SkullOwner", profileTag);
                if (data.name() != null) {
                    profileTag.putString("Name", data.name());
                }

                if (data.id() != null) {
                    profileTag.put("Id", new IntArrayTag(UUIDUtil.toIntArray(data.id())));
                }

                CompoundTag propertiesTag = new CompoundTag();

                for (GameProfile.Property property : data.properties()) {
                    ListTag<CompoundTag> values = new ListTag<>(CompoundTag.class);
                    CompoundTag propertyTag = new CompoundTag();
                    propertyTag.putString("Value", property.value());
                    if (property.signature() != null) {
                        propertyTag.putString("Signature", property.signature());
                    }

                    values.add(propertyTag);
                    propertiesTag.put(property.name(), values);
                }
            }
        });
        this.register(StructuredDataKey.INSTRUMENT, (data, tag) -> {
            if (!data.hasId()) {
                if (backupInconvertibleData) {
                    CompoundTag backupTag = new CompoundTag();
                    Instrument instrument = data.value();
                    if (instrument.soundEvent().hasId()) {
                        backupTag.putInt("sound_event", instrument.soundEvent().id());
                    } else {
                        CompoundTag soundEventTag = new CompoundTag();
                        SoundEvent soundEvent = instrument.soundEvent().value();
                        soundEventTag.putString("identifier", soundEvent.identifier());
                        if (soundEvent.fixedRange() != null) {
                            soundEventTag.putFloat("fixed_range", soundEvent.fixedRange());
                        }

                        backupTag.put("sound_event", soundEventTag);
                    }

                    backupTag.putInt("use_duration", instrument.useDuration());
                    backupTag.putFloat("range", instrument.range());
                    getBackupTag(tag).put("instrument", backupTag);
                }
            } else {
                String identifier = Instruments1_20_3.idToKey(data.id());
                if (identifier != null) {
                    tag.putString("instrument", identifier);
                }
            }
        });
        this.register(StructuredDataKey.BEES, (data, tag) -> {
            ListTag<CompoundTag> bees = new ListTag<>(CompoundTag.class);

            for (Bee bee : data) {
                CompoundTag beeTag = new CompoundTag();
                beeTag.put("EntityData", bee.entityData());
                beeTag.putInt("TicksInHive", bee.ticksInHive());
                beeTag.putInt("MinOccupationTicks", bee.minTicksInHive());
                bees.add(beeTag);
            }

            getBlockEntityTag(tag).put("Bees", bees);
        });
        this.register(StructuredDataKey.LOCK, (data, tag) -> getBlockEntityTag(tag).put("Lock", data));
        this.register(
            StructuredDataKey.NOTE_BLOCK_SOUND,
            (data, tag) -> getBlockEntityTag(tag).putString("note_block_sound", data)
        );
        this.register(StructuredDataKey.POT_DECORATIONS, (data, tag) -> {
            IntArrayTag originalSherds = null;
            ListTag<StringTag> sherds = new ListTag<>(StringTag.class);

            for (int id : data.itemIds()) {
                String name = this.toMappedItemName(id);
                if (name.isEmpty()) {
                    if (backupInconvertibleData && originalSherds == null) {
                        originalSherds = new IntArrayTag(data.itemIds());
                    }
                } else {
                    sherds.add(new StringTag(name));
                }
            }

            if (originalSherds != null) {
                getBackupTag(tag).put("pot_decorations", originalSherds);
            }

            getBlockEntityTag(tag).put("sherds", sherds);
        });
        this.register(StructuredDataKey.CREATIVE_SLOT_LOCK, (data, tag) -> {
            CompoundTag var10000 = tag.put("CustomCreativeLock", new CompoundTag());
        });
        this.register(StructuredDataKey.DEBUG_STICK_STATE, (data, tag) -> {
            CompoundTag var10000 = tag.put("DebugProperty", data);
        });
        this.register(StructuredDataKey.RECIPES, (data, tag) -> tag.put("Recipes", data));
        this.register(StructuredDataKey.ENTITY_DATA, (data, tag) -> {
            CompoundTag var10000 = tag.put("EntityTag", data);
        });
        this.register(StructuredDataKey.BUCKET_ENTITY_DATA, (data, tag) -> {
            for (String mobTagName : BlockItemPacketRewriter1_20_5.MOB_TAGS) {
                if (data.contains(mobTagName)) {
                    tag.put(mobTagName, data.get(mobTagName));
                }
            }
        });
        this.register(StructuredDataKey.BLOCK_ENTITY_DATA, (data, tag) -> tag.put("BlockEntityTag", data));
        this.register(StructuredDataKey.CONTAINER_LOOT, (data, tag) -> {
            Tag lootTable = data.get("loot_table");
            if (lootTable != null) {
                tag.put("LootTable", lootTable);
            }

            Tag lootTableSeed = data.get("loot_table_seed");
            if (lootTableSeed != null) {
                tag.put("LootTableSeed", lootTableSeed);
            }
        });
        this.register(StructuredDataKey.ENCHANTMENT_GLINT_OVERRIDE, (data, tag) -> {
            if (backupInconvertibleData) {
                getBackupTag(tag).putBoolean("enchantment_glint_override", data);
            }

            if (data) {
                ListTag<CompoundTag> enchantmentsTag = tag.getListTag("Enchantments", CompoundTag.class);
                if (enchantmentsTag == null) {
                    enchantmentsTag = new ListTag<>(CompoundTag.class);
                    tag.put("Enchantments", enchantmentsTag);
                }

                CompoundTag invalidEnchantment = new CompoundTag();
                invalidEnchantment.putString("id", "");
                enchantmentsTag.add(invalidEnchantment);
            }
        });
        this.register(StructuredDataKey.POTION_CONTENTS, (data, tag) -> {
            if (data.potion() != null) {
                String potion = Potions1_20_5.idToKey(data.potion());
                if (potion != null) {
                    tag.putString("Potion", potion);
                }
            }

            if (data.customColor() != null) {
                tag.putInt("CustomPotionColor", data.customColor());
            }

            ListTag<CompoundTag> customPotionEffectsTag = new ListTag<>(CompoundTag.class);

            for (PotionEffect effect : data.customEffects()) {
                CompoundTag effectTag = new CompoundTag();
                String id = PotionEffects1_20_5.idToKey(effect.effect());
                if (id != null) {
                    effectTag.putString("id", id);
                }

                PotionEffectData details = effect.effectData();
                effectTag.putByte("amplifier", (byte)details.amplifier());
                effectTag.putInt("duration", details.duration());
                effectTag.putBoolean("ambient", details.ambient());
                effectTag.putBoolean("show_particles", details.showParticles());
                effectTag.putBoolean("show_icon", details.showIcon());
                customPotionEffectsTag.add(effectTag);
            }

            tag.put("custom_potion_effects", customPotionEffectsTag);
        });
        this.register(StructuredDataKey.SUSPICIOUS_STEW_EFFECTS, (data, tag) -> {
            ListTag<CompoundTag> effectsTag = new ListTag<>(CompoundTag.class);

            for (SuspiciousStewEffect effect : data) {
                CompoundTag effectTag = new CompoundTag();
                String id = PotionEffects1_20_5.idToKey(effect.mobEffect());
                if (id != null) {
                    effectTag.putString("id", id);
                }

                effectTag.putInt("duration", effect.duration());
                effectsTag.add(effectTag);
            }

            tag.put("effects", effectsTag);
        });
        this.register(
            StructuredDataKey.BANNER_PATTERNS,
            (data, tag) -> {
                if (backupInconvertibleData
                    && Arrays.stream(data)
                        .anyMatch(
                            layerx -> layerx.pattern().isDirect()
                                || BannerPatterns1_20_5.idToKey(layerx.pattern().id()) == null
                        )) {
                    ListTag<CompoundTag> originalPatterns = new ListTag<>(CompoundTag.class);

                    for (BannerPatternLayer layer : data) {
                        CompoundTag layerTag = new CompoundTag();
                        CompoundTag patternTag = new CompoundTag();
                        BannerPattern pattern = layer.pattern().value();
                        patternTag.putString("asset_id", pattern.assetId());
                        patternTag.putString("translation_key", pattern.translationKey());
                        layerTag.put("pattern", patternTag);
                        layerTag.putInt("dye_color", layer.dyeColor());
                        originalPatterns.add(layerTag);
                    }

                    getBackupTag(tag).put("banner_patterns", originalPatterns);
                }

                ListTag<CompoundTag> patternsTag = new ListTag<>(CompoundTag.class);

                for (BannerPatternLayer layer : data) {
                    if (!layer.pattern().isDirect()) {
                        String key = BannerPatterns1_20_5.idToKey(layer.pattern().id());
                        if (key != null) {
                            String compactKey = BannerPatterns1_20_5.fullIdToCompact(key);
                            CompoundTag patternTag = new CompoundTag();
                            patternTag.putString("Pattern", compactKey);
                            patternTag.putInt("Color", layer.dyeColor());
                            patternsTag.add(patternTag);
                        }
                    }
                }

                tag.put("Patterns", patternsTag);
            }
        );
        this.register(StructuredDataKey.CONTAINER, (data, tag) -> this.convertItemList(data, tag, "Items"));
        this.register(
            StructuredDataKey.CAN_PLACE_ON, (data, tag) -> this.convertBlockPredicates(tag, data, "CanPlaceOn", 16)
        );
        this.register(
            StructuredDataKey.CAN_BREAK, (data, tag) -> this.convertBlockPredicates(tag, data, "CanDestroy", 8)
        );
        this.register(StructuredDataKey.MAP_POST_PROCESSING, (data, tag) -> {
            if (data != null) {
                if (data == 0) {
                    tag.putBoolean("map_to_lock", true);
                } else if (data == 1) {
                    tag.putInt("map_scale_direction", 1);
                }
            }
        });
        this.register(
            StructuredDataKey.TRIM,
            (data, tag) -> {
                CompoundTag trimTag = new CompoundTag();
                if (data.material().isDirect()) {
                    CompoundTag materialTag = new CompoundTag();
                    ArmorTrimMaterial material = data.material().value();
                    materialTag.putString("asset_name", material.assetName());
                    String ingredientName = this.toMappedItemName(material.itemId());
                    if (backupInconvertibleData && ingredientName.isEmpty()) {
                        getBackupTag(materialTag).putInt("VV|Id", material.itemId());
                    }

                    materialTag.putString("ingredient", ingredientName);
                    materialTag.put("item_model_index", new FloatTag(material.itemModelIndex()));
                    CompoundTag overrideArmorMaterials = new CompoundTag();
                    if (!material.overrideArmorMaterials().isEmpty()) {
                        for (Int2ObjectMap.Entry<String> entry : material.overrideArmorMaterials().int2ObjectEntrySet()) {
                            overrideArmorMaterials.put(
                                Integer.toString(entry.getIntKey()), new StringTag(entry.getValue())
                            );
                        }

                        materialTag.put("override_armor_materials", overrideArmorMaterials);
                    }

                    materialTag.put("description", material.description());
                    trimTag.put("material", materialTag);
                } else {
                    String oldKey = TrimMaterials1_20_3.idToKey(data.material().id());
                    if (oldKey != null) {
                        trimTag.putString("material", oldKey);
                    }
                }

                if (data.pattern().isDirect()) {
                    CompoundTag patternTag = new CompoundTag();
                    ArmorTrimPattern pattern = data.pattern().value();
                    patternTag.putString("assetId", pattern.assetName());
                    String itemName = this.toMappedItemName(pattern.itemId());
                    if (backupInconvertibleData && itemName.isEmpty()) {
                        getBackupTag(patternTag).putInt("VV|Id", pattern.itemId());
                    }

                    patternTag.putString("templateItem", itemName);
                    patternTag.put("description", pattern.description());
                    patternTag.putBoolean("decal", pattern.decal());
                    trimTag.put("pattern", patternTag);
                } else {
                    String oldKey = TrimMaterials1_20_3.idToKey(data.pattern().id());
                    if (oldKey != null) {
                        trimTag.putString("pattern", oldKey);
                    }
                }

                tag.put("Trim", trimTag);
                if (!data.showInTooltip()) {
                    this.putHideFlag(tag, 128);
                }
            }
        );
        this.register(StructuredDataKey.BLOCK_STATE, (data, tag) -> {
            CompoundTag blockStateTag = new CompoundTag();
            tag.put("BlockStateTag", blockStateTag);

            for (Entry<String, String> entry : data.properties().entrySet()) {
                blockStateTag.putString(entry.getKey(), entry.getValue());
            }
        });
        this.register(StructuredDataKey.HIDE_TOOLTIP, (data, tag) -> {
            this.putHideFlag(tag, 255);
            if (backupInconvertibleData) {
                getBackupTag(tag).putBoolean("hide_tooltip", true);
            }
        });
        this.register(StructuredDataKey.INTANGIBLE_PROJECTILE, (data, tag) -> {
            if (backupInconvertibleData) {
                getBackupTag(tag).put("intangible_projectile", data);
            }
        });
        this.register(StructuredDataKey.MAX_STACK_SIZE, (data, tag) -> {
            if (backupInconvertibleData) {
                getBackupTag(tag).putInt("max_stack_size", data);
            }
        });
        this.register(StructuredDataKey.MAX_DAMAGE, (data, tag) -> {
            if (backupInconvertibleData) {
                getBackupTag(tag).putInt("max_damage", data);
            }
        });
        this.register(StructuredDataKey.RARITY, (data, tag) -> {
            if (backupInconvertibleData) {
                getBackupTag(tag).putInt("rarity", data);
            }
        });
        this.register(StructuredDataKey.FOOD, (data, tag) -> {
            if (backupInconvertibleData) {
                CompoundTag backupTag = new CompoundTag();
                backupTag.putInt("nutrition", data.nutrition());
                backupTag.putFloat("saturation_modifier", data.saturationModifier());
                backupTag.putBoolean("can_always_eat", data.canAlwaysEat());
                backupTag.putFloat("eat_seconds", data.eatSeconds());
                ListTag<CompoundTag> possibleEffectsTag = new ListTag<>(CompoundTag.class);

                for (FoodEffect effect : data.possibleEffects()) {
                    CompoundTag effectTag = new CompoundTag();
                    PotionEffect potionEffect = effect.effect();
                    CompoundTag potionEffectTag = new CompoundTag();
                    potionEffectTag.putInt("effect", potionEffect.effect());
                    potionEffectTag.put("effect_data", this.convertPotionEffectData(potionEffect.effectData()));
                    effectTag.putFloat("probability", effect.probability());
                    effectTag.put("effect", potionEffectTag);
                    possibleEffectsTag.add(effectTag);
                }

                backupTag.put("possible_effects", possibleEffectsTag);
                getBackupTag(tag).put("food", backupTag);
            }
        });
        this.register(StructuredDataKey.FIRE_RESISTANT, (data, tag) -> {
            if (backupInconvertibleData) {
                getBackupTag(tag).putBoolean("fire_resistant", true);
            }
        });
        this.register(StructuredDataKey.TOOL, (data, tag) -> {
            if (backupInconvertibleData) {
                CompoundTag backupTag = new CompoundTag();
                ListTag<CompoundTag> rulesTag = new ListTag<>(CompoundTag.class);

                for (ToolRule rule : data.rules()) {
                    CompoundTag ruleTag = new CompoundTag();
                    HolderSet set = rule.blocks();
                    if (set.hasTagKey()) {
                        ruleTag.putString("blocks", set.tagKey());
                    } else {
                        ruleTag.put("blocks", new IntArrayTag(set.ids()));
                    }

                    if (rule.speed() != null) {
                        ruleTag.putFloat("speed", rule.speed());
                    }

                    if (rule.correctForDrops() != null) {
                        ruleTag.putBoolean("correct_for_drops", rule.correctForDrops());
                    }

                    rulesTag.add(ruleTag);
                }

                backupTag.put("rules", rulesTag);
                backupTag.putFloat("default_mining_speed", data.defaultMiningSpeed());
                backupTag.putInt("damage_per_block", data.damagePerBlock());
                getBackupTag(tag).put("tool", backupTag);
            }
        });
        this.register(StructuredDataKey.OMINOUS_BOTTLE_AMPLIFIER, (data, tag) -> {
            if (backupInconvertibleData) {
                getBackupTag(tag).putInt("ominous_bottle_amplifier", data);
            }
        });
    }

    private int unmappedItemId(int id) {
        return Protocol1_20_5To1_20_3.MAPPINGS.getOldItemId(id);
    }

    private String toMappedItemName(int id) {
        int mappedId = this.unmappedItemId(id);
        return mappedId != -1 ? Protocol1_20_5To1_20_3.MAPPINGS.getFullItemMappings().identifier(mappedId) : "";
    }

    private static CompoundTag getBlockEntityTag(CompoundTag tag) {
        return getOrCreate(tag, "BlockEntityTag");
    }

    private static CompoundTag getDisplayTag(CompoundTag tag) {
        return getOrCreate(tag, "display");
    }

    private static CompoundTag getBackupTag(CompoundTag tag) {
        return getOrCreate(tag, "VV|DataComponents");
    }

    private static CompoundTag getOrCreate(CompoundTag tag, String key) {
        CompoundTag subTag = tag.getCompoundTag(key);
        if (subTag == null) {
            subTag = new CompoundTag();
            tag.put(key, subTag);
        }

        return subTag;
    }

    static @Nullable CompoundTag removeBackupTag(CompoundTag tag) {
        CompoundTag backupTag = tag.getCompoundTag("VV|DataComponents");
        if (backupTag != null) {
            tag.remove("VV|DataComponents");
        }

        return backupTag;
    }

    static int removeItemBackupTag(CompoundTag tag, int unmappedId) {
        if (unmappedId != -1) {
            return unmappedId;
        } else {
            IntTag itemBackupTag = tag.getIntTag("VV|Id");
            if (itemBackupTag != null) {
                tag.remove("VV|Id");
                return itemBackupTag.asInt();
            } else {
                return -1;
            }
        }
    }

    private void convertBlockPredicates(CompoundTag tag, AdventureModePredicate data, String key, int hideFlag) {
        ListTag<StringTag> predicatedListTag = new ListTag<>(StringTag.class);

        for (BlockPredicate predicate : data.predicates()) {
            HolderSet holders = predicate.holderSet();
            if (holders == null) {
                if (this.backupInconvertibleData) {
                }
            } else if (holders.hasTagKey()) {
                String tagKey = "#" + holders.tagKey();
                predicatedListTag.add(this.serializeBlockPredicate(predicate, tagKey));
            } else {
                for (int id : holders.ids()) {
                    String name = this.toMappedItemName(id);
                    if (name.isEmpty()) {
                        if (this.backupInconvertibleData) {
                        }
                    } else {
                        predicatedListTag.add(this.serializeBlockPredicate(predicate, name));
                    }
                }
            }
        }

        tag.put(key, predicatedListTag);
        if (!data.showInTooltip()) {
            this.putHideFlag(tag, hideFlag);
        }
    }

    private StringTag serializeBlockPredicate(BlockPredicate predicate, String identifier) {
        StringBuilder builder = new StringBuilder(identifier);
        if (predicate.propertyMatchers() != null) {
            for (StatePropertyMatcher matcher : predicate.propertyMatchers()) {
                if (matcher.matcher().isLeft()) {
                    builder.append(matcher.name()).append('=');
                    builder.append(matcher.matcher().left());
                }
            }
        }

        if (predicate.tag() != null) {
            builder.append(predicate.tag());
        }

        return new StringTag(builder.toString());
    }

    private CompoundTag convertExplosion(FireworkExplosion explosion) {
        CompoundTag explosionTag = new CompoundTag();
        explosionTag.putInt("Type", explosion.shape());
        explosionTag.put("Colors", new IntArrayTag((int[])explosion.colors().clone()));
        explosionTag.put("FadeColors", new IntArrayTag((int[])explosion.fadeColors().clone()));
        explosionTag.putBoolean("Trail", explosion.hasTrail());
        explosionTag.putBoolean("Flicker", explosion.hasTwinkle());
        return explosionTag;
    }

    private CompoundTag convertPotionEffectData(PotionEffectData data) {
        CompoundTag effectDataTag = new CompoundTag();
        effectDataTag.putInt("amplifier", data.amplifier());
        effectDataTag.putInt("duration", data.duration());
        effectDataTag.putBoolean("ambient", data.ambient());
        effectDataTag.putBoolean("show_particles", data.showParticles());
        effectDataTag.putBoolean("show_icon", data.showIcon());
        if (data.hiddenEffect() != null) {
            effectDataTag.put("hidden_effect", this.convertPotionEffectData(data.hiddenEffect()));
        }

        return effectDataTag;
    }

    private void convertItemList(Item[] items, CompoundTag tag, String key) {
        ListTag<CompoundTag> itemsTag = new ListTag<>(CompoundTag.class);

        for (Item item : items) {
            CompoundTag savedItem = new CompoundTag();
            String name = this.toMappedItemName(item.identifier());
            savedItem.putString("id", name);
            if (this.backupInconvertibleData && name.isEmpty()) {
                savedItem.putInt("VV|Id", item.identifier());
            }

            savedItem.putByte("Count", (byte)item.amount());
            CompoundTag itemTag = new CompoundTag();

            for (StructuredData<?> data : item.structuredData().data().values()) {
                this.writeToTag(data, itemTag);
            }

            savedItem.put("tag", itemTag);
            itemsTag.add(savedItem);
        }

        tag.put(key, itemsTag);
    }

    private void convertEnchantments(Enchantments data, CompoundTag tag, boolean storedEnchantments) {
        ListTag<CompoundTag> enchantments = new ListTag<>(CompoundTag.class);

        for (Int2IntMap.Entry entry : data.enchantments().int2IntEntrySet()) {
            int enchantmentId = entry.getIntKey();
            String identifier = Enchantments1_20_5.idToKey(enchantmentId);
            if (identifier != null
                && !identifier.equals("density")
                && !identifier.equals("breach")
                && !identifier.equals("wind_burst")) {
                if (identifier.equals("sweeping_edge")) {
                    identifier = "sweeping";
                }

                CompoundTag enchantment = new CompoundTag();
                enchantment.putString("id", identifier);
                enchantment.putShort("lvl", (short)entry.getIntValue());
                enchantments.add(enchantment);
            }
        }

        tag.put(storedEnchantments ? "StoredEnchantments" : "Enchantments", enchantments);
        if (!data.showInTooltip()) {
            this.putHideFlag(tag, storedEnchantments ? 32 : 1);
        }
    }

    private void putHideFlag(CompoundTag tag, int value) {
        tag.putInt("HideFlags", tag.getInt("HideFlags") | value);
    }

    public <T> void writeToTag(StructuredData<T> data, CompoundTag tag) {
        if (!data.isEmpty()) {
            StructuredDataConverter.DataConverter<T> converter = (StructuredDataConverter.DataConverter<T>)this.rewriters
                .get(data.key());
            Preconditions.checkNotNull(converter, "No converter for %s found", new Object[]{data.key()});
            converter.convert(data.value(), tag);
        }
    }

    private <T> void register(StructuredDataKey<T> key, StructuredDataConverter.DataConverter<T> converter) {
        this.rewriters.put(key, converter);
    }

    @FunctionalInterface
    interface DataConverter<T> {
        void convert(T var1, CompoundTag var2);
    }
}
