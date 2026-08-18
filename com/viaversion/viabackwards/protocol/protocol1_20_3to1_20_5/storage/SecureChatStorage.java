package com.viaversion.viabackwards.protocol.protocol1_20_3to1_20_5.storage;

import com.viaversion.viaversion.api.connection.StorableObject;

public final class SecureChatStorage implements StorableObject {
    private boolean enforcesSecureChat;

    public void setEnforcesSecureChat(boolean enforcesSecureChat) {
        this.enforcesSecureChat = enforcesSecureChat;
    }

    public boolean enforcesSecureChat() {
        return this.enforcesSecureChat;
    }
}
