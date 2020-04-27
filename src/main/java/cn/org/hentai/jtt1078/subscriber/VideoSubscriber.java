package cn.org.hentai.jtt1078.subscriber;

import cn.org.hentai.jtt1078.codec.MP3Encoder;
import cn.org.hentai.jtt1078.flv.AudioTag;
import cn.org.hentai.jtt1078.flv.FlvAudioTagEncoder;
import cn.org.hentai.jtt1078.flv.FlvEncoder;
import cn.org.hentai.jtt1078.util.ByteBufUtils;
import cn.org.hentai.jtt1078.util.FLVUtils;
import cn.org.hentai.jtt1078.util.HttpChunk;
import io.netty.buffer.ByteBuf;
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

    private FlvAudioTagEncoder audioEncoder = new FlvAudioTagEncoder();
    MP3Encoder mp3Encoder = new MP3Encoder();

    @Override
    public void onAudioData(long timeoffset, byte[] data, FlvEncoder flvEncoder)
    {
        byte[] mp3Data = mp3Encoder.encode(data);
        if (mp3Data.length == 0) return;
        AudioTag audioTag = new AudioTag(0, mp3Data.length + 1, AudioTag.MP3, (byte) 0, (byte)0, (byte) 1, mp3Data);
        byte[] frameData = null;
        try
        {
            ByteBuf audioBuf = audioEncoder.encode(audioTag);
            frameData = ByteBufUtils.readReadableBytes(audioBuf);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (lastAudioFrameTimeOffset == 0) lastAudioFrameTimeOffset = timeoffset;

        if (data == null) return;

        FLVUtils.resetTimestamp(frameData, (int) audioTimestamp);
        audioTimestamp += (int)(timeoffset - lastAudioFrameTimeOffset);
        lastAudioFrameTimeOffset = timeoffset;

        if (videoHeaderSent) enqueue(HttpChunk.make(frameData));
    }

    @Override
    public void close()
    {
        super.close();
        mp3Encoder.close();
    }
}
