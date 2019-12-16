package cn.org.hentai.jtt1078.publisher;

import cn.org.hentai.jtt1078.publisher.entity.Media;
import cn.org.hentai.jtt1078.util.FLVUtils;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

/**
 * Created by houcheng on 2019-12-11.
 */
public class Subscriber extends Thread
{
    static Logger logger = LoggerFactory.getLogger(Subscriber.class);

    private Object lock;
    // 1. 我需要什么？
    private String tag;
    // 2. 我读到哪了？
    private Long currentIndex;

    private ChannelHandlerContext ctx;
    private LinkedList<Media> messages;

    public Subscriber(String tag, ChannelHandlerContext ctx)
    {
        this.tag = tag;
        this.currentIndex = -1L;
        this.ctx = ctx;
        this.lock = new Object();
        this.messages = new LinkedList<Media>();
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
        long lastAwareTime = System.currentTimeMillis();
        int timestamp = 0;
        while (!this.isInterrupted())
        {
            try
            {
                Media media = null;
                synchronized (lock)
                {
                    while (messages.size() == 0) lock.wait(1000);
                    media = messages.removeFirst();
                    if (lastAwareTime == 0L) lastAwareTime = System.currentTimeMillis();
                }

                long duration = System.currentTimeMillis() - lastAwareTime;
                timestamp += duration;

                ctx.writeAndFlush(String.format("%x\r\n", media.data.length).getBytes());
                ctx.writeAndFlush(FLVUtils.resetTimestamp(media.data, timestamp));
                ctx.writeAndFlush("\r\n".getBytes());

                // System.out.println(String.format("timestamp = %8d", timestamp));

                lastAwareTime = System.currentTimeMillis();
            }
            catch(Exception ex)
            {
                logger.error("send error", ex);
                break;
            }
        }
    }

    public String getTag()
    {
        return tag;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public Long getCurrentIndex()
    {
        return currentIndex;
    }

    public void setCurrentIndex(Long currentIndex)
    {
        this.currentIndex = currentIndex;
    }
}
