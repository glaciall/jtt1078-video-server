package cn.org.hentai.jtt1078.publisher.entity;

/**
 * Created by houcheng on 2019-12-11.
 */
public class Video extends Media
{
    public static enum FlvType { Header, Description, Tag };

    public boolean isKeyFrame;
    public FlvType flvType;
    public Video(FlvType flvType, byte[] data)
    {
        super(Media.Type.video, data);
        this.flvType = flvType;
        this.isKeyFrame = ((data[11] >> 4) & 0x0f) == 1;
    }
}
