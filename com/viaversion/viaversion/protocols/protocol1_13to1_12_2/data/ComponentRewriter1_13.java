package com.viaversion.viaversion.protocols.protocol1_13to1_12_2.data;

import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.minecraft.item.DataItem;
import com.viaversion.viaversion.api.minecraft.item.Item;
import com.viaversion.viaversion.api.protocol.Protocol;
import com.viaversion.viaversion.api.protocol.packet.ClientboundPacketType;
import com.viaversion.viaversion.libs.gson.JsonArray;
import com.viaversion.viaversion.libs.gson.JsonElement;
import com.viaversion.viaversion.libs.gson.JsonObject;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.CompoundTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.NumberTag;
import com.viaversion.viaversion.libs.opennbt.tag.builtin.ShortTag;
import com.viaversion.viaversion.protocols.protocol1_13to1_12_2.Protocol1_13To1_12_2;
import com.viaversion.viaversion.rewriter.ComponentRewriter;
import com.viaversion.viaversion.util.ComponentUtil;
import com.viaversion.viaversion.util.SerializerVersion;
import java.util.logging.Level;

public class ComponentRewriter1_13<C extends ClientboundPacketType> extends ComponentRewriter<C> {
    public ComponentRewriter1_13(Protocol<C, ?, ?, ?> protocol) {
        super(protocol, ComponentRewriter.ReadType.JSON);
    }

    @Override
    protected void handleHoverEvent(UserConnection connection, JsonObject hoverEvent) {
        super.handleHoverEvent(connection, hoverEvent);
        String action = hoverEvent.getAsJsonPrimitive("action").getAsString();
        if (action.equals("show_item")) {
            JsonElement value = hoverEvent.get("value");
            if (value != null) {
                CompoundTag tag;
                try {
                    tag = ComponentUtil.deserializeLegacyShowItem(value, SerializerVersion.V1_12);
                } catch (Exception e) {
                    if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                        Via.getPlatform()
                            .getLogger()
                            .log(Level.WARNING, "Error reading 1.12.2 NBT in show_item: " + value, e);
                    }

                    return;
                }

                CompoundTag itemTag = tag.getCompoundTag("tag");
                NumberTag damageTag = tag.getNumberTag("Damage");
                short damage = damageTag != null ? damageTag.asShort() : 0;
                Item item = new DataItem();
                item.setData(damage);
                item.setTag(itemTag);
                this.protocol.getItemRewriter().handleItemToClient(null, item);
                if (damage != item.data()) {
                    tag.put("Damage", new ShortTag(item.data()));
                }

                if (itemTag != null) {
                    tag.put("tag", itemTag);
                }

                JsonArray newValue = new JsonArray();
                JsonObject showItem = new JsonObject();
                newValue.add(showItem);

                try {
                    showItem.addProperty("text", SerializerVersion.V1_13.toSNBT(tag));
                    hoverEvent.add("value", newValue);
                } catch (Exception e) {
                    if (!Via.getConfig().isSuppressConversionWarnings() || Via.getManager().isDebug()) {
                        Via.getPlatform()
                            .getLogger()
                            .log(Level.WARNING, "Error writing 1.13 NBT in show_item: " + value, e);
                    }
                }
            }
        }
    }

    @Override
    protected void handleTranslate(JsonObject object, String translate) {
        super.handleTranslate(object, translate);
        String newTranslate = Protocol1_13To1_12_2.MAPPINGS.getTranslateMapping().get(translate);
        if (newTranslate == null) {
            newTranslate = Protocol1_13To1_12_2.MAPPINGS.getMojangTranslation().get(translate);
        }

        if (newTranslate != null) {
            object.addProperty("translate", newTranslate);
        }
    }
}
