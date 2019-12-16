package cn.org.hentai.jtt1078.test;

import cn.org.hentai.jtt1078.server.Jtt1078Decoder;
import cn.org.hentai.jtt1078.util.Packet;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by matrixy on 2019/12/16.
 */
public class RTPTest
{
    public static void main(String[] args) throws Exception
    {
        InputStream fis = new FileInputStream("e:\\test\\streaming.hex");
        int len = -1;
        byte[] block = new byte[512];
        Jtt1078Decoder decoder = new Jtt1078Decoder();
        while ((len = fis.read(block)) > -1)
        {
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

                if (dataType == 0x03 || dataType == 0x04) continue;
                if (dataType == 0x00 || dataType == 0x01 || dataType == 0x02)
                {
                    long tag = p.seek(lengthOffset + 2).nextLong();
                    System.out.println(Long.toHexString(tag));
                }
            }
        }
    }
}
