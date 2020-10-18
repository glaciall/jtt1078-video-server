package cn.org.hentai.jtt1078.subscriber;

import cn.org.hentai.jtt1078.codec.MP3Encoder;
import cn.org.hentai.jtt1078.flv.AudioTag;
import cn.org.hentai.jtt1078.flv.FlvAudioTagEncoder;
import cn.org.hentai.jtt1078.flv.FlvEncoder;
import cn.org.hentai.jtt1078.util.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

public class RTMPPublisher extends Thread
{
    static Logger logger = LoggerFactory.getLogger(RTMPPublisher.class);

    String tag = null;
    Process process = null;

    public RTMPPublisher(String tag)
    {
        this.tag = tag;
    }

    @Override
    public void run()
    {
        InputStream stderr = null;
        int len = -1;
        byte[] buff = new byte[512];
        boolean debugMode = "on".equalsIgnoreCase(Configs.get("debug.mode"));

        try
        {
            String rtmpUrl = Configs.get("rtmp.url").replaceAll("\\{TAG\\}", tag);
            String cmd = String.format("%s -i http://localhost:%d/video/%s -vcodec copy -acodec aac -f flv %s",
                        Configs.get("ffmpeg.path"),
                        Configs.getInt("server.http.port", 3333),
                        tag,
                        rtmpUrl
                    );
            logger.info("Execute: {}", cmd);
            process = Runtime.getRuntime().exec(cmd);
            stderr = process.getErrorStream();
            while ((len = stderr.read(buff)) > -1)
            {
                if (debugMode) System.out.print(new String(buff, 0, len));
            }
            logger.info("Process FFMPEG exited...");
        }
        catch(Exception ex)
        {
            logger.error("publish failed", ex);
        }
    }

    public void close()
    {
        try { if (process != null) process.destroyForcibly(); } catch(Exception e) { }
    }
}