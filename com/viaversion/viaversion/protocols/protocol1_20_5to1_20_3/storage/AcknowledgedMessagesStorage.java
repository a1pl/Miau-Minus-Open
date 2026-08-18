package com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.minecraft.ProfileKey;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_20_3to1_20_2.packet.ServerboundPackets1_20_3;
import com.viaversion.viaversion.protocols.protocol1_20_5to1_20_3.Protocol1_20_5To1_20_3;
import java.util.Arrays;
import java.util.BitSet;
import java.util.UUID;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class AcknowledgedMessagesStorage implements StorableObject {
    private static final int MAX_HISTORY = 20;
    private final boolean[] trackedMessages = new boolean[20];
    private Boolean secureChatEnforced;
    private AcknowledgedMessagesStorage.ChatSession chatSession;
    private int offset;
    private int tail;
    private byte[] lastMessage;

    public boolean add(byte[] message) {
        if (Arrays.equals(message, this.lastMessage)) {
            return false;
        }

        this.lastMessage = message;
        this.offset++;
        this.trackedMessages[this.tail] = true;
        this.tail = (this.tail + 1) % 20;
        return true;
    }

    public BitSet toAck() {
        BitSet acks = new BitSet(20);

        for (int i = 0; i < 20; i++) {
            int messageIndex = (this.tail + i) % 20;
            acks.set(i, this.trackedMessages[messageIndex]);
        }

        return acks;
    }

    public int offset() {
        return this.offset;
    }

    public void clearOffset() {
        this.offset = 0;
    }

    public void setSecureChatEnforced(boolean secureChatEnforced) {
        this.secureChatEnforced = secureChatEnforced;
    }

    public @Nullable Boolean secureChatEnforced() {
        return this.secureChatEnforced;
    }

    public boolean isSecureChatEnforced() {
        return this.secureChatEnforced == null || this.secureChatEnforced;
    }

    public void queueChatSession(UUID sessionId, ProfileKey profileKey) {
        this.chatSession = new AcknowledgedMessagesStorage.ChatSession(sessionId, profileKey);
    }

    public void sendQueuedChatSession(PacketWrapper wrapper) throws Exception {
        if (this.chatSession != null) {
            PacketWrapper chatSessionUpdate = wrapper.create(ServerboundPackets1_20_3.CHAT_SESSION_UPDATE);
            chatSessionUpdate.write(Type.UUID, this.chatSession.sessionId());
            chatSessionUpdate.write(Type.PROFILE_KEY, this.chatSession.profileKey());
            chatSessionUpdate.sendToServer(Protocol1_20_5To1_20_3.class);
            this.chatSession = null;
        }
    }

    public void clear() {
        this.offset = 0;
        this.tail = 0;
        this.lastMessage = null;
        Arrays.fill(this.trackedMessages, false);
    }

    @Override
    public boolean clearOnServerSwitch() {
        return false;
    }

    public static final class ChatSession {
        private final UUID sessionId;
        private final ProfileKey profileKey;

        public ChatSession(UUID sessionId, ProfileKey profileKey) {
            this.sessionId = sessionId;
            this.profileKey = profileKey;
        }

        public UUID sessionId() {
            return this.sessionId;
        }

        public ProfileKey profileKey() {
            return this.profileKey;
        }
    }
}
