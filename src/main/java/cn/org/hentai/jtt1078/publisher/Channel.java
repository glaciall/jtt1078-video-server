package cn.org.hentai.jtt1078.publisher;

import cn.org.hentai.jtt1078.codec.AudioCodec;
import cn.org.hentai.jtt1078.flv.AudioTag;
import cn.org.hentai.jtt1078.flv.FlvAudioTagEncoder;
import cn.org.hentai.jtt1078.flv.FlvEncoder;
import cn.org.hentai.jtt1078.subscriber.AudioSubscriber;
import cn.org.hentai.jtt1078.subscriber.Subscriber;
import cn.org.hentai.jtt1078.subscriber.VideoSubscriber;
import cn.org.hentai.jtt1078.util.ByteBufUtils;
import cn.org.hentai.jtt1078.util.ByteHolder;
import de.sciss.jump3r.lowlevel.LameEncoder;
import de.sciss.jump3r.mp3.Lame;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.util.Length;

import javax.sound.sampled.AudioFormat;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    boolean publishing;
    ByteHolder buffer;
    AudioCodec audioCodec;
    FlvEncoder flvEncoder;
    private long firstTimestamp;
    private ByteBuf audioContent = Unpooled.buffer();
    //需要缓冲一定量的样本数据才能转换
    private int audioBufSize = 1024 * 20;
    private FlvAudioTagEncoder audioEncoder = new FlvAudioTagEncoder();
    private final File tmpFile ;
    private FileOutputStream fileOutputStream ;

    public Channel(String tag)
    {
        this.tag = tag;
        this.videoSubscribers = new LinkedList<Subscriber>();
        this.audioSubscribers = new LinkedList<Subscriber>();
        this.flvEncoder = new FlvEncoder(true, true);
        this.buffer = new ByteHolder(409600);
        this.tmpFile = new File(getClass().getClassLoader().getResource("").getFile() + File.separator + tag + "tmp.mp3");
        if (tmpFile.exists()) {
            tmpFile.delete();
        }
    }

    public boolean isPublishing()
    {
        return publishing;
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
        this.publishing = true;
        if (this.audioCodec == null) this.audioCodec = AudioCodec.getCodec(payloadType);
        byte[] pcmData = this.audioCodec.toPCM(raw);
        broadcastAudio(timeoffset, pcmData);
    }

    public void writeA(long timestamp, int pt, byte[] data) {
        if (firstTimestamp == 0) firstTimestamp = timestamp;
        if (this.audioCodec == null) this.audioCodec = AudioCodec.getCodec(pt);
        byte[] pcmData = this.audioCodec.toPCM(data);
        writeFile(pcmData);
        audioContent.writeBytes(pcmData);
        if (audioContent.readableBytes() < audioBufSize) return;
        byte[] mp3Data = encodePcmToMp3(ByteBufUtils.readReadableBytes(audioContent));
        audioContent.discardReadBytes();
        AudioTag audioTag = newMp3AudioTag(mp3Data, AudioTag.MP3, (byte) 0, (int) (timestamp - firstTimestamp));
        try {
            ByteBuf audioBuf = audioEncoder.encode(audioTag);
            byte[] frameData = ByteBufUtils.readReadableBytes(audioBuf);
            for (Subscriber videoSubscriber : videoSubscribers) {
                videoSubscriber.onData(0,frameData,flvEncoder);
            }
//            lastATimeStamp = -1;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeFile(byte[] mp3Data) {
        try {
            if (!tmpFile.exists()) {
            tmpFile.createNewFile();
            }
            fileOutputStream = new FileOutputStream(tmpFile,true);
            fileOutputStream.write(mp3Data);
        } catch (IOException e) {
            logger.warn("创建临时文件错误",e);
        }
    }

    private AudioTag newMp3AudioTag(byte[] audioData, byte flvAudioType, byte rate, int offSetTimeStamp) {
        //一个字节的格式和码流等信息
        AudioTag audioTag = new AudioTag(offSetTimeStamp,audioData.length+1,AudioTag.MP3,rate,(byte)1,(byte)0,audioData);
        return audioTag;
    }
    public byte[] encodePcmToMp3(byte[] pcm) {
        AudioFormat sourceFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 8000, 16, 1, 1 * 2, -1, false);
        LameEncoder encoder = new LameEncoder(sourceFormat, 256, 3, Lame.MEDIUM, false);

        ByteArrayOutputStream mp3 = new ByteArrayOutputStream();
        byte[] buffer = new byte[encoder.getPCMBufferSize()];

        int bytesToTransfer = Math.min(buffer.length, pcm.length);
        int bytesWritten;
        int currentPcmPosition = 0;
        while (0 < (bytesWritten = encoder.encodeBuffer(pcm, currentPcmPosition, bytesToTransfer, buffer))) {
            currentPcmPosition += bytesToTransfer;
            bytesToTransfer = Math.min(buffer.length, pcm.length - currentPcmPosition);

            mp3.write(buffer, 0, bytesWritten);
        }
        encoder.close();
        return mp3.toByteArray();
    }
    public void writeVideo(long sequence, long timeoffset, int payloadType, byte[] h264)
    {
        if (firstTimestamp == 0) firstTimestamp = timeoffset;
        this.publishing = true;
        this.buffer.write(h264);
        while (true)
        {
            byte[] nalu = readNalu();
            if (nalu == null) break;
            if (nalu.length < 4) continue;

            byte[] flvTag = this.flvEncoder.write(nalu, (int) (timeoffset-firstTimestamp));
            if (flvTag == null) continue;

            // 广播给所有的观众
            broadcastVideo(timeoffset, flvTag);
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
                return nalu;
            }
        }
        return null;
    }
}
