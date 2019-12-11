package cn.org.hentai.jtt1078.publisher.entity;

/**
 * Created by houcheng on 2019-12-11.
 * 数据流，可能是视频或是音频，视频为FLV封装，音频为PCM编码的片断
 */
public class Media
{
    public enum Type { video, audio };

    public Type type;
    public long sequence;
    public byte[] data;

    public Media(Type type, byte[] data)
    {
        this.type = type;
        this.data = data;
    }
}
