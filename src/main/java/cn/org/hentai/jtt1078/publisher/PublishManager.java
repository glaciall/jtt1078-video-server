package cn.org.hentai.jtt1078.publisher;

import cn.org.hentai.jtt1078.entity.Media;
import cn.org.hentai.jtt1078.subscriber.AudioSubscriber;
import cn.org.hentai.jtt1078.subscriber.Subscriber;
import cn.org.hentai.jtt1078.subscriber.VideoSubscriber;
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

    public void subscribe(String tag, ChannelHandlerContext ctx)
    {
        ConcurrentLinkedQueue<Subscriber> listeners = subscriberMap.get(tag);
        if (listeners == null)
        {
            listeners = new ConcurrentLinkedQueue<Subscriber>();
            subscriberMap.put(tag, listeners);
        }

        Subscriber subscriber = null;
        if (tag.startsWith("video"))
        {
            subscriber = new VideoSubscriber(tag, ctx);
            // 如果已经有视频流了，那就把前三个关键片断发过去
            ConcurrentLinkedDeque<Media> segments = channelMap.get(tag);
            if (segments != null)
            {
                int i = 0;
                for (Media media : segments)
                {
                    if (i++ == 3) break;
                    subscriber.aware(media);
                }
            }
        }
        else
        {
            subscriber = new AudioSubscriber(tag, ctx);
        }

        listeners.add(subscriber);
        subscriber.start();
    }

    public void publish(String tag, Media media)
    {
        // if (tag.startsWith("video")) logger.info("published: {}", media.sequence);
        ConcurrentLinkedDeque<Media> segments = channelMap.get(tag);
        if (segments == null)
        {
            segments = new ConcurrentLinkedDeque<Media>();
            channelMap.put(tag, segments);
        }

        // 如果是音频，就先缓存起来
        // 如果是视频，就把这个时间点以前的音频全部广播出去

        // 只用缓存前三个消息包就可以了
        if (tag.startsWith("video"))
        {
            if (segments.size() < 3) segments.addLast(media);

            long currentVideoIndex = media.sequence;

            // 广播这个序号之前的所有音频片段
            String audioTag = tag.replace("video", "audio");
            ConcurrentLinkedDeque<Media> audioSegments = channelMap.get(audioTag);
            if (audioSegments != null && audioSegments.size() > 0)
            {
                ConcurrentLinkedQueue<Subscriber> listeners = subscriberMap.get(audioTag);
                while (listeners != null && audioSegments.size() > 0)
                {
                    Media audio = audioSegments.removeFirst();
                    if (audio.sequence >= currentVideoIndex)
                    {
                        audioSegments.addFirst(audio);
                        break;
                    }
                    for (Subscriber listener : listeners)
                    {
                        try
                        {
                            listener.aware(audio);
                        }
                        catch(Exception ex)
                        {
                            logger.error("aware failed", ex);
                            listeners.remove(listener);
                        }
                    }
                }
            }
        }
        if (tag.startsWith("audio"))
        {
            segments.addLast(media);
            return;
        }

        // 广播到所有的订阅者，直接发，先不等待关键祯
        ConcurrentLinkedQueue<Subscriber> listeners = subscriberMap.get(tag);
        if (listeners == null) return;

        for (Subscriber listener : listeners)
        {
            try
            {
                listener.aware(media);
            }
            catch(Exception ex)
            {
                logger.error("aware error", ex);
                listeners.remove(listener);
            }
        }
    }

    // 释放，chunked连接断开时，释放subscriberMap里的内容
    // ffmpeg退出时，释放flv tag


    static final PublishManager instance = new PublishManager();
    public static void init() { }
    public static PublishManager getInstance()
    {
        return instance;
    }
}
