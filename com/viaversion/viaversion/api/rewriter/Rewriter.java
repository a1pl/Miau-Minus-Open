package com.viaversion.viaversion.api.rewriter;

import com.viaversion.viaversion.api.protocol.Protocol;

public interface Rewriter<T extends Protocol> extends MappingDataListener {
    void register();

    T protocol();
}
