package cn.org.hentai.jtt1078.entity;

/**
 * Created by houcheng on 2019-12-11.
 */
public class Audio extends Media
{
    public Audio(int encoding, byte[] data)
    {
        super(Type.audio, encoding, data);
    }
}
