package cn.org.hentai.jtt1078.audio;

import cn.org.hentai.jtt1078.media.Media;

/**
 * Created by houcheng on 2019-12-11.
 */
public abstract class AudioCodec
{
    public abstract byte[] toPCM(byte[] data);
    public abstract byte[] fromPCM(byte[] data);

    public static AudioCodec getCodec(Media.Encoding audioEncoding)
    {
        if (Media.Encoding.ADPCMA.equals(audioEncoding)) return new ADPCMCodec();
        else if (Media.Encoding.G711A.equals(audioEncoding)) return new G711Codec();
        else if (Media.Encoding.G711U.equals(audioEncoding)) return new G711UCodec();
        // else if (Media.Encoding.G726.equals(audioEncoding)) ;
        else return new RawDataCopyCodec();
    }
}
