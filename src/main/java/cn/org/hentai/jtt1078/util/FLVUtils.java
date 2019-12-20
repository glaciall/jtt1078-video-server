package cn.org.hentai.jtt1078.util;

import java.util.Arrays;

/**
 * Created by matrixy on 2019/12/16.
 */
public final class FLVUtils
{
    // 重置FLV的时间戳
    public static byte[] resetTimestamp(byte[] packet, int timestamp)
    {
        // 0 1 2 3
        // 4 5 6 7
        byte[] flvData = Arrays.copyOf(packet, packet.length);
        flvData[4] = (byte)((timestamp >> 16) & 0xff);
        flvData[5] = (byte)((timestamp >>  8) & 0xff);
        flvData[6] = (byte)((timestamp >>  0) & 0xff);
        flvData[7] = (byte)((timestamp >> 24) & 0xff);

        // ByteUtils.dump(flvData, 32);

        return flvData;
    }
}

