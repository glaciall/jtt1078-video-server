package cn.org.hentai.jtt1078.codec;

/**
 * Created by houcheng on 2019-12-11.
 */
public abstract class AudioCodec
{
    public abstract byte[] toPCM(byte[] data);
    public abstract byte[] fromPCM(byte[] data);
}
