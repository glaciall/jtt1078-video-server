package cn.org.hentai.jtt1078.test;

import de.sciss.jump3r.lowlevel.LameEncoder;
import de.sciss.jump3r.mp3.Lame;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by matrixy on 2020/4/27.
 */
public class MP3Test
{
    public static void main(String[] args) throws Exception
    {
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 16, 1, 1 * 2, -1, false);
        LameEncoder encoder = new LameEncoder(sourceFormat, 256, 3, Lame.MEDIUM, false);

        byte[] block = new byte[320];
        int len = -1;
        FileInputStream fis = new FileInputStream("d:\\temp\\hello.pcm");
        ByteArrayOutputStream mp3 = new ByteArrayOutputStream(encoder.getOutputBufferSize());
        byte[] buffer = new byte[encoder.getPCMBufferSize()];
        int bytesToTransfer = 0;
        int bytesWritten;
        int currentPcmPosition = 0;

        FileOutputStream fos = new FileOutputStream("d:\\temp\\fuck.mp3");

        while ((len = fis.read(block)) > -1)
        {
            bytesToTransfer = len;
            currentPcmPosition = 0;
            while (0 < (bytesWritten = encoder.encodeBuffer(block, currentPcmPosition, bytesToTransfer, buffer)))
            {
                currentPcmPosition += bytesToTransfer;
                bytesToTransfer = Math.min(buffer.length, len - currentPcmPosition);

                mp3.write(buffer, 0, bytesWritten);
                fos.write(buffer, 0, bytesWritten);

                System.out.println(String.format("pcm data: %4d, written: %4d, pos: %4d", len, bytesWritten, currentPcmPosition));
            }
            System.out.println();
        }

        fos.close();
        fis.close();

        encoder.close();
    }
}
