package com.viaversion.viaversion.api.minecraft.item;

import com.viaversion.viaversion.api.minecraft.data.StructuredDataContainer;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import org.checkerframework.checker.nullness.qual.Nullable;

public class StructuredItem implements Item {
    private final StructuredDataContainer data;
    private int identifier;
    private int amount;

    public StructuredItem(int identifier, int amount) {
        this(identifier, amount, new StructuredDataContainer());
    }

    public StructuredItem(int identifier, int amount, StructuredDataContainer data) {
        this.identifier = identifier;
        this.amount = amount;
        this.data = data;
    }

    @Override
    public int identifier() {
        return this.identifier;
    }

    @Override
    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    @Override
    public int amount() {
        return this.amount;
    }

    @Override
    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public @Nullable CompoundTag tag() {
        return null;
    }

    @Override
    public void setTag(@Nullable CompoundTag tag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StructuredDataContainer structuredData() {
        return this.data;
    }

    public StructuredItem copy() {
        return new StructuredItem(this.identifier, this.amount, this.data.copy());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o != null && this.getClass() == o.getClass()) {
            StructuredItem that = (StructuredItem)o;
            if (this.identifier != that.identifier) {
                return false;
            } else {
                return this.amount != that.amount ? false : this.data.equals(that.data);
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        int result = this.data.hashCode();
        result = 31 * result + this.identifier;
        return 31 * result + this.amount;
    }

    @Override
    public String toString() {
        return "StructuredItem{data=" + this.data + ", identifier=" + this.identifier + ", amount=" + this.amount + '}';
    }
}
