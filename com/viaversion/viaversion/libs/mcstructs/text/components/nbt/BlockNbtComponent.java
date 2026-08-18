package com.viaversion.viaversion.libs.mcstructs.text.components.nbt;

import com.viaversion.viaversion.libs.mcstructs.core.utils.ToString;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import com.viaversion.viaversion.libs.mcstructs.text.components.NbtComponent;
import java.util.Objects;

public class BlockNbtComponent extends NbtComponent {
    private String pos;

    public BlockNbtComponent(String rawComponent, boolean resolve, String pos) {
        super(rawComponent, resolve);
        this.pos = pos;
    }

    public BlockNbtComponent(String rawComponent, boolean resolve, ATextComponent separator, String pos) {
        super(rawComponent, resolve, separator);
        this.pos = pos;
    }

    public String getPos() {
        return this.pos;
    }

    public BlockNbtComponent setPos(String pos) {
        this.pos = pos;
        return this;
    }

    @Override
    public ATextComponent copy() {
        return this.getSeparator() == null
            ? this.putMetaCopy(
                new BlockNbtComponent(this.getComponent(), this.isResolve(), this.getSeparator(), this.pos)
            )
            : this.putMetaCopy(
                new BlockNbtComponent(this.getComponent(), this.isResolve(), this.getSeparator().copy(), this.pos)
            );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            BlockNbtComponent that = (BlockNbtComponent)o;
            return Objects.equals(this.getSiblings(), that.getSiblings())
                && Objects.equals(this.getStyle(), that.getStyle())
                && Objects.equals(this.pos, that.pos);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getSiblings(), this.getStyle(), this.pos);
    }

    @Override
    public String toString() {
        return ToString.of(this)
            .add("siblings", this.getSiblings(), siblings -> !siblings.isEmpty())
            .add("style", this.getStyle(), style -> !style.isEmpty())
            .add("pos", this.pos)
            .toString();
    }
}
