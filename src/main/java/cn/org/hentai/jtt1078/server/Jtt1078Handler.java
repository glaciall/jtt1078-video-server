package cn.org.hentai.jtt1078.server;

import cn.org.hentai.jtt1078.publisher.PublishManager;
import cn.org.hentai.jtt1078.entity.Audio;
import cn.org.hentai.jtt1078.util.Configs;
import cn.org.hentai.jtt1078.util.Packet;
import cn.org.hentai.jtt1078.video.FFMpegManager;
import cn.org.hentai.jtt1078.video.StdoutCleaner;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * Created by matrixy on 2019/4/9.
 */
public class Jtt1078Handler extends SimpleChannelInboundHandler<Packet>
{
    static Logger logger = LoggerFactory.getLogger(Jtt1078Handler.class);
    private static final AttributeKey<Session> SESSION_KEY = AttributeKey.valueOf("session-key");
    private ChannelHandlerContext context;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception
    {
        this.context = ctx;
        packet.seek(8);
        String sim = packet.nextBCD() + packet.nextBCD() + packet.nextBCD() + packet.nextBCD() + packet.nextBCD() + packet.nextBCD();
        int channel = packet.nextByte() & 0xff;

        // 因为FFMPEG推送有缓冲，所以在停止后又立即发起视频推送是会出现推送通道冲突的情况
        // 所以最好能够每次都分配到新的rtmp通道上去
        String rtmpURL = Configs.get("rtmp.format").replace("{sim}", sim).replace("{channel}", String.valueOf(channel));

        Session session = getSession();
        if (null == session)
        {
            setSession(session = new Session());
        }

        String channelKey = String.format("publisher-%d", channel);
        String tag = sim + "-" + channel;
        Long publisherId = session.get(channelKey);
        if (publisherId == null)
        {
            publisherId = FFMpegManager.getInstance().request("video-" + tag, rtmpURL);
            if (publisherId == -1) throw new RuntimeException("exceed max concurrent stream pushing limitation");
            session.set(channelKey, publisherId);

            long sessionId = SessionManager.getInstance().register(tag);
            session.set("sessionId", sessionId);
            session.set("tag", tag);
        }

        Integer sequence = session.get("video-sequence");
        if (sequence == null) sequence = 0;
        // 1. 做好序号
        // 2. 音频需要转码后提供订阅
        int lengthOffset = 28;
        int dataType = (packet.seek(15).nextByte() >> 4) & 0x0f;
        int pkType = packet.seek(15).nextByte() & 0x0f;
        // 透传数据类型：0100，没有后面的时间以及Last I Frame Interval和Last Frame Interval字段
        if (dataType == 0x04) lengthOffset = 28 - 8 - 2 - 2;
        else if (dataType == 0x03) lengthOffset = 28 - 4;

        int pt = packet.seek(5).nextByte() & 0x7f;

        if (dataType == 0x00 || dataType == 0x01 || dataType == 0x02)
        {
            // 碰到结束标记时，序号+1
            if (pkType == 0 || pkType == 2)
            {
                sequence += 1;
                session.set("video-sequence", sequence);
            }
            FFMpegManager.getInstance().feed(publisherId, packet.seek(lengthOffset + 2).nextBytes());
        }
        else if (dataType == 0x03)
        {
            PublishManager.getInstance().publish("audio-" + tag, new Audio(sequence, pt, packet.seek(lengthOffset + 2).nextBytes()));
        }
    }

    public final Session getSession()
    {
        Attribute<Session> attr = context.channel().attr(SESSION_KEY);
        if (null == attr) return null;
        else return attr.get();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        super.channelInactive(ctx);
        release();
    }

    public final void setSession(Session session)
    {
        context.channel().attr(SESSION_KEY).set(session);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        // super.exceptionCaught(ctx, cause);
        cause.printStackTrace();
        release();
        ctx.close();
    }

    private void release()
    {
        String tag = getSession().get("tag");
        if (tag != null) SessionManager.getInstance().unregister(tag);

        Session session = getSession();
        if (session != null)
        {
            Iterator itr = session.attributes.keySet().iterator();
            while (itr.hasNext())
            {
                Object key = itr.next();
                Object val = session.attributes.get(key);

                if (val instanceof java.lang.Long && key.toString().startsWith("publisher"))
                {
                    long channel = Long.parseLong(val.toString());
                    FFMpegManager.getInstance().close(channel);
                    StdoutCleaner.getInstance().unwatch(channel);
                }
            }
        }
    }
}
