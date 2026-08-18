package com.viaversion.viaversion.libs.mcstructs.text.components;

import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import javax.annotation.Nullable;

public abstract class NbtComponent extends ATextComponent {
    private String component;
    private boolean resolve;
    private ATextComponent separator;

    public NbtComponent(String component, boolean resolve) {
        this(component, resolve, null);
    }

    public NbtComponent(String component, boolean resolve, @Nullable ATextComponent separator) {
        this.component = component;
        this.resolve = resolve;
        this.separator = separator;
    }

    public String getComponent() {
        return this.component;
    }

    public NbtComponent setComponent(String component) {
        this.component = component;
        return this;
    }

    public boolean isResolve() {
        return this.resolve;
    }

    public NbtComponent setResolve(boolean resolve) {
        this.resolve = resolve;
        return this;
    }

    @Nullable
    public ATextComponent getSeparator() {
        return this.separator;
    }

    public NbtComponent setSeparator(ATextComponent separator) {
        this.separator = separator;
        return this;
    }

    @Override
    public String asSingleString() {
        return "";
    }
}
