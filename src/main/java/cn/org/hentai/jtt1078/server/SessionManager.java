package cn.org.hentai.jtt1078.server;

import io.netty.channel.Channel;
import org.apache.commons.collections.map.HashedMap;

import java.util.Map;

public final class SessionManager
{
    private static final Map<String, Object> mappings = new HashedMap();

    public static void init()
    {
        // ...
    }

    public static <T> T get(Channel channel, String key)
    {
        return (T) mappings.get(channel.id().asLongText() + key);
    }

    public static void set(Channel channel, String key, Object value)
    {
        mappings.put(channel.id().asLongText() + key, value);
    }

    public static boolean contains(Channel channel, String key)
    {
        return mappings.containsKey(channel.id().asLongText() + key);
    }
}