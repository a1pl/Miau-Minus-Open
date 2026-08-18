package com.viaversion.viaversion.api.data;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.minecraft.RegistryType;
import com.viaversion.viaversion.api.minecraft.TagData;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

public class MappingDataBase implements MappingData {
    protected final String unmappedVersion;
    protected final String mappedVersion;
    protected BiMappings itemMappings;
    protected FullMappings argumentTypeMappings;
    protected FullMappings entityMappings;
    protected FullMappings recipeSerializerMappings;
    protected FullMappings itemDataSerializerMappings;
    protected ParticleMappings particleMappings;
    protected Mappings blockMappings;
    protected Mappings blockStateMappings;
    protected Mappings blockEntityMappings;
    protected Mappings soundMappings;
    protected Mappings statisticsMappings;
    protected Mappings enchantmentMappings;
    protected Mappings paintingMappings;
    protected Mappings menuMappings;
    protected Mappings attributeMappings;
    protected Map<RegistryType, List<TagData>> tags;

    public MappingDataBase(String unmappedVersion, String mappedVersion) {
        this.unmappedVersion = unmappedVersion;
        this.mappedVersion = mappedVersion;
    }

    @Override
    public void load() {
        if (Via.getManager().isDebug()) {
            this.getLogger().info("Loading " + this.unmappedVersion + " -> " + this.mappedVersion + " mappings...");
        }

        CompoundTag data = this.readMappingsFile(
            "mappings-" + this.unmappedVersion + "to" + this.mappedVersion + ".nbt"
        );
        this.blockMappings = this.loadMappings(data, "blocks");
        this.blockStateMappings = this.loadMappings(data, "blockstates");
        this.blockEntityMappings = this.loadMappings(data, "blockentities");
        this.soundMappings = this.loadMappings(data, "sounds");
        this.statisticsMappings = this.loadMappings(data, "statistics");
        this.menuMappings = this.loadMappings(data, "menus");
        this.enchantmentMappings = this.loadMappings(data, "enchantments");
        this.paintingMappings = this.loadMappings(data, "paintings");
        this.attributeMappings = this.loadMappings(data, "attributes");
        CompoundTag unmappedIdentifierData = this.readUnmappedIdentifiersFile(
            "identifiers-" + this.unmappedVersion + ".nbt"
        );
        CompoundTag mappedIdentifierData = this.readMappedIdentifiersFile("identifiers-" + this.mappedVersion + ".nbt");
        if (unmappedIdentifierData != null && mappedIdentifierData != null) {
            this.itemMappings = this.loadFullMappings(data, unmappedIdentifierData, mappedIdentifierData, "items");
            this.entityMappings = this.loadFullMappings(data, unmappedIdentifierData, mappedIdentifierData, "entities");
            this.argumentTypeMappings = this.loadFullMappings(
                data, unmappedIdentifierData, mappedIdentifierData, "argumenttypes"
            );
            this.recipeSerializerMappings = this.loadFullMappings(
                data, unmappedIdentifierData, mappedIdentifierData, "recipe_serializers"
            );
            this.itemDataSerializerMappings = this.loadFullMappings(
                data, unmappedIdentifierData, mappedIdentifierData, "data_component_type"
            );
            List<String> unmappedParticles = this.identifiersFromGlobalIds(unmappedIdentifierData, "particles");
            List<String> mappedParticles = this.identifiersFromGlobalIds(mappedIdentifierData, "particles");
            if (unmappedParticles != null && mappedParticles != null) {
                Mappings particleMappings = this.loadMappings(data, "particles");
                if (particleMappings == null) {
                    particleMappings = new IdentityMappings(unmappedParticles.size(), mappedParticles.size());
                }

                this.particleMappings = new ParticleMappings(unmappedParticles, mappedParticles, particleMappings);
            }
        } else {
            this.itemMappings = this.loadBiMappings(data, "items");
        }

        CompoundTag tagsTag = data.getCompoundTag("tags");
        if (tagsTag != null) {
            this.tags = new EnumMap<>(RegistryType.class);
            this.loadTags(RegistryType.ITEM, tagsTag);
            this.loadTags(RegistryType.BLOCK, tagsTag);
        }

        this.loadExtras(data);
    }

    protected @Nullable List<String> identifiersFromGlobalIds(CompoundTag mappingsTag, String key) {
        return MappingDataLoader.INSTANCE.identifiersFromGlobalIds(mappingsTag, key);
    }

    protected @Nullable CompoundTag readMappingsFile(String name) {
        return MappingDataLoader.INSTANCE.loadNBT(name);
    }

    protected @Nullable CompoundTag readUnmappedIdentifiersFile(String name) {
        return MappingDataLoader.INSTANCE.loadNBT(name, true);
    }

    protected @Nullable CompoundTag readMappedIdentifiersFile(String name) {
        return MappingDataLoader.INSTANCE.loadNBT(name, true);
    }

    protected @Nullable Mappings loadMappings(CompoundTag data, String key) {
        return MappingDataLoader.INSTANCE.loadMappings(data, key);
    }

    protected @Nullable FullMappings loadFullMappings(
        CompoundTag data, CompoundTag unmappedIdentifiers, CompoundTag mappedIdentifiers, String key
    ) {
        return MappingDataLoader.INSTANCE.loadFullMappings(data, unmappedIdentifiers, mappedIdentifiers, key);
    }

    protected @Nullable BiMappings loadBiMappings(CompoundTag data, String key) {
        Mappings mappings = this.loadMappings(data, key);
        return mappings != null ? BiMappings.of(mappings) : null;
    }

    private void loadTags(RegistryType type, CompoundTag data) {
        CompoundTag tag = data.getCompoundTag(type.resourceLocation());
        if (tag != null) {
            List<TagData> tagsList = new ArrayList<>(this.tags.size());

            for (Entry<String, Tag> entry : tag.entrySet()) {
                IntArrayTag entries = (IntArrayTag)entry.getValue();
                tagsList.add(new TagData(entry.getKey(), entries.getValue()));
            }

            this.tags.put(type, tagsList);
        }
    }

    @Override
    public int getNewBlockStateId(int id) {
        return this.checkValidity(id, this.blockStateMappings.getNewId(id), "blockstate");
    }

    @Override
    public int getNewBlockId(int id) {
        return this.checkValidity(id, this.blockMappings.getNewId(id), "block");
    }

    @Override
    public int getNewItemId(int id) {
        return this.checkValidity(id, this.itemMappings.getNewId(id), "item");
    }

    @Override
    public int getOldItemId(int id) {
        return this.itemMappings.inverse().getNewIdOrDefault(id, 1);
    }

    @Override
    public int getNewParticleId(int id) {
        return this.checkValidity(id, this.particleMappings.getNewId(id), "particles");
    }

    @Override
    public int getNewAttributeId(int id) {
        return this.checkValidity(id, this.attributeMappings.getNewId(id), "attributes");
    }

    @Override
    public @Nullable List<TagData> getTags(RegistryType type) {
        return this.tags != null ? this.tags.get(type) : null;
    }

    @Override
    public @Nullable BiMappings getItemMappings() {
        return this.itemMappings;
    }

    @Override
    public @Nullable FullMappings getFullItemMappings() {
        return this.itemMappings instanceof FullMappings ? (FullMappings)this.itemMappings : null;
    }

    @Override
    public @Nullable ParticleMappings getParticleMappings() {
        return this.particleMappings;
    }

    @Override
    public @Nullable Mappings getBlockMappings() {
        return this.blockMappings;
    }

    @Override
    public @Nullable Mappings getBlockEntityMappings() {
        return this.blockEntityMappings;
    }

    @Override
    public @Nullable Mappings getBlockStateMappings() {
        return this.blockStateMappings;
    }

    @Override
    public @Nullable Mappings getSoundMappings() {
        return this.soundMappings;
    }

    @Override
    public @Nullable Mappings getStatisticsMappings() {
        return this.statisticsMappings;
    }

    @Override
    public @Nullable Mappings getMenuMappings() {
        return this.menuMappings;
    }

    @Override
    public @Nullable Mappings getEnchantmentMappings() {
        return this.enchantmentMappings;
    }

    @Override
    public @Nullable Mappings getAttributeMappings() {
        return this.attributeMappings;
    }

    @Override
    public @Nullable FullMappings getEntityMappings() {
        return this.entityMappings;
    }

    @Override
    public @Nullable FullMappings getArgumentTypeMappings() {
        return this.argumentTypeMappings;
    }

    @Override
    public @Nullable FullMappings getDataComponentSerializerMappings() {
        return this.itemDataSerializerMappings;
    }

    @Override
    public @Nullable Mappings getPaintingMappings() {
        return this.paintingMappings;
    }

    @Override
    public @Nullable FullMappings getRecipeSerializerMappings() {
        return this.recipeSerializerMappings;
    }

    protected Logger getLogger() {
        return Via.getPlatform().getLogger();
    }

    protected int checkValidity(int id, int mappedId, String type) {
        if (mappedId == -1) {
            if (!Via.getConfig().isSuppressConversionWarnings()) {
                this.getLogger()
                    .warning(
                        String.format(
                            "Missing %s %s for %s %s %d", this.mappedVersion, type, this.unmappedVersion, type, id
                        )
                    );
            }

            return 0;
        } else {
            return mappedId;
        }
    }

    protected void loadExtras(CompoundTag data) {
    }
}
