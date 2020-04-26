package cn.org.hentai.jtt1078.test;

import cn.org.hentai.jtt1078.util.ByteUtils;
import cn.org.hentai.jtt1078.util.Packet;

import java.io.DataInputStream;
import java.io.FileInputStream;

/**
 * Created by matrixy on 2020/3/19.
 */
public class FuckTest
{
    public static void main(String[] args) throws Exception
    {
        String line;
        DataInputStream dis = new DataInputStream(new FileInputStream("d:\\temp\\rtp.txt"));
        while ((line = dis.readLine()) != null)
        {
            byte[] rtp = ByteUtils.parse(line);
            Packet packet = Packet.create(rtp);

            int lengthOffset = 28;
            int dataType = (packet.seek(15).nextByte() >> 4) & 0x0f;
            int pkType = packet.seek(15).nextByte() & 0x0f;
            // 透传数据类型：0100，没有后面的时间以及Last I Frame Interval和Last Frame Interval字段
            if (dataType == 0x04) lengthOffset = 28 - 8 - 2 - 2;
            else if (dataType == 0x03) lengthOffset = 28 - 4;

            if (dataType <= 0x02)
            {
                if (dataType == 0x00) System.out.println("I Frame---------------------------------");
                if (dataType == 0x01) System.out.println("P Frame");
                if (dataType == 0x02) System.out.println("B Frame");
            }
        }
    }
}
