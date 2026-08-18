package miau.util.client;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.openal.AL10;

public class Mp3Util {
    private static final Minecraft mc = Minecraft.func_71410_x();
    private static int lastSource = 0;
    private static int lastBuffer = 0;

    public static void play(String assetPath) {
        if (mc.func_147118_V() != null) {
            try {
                ResourceLocation loc = new ResourceLocation("minecraft", assetPath);
                InputStream is = mc.func_110442_L().func_110536_a(loc).func_110527_b();
                stopCurrent();
                int[] result = playStream(new BufferedInputStream(is));
                lastSource = result[0];
                lastBuffer = result[1];
                is.close();
            } catch (Exception var4) {
            }
        }
    }

    private static int[] playStream(InputStream in) throws Exception {
        Bitstream bitstream = new Bitstream(in);
        Decoder decoder = new Decoder();
        List<short[]> frames = new ArrayList<>();
        int totalSamples = 0;
        int sampleRate = 44100;
        int channels = 2;

        Header header;
        try {
            for (; (header = bitstream.readFrame()) != null; bitstream.closeFrame()) {
                SampleBuffer sb = (SampleBuffer)decoder.decodeFrame(header, bitstream);
                if (sb != null) {
                    short[] pcm = sb.getBuffer();
                    frames.add(pcm);
                    totalSamples += pcm.length;
                    sampleRate = sb.getSampleFrequency();
                    channels = sb.getChannelCount();
                }
            }
        } finally {
            bitstream.close();
        }

        if (!frames.isEmpty() && totalSamples != 0) {
            short[] pcm = new short[totalSamples];
            int offset = 0;

            for (short[] frame : frames) {
                System.arraycopy(frame, 0, pcm, offset, frame.length);
                offset += frame.length;
            }

            int format = channels == 1 ? 4353 : 4355;
            ByteBuffer data = ByteBuffer.allocateDirect(pcm.length * 2).order(ByteOrder.nativeOrder());
            ShortBuffer sbuf = data.asShortBuffer();
            sbuf.put(pcm);
            ((Buffer)data).rewind();
            int buffer = AL10.alGenBuffers();
            AL10.alBufferData(buffer, format, data, sampleRate);
            int source = AL10.alGenSources();
            AL10.alSourcei(source, 4105, buffer);
            AL10.alSourcef(source, 4106, 1.0F);
            AL10.alSourcePlay(source);
            return new int[]{source, buffer};
        } else {
            return new int[]{0, 0};
        }
    }

    private static void stopCurrent() {
        if (lastSource != 0) {
            try {
                AL10.alSourceStop(lastSource);
                AL10.alDeleteSources(lastSource);
            } catch (Exception var2) {
            }

            lastSource = 0;
        }

        if (lastBuffer != 0) {
            try {
                AL10.alDeleteBuffers(lastBuffer);
            } catch (Exception var1) {
            }

            lastBuffer = 0;
        }
    }
}
