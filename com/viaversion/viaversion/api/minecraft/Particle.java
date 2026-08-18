package com.viaversion.viaversion.api.minecraft;

import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.util.IdHolder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;

public final class Particle implements IdHolder {
    private final List<Particle.ParticleData<?>> arguments = new ArrayList<>(4);
    private int id;

    public Particle(int id) {
        this.id = id;
    }

    @Deprecated
    public int getId() {
        return this.id;
    }

    @Override
    public int id() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public <T> Particle.ParticleData<T> getArgument(int index) {
        return (Particle.ParticleData<T>)this.arguments.get(index);
    }

    public <T> Particle.ParticleData<T> removeArgument(int index) {
        return (Particle.ParticleData<T>)this.arguments.remove(index);
    }

    public List<Particle.ParticleData<?>> getArguments() {
        return this.arguments;
    }

    public <T> void add(Type<T> type, T value) {
        this.arguments.add(new Particle.ParticleData<>(type, value));
    }

    public <T> void add(int index, Type<T> type, T value) {
        this.arguments.add(index, new Particle.ParticleData<>(type, value));
    }

    public <T> void set(int index, Type<T> type, T value) {
        this.arguments.set(index, new Particle.ParticleData<>(type, value));
    }

    @Override
    public String toString() {
        return "Particle{arguments=" + this.arguments + ", id=" + this.id + '}';
    }

    public static final class ParticleData<T> {
        private final Type<T> type;
        private T value;

        public ParticleData(Type<T> type, T value) {
            this.type = type;
            this.value = value;
        }

        public Type<T> getType() {
            return this.type;
        }

        public T getValue() {
            return this.value;
        }

        public void setValue(T value) {
            this.value = value;
        }

        public void write(ByteBuf buf) throws Exception {
            this.type.write(buf, this.value);
        }

        public void write(PacketWrapper wrapper) {
            wrapper.write(this.type, this.value);
        }

        @Override
        public String toString() {
            return "ParticleData{type=" + this.type + ", value=" + this.value + '}';
        }
    }
}
