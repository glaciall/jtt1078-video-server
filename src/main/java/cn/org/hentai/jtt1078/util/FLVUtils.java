package cn.org.hentai.jtt1078.util;

import java.util.Arrays;

/**
 * Created by matrixy on 2019/12/16.
 */
public final class FLVUtils
{
    // 重置FLV的时间戳
    public static void resetTimestamp(byte[] packet, int timestamp)
    {
        // 0 1 2 3
        // 4 5 6 7
        // 只对视频类的TAG进行修改
        if (packet[0] != 9 && packet[0] != 8) return;

        packet[4] = (byte)((timestamp >> 16) & 0xff);
        packet[5] = (byte)((timestamp >>  8) & 0xff);
        packet[6] = (byte)((timestamp >>  0) & 0xff);
        packet[7] = (byte)((timestamp >> 24) & 0xff);
    }
}

