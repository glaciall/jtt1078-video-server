package cn.org.hentai.jtt1078.entity;

/**
 * Created by houcheng on 2019-12-11.
 * 数据流，可能是视频或是音频，视频为FLV封装，音频为PCM编码的片断
 */
public class Media
{
    public enum Type { Video, Audio };

    public Type type;
    public MediaEncoding.Encoding encoding;
    public long sequence;
    public byte[] data;

    public Media(long seq, MediaEncoding.Encoding encoding, byte[] data)
    {
        this.type = type;
        this.data = data;
        this.encoding = encoding;
        this.sequence = seq;
    }
}
