package cn.org.hentai.jtt1078.publisher;

import cn.org.hentai.jtt1078.codec.AudioCodec;
import cn.org.hentai.jtt1078.codec.MP3Encoder;
import cn.org.hentai.jtt1078.entity.Media;
import cn.org.hentai.jtt1078.entity.MediaEncoding;
import cn.org.hentai.jtt1078.flv.AudioTag;
import cn.org.hentai.jtt1078.flv.FlvAudioTagEncoder;
import cn.org.hentai.jtt1078.flv.FlvEncoder;
import cn.org.hentai.jtt1078.subscriber.Subscriber;
import cn.org.hentai.jtt1078.subscriber.VideoSubscriber;
import cn.org.hentai.jtt1078.util.ByteBufUtils;
import cn.org.hentai.jtt1078.util.ByteHolder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by matrixy on 2020/1/11.
 */
public class Channel
{
    static Logger logger = LoggerFactory.getLogger(Channel.class);

    ConcurrentLinkedQueue<Subscriber> subscribers;

    String tag;
    boolean publishing;
    ByteHolder buffer;
    AudioCodec audioCodec;
    FlvEncoder flvEncoder;
    private long firstTimestamp = -1;

    public Channel(String tag)
    {
        this.tag = tag;
        this.subscribers = new ConcurrentLinkedQueue<Subscriber>();
        this.flvEncoder = new FlvEncoder(true, true);
        this.buffer = new ByteHolder(2048 * 100);
    }

    public boolean isPublishing()
    {
        return publishing;
    }

    public Subscriber subscribe(ChannelHandlerContext ctx)
    {
        logger.info("channel: {} -> {}, subscriber: {}", Long.toHexString(hashCode() & 0xffffffffL), tag, ctx.channel().remoteAddress().toString());

        Subscriber subscriber = new VideoSubscriber(this.tag, ctx);
        this.subscribers.add(subscriber);
        return subscriber;
    }

    public void writeAudio(long timestamp, int pt, byte[] data)
    {
        if (audioCodec == null)
        {
            audioCodec = AudioCodec.getCodec(pt);
            logger.info("audio codec: {}", MediaEncoding.getEncoding(Media.Type.Audio, pt));
        }
        broadcastAudio(timestamp, audioCodec.toPCM(data));
    }

    public void writeVideo(long sequence, long timeoffset, int payloadType, byte[] h264)
    {
        if (firstTimestamp == -1) firstTimestamp = timeoffset;
        this.publishing = true;
        this.buffer.write(h264);
        while (true)
        {
            byte[] nalu = readNalu();
            if (nalu == null) break;
            if (nalu.length < 4) continue;

            byte[] flvTag = this.flvEncoder.write(nalu, (int) (timeoffset - firstTimestamp));

            if (flvTag == null) continue;

            // 广播给所有的观众
            broadcastVideo(timeoffset, flvTag);
        }
    }

    public void broadcastVideo(long timeoffset, byte[] flvTag)
    {
        for (Subscriber subscriber : subscribers)
        {
            subscriber.onVideoData(timeoffset, flvTag, flvEncoder);
        }
    }

    public void broadcastAudio(long timeoffset, byte[] flvTag)
    {
        for (Subscriber subscriber : subscribers)
        {
            subscriber.onAudioData(timeoffset, flvTag, flvEncoder);
        }
    }

    public void unsubscribe(long watcherId)
    {
        for (Iterator<Subscriber> itr = subscribers.iterator(); itr.hasNext(); )
        {
            Subscriber subscriber = itr.next();
            if (subscriber.getId() == watcherId)
            {
                itr.remove();
                subscriber.close();
                return;
            }
        }
    }

    public void close()
    {
        for (Iterator<Subscriber> itr = subscribers.iterator(); itr.hasNext(); )
        {
            Subscriber subscriber = itr.next();
            subscriber.close();
            itr.remove();
        }
    }

    private byte[] readNalu()
    {
        for (int i = 0; i < buffer.size(); i++)
        {
            int a = buffer.get(i + 0) & 0xff;
            int b = buffer.get(i + 1) & 0xff;
            int c = buffer.get(i + 2) & 0xff;
            int d = buffer.get(i + 3) & 0xff;
            if (a == 0x00 && b == 0x00 && c == 0x00 && d == 0x01)
            {
                if (i == 0) continue;
                byte[] nalu = new byte[i];
                buffer.sliceInto(nalu, i);
                return nalu;
            }
        }
        return null;
    }
}
