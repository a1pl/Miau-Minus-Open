package javazoom.jl.player.advanced;

import java.io.InputStream;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.JavaLayerException;
import javazoom.jl.decoder.SampleBuffer;
import javazoom.jl.player.AudioDevice;
import javazoom.jl.player.FactoryRegistry;

public class AdvancedPlayer {
    private Bitstream bitstream;
    private Decoder decoder;
    private AudioDevice audio;
    private boolean closed = false;
    private boolean complete = false;
    private int lastPosition = 0;
    private PlaybackListener listener;

    public AdvancedPlayer(InputStream stream) throws JavaLayerException {
        this(stream, null);
    }

    public AdvancedPlayer(InputStream stream, AudioDevice device) throws JavaLayerException {
        this.bitstream = new Bitstream(stream);
        if (device != null) {
            this.audio = device;
        } else {
            this.audio = FactoryRegistry.systemRegistry().createAudioDevice();
        }

        this.audio.open(this.decoder = new Decoder());
    }

    public void play() throws JavaLayerException {
        this.play(Integer.MAX_VALUE);
    }

    public boolean play(int frames) throws JavaLayerException {
        boolean ret = true;
        if (this.listener != null) {
            this.listener.playbackStarted(this.createEvent(PlaybackEvent.STARTED));
        }

        while (frames-- > 0 && ret) {
            ret = this.decodeFrame();
        }

        AudioDevice out = this.audio;
        if (out != null) {
            out.flush();
            synchronized (this) {
                this.complete = !this.closed;
                this.close();
            }

            if (this.listener != null) {
                this.listener.playbackFinished(this.createEvent(out, PlaybackEvent.STOPPED));
            }
        }

        return ret;
    }

    public synchronized void close() {
        AudioDevice out = this.audio;
        if (out != null) {
            this.closed = true;
            this.audio = null;
            out.close();
            this.lastPosition = out.getPosition();

            try {
                this.bitstream.close();
            } catch (BitstreamException ex) {
            }
        }
    }

    protected boolean decodeFrame() throws JavaLayerException {
        try {
            AudioDevice out = this.audio;
            if (out == null) {
                return false;
            }

            Header h = this.bitstream.readFrame();
            if (h == null) {
                return false;
            }

            SampleBuffer output = (SampleBuffer)this.decoder.decodeFrame(h, this.bitstream);
            synchronized (this) {
                out = this.audio;
                if (out != null) {
                    out.write(output.getBuffer(), 0, output.getBufferLength());
                }
            }

            this.bitstream.closeFrame();
            return true;
        } catch (RuntimeException ex) {
            throw new JavaLayerException("Exception decoding audio frame", ex);
        }
    }

    protected boolean skipFrame() throws JavaLayerException {
        Header h = this.bitstream.readFrame();
        if (h == null) {
            return false;
        }

        this.bitstream.closeFrame();
        return true;
    }

    public boolean play(int start, int end) throws JavaLayerException {
        boolean ret = true;
        int offset = start;

        while (offset-- > 0 && ret) {
            ret = this.skipFrame();
        }

        return this.play(end - start);
    }

    private PlaybackEvent createEvent(int id) {
        return this.createEvent(this.audio, id);
    }

    private PlaybackEvent createEvent(AudioDevice dev, int id) {
        return new PlaybackEvent(this, id, dev.getPosition());
    }

    public void setPlayBackListener(PlaybackListener listener) {
        this.listener = listener;
    }

    public PlaybackListener getPlayBackListener() {
        return this.listener;
    }

    public void stop() {
        this.listener.playbackFinished(this.createEvent(PlaybackEvent.STOPPED));
        this.close();
    }
}
