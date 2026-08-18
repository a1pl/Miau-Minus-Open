package miau.util.io;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class IOUtility {
    public static ByteBuffer ioResourceToByteBuffer(InputStream inputStream, int bufferSize) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        try {
            BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
            ReadableByteChannel channel = Channels.newChannel(bufferedStream);
            ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);

            while (true) {
                int bytes = channel.read(buffer);
                if (bytes == -1) {
                    ((Buffer)buffer).flip();
                    channel.close();
                    bufferedStream.close();
                    return buffer;
                }

                if (buffer.remaining() == 0) {
                    ByteBuffer newBuffer = ByteBuffer.allocateDirect(buffer.capacity() * 2);
                    ((Buffer)buffer).flip();
                    newBuffer.put(buffer);
                    buffer = newBuffer;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource into ByteBuffer", e);
        }
    }

    public static byte[] readAllBytes(InputStream inputStream) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        try {
            BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);
            byte[] buffer = new byte[8192];
            ByteArrayOutputStream output = new ByteArrayOutputStream();

            int bytesRead;
            while ((bytesRead = bufferedStream.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }

            bufferedStream.close();
            return output.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read resource into byte array", e);
        }
    }
}
