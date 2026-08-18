package com.viaversion.viaversion.util;

import com.google.common.base.Preconditions;
import java.util.Objects;

public class IdAndData {
    private int id;
    private byte data;

    public IdAndData(int id) {
        this.id = id;
        this.data = -1;
    }

    public IdAndData(int id, int data) {
        Preconditions.checkArgument(
            data >= 0 && data <= 15, "Data has to be between 0 and 15: (id: " + id + " data: " + data + ")"
        );
        this.id = id;
        this.data = (byte)data;
    }

    public static int getId(int rawData) {
        return rawData >> 4;
    }

    public static int getData(int rawData) {
        return rawData & 15;
    }

    public static int toRawData(int id) {
        return id << 4;
    }

    public static int removeData(int data) {
        return data & -16;
    }

    public static IdAndData fromRawData(int rawData) {
        return new IdAndData(rawData >> 4, rawData & 15);
    }

    public static int toRawData(int id, int data) {
        return id << 4 | data & 15;
    }

    public int toRawData() {
        return toRawData(this.id, this.data);
    }

    public IdAndData withData(int data) {
        return new IdAndData(this.id, data);
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public byte getData() {
        return this.data;
    }

    public void setData(int data) {
        this.data = (byte)data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            IdAndData idAndData = (IdAndData)o;
            return this.id == idAndData.id && this.data == idAndData.data;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id, this.data);
    }

    @Override
    public String toString() {
        return "IdAndData{id=" + this.id + ", data=" + this.data + '}';
    }
}
