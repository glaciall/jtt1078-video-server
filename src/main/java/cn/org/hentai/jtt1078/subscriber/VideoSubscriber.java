package cn.org.hentai.jtt1078.subscriber;

import cn.org.hentai.jtt1078.flv.FlvEncoder;
import cn.org.hentai.jtt1078.util.FLVUtils;
import cn.org.hentai.jtt1078.util.HttpChunk;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by matrixy on 2020/1/13.
 */
public class VideoSubscriber extends Subscriber
{
    private long videoTimestamp = 0;
    private long audioTimestamp = 0;
    private long lastVideoFrameTimeOffset = 0;
    private long lastAudioFrameTimeOffset = 0;
    private boolean videoHeaderSent = false;
    private boolean audioHeaderSent = false;

    public VideoSubscriber(String tag, ChannelHandlerContext ctx)
    {
        super(tag, ctx);
    }

    @Override
    public void onVideoData(long timeoffset, byte[] data, FlvEncoder flvEncoder)
    {
        if (lastVideoFrameTimeOffset == 0) lastVideoFrameTimeOffset = timeoffset;

        // 之前是不是已经发送过了？没有的话，需要补发FLV HEADER的。。。
        if (videoHeaderSent == false && flvEncoder.videoReady())
        {
            enqueue(HttpChunk.make(flvEncoder.getHeader().getBytes()));
            enqueue(HttpChunk.make(flvEncoder.getVideoHeader().getBytes()));

            // 如果第一次发送碰到的不是I祯，那就把上一个缓存的I祯发下去
            if ((data[4] & 0x1f) != 0x05)
            {
                byte[] iFrame = flvEncoder.getLastIFrame();
                if (iFrame != null)
                {
                    FLVUtils.resetTimestamp(iFrame, (int) videoTimestamp);
                    enqueue(HttpChunk.make(iFrame));
                }
            }

            videoHeaderSent = true;
        }

        if (data == null) return;

        // 修改时间戳
        // System.out.println("Time: " + videoTimestamp + ", current: " + timeoffset);
        FLVUtils.resetTimestamp(data, (int) videoTimestamp);
        videoTimestamp += (int)(timeoffset - lastVideoFrameTimeOffset);
        lastVideoFrameTimeOffset = timeoffset;

        enqueue(HttpChunk.make(data));
    }

    @Override
    public void onAudioData(long timeoffset, byte[] data, FlvEncoder flvEncoder)
    {
        if (lastAudioFrameTimeOffset == 0) lastAudioFrameTimeOffset = timeoffset;

        if (data == null) return;

        if (audioHeaderSent == false)
        {
            audioHeaderSent = true;
        }

        FLVUtils.resetTimestamp(data, (int) audioTimestamp);
        audioTimestamp += (int)(timeoffset - lastAudioFrameTimeOffset);
        lastAudioFrameTimeOffset = timeoffset;

        if (videoHeaderSent) enqueue(HttpChunk.make(data));
    }
}
