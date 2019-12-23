package cn.org.hentai.jtt1078.subscriber;

import cn.org.hentai.jtt1078.entity.Media;
import cn.org.hentai.jtt1078.util.FLVUtils;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by matrixy on 2019/12/20.
 */
public class VideoSubscriber extends Subscriber
{
    private int timestamp;

    public VideoSubscriber(String tag, ChannelHandlerContext ctx)
    {
        super(tag, ctx);
        this.setThreadNameTag("video-subscriber");
        this.timestamp = 0;
    }

    @Override
    public void onData(ChannelHandlerContext ctx, Media media) throws Exception
    {
        // System.out.println(String.format("[Video] type: %s, size: %6d", media.type, media.data.length));
        long duration = System.currentTimeMillis() - getLastDataSendTime();
        timestamp += duration;

        ctx.writeAndFlush(String.format("%x\r\n", media.data.length).getBytes());
        ctx.writeAndFlush(FLVUtils.resetTimestamp(media.data, timestamp));
        ctx.writeAndFlush("\r\n".getBytes()).await();
    }
}
