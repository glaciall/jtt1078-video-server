package cn.org.hentai.jtt1078.codec;

/**
 * Created by houcheng on 2019-12-11.
 */
public final class CodecFactory
{
    public static AudioCodec getCodec(int audioCodecId)
    {
        AudioCodec codec = null;
        switch (audioCodecId)
        {
            case  6 : codec = new G711Codec(); break;
            case  7 : codec = new G711UCodec(); break;
            case 26 : codec = new ADPCMCodec(); break;
            default : return new RawDataCopyCodec();
        }
        return codec;
    }
}
