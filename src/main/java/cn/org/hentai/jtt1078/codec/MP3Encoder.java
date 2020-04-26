package cn.org.hentai.jtt1078.codec;

import de.sciss.jump3r.lowlevel.LameEncoder;
import de.sciss.jump3r.mp3.Lame;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;

/**
 * Created by matrixy on 2020/4/27.
 */
public class MP3Encoder
{
    static final AudioFormat PCM_FORMAT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 16, 1, 1 * 2, -1, false);

    byte[] buffer = null;
    ByteArrayOutputStream mp3Data;
    LameEncoder encoder = null;

    public MP3Encoder()
    {
        encoder = new LameEncoder(PCM_FORMAT, 256, 3, Lame.MEDIUM, false);
        buffer = new byte[encoder.getPCMBufferSize()];
        mp3Data = new ByteArrayOutputStream(encoder.getOutputBufferSize());
    }

    public byte[] encode(byte[] pcm)
    {
        int bytesToTransfer = Math.min(encoder.getPCMBufferSize(), pcm.length);
        int bytesWritten;
        int currentPcmPosition = 0;

        mp3Data.reset();

        while (0 < (bytesWritten = encoder.encodeBuffer(pcm, currentPcmPosition, bytesToTransfer, buffer)))
        {
            currentPcmPosition += bytesToTransfer;
            bytesToTransfer = Math.min(buffer.length, pcm.length - currentPcmPosition);

            mp3Data.write(buffer, 0, bytesWritten);
        }

        return mp3Data.toByteArray();
    }

    public void close()
    {
        encoder.close();
    }
}
