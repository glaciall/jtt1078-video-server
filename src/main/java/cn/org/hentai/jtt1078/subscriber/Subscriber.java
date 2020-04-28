package cn.org.hentai.jtt1078.subscriber;

import cn.org.hentai.jtt1078.flv.FlvEncoder;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by matrixy on 2020/1/11.
 */
public abstract class Subscriber extends Thread
{
    static Logger logger = LoggerFactory.getLogger(Subscriber.class);
    static final AtomicLong SEQUENCE = new AtomicLong(0L);

    private long id;
    private String tag;
    private Object lock;
    private ChannelHandlerContext context;
    protected LinkedList<byte[]> messages;

    public Subscriber(String tag, ChannelHandlerContext ctx)
    {
        this.tag = tag;
        this.context = ctx;
        this.lock = new Object();
        this.messages = new LinkedList<byte[]>();

        this.id = SEQUENCE.getAndAdd(1L);
    }

    public long getId()
    {
        return this.id;
    }

    public abstract void onVideoData(long timeoffset, byte[] data, FlvEncoder flvEncoder);

    public abstract void onAudioData(long timeoffset, byte[] data, FlvEncoder flvEncoder);

    public void enqueue(byte[] data)
    {
        if (data == null) return;
        synchronized (lock)
        {
            messages.addLast(data);
            lock.notify();
        }
    }

    public void run()
    {
        loop : while (!this.isInterrupted())
        {
            try
            {
                byte[] data = null;
                synchronized (lock)
                {
                    while (messages.isEmpty())
                    {
                        lock.wait(100);
                        if (this.isInterrupted()) break loop;
                    }
                    data = messages.removeFirst();
                }
                send(data).await();
            }
            catch(Exception ex)
            {
                logger.error("send failed", ex);
            }
        }
        logger.info("subscriber closed");
    }

    public void close()
    {
        this.interrupt();
    }

    public ChannelFuture send(byte[] message)
    {
        return context.writeAndFlush(message);
    }
}
