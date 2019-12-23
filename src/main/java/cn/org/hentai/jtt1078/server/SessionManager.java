package cn.org.hentai.jtt1078.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by matrixy on 2019/12/23.
 */
public final class SessionManager
{
    AtomicLong sequence;
    ConcurrentHashMap<String, Long> sessions;

    private SessionManager()
    {
        sequence = new AtomicLong(0L);
        sessions = new ConcurrentHashMap<String, Long>();
    }

    public long register(String tag)
    {
        if (sessions.containsKey(tag)) throw new RuntimeException("channel already publishing");
        long seq = sequence.addAndGet(1L);
        sessions.put(tag, seq);
        return seq;
    }

    public boolean isAlive(String tag)
    {
        return sessions.containsKey(tag);
    }

    public void unregister(String tag)
    {
        sessions.remove(tag);
    }

    private static final SessionManager instance = new SessionManager();
    public static void init()
    {
        // ..
    }

    public static SessionManager getInstance()
    {
        return instance;
    }
}
