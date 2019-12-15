package cn.org.hentai.jtt1078.video;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by matrixy on 2019/4/10.
 */
public final class FFMpegManager
{
    static Logger logger = LoggerFactory.getLogger(FFMpegManager.class);

    static final AtomicLong sequence = new AtomicLong(0L);

    Map<Long, VideoFeeder> feeders;
    Object lock;

    private FFMpegManager()
    {
        lock = new Object();
        feeders = new HashMap<Long, VideoFeeder>();
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
    public long request(String tag, String rtmpURL) throws Exception
    {
        if (feeders.size() >= 1024) return -1;

        long id = sequence.addAndGet(1L);
        synchronized (lock)
        {
            VideoFeeder feeder = new VideoFeeder(id);
            feeder.open(tag, rtmpURL);
            feeders.put(id, feeder);
        }
        return id;
    }

    // 转发H264数据到ffmpeg子进程
    public void feed(long id, byte[] data) throws Exception
    {
        VideoFeeder feeder = feeders.get(id);
        if (null == feeder)
        {
            throw new RuntimeException("no such publisher: " + id);
        }

        if (feeder.publish(data) == false)
        {
            feeder.close();
            synchronized (lock)
            {
                feeders.remove(feeder.channel);
                lock.notifyAll();
            }
        }
    }

    public void close(long id)
    {
        VideoFeeder feeder = feeders.get(id);
        if (feeder != null) feeder.close();
    }

    // 清理超时无数据交换的进程
    private void purge()
    {
        synchronized (lock)
        {
            Iterator<Long> itr = feeders.keySet().iterator();
            while (itr.hasNext())
            {
                VideoFeeder feeder = feeders.get(itr.next());
                if (feeder.isTimeout())
                {
                    itr.remove();
                    feeder.close();
                    logger.debug("publisher-{} timeout and close automatically", feeder.channel);
                }
            }
            lock.notifyAll();
        }
    }

    public void shutdown()
    {
        synchronized (lock)
        {
            Iterator<Long> itr = feeders.keySet().iterator();
            while (itr.hasNext())
            {
                VideoFeeder feeder = feeders.get(itr.next());
                feeder.close();
            }
        }
    }

    static FFMpegManager instance;
    public static synchronized FFMpegManager getInstance()
    {
        if (null == instance) instance = new FFMpegManager();
        return instance;
    }
}
