package cn.org.hentai.jtt1078.video;

import cn.org.hentai.jtt1078.publisher.PublishManager;
import cn.org.hentai.jtt1078.publisher.entity.Video;
import cn.org.hentai.jtt1078.util.ByteHolder;
import cn.org.hentai.jtt1078.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by matrixy on 2019/12/15.
 */
public class VideoPublisher extends Thread
{
    static Logger logger = LoggerFactory.getLogger(VideoPublisher.class);
    static final AtomicLong sequence = new AtomicLong(0L);

    private String tag;
    private InputStream input;

    public VideoPublisher(String tag, InputStream input)
    {
        this.tag = tag;
        this.input = input;

        this.setName("video-publisher-" + sequence.addAndGet(1));
    }

    public void run()
    {
        int len = -1;
        byte[] block = new byte[1024];
        ByteHolder buffer = new ByteHolder(1024 * 32);

        boolean eof = false;
        boolean headerParsed = false;

        while (!this.isInterrupted())
        {
            try
            {
                len = input.read(block);
                if (len == -1) break;
                buffer.write(block, 0, len);

                eof = false;
                while (!eof)
                {
                    if (!headerParsed)
                    {
                        eof = parseHeader(buffer);
                        if (!eof) headerParsed = true;
                    }
                    else eof = parseTag(buffer);
                }
            }
            catch(Exception ex)
            {
                logger.error("publish error", ex);
                break;
            }
        }
        try { input.close(); } catch(Exception e) { }
    }

    private boolean parseHeader(ByteHolder buffer)
    {
        if (buffer.size() < 13) return true;
        int version = buffer.get(3) & 0xff;
        int flag = buffer.get(4);
        int headerSize = buffer.getInt(5);
        boolean hasAudio = (flag & (1 << 2)) > 0;
        boolean hasVideo = (flag & (1 << 0)) > 0;
        System.out.println(String.format("FLV [ version: %d, audio: %s, video: %s, header: %d ]", version, hasAudio, hasVideo, headerSize));
        byte[] header = new byte[13];
        buffer.sliceInto(header, 13);
        PublishManager.getInstance().publish(tag, new Video(Video.FlvType.Header, header));
        return false;
    }

    private boolean parseTag(ByteHolder buffer)
    {
        if (buffer.size() < 16) return true;
        int type = buffer.get(0) & 0xff;                                         // 0 : TAG类型，8为audio，9为video，18为script data，其它值为保留值
        int size = (buffer.getInt(1) >> 8) & 0xffffff;                  // 1 - 3 : 数据体大小，除了header(固定11字节）
        int timeLow = (buffer.getInt(4) >> 8) & 0xffffff;             // 4 - 6 ： 时间戳，从0开始的过去的毫秒数
        int timeHigh = buffer.get(7);                                         // 7 ： 时间戳的高8位，组成32位
        int streamId = (buffer.getInt(8) >> 8) & 0xffffff;                       // 8 - 10 ： 流ID，总是0

        if (buffer.size() < size + 15) return true;
        int timestamp = timeLow | (timeHigh << 24);

        System.out.println(String.format("Tag [ type: %2d, size: %8d, timestamp: %10d, stream: %2d ]", type, size, timestamp, streamId));
        byte[] data = new byte[size + 15];
        buffer.sliceInto(data, size + 15);
        PublishManager.getInstance().publish(tag, new Video(type == 18 ? Video.FlvType.Description : Video.FlvType.Tag, data));
        return false;
    }
}
