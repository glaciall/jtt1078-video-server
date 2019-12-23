package cn.org.hentai.jtt1078.subscriber;

import cn.org.hentai.jtt1078.entity.Media;
import cn.org.hentai.jtt1078.server.SessionManager;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by houcheng on 2019-12-11.
 */
public abstract class Subscriber extends Thread
{
    static Logger logger = LoggerFactory.getLogger(Subscriber.class);
    protected static AtomicLong sequence = new AtomicLong(0L);

    private Object lock;
    private String tag;

    private long sessionId;
    private long lastDataSendTime;

    private ChannelHandlerContext ctx;
    private LinkedList<Media> messages;

    public Subscriber(String tag, ChannelHandlerContext ctx)
    {
        this.tag = tag;
        this.ctx = ctx;
        this.lock = new Object();
        this.messages = new LinkedList<Media>();
        this.lastDataSendTime = System.currentTimeMillis();
    }

    public void setThreadNameTag(String nameTag)
    {
        this.setName(nameTag + "-" + sequence.addAndGet(1L));
    }

    public void aware(Media message)
    {
        synchronized (this.lock)
        {
            this.messages.addLast(message);
            lock.notify();
        }
    }

    public void run()
    {
        String sessionTag = tag.substring(6);
        SessionManager sessionManager = SessionManager.getInstance();
        loop : while (!this.isInterrupted())
        {
            try
            {
                Media media = null;
                synchronized (lock)
                {
                    while (messages.size() == 0)
                    {
                        lock.wait(1000);
                        // TODO: 需要检查一下终端连接的状态，是否依然在推流中
                        if (sessionManager.isAlive(sessionTag) == false) break loop;
                    }
                    media = messages.removeFirst();
                    if (lastDataSendTime == 0L) lastDataSendTime = System.currentTimeMillis();
                }

                // System.out.println(String.format("send %s: %6d", media.type, media.sequence));
                onData(ctx, media);
                lastDataSendTime = System.currentTimeMillis();
            }
            catch(Exception ex)
            {
                logger.error("send error", ex);
                break;
            }
        }

        logger.info("subscriber: {} finished...", tag);
    }

    protected long getLastDataSendTime()
    {
        return lastDataSendTime;
    }

    // 当数据到达时调用，需要完成转码并下发的过程
    public abstract void onData(ChannelHandlerContext ctx, Media media) throws Exception;

    public String getTag()
    {
        return tag;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }
}
