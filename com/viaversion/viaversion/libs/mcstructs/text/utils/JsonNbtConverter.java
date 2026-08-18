package com.viaversion.viaversion.libs.mcstructs.text.utils;

import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonNull;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.gson.JsonPrimitive;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ByteTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.DoubleTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.FloatTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.IntTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ListTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.LongArrayTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.LongTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ShortTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.StringTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.Tag;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.Nullable;

public class JsonNbtConverter {
    @Nullable
    public static JsonElement toJson(@Nullable Tag tag) {
        if (tag == null) {
            return null;
        }

        if (tag instanceof NumberTag) {
            return new JsonPrimitive(((NumberTag)tag).getValue());
        }

        if (tag instanceof ByteArrayTag) {
            JsonArray byteArray = new JsonArray();

            for (byte b : ((ByteArrayTag)tag).getValue()) {
                byteArray.add(b);
            }

            return byteArray;
        } else {
            if (tag instanceof StringTag) {
                return new JsonPrimitive(((StringTag)tag).getValue());
            }

            if (tag instanceof ListTag) {
                JsonArray list = new JsonArray();
                ListTag<Tag> listTag = (ListTag<Tag>)tag;

                for (Tag tagInList : listTag.getValue()) {
                    if (CompoundTag.class == listTag.getElementType()) {
                        CompoundTag compound = (CompoundTag)tagInList;
                        if (compound.size() == 1) {
                            Tag wrappedTag = compound.get("");
                            if (wrappedTag != null) {
                                tagInList = wrappedTag;
                            }
                        }
                    }

                    list.add(toJson(tagInList));
                }

                return list;
            } else if (tag instanceof CompoundTag) {
                JsonObject compound = new JsonObject();

                for (Entry<String, Tag> entry : ((CompoundTag)tag).getValue().entrySet()) {
                    compound.add(entry.getKey(), toJson(entry.getValue()));
                }

                return compound;
            } else if (tag instanceof IntArrayTag) {
                JsonArray intArray = new JsonArray();

                for (int i : ((IntArrayTag)tag).getValue()) {
                    intArray.add(i);
                }

                return intArray;
            } else {
                if (!(tag instanceof LongArrayTag)) {
                    throw new IllegalArgumentException("Unknown Nbt type: " + tag);
                }

                JsonArray longArray = new JsonArray();

                for (long l : ((LongArrayTag)tag).getValue()) {
                    longArray.add(l);
                }

                return longArray;
            }
        }
    }

    @Nullable
    public static Tag toNbt(@Nullable JsonElement element) {
        if (element == null) {
            return null;
        }

        if (element instanceof JsonObject) {
            JsonObject object = element.getAsJsonObject();
            CompoundTag compound = new CompoundTag();

            for (Entry<String, JsonElement> entry : object.entrySet()) {
                compound.put(entry.getKey(), toNbt(entry.getValue()));
            }

            return compound;
        } else if (!(element instanceof JsonArray)) {
            if (element instanceof JsonNull) {
                return null;
            }

            if (element instanceof JsonPrimitive) {
                JsonPrimitive primitive = element.getAsJsonPrimitive();
                if (primitive.isString()) {
                    return new StringTag(primitive.getAsString());
                }

                if (primitive.isBoolean()) {
                    return new ByteTag(primitive.getAsBoolean());
                }

                BigDecimal number = primitive.getAsBigDecimal();

                try {
                    long l = number.longValueExact();
                    if ((byte)l == l) {
                        return new ByteTag((byte)l);
                    } else if ((short)l == l) {
                        return new ShortTag((short)l);
                    } else {
                        return (int)l == l ? new IntTag((int)l) : new LongTag(l);
                    }
                } catch (ArithmeticException e) {
                    double d = number.doubleValue();
                    return (float)d == d ? new FloatTag((float)d) : new DoubleTag(d);
                }
            } else {
                throw new IllegalArgumentException("Unknown JsonElement type: " + element.getClass().getName());
            }
        } else {
            JsonArray array = element.getAsJsonArray();
            List<Tag> nbtTags = new ArrayList<>();
            Tag listType = null;
            boolean mixedList = false;

            for (JsonElement arrayElement : array) {
                Tag tag = toNbt(arrayElement);
                nbtTags.add(tag);
                listType = getListType(listType, tag);
                if (listType == null) {
                    mixedList = true;
                }
            }

            if (listType == null) {
                return new ListTag();
            }

            if (mixedList) {
                ListTag<CompoundTag> list = new ListTag<>();

                for (Tag tag : nbtTags) {
                    if (tag instanceof CompoundTag) {
                        list.add((CompoundTag)tag);
                    } else {
                        CompoundTag entries = new CompoundTag();
                        entries.put("", tag);
                        list.add(entries);
                    }
                }

                return list;
            } else if (listType instanceof ByteTag) {
                byte[] bytes = new byte[nbtTags.size()];

                for (int i = 0; i < nbtTags.size(); i++) {
                    bytes[i] = ((NumberTag)nbtTags.get(i)).asByte();
                }

                return new ByteArrayTag(bytes);
            } else if (listType instanceof IntTag) {
                int[] ints = new int[nbtTags.size()];

                for (int i = 0; i < nbtTags.size(); i++) {
                    ints[i] = ((NumberTag)nbtTags.get(i)).asInt();
                }

                return new IntArrayTag(ints);
            } else {
                if (!(listType instanceof LongTag)) {
                    return new ListTag<>(nbtTags);
                }

                long[] longs = new long[nbtTags.size()];

                for (int i = 0; i < nbtTags.size(); i++) {
                    longs[i] = ((NumberTag)nbtTags.get(i)).asLong();
                }

                return new LongArrayTag(longs);
            }
        }
    }

    private static Tag getListType(Tag current, Tag tag) {
        if (current == null) {
            return tag;
        } else {
            return current != tag ? null : current;
        }
    }
}
