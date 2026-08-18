package com.viaversion.viaversion.libs.mcstructs.text.components.nbt;

import com.viaversion.viaversion.libs.mcstructs.core.utils.ToString;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.NbtComponent;
import java.util.Objects;

public class EntityNbtComponent extends NbtComponent {
    private String selector;

    public EntityNbtComponent(String component, boolean resolve, String selector) {
        super(component, resolve);
        this.selector = selector;
    }

    public EntityNbtComponent(String component, boolean resolve, ATextComponent separator, String selector) {
        super(component, resolve, separator);
        this.selector = selector;
    }

    public String getSelector() {
        return this.selector;
    }

    public EntityNbtComponent setSelector(String selector) {
        this.selector = selector;
        return this;
    }

    @Override
    public ATextComponent copy() {
        return this.getSeparator() == null
            ? this.putMetaCopy(new EntityNbtComponent(this.getComponent(), this.isResolve(), null, this.selector))
            : this.putMetaCopy(
                new EntityNbtComponent(this.getComponent(), this.isResolve(), this.getSeparator(), this.selector)
            );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            EntityNbtComponent that = (EntityNbtComponent)o;
            return Objects.equals(this.getSiblings(), that.getSiblings())
                && Objects.equals(this.getStyle(), that.getStyle())
                && Objects.equals(this.selector, that.selector);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getSiblings(), this.getStyle(), this.selector);
    }

    @Override
    public String toString() {
        return ToString.of(this)
            .add("siblings", this.getSiblings(), siblings -> !siblings.isEmpty())
            .add("style", this.getStyle(), style -> !style.isEmpty())
            .add("selector", this.selector)
            .toString();
    }
}
