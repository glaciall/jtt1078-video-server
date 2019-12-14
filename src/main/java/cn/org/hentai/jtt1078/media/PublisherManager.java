package cn.org.hentai.jtt1078.media;

import cn.org.hentai.jtt1078.media.publisher.Publisher;
import cn.org.hentai.jtt1078.media.publisher.VideoPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by matrixy on 2019/4/10.
 */
public final class PublisherManager
{
    static Logger logger = LoggerFactory.getLogger(PublisherManager.class);

    static final AtomicLong sequence = new AtomicLong(0L);

    Map<Long, Publisher> publishers;
    Object lock;

    private PublisherManager()
    {
        lock = new Object();
        publishers = new HashMap<Long, Publisher>();
    }

    // 初始化，清理文件、启动定时器
    public void init()
    {
        // 定时清理超时的转发进程
        new Timer().scheduleAtFixedRate(new TimerTask()
        {
            @Override
            public void run()
            {
                purge();
            }
        }, 10000, 5000);
    }

    // 先申请一个推送通道
    public long request(String rtmpURL) throws Exception
    {
        if (publishers.size() >= 1024) return -1;

        long id = sequence.addAndGet(1L);
        synchronized (lock)
        {
            Publisher publisher = new Publisher(id);
            publisher.open(rtmpURL);
            publishers.put(id, publisher);
        }
        return id;
    }

    // 分发媒体数据，分发到两个线程，通过原始音视频编码向统一的音频（PCM）/视频（H264）编码转码然后再推流到RTMP服务器
    public void publish(long id, Media.Type mediaType, Media.Encoding mediaEncoding, byte[] data) throws Exception
    {
        Publisher publisher = publishers.get(id);
        if (null == publisher)
        {
            throw new RuntimeException("no such publisher: " + id);
        }

        if (publisher.publish(mediaType, mediaEncoding, data) == false)
        {
            logger.error("publisher closed for child process exited...");

            publisher.close();
            synchronized (lock)
            {
                publishers.remove(publisher.getChannel());
                lock.notifyAll();
            }
        }
    }

    public void close(long id)
    {
        Publisher publisher = publishers.get(id);
        if (publisher != null) publisher.close();
    }

    // 清理超时无数据交换的进程
    private void purge()
    {
        synchronized (lock)
        {
            Iterator<Long> itr = publishers.keySet().iterator();
            while (itr.hasNext())
            {
                Publisher publisher = publishers.get(itr.next());
                if (publisher.isTimeout())
                {
                    itr.remove();
                    publisher.close();
                    logger.debug("publisher-{} timeout and close automatically", publisher.getChannel());
                }
            }
            lock.notifyAll();
        }
    }

    public void shutdown()
    {
        synchronized (lock)
        {
            Iterator<Long> itr = publishers.keySet().iterator();
            while (itr.hasNext())
            {
                Publisher publisher = publishers.get(itr.next());
                publisher.close();
            }
        }
    }

    static PublisherManager instance;
    public static synchronized PublisherManager getInstance()
    {
        if (null == instance) instance = new PublisherManager();
        return instance;
    }
}
