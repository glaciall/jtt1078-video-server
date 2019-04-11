package cn.org.hentai.jtt1078.server;

import cn.org.hentai.jtt1078.util.ByteUtils;
import cn.org.hentai.jtt1078.util.Configs;
import cn.org.hentai.jtt1078.util.Packet;
import cn.org.hentai.jtt1078.video.PublisherManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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
        Long publisherId = session.get(channelKey);
        if (publisherId == null)
        {
            publisherId = PublisherManager.getInstance().request(rtmpURL);
            if (publisherId == -1) throw new RuntimeException("exceed max concurrent stream pushing limitation");
            session.set(channelKey, publisherId);

            logger.info("start streaming to {}", rtmpURL);
        }

        PublisherManager.getInstance().publish(publisherId, packet.seek(30).nextBytes());
    }

    public final Session getSession()
    {
        Attribute<Session> attr = context.channel().attr(SESSION_KEY);
        if (null == attr) return null;
        else return attr.get();
    }

    public final void setSession(Session session)
    {
        context.channel().attr(SESSION_KEY).set(session);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        super.exceptionCaught(ctx, cause);
        ctx.close();
    }
}
