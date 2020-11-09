package cn.org.hentai.jtt1078.test;

import cn.org.hentai.jtt1078.server.Jtt1078Decoder;
import cn.org.hentai.jtt1078.util.Packet;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class UnPack
{
    public static void main(String[] args) throws Exception
    {
        FileInputStream input = new FileInputStream("d:\\test\\1078\\d.bin");
        FileOutputStream output = new FileOutputStream("d:\\test\\1078\\fuck.1078.xxx");

        int len = -1;
        byte[] block = new byte[1024];
        Jtt1078Decoder decoder = new Jtt1078Decoder();
        while (true)
        {
            len = input.read(block);
            if (len == -1) break;
            decoder.write(block, 0, len);

            while (true)
            {
                Packet p = decoder.decode();
                if (p == null) break;

                int lengthOffset = 28;
                int dataType = (p.seek(15).nextByte() >> 4) & 0x0f;
                // 透传数据类型：0100，没有后面的时间以及Last I Frame Interval和Last Frame Interval字段
                if (dataType == 0x04) lengthOffset = 28 - 8 - 2 - 2;
                else if (dataType == 0x03) lengthOffset = 28 - 4;

                // FFMpegManager.getInstance().feed(publisherId, packet.seek(lengthOffset + 2).nextBytes());
                if (dataType == 0x00 || dataType == 0x01 || dataType == 0x02)
                {
                    // 视频
                    // p.seek(lengthOffset + 2).nextBytes()
                    // output.write(p.seek(lengthOffset + 2).nextBytes());
                    System.out.println(p.seek(lengthOffset).nextShort());
                }
                else
                {
                    // 音频
                    // p.seek(lengthOffset + 2).nextBytes()
                }
            }
        }
        output.flush();
        output.close();
        input.close();
    }
}