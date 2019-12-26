package cn.org.hentai.jtt1078.video;

import cn.org.hentai.jtt1078.util.Configs;

import java.io.OutputStream;
import java.util.LinkedList;

/**
 * Created by matrixy on 2019/12/15.
 */
public class VideoFeeder extends Thread
{
    long channel;
    Process process;
    OutputStream output;
    long lastActiveTime;
    boolean publishing = false;
    Object lock = null;
    LinkedList<byte[]> packets = null;

    public VideoFeeder(long channel)
    {
        this.channel = channel;
        this.lastActiveTime = System.currentTimeMillis();
        this.lock = new Object();
        this.packets = new LinkedList<>();

        this.setName("video-feeder-" + channel);
    }

    public boolean isTimeout()
    {
        if (publishing) return false;
        return System.currentTimeMillis() - lastActiveTime > 5000;
    }

    public void open(String tag) throws Exception
    {
        process = Runtime.getRuntime().exec(
                String.format(Configs.get("ffmpeg.command.pattern"), Configs.get("ffmpeg.path")));
        output = process.getOutputStream();
        new VideoPublisher(tag, process.getInputStream()).start();
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
        return "VideoFeeder{hash=" + hashCode() + ", " + "channel=" + channel + ", lastActiveTime=" + lastActiveTime + '}';
    }
}
