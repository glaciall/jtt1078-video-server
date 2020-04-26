package cn.org.hentai.jtt1078.server;

import cn.org.hentai.jtt1078.entity.Media;
import cn.org.hentai.jtt1078.entity.MediaEncoding;
import cn.org.hentai.jtt1078.publisher.Channel;
import cn.org.hentai.jtt1078.publisher.PublishManager;
import cn.org.hentai.jtt1078.entity.Audio;
import cn.org.hentai.jtt1078.util.Packet;
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
        String tag = sim + "-" + channel;

        Session session = getSession();
        if (null == session)
        {
            setSession(session = new Session());
            session.set("tag", tag);

            Channel chl = PublishManager.getInstance().open(tag);
            logger.info("start publishing: {} -> {}-{}", Long.toHexString(chl.hashCode() & 0xffffffffL), sim, channel);
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
            long timestamp = packet.seek(16).nextLong();
            PublishManager.getInstance().publishVideo(tag, sequence, timestamp, pt, packet.seek(lengthOffset + 2).nextBytes());
        }
        else if (dataType == 0x03)
        {
            long timestamp = packet.seek(16).nextLong();
            byte[] data = packet.seek(lengthOffset + 2).nextBytes();
            PublishManager.getInstance().publishAudio(tag, sequence, timestamp, pt, data);
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
        if (tag != null)
        {
            logger.info("close netty channel: {}", tag);
            PublishManager.getInstance().close(tag);
        }
    }
}
