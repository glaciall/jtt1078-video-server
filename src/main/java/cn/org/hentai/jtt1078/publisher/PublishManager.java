package cn.org.hentai.jtt1078.publisher;

import cn.org.hentai.jtt1078.publisher.entity.Media;
import cn.org.hentai.jtt1078.publisher.entity.Video;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by houcheng on 2019-12-11.
 */
public final class PublishManager
{
    static Logger logger = LoggerFactory.getLogger(PublishManager.class);

    ConcurrentHashMap<String, ConcurrentLinkedDeque<Media>> channelMap;
    ConcurrentHashMap<String, ConcurrentLinkedQueue<Subscriber>> subscriberMap;

    private PublishManager()
    {
        channelMap = new ConcurrentHashMap<String, ConcurrentLinkedDeque<Media>>();
        subscriberMap = new ConcurrentHashMap<String, ConcurrentLinkedQueue<Subscriber>>();
    }

    // 1. 订阅是怎么订阅的？

    // 2. 需要持续持有关键祯

    // 3. 需要能够删除上一个系列

    // 4. 需要记录每个订阅者的索引

    // 5. 需要提供视频与音频
    public void subscribe(String tag, ChannelHandlerContext ctx)
    {
        ConcurrentLinkedQueue<Subscriber> listeners = subscriberMap.get(tag);
        if (listeners == null)
        {
            listeners = new ConcurrentLinkedQueue<Subscriber>();
            subscriberMap.put(tag, listeners);
        }

        Subscriber subscriber = new Subscriber(tag, ctx);
        subscriber.setName("subscriber-" + ctx.channel().remoteAddress().toString());
        listeners.add(subscriber);
        subscriber.start();

        // TODO: 先取一个完整的关键祯序列下来，怎么锁？
    }

    public void publish(String tag, Media media)
    {
        // 是不是考虑不做缓存？
        ConcurrentLinkedDeque<Media> medias = channelMap.get(tag);
        if (medias == null)
        {
            medias = new ConcurrentLinkedDeque<Media>();
            channelMap.put(tag, medias);
        }

        // 如果是视频关键祯，则删除掉前面的缓存祯
        /*
        if (media.type.equals(Media.Type.video))
        {
            Video video = (Video)media;
            if (video.isKeyFrame)
            {
                while (medias.size() > 0)
                {
                    Media m = medias.removeFirst();
                    if (m == null) break;
                }
            }
        }
        */

        // medias.addLast(media);

        // 广播到所有的订阅者，直接发，先不等待关键祯
        ConcurrentLinkedQueue<Subscriber> listeners = subscriberMap.get(tag);
        if (listeners == null)
        {
            listeners = new ConcurrentLinkedQueue<Subscriber>();
            subscriberMap.put(tag, listeners);
        }
        for (Subscriber listener : listeners)
        {
            try
            {
                listener.aware(media);
            }
            catch(Exception ex)
            {
                logger.error("send error", ex);
                listeners.remove(listener);
            }
        }
    }

    static final PublishManager instance = new PublishManager();
    public static void init() { }
    public static PublishManager getInstance()
    {
        return instance;
    }
}
