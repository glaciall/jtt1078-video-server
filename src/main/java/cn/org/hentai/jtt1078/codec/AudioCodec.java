package cn.org.hentai.jtt1078.codec;

import cn.org.hentai.jtt1078.entity.Audio;
import cn.org.hentai.jtt1078.entity.MediaEncoding;

/**
 * Created by houcheng on 2019-12-11.
 */
public abstract class AudioCodec
{
    public abstract byte[] toPCM(byte[] data);
    public abstract byte[] fromPCM(byte[] data);

    public static AudioCodec getCodec(MediaEncoding.Encoding encoding)
    {
        if (MediaEncoding.Encoding.ADPCMA.equals(encoding)) return new ADPCMCodec();
        else if (MediaEncoding.Encoding.G711A.equals(encoding)) return new G711Codec();
        else if (MediaEncoding.Encoding.G711U.equals(encoding)) return new G711UCodec();
        // else if (Audio.Encoding.G726.equals(encoding)) ;
        else return new RawDataCopyCodec();
    }
}
