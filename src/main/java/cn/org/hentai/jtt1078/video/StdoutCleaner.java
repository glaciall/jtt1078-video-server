package cn.org.hentai.jtt1078.video;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by matrixy on 2019/6/29.
 */
public class StdoutCleaner extends Thread
{
    static Logger logger = LoggerFactory.getLogger(StdoutCleaner.class);

    Object lock = null;
    HashMap<Long, Process> processes;
    LinkedList<Long> readyToClose;

    private StdoutCleaner()
    {
        lock = new Object();
        processes = new HashMap<>(128);
        readyToClose = new LinkedList<>();

        setName("stdout-cleaner");
    }

    public void watch(Long channel, Process process)
    {
        synchronized (lock)
        {
            processes.put(channel, process);
        }

        logger.debug("watch: {}", channel);
    }

    public void unwatch(Long channel)
    {
        synchronized (lock)
        {
            readyToClose.add(channel);
        }

        logger.debug("unwatch: {}", channel);
    }

    public void run()
    {
        byte[] block = new byte[512];
        while (!this.isInterrupted())
        {
            try
            {
                Iterator<Long> itr = processes.keySet().iterator();
                while (itr.hasNext())
                {
                    Long channel = itr.next();
                    Process process = processes.get(channel);
                    if (process.isAlive() == false)
                    {
                        synchronized (lock)
                        {
                            readyToClose.add(channel);
                        }
                        continue;
                    }

                    // 清理一下输出流
                    InputStream stdout = process.getInputStream();
                    InputStream stderr = process.getErrorStream();

                    int buffLength = 0;
                    try { buffLength = stdout.available(); }
                    catch (IOException e)
                    {
                        synchronized (lock)
                        {
                            readyToClose.add(channel);
                        }
                        break;
                    }
                    if (buffLength > 0) stdout.read(block, 0, Math.min(buffLength, block.length));

                    try { buffLength = stderr.available(); }
                    catch (IOException e)
                    {
                        synchronized (lock)
                        {
                            readyToClose.add(channel);
                        }
                        break;
                    }
                    if (buffLength > 0) stderr.read(block, 0, Math.min(buffLength, block.length));
                }

                synchronized (lock)
                {
                    while (readyToClose.size() > 0)
                    {
                        Long channel = readyToClose.removeFirst();
                        if (processes.containsKey(channel))
                        {
                            processes.remove(channel);
                        }
                    }
                }

                Thread.sleep(2);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    static StdoutCleaner instance;

    public static synchronized void init()
    {
        instance = new StdoutCleaner();
        instance.start();
    }

    public static synchronized StdoutCleaner getInstance()
    {
        return instance;
    }
}
