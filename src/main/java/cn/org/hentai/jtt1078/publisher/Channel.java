package cn.org.hentai.jtt1078.publisher;

import cn.org.hentai.jtt1078.codec.AudioCodec;
import cn.org.hentai.jtt1078.flv.FlvEncoder;
import cn.org.hentai.jtt1078.util.ByteHolder;
import cn.org.hentai.jtt1078.subscriber.AudioSubscriber;
import cn.org.hentai.jtt1078.subscriber.Subscriber;
import cn.org.hentai.jtt1078.subscriber.VideoSubscriber;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by matrixy on 2020/1/11.
 */
public class Channel
{
    static Logger logger = LoggerFactory.getLogger(Channel.class);

    LinkedList<Subscriber> videoSubscribers;
    LinkedList<Subscriber> audioSubscribers;

    String tag;
    ByteHolder buffer;
    AudioCodec audioCodec;
    FlvEncoder flvEncoder;

    public Channel(String tag)
    {
        this.tag = tag;
        this.videoSubscribers = new LinkedList<Subscriber>();
        this.audioSubscribers = new LinkedList<Subscriber>();
        this.flvEncoder = new FlvEncoder(true, false);
        this.buffer = new ByteHolder(409600);
    }

    public Subscriber subscribeVideo(ChannelHandlerContext ctx)
    {
        logger.info("channel: {} -> {}, video subscriber: {}", Long.toHexString(hashCode() & 0xffffffffL), tag, ctx.channel().remoteAddress().toString());

        Subscriber subscriber = new VideoSubscriber(this.tag, ctx);
        this.videoSubscribers.addLast(subscriber);
        return subscriber;
    }

    public Subscriber subscribeAudio(ChannelHandlerContext ctx)
    {
        logger.info("channel: {} -> {}, audio subscriber: {}", Long.toHexString(hashCode() & 0xffffffffL), tag, ctx.channel().remoteAddress().toString());

        Subscriber subscriber = new AudioSubscriber(this.tag, ctx);
        this.audioSubscribers.addLast(subscriber);
        return subscriber;
    }

    public void writeAudio(long sequence, long timeoffset, int payloadType, byte[] raw)
    {
        if (this.audioCodec == null) this.audioCodec = AudioCodec.getCodec(payloadType);
        byte[] pcmData = this.audioCodec.toPCM(raw);
        broadcastAudio(timeoffset, pcmData);
    }

    public void writeVideo(long sequence, long timeoffset, int payloadType, byte[] h264)
    {
        this.buffer.write(h264);
        while (true)
        {
            byte[] nalu = readNalu();
            if (nalu == null) break;

            byte[] flvTag = this.flvEncoder.write(nalu, 0);

            // 广播给所有的观众
            broadcastVideo(timeoffset, flvTag);

            // logger.info("broadcast: {}", Long.toHexString(hashCode() & 0xffffffffL));
        }
    }

    public void broadcastAudio(long timeoffset, byte[] pcm)
    {
        for (Subscriber subscriber : audioSubscribers)
        {
            subscriber.onData(timeoffset, pcm, flvEncoder);
        }
    }

    public void broadcastVideo(long timeoffset, byte[] flvTag)
    {
        for (Subscriber subscriber : videoSubscribers)
        {
            subscriber.onData(timeoffset, flvTag, flvEncoder);
        }
    }

    public void unsubscribe(long watcherId)
    {
        for (Iterator<Subscriber> itr = audioSubscribers.iterator(); itr.hasNext(); )
        {
            Subscriber subscriber = itr.next();
            if (subscriber.getId() == watcherId)
            {
                itr.remove();
                return;
            }
        }

        for (Iterator<Subscriber> itr = videoSubscribers.iterator(); itr.hasNext(); )
        {
            Subscriber subscriber = itr.next();
            if (subscriber.getId() == watcherId)
            {
                itr.remove();
                return;
            }
        }
    }

    public void close()
    {
        for (Iterator<Subscriber> itr = audioSubscribers.iterator(); itr.hasNext(); )
        {
            Subscriber subscriber = itr.next();
            subscriber.close();
            itr.remove();
        }

        for (Iterator<Subscriber> itr = videoSubscribers.iterator(); itr.hasNext(); )
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
                i = 0;

                if (nalu.length < 4)
                {
                    i = 0;
                    buffer.slice(nalu.length);
                    continue;
                }

                return nalu;
            }
        }
        return null;
    }
}
