package com.viaversion.viaversion.api.protocol;

import com.viaversion.viaversion.api.protocol.packet.Direction;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface ProtocolPipeline extends SimpleProtocol {
    void add(Protocol var1);

    void add(Collection<Protocol> var1);

    boolean contains(Class<? extends Protocol> var1);

    @Deprecated
    <P extends Protocol> @Nullable P getProtocol(Class<P> var1);

    List<Protocol> pipes(@Nullable Class<? extends Protocol> var1, boolean var2, Direction var3);

    List<Protocol> pipes();

    List<Protocol> reversedPipes();

    int baseProtocolCount();

    boolean hasNonBaseProtocols();

    void cleanPipes();
}
