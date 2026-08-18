package com.viaversion.viaversion.api.data;

public interface Mappings {
    int getNewId(int var1);

    default int getNewIdOrDefault(int id, int def) {
        int mappedId = this.getNewId(id);
        return mappedId != -1 ? mappedId : def;
    }

    default boolean contains(int id) {
        return this.getNewId(id) != -1;
    }

    void setNewId(int var1, int var2);

    int size();

    int mappedSize();

    Mappings inverse();
}
