package cn.org.hentai.jtt1078.subscriber;

import cn.org.hentai.jtt1078.flv.FlvEncoder;
import cn.org.hentai.jtt1078.util.FLVUtils;
import cn.org.hentai.jtt1078.util.HttpChunk;
import io.netty.channel.ChannelHandlerContext;

import java.io.FileOutputStream;

/**
 * Created by matrixy on 2020/1/13.
 */
public class VideoSubscriber extends Subscriber
{
    private int timestamp = 0;
    private long lastFrameTimeOffset = 0;
    private boolean headerSend = false;

    FileOutputStream fos = null;

    public VideoSubscriber(String tag, ChannelHandlerContext ctx)
    {
        super(tag, ctx);
    }

    private void write(byte[] data)
    {
        try
        {
            // if (fos == null) fos = new FileOutputStream("d:\\fuck11111.flv");
            // fos.write(data);
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public void onData(long timeoffset, byte[] data, FlvEncoder flvEncoder)
    {
        if (lastFrameTimeOffset == 0) lastFrameTimeOffset = timeoffset;

        // 之前是不是已经发送过了？没有的话，需要补发FLV HEADER的。。。
        if (headerSend == false && flvEncoder.videoReady())
        {
            enqueue(HttpChunk.make(flvEncoder.getHeader().getBytes()));
            enqueue(HttpChunk.make(flvEncoder.getVideoHeader().getBytes()));

            write(flvEncoder.getHeader().getBytes());
            write(flvEncoder.getVideoHeader().getBytes());

            // 如果第一次发送碰到的不是I祯，那就把上一个缓存的I祯发下去
            if ((data[4] & 0x1f) != 0x05)
            {
                byte[] iFrame = flvEncoder.getLastIFrame();
                if (iFrame != null)
                {
                    FLVUtils.resetTimestamp(iFrame, timestamp);
                    enqueue(HttpChunk.make(iFrame));
                }
            }

            headerSend = true;
        }

        if (data == null) return;

        // 修改时间戳
        FLVUtils.resetTimestamp(data, timestamp);
        timestamp += (int)(timeoffset - lastFrameTimeOffset);
        lastFrameTimeOffset = timeoffset;

        write(data);

        enqueue(HttpChunk.make(data));
    }
}
