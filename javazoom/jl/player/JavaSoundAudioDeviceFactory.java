package javazoom.jl.player;

import javazoom.jl.decoder.JavaLayerException;

public class JavaSoundAudioDeviceFactory extends AudioDeviceFactory {
    private boolean tested = false;
    private static final String DEVICE_CLASS_NAME = "javazoom.jl.player.JavaSoundAudioDevice";

    @Override
    public synchronized AudioDevice createAudioDevice() throws JavaLayerException {
        if (!this.tested) {
            this.testAudioDevice();
            this.tested = true;
        }

        try {
            return this.createAudioDeviceImpl();
        } catch (Exception ex) {
            throw new JavaLayerException("unable to create JavaSound device: " + ex);
        } catch (LinkageError ex) {
            throw new JavaLayerException("unable to create JavaSound device: " + ex);
        }
    }

    protected JavaSoundAudioDevice createAudioDeviceImpl() throws JavaLayerException {
        ClassLoader loader = this.getClass().getClassLoader();

        try {
            return (JavaSoundAudioDevice)this.instantiate(loader, "javazoom.jl.player.JavaSoundAudioDevice");
        } catch (Exception ex) {
            throw new JavaLayerException("Cannot create JavaSound device", ex);
        } catch (LinkageError ex) {
            throw new JavaLayerException("Cannot create JavaSound device", ex);
        }
    }

    public void testAudioDevice() throws JavaLayerException {
        JavaSoundAudioDevice dev = this.createAudioDeviceImpl();
        dev.test();
    }
}
