package cn.org.hentai.jtt1078.media.publisher;

import cn.org.hentai.jtt1078.media.Media;
import cn.org.hentai.jtt1078.media.StdoutCleaner;
import cn.org.hentai.jtt1078.util.Configs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.UUID;

/**
 * Created by matrixy on 2019/12/14.
 */
public class Publisher
{
    static Logger logger = LoggerFactory.getLogger(Publisher.class);

    long channel;
    MediaStreamPublisher videoPublisher;
    MediaStreamPublisher audioPublisher;

    Process process = null;

    public Publisher(long channel)
    {
        this.channel = channel;
    }

    public long getChannel()
    {
        return this.channel;
    }

    // 推送是否超时
    public boolean isTimeout()
    {
        return false;
    }

    // 打开推送通道（打开FIFO文件输出流）
    public void open(String rtmpURL) throws Exception
    {
        String videoFifoPath = mkfifo();
        String audioFifoPath = mkfifo();

        process = Runtime.getRuntime().exec(
                String.format("%s -report -re -r 25 -f h264 -i %s -f h264 -f s16le -ar 8000 -ac 1 -i %s -vcodec copy -acodec aac -strict -2 "
                                + " -map 0:v:0 -map 1:a:0 -probesize 512 -analyzeduration 100 -f flv %s",
                                Configs.get("ffmpeg.path"),
                                videoFifoPath,
                                audioFifoPath,
                                rtmpURL
                )
        );

        if ("true".equalsIgnoreCase(Configs.get("ffmpeg.debug")))
            StdoutCleaner.getInstance().watch(channel, process);

        videoPublisher = new VideoPublisher(channel, "video", process);
        audioPublisher = new AudioPublisher(channel, "audio", process);

        videoPublisher.open(videoFifoPath);
        audioPublisher.open(audioFifoPath);

        logger.debug("audio/video publisher started for: {}", channel);
    }

    public boolean publish(Media.Type mediaType, Media.Encoding mediaEncoding, byte[] data)
    {
        // logger.debug("publish: {}, encoding: {}, length: {}", mediaType, mediaEncoding, data.length);
        MediaStreamPublisher publisher = mediaType.equals(Media.Type.Audio) ? audioPublisher : videoPublisher;
        publisher.publish(mediaEncoding, data);
        return true;
    }

    // 关闭推流
    public void close()
    {
        process.destroy();
        logger.info("child process exited...");
        try { audioPublisher.close(); } catch(Exception e) { }
        try { videoPublisher.close(); } catch(Exception e) { }
    }

    private String mkfifo()
    {
        try
        {
            String mkfifoPath = Configs.get("mkfifo.path");
            String path = Configs.get("fifo-pool.path") + File.separator + UUID.randomUUID().toString().replaceAll("-", "");

            int exitCode = Runtime.getRuntime().exec(String.format("%s %s", mkfifoPath, path)).waitFor();
            logger.debug(String.format("execute: %s %s ==> %d", mkfifoPath, path, exitCode));
            if (exitCode != 0) throw new RuntimeException("mkfifo error: " + exitCode);

            return path;
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }
}
