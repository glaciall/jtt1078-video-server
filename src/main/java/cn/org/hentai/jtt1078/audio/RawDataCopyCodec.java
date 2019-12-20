package cn.org.hentai.jtt1078.audio;

import java.util.Arrays;

/**
 * Created by houcheng on 2019-12-11.
 */
public class RawDataCopyCodec extends AudioCodec
{
    @Override
    public byte[] toPCM(byte[] data)
    {
        return Arrays.copyOf(data, data.length);
    }

    @Override
    public byte[] fromPCM(byte[] data)
    {
        return Arrays.copyOf(data, data.length);
    }
}
