package com.viaversion.viaversion.libs.mcstructs.text.components;

import com.viaversion.viaversion.libs.mcstructs.core.utils.ToString;
import com.viaversion.viaversion.libs.mcstructs.text.ATextComponent;
import java.util.Objects;

public class StringComponent extends ATextComponent {
    private String text;

    public StringComponent() {
        this("");
    }

    public StringComponent(String text) {
        this.text = text;
    }

    public String getText() {
        return this.text;
    }

    public StringComponent setText(String text) {
        this.text = text;
        return this;
    }

    @Override
    public String asSingleString() {
        return this.text;
    }

    @Override
    public ATextComponent copy() {
        return this.putMetaCopy(new StringComponent(this.text));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            StringComponent that = (StringComponent)o;
            return Objects.equals(this.getSiblings(), that.getSiblings())
                && Objects.equals(this.getStyle(), that.getStyle())
                && Objects.equals(this.text, that.text);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getSiblings(), this.getStyle(), this.text);
    }

    @Override
    public String toString() {
        return ToString.of(this)
            .add("siblings", this.getSiblings(), siblings -> !siblings.isEmpty())
            .add("style", this.getStyle(), style -> !style.isEmpty())
            .add("text", this.text)
            .toString();
    }
}
