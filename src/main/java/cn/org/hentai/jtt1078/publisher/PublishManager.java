package cn.org.hentai.jtt1078.publisher;

import cn.org.hentai.jtt1078.entity.Media;
import cn.org.hentai.jtt1078.subscriber.Subscriber;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by houcheng on 2019-12-11.
 */
public final class PublishManager
{
    static Logger logger = LoggerFactory.getLogger(PublishManager.class);
    ConcurrentHashMap<String, Channel> channels;

    private PublishManager()
    {
        channels = new ConcurrentHashMap<String, Channel>();
    }

    public Subscriber subscribe(String tag, Media.Type type, ChannelHandlerContext ctx)
    {
        Channel chl = channels.get(tag);
        if (chl == null)
        {
            chl = new Channel(tag);
            channels.put(tag, chl);
        }
        Subscriber subscriber = null;
        if (type.equals(Media.Type.Video)) subscriber = chl.subscribe(ctx);
        else throw new RuntimeException("unknown media type: " + type);

        subscriber.setName("subscriber-" + tag + "-" + subscriber.getId());
        subscriber.start();

        return subscriber;
    }

    public void publishAudio(String tag, int sequence, long timestamp, int payloadType, byte[] data)
    {
        Channel chl = channels.get(tag);
        if (chl != null) chl.writeAudio(timestamp, payloadType, data);
    }

    public void publishVideo(String tag, int sequence, long timestamp, int payloadType, byte[] data)
    {
        Channel chl = channels.get(tag);
        if (chl != null) chl.writeVideo(sequence, timestamp, payloadType, data);
    }

    public Channel open(String tag)
    {
        Channel chl = channels.get(tag);
        if (chl == null)
        {
            chl = new Channel(tag);
            channels.put(tag, chl);
        }
        if (chl.isPublishing()) throw new RuntimeException("channel already publishing");
        return chl;
    }

    public void close(String tag)
    {
        Channel chl = channels.remove(tag);
        if (chl != null) chl.close();
    }

    public void unsubscribe(String tag, long watcherId)
    {
        Channel chl = channels.get(tag);
        if (chl != null) chl.unsubscribe(watcherId);
        logger.info("unsubscribe: {} - {}", tag, watcherId);
    }
    static final PublishManager instance = new PublishManager();
    public static void init() { }

    public static PublishManager getInstance()
    {
        return instance;
    }
}
