package cn.org.hentai.jtt1078.server;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by matrixy on 2019/4/10.
 */
public final class Session
{
    Map<String, Object> attributes;
    public Session()
    {
        this.attributes = new HashMap<String, Object>();
    }

    public Session set(String key, Object value)
    {
        this.attributes.put(key, value);
        return this;
    }

    public boolean has(String key)
    {
        return this.attributes.containsKey(key);
    }

    public Set<String> keys()
    {
        return this.attributes.keySet();
    }

    public <T> T get(String key)
    {
        return (T) this.attributes.get(key);
    }
}
