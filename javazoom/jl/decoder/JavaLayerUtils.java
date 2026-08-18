package javazoom.jl.decoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;

public class JavaLayerUtils {
    private static JavaLayerHook hook = null;

    public static Object deserialize(InputStream in, Class cls) throws IOException {
        if (cls == null) {
            throw new NullPointerException("cls");
        } else {
            Object obj = deserialize(in, cls);
            if (!cls.isInstance(obj)) {
                throw new InvalidObjectException("type of deserialized instance not of required class.");
            } else {
                return obj;
            }
        }
    }

    public static Object deserialize(InputStream in) throws IOException {
        if (in == null) {
            throw new NullPointerException("in");
        }

        ObjectInputStream objIn = new ObjectInputStream(in);

        try {
            return objIn.readObject();
        } catch (ClassNotFoundException ex) {
            throw new InvalidClassException(ex.toString());
        }
    }

    public static Object deserializeArray(InputStream in, Class elemType, int length) throws IOException {
        if (elemType == null) {
            throw new NullPointerException("elemType");
        }

        if (length < -1) {
            throw new IllegalArgumentException("length");
        }

        Object obj = deserialize(in);
        Class cls = obj.getClass();
        if (!cls.isArray()) {
            throw new InvalidObjectException("object is not an array");
        }

        Class arrayElemType = cls.getComponentType();
        if (arrayElemType != elemType) {
            throw new InvalidObjectException("unexpected array component type");
        }

        if (length != -1) {
            int arrayLength = Array.getLength(obj);
            if (arrayLength != length) {
                throw new InvalidObjectException("array length mismatch");
            }
        }

        return obj;
    }

    public static Object deserializeArrayResource(String name, Class elemType, int length) throws IOException {
        InputStream str = getResourceAsStream(name);
        if (str == null) {
            throw new IOException("unable to load resource '" + name + "'");
        } else {
            return deserializeArray(str, elemType, length);
        }
    }

    public static void serialize(OutputStream out, Object obj) throws IOException {
        if (out == null) {
            throw new NullPointerException("out");
        }

        if (obj == null) {
            throw new NullPointerException("obj");
        }

        ObjectOutputStream objOut = new ObjectOutputStream(out);
        objOut.writeObject(obj);
    }

    public static synchronized void setHook(JavaLayerHook hook0) {
        hook = hook0;
    }

    public static synchronized JavaLayerHook getHook() {
        return hook;
    }

    public static synchronized InputStream getResourceAsStream(String name) {
        InputStream is = null;
        if (hook != null) {
            is = hook.getResourceAsStream(name);
        } else {
            Class cls = JavaLayerUtils.class;
            is = cls.getResourceAsStream(name);
        }

        return is;
    }
}
