package com.viaversion.viaversion.api.minecraft.item.data;

import com.viaversion.viaversion.api.type.Type;
import io.netty.buffer.ByteBuf;

public final class WrittenBook {
    public static final Type<WrittenBook> TYPE = new Type<WrittenBook>(WrittenBook.class) {
        public WrittenBook read(ByteBuf buffer) throws Exception {
            FilterableString title = FilterableString.TYPE.read(buffer);
            String author = Type.STRING.read(buffer);
            int generation = Type.VAR_INT.readPrimitive(buffer);
            FilterableComponent[] pages = FilterableComponent.ARRAY_TYPE.read(buffer);
            boolean resolved = buffer.readBoolean();
            return new WrittenBook(title, author, generation, pages, resolved);
        }

        public void write(ByteBuf buffer, WrittenBook value) throws Exception {
            FilterableString.TYPE.write(buffer, value.title);
            Type.STRING.write(buffer, value.author);
            Type.VAR_INT.writePrimitive(buffer, value.generation);
            FilterableComponent.ARRAY_TYPE.write(buffer, value.pages);
            buffer.writeBoolean(value.resolved);
        }
    };
    private final FilterableString title;
    private final String author;
    private final int generation;
    private final FilterableComponent[] pages;
    private final boolean resolved;

    public WrittenBook(
        FilterableString title, String author, int generation, FilterableComponent[] pages, boolean resolved
    ) {
        this.title = title;
        this.author = author;
        this.generation = generation;
        this.pages = pages;
        this.resolved = resolved;
    }

    public FilterableString title() {
        return this.title;
    }

    public String author() {
        return this.author;
    }

    public int generation() {
        return this.generation;
    }

    public FilterableComponent[] pages() {
        return this.pages;
    }

    public boolean resolved() {
        return this.resolved;
    }
}
