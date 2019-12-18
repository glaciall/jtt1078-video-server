package cn.org.hentai.jtt1078.test;

import cn.org.hentai.jtt1078.util.ByteUtils;
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
                    .addBytes(ByteUtils.toLEBytes(len + 36))
                    .addBytes("WAVE".getBytes())                                    // wave type
                    .addBytes("fmt ".getBytes())                                    // fmt id
                    .addInt(0x10000000)                                             // fmt chunk size
                    .addShort((short)0x0100)                                        // format: 1 -> PCM
                    .addShort((short)0x0100)                                        // channels: 1
                    .addBytes(ByteUtils.toLEBytes(8000))                            // samples per second
                    .addBytes(ByteUtils.toLEBytes(1 * 8000 * 16 / 8))               // BPSecond
                    .addBytes(ByteUtils.toLEBytes((short)(1 * 16 / 8)))             // BPSample
                    .addBytes(ByteUtils.toLEBytes((short)(1 * 16)))                 // bPSecond
                    .addBytes("data".getBytes())                                    // data id
                    .addBytes(ByteUtils.toLEBytes(len));                            // data chunk size

            p.addBytes(block, len);

            FileOutputStream fos = new FileOutputStream("d:\\fuck.wav");
            fos.write(p.getBytes());
            fos.flush();
            fos.close();
        }
    }
}
