package cn.org.hentai.jtt1078.video;

import cn.org.hentai.jtt1078.util.Configs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by matrixy on 2019/4/10.
 */
public final class FFMpegManager
{
    static Logger logger = LoggerFactory.getLogger(FFMpegManager.class);

    static final AtomicLong sequence = new AtomicLong(0L);

    Map<Long, Publisher> publishers;
    Object lock;

    private FFMpegManager()
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

    // 转发H264数据到ffmpeg子进程
    public void feed(long id, byte[] data) throws Exception
    {
        Publisher publisher = publishers.get(id);
        if (null == publisher)
        {
            throw new RuntimeException("no such publisher: " + id);
        }

        if (publisher.publish(data) == false)
        {
            publisher.close();
            synchronized (lock)
            {
                publishers.remove(publisher.channel);
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
                    logger.debug("publisher-{} timeout and close automatically", publisher.channel);
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

    static FFMpegManager instance;
    public static synchronized FFMpegManager getInstance()
    {
        if (null == instance) instance = new FFMpegManager();
        return instance;
    }

    static class Publisher extends Thread
    {
        long channel;
        Process process;
        OutputStream output;
        long lastActiveTime;
        boolean publishing = false;
        Object lock = null;
        LinkedList<byte[]> packets = null;

        public Publisher(long channel)
        {
            this.channel = channel;
            this.lastActiveTime = System.currentTimeMillis();
            this.lock = new Object();
            this.packets = new LinkedList<>();

            this.setName("publisher-" + channel);
        }

        public boolean isTimeout()
        {
            if (publishing) return false;
            return System.currentTimeMillis() - lastActiveTime > 5000;
        }

        public void open(String rtmpURL) throws Exception
        {
            process = Runtime.getRuntime().exec(
                    String.format("%s -re -i - -c copy -f flv -", Configs.get("ffmpeg.path")));
            output = process.getOutputStream();
            // TODO：写入了多少祯，读到了多少个FLV封包？
            // StdoutCleaner.getInstance().watch(channel, process);
            this.start();
        }

        // 将data写入到FIFO文件中去，好让
        public boolean publish(byte[] data) throws Exception
        {
            if (process.isAlive() == false) return false;
            synchronized (lock)
            {
                packets.add(data);
                lock.notify();
            }
            return true;
        }

        public void run()
        {
            while (!this.isInterrupted())
            {
                byte[] data = null;
                synchronized (lock)
                {
                    while (this.isInterrupted() == false && packets.size() == 0) try { lock.wait(); } catch(Exception e) { }
                    if (packets.size() == 0) break;
                    data = packets.removeFirst();
                }

                publishing = true;
                try
                {
                    output.write(data);
                    output.flush();
                }
                catch(Exception e)
                {
                    throw new RuntimeException(e);
                }
                publishing = false;
                this.lastActiveTime = System.currentTimeMillis();
            }
        }

        // 回收，关闭掉相关的资源
        public void close()
        {
            try { this.interrupt(); } catch(Exception e) { }
            try
            {
                synchronized (lock)
                {
                    lock.notifyAll();
                }
            }
            catch(Exception e) { }

            try
            {
                if (process != null && process.isAlive())
                {
                    process.destroy();
                    // boolean exited = process.waitFor(30, TimeUnit.SECONDS);
                    // if (!exited) throw new RuntimeException("unable to terminate process: " + process);
                }
            }
            catch(Exception ex)
            {
                // logger.error("close feed channel failed", ex);
            }
            try { if (output != null) output.close(); } catch(Exception e) { }
            process = null;

            StdoutCleaner.getInstance().unwatch(channel);
        }

        @Override
        public String toString()
        {
            return "Publisher{hash=" + hashCode() + ", " + "channel=" + channel + ", lastActiveTime=" + lastActiveTime + '}';
        }
    }
}
