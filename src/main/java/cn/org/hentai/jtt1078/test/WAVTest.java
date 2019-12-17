package cn.org.hentai.jtt1078.test;

import cn.org.hentai.jtt1078.util.Packet;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by matrixy on 2019/12/18.
 */
public class WAVTest
{
    public static void main(String[] args) throws Exception
    {
        int len;
        byte[] block = new byte[1024 * 2048];
        FileInputStream fis = new FileInputStream("d:\\temp\\xxoo.pcm");
        Packet p = Packet.create(1024 * 2048);
        while ((len = fis.read(block)) > -1)
        {
            p.reset();
            p.addBytes("RIFF".getBytes())
                    .addInt(len + 36)
                    .addBytes("WAVE".getBytes())            // wave type
                    .addBytes("fmt ".getBytes())            // fmt id
                    .addInt(16)                             // fmt chunk size
                    .addShort((short)0x0100)                // format: 1 -> PCM
                    .addShort((short)0x0100)                // channels: 1
                    .addInt(8000)                           // samples per second
                    .addInt(1 * 8000 * 16 / 8)              // BPSecond
                    .addShort((short)(1 * 16 / 8))          // BPSample
                    .addShort((short)(1 * 16))              // bPSecond
                    .addByte((byte)0)
                    .addBytes("data".getBytes())            // data id
                    .addInt(len);                             // data chunk size

            p.addBytes(block, len);

            FileOutputStream fos = new FileOutputStream("d:\\fuck.wav");
            fos.write(p.getBytes());
            fos.flush();
            fos.close();
        }
    }
}
