package cn.org.hentai.jtt1078.video;

import cn.org.hentai.jtt1078.util.Configs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
        // 先删个精光
        File basePath = new File(Configs.get("fifo-pool.path"));
        for (File file : basePath.listFiles())
        {
            file.delete();
        }

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
        int poolSize = Configs.getInt("fifo-pool.size", 64);
        if (publishers.size() >= poolSize) return -1;

        long id = sequence.addAndGet(1L);
        synchronized (lock)
        {
            Publisher publisher = new Publisher(id);
            publisher.open(rtmpURL);

            publishers.put(id, publisher);
        }
        return id;
    }

    // 转发H264数据到FIFO中
    public void publish(long id, byte[] data) throws Exception
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

    static PublisherManager instance;
    public static synchronized PublisherManager getInstance()
    {
        if (null == instance) instance = new PublisherManager();
        return instance;
    }

    static class Publisher extends Thread
    {
        long channel;
        Process process;
        FileChannel fileChannel;
        ByteBuffer byteBuffer;
        String fifoFilePath;
        long lastActiveTime;
        boolean publishing = false;
        Object lock = null;
        LinkedList<byte[]> packets = null;

        public Publisher(long channel)
        {
            this.channel = channel;
            this.fifoFilePath = new File(new File(Configs.get("fifo-pool.path")), channel + ".fifo").getAbsolutePath();
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
            mkfifo(fifoFilePath);
            File fifo = new File(fifoFilePath);
            process = Runtime.getRuntime().exec(String.format("%s -i %s -c copy -f flv %s", Configs.get("ffmpeg.path"), fifo.getAbsolutePath(), rtmpURL));
            fileChannel = FileChannel.open(Paths.get(fifoFilePath), EnumSet.of(StandardOpenOption.WRITE));
            byteBuffer = ByteBuffer.allocate(8192);

            StdoutCleaner.getInstance().watch(channel, process);

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
                    byteBuffer.clear();
                    byteBuffer.put(data);
                    byteBuffer.flip();
                    fileChannel.write(byteBuffer);
                    byteBuffer.flip();
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
                // logger.error("close publish channel failed", ex);
            }
            try { new File(fifoFilePath).delete(); } catch(Exception e) { }
            try { fileChannel.close(); } catch(Exception e) { }
            byteBuffer = null;
            fileChannel = null;
            process = null;

            StdoutCleaner.getInstance().unwatch(channel);
        }

        @Override
        public String toString()
        {
            return "Publisher{hash=" + hashCode() + ", " + "channel=" + channel + ", fifoFilePath='" + fifoFilePath + '\'' + ", lastActiveTime=" + lastActiveTime + '}';
        }
    }

    private static void mkfifo(String path)
    {
        try
        {
            String mkfifoPath = Configs.get("mkfifo.path");
            int exitCode = Runtime.getRuntime().exec(String.format("%s %s", mkfifoPath, path)).waitFor();
            logger.debug(String.format("execute: %s %s ==> %d", mkfifoPath, path, exitCode));
            if (exitCode != 0) throw new RuntimeException("mkfifo error: " + exitCode);
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
