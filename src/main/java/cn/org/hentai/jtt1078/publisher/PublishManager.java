package cn.org.hentai.jtt1078.publisher;

import cn.org.hentai.jtt1078.publisher.entity.Stream;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by houcheng on 2019-12-11.
 */
public final class PublishManager
{
    ConcurrentHashMap<String, ConcurrentLinkedDeque<Stream>> channels;

    // 1. 订阅是怎么订阅的？

    // 2. 需要持续持有关键祯

    // 3. 需要能够删除上一个系列

    // 4. 需要记录每个订阅者的索引

    // 5. 需要提供视频与音频

    static final PublishManager instance = new PublishManager();
    public static void init() { }
    public static PublishManager getInstance()
    {
        return instance;
    }
}
