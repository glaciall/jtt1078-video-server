package cn.org.hentai.jtt1078.media.publisher;

import cn.org.hentai.jtt1078.media.Media;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;

/**
 * Created by matrixy on 2019/12/14.
 */
public abstract class MediaStreamPublisher extends Thread
{
    static Logger logger = LoggerFactory.getLogger(MediaStreamPublisher.class);

    long channel;
    Process process;
    long lastActiveTime;
    boolean publishing = false;
    String fifoPath = null;
    OutputStream output;
    Object lock = null;
    LinkedList<MediaPacket> packets = null;

    public MediaStreamPublisher(long channel, String tag, Process process)
    {
        this.channel = channel;
        this.process = process;
        this.lastActiveTime = System.currentTimeMillis();
        this.lock = new Object();
        this.packets = new LinkedList<>();

        this.setName("publisher-" + tag + "-" + channel);
    }

    public boolean isTimeout()
    {
        if (publishing) return false;
        return System.currentTimeMillis() - lastActiveTime > 5000;
    }

    public void open(String fifoPath) throws Exception
    {
        this.fifoPath = fifoPath;
        this.start();
    }

    // 将data写入到FIFO文件中去，好让
    public boolean publish(Media.Encoding mediaEncoding, byte[] data)
    {
        if (process.isAlive() == false) return false;
        synchronized (lock)
        {
            packets.add(new MediaPacket(mediaEncoding, data));
            lock.notify();
        }
        return true;
    }

    public void run()
    {
        while (!Thread.interrupted())
        {
            try
            {
                MediaPacket packet = null;
                synchronized (lock)
                {
                    while (Thread.interrupted() == false && packets.size() == 0) try { lock.wait(); } catch(Exception e) { }
                    if (packets.size() == 0) break;
                    packet = packets.removeFirst();
                }

                if (output == null)
                {
                    output = new FileOutputStream(fifoPath);
                    Thread.sleep(100);
                }
                publishing = true;
                try
                {
                    transcodeTo(packet.mediaEncoding, packet.mediaData, output);
                }
                catch(Exception e)
                {
                    throw new RuntimeException(e);
                }
                publishing = false;
                this.lastActiveTime = System.currentTimeMillis();
            }
            catch(Exception ex)
            {
                logger.error("data transcode error", ex);
                break;
            }
        }
    }

    public abstract void transcodeTo(Media.Encoding mediaEncoding, byte[] data, OutputStream output) throws Exception;

    // 回收，关闭掉相关的资源
    public void close()
    {
        try
        {
            this.interrupt();
            synchronized (lock)
            {
                lock.notifyAll();
            }
        }
        catch(Exception e) { }

        try { if (output != null) output.close(); } catch(Exception e) { }
    }

    static class MediaPacket
    {
        public Media.Encoding mediaEncoding;
        public byte[] mediaData;

        public MediaPacket(Media.Encoding mediaEncoding, byte[] mediaData)
        {
            this.mediaEncoding = mediaEncoding;
            this.mediaData = mediaData;
        }
    }
}
