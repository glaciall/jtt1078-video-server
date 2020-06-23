package cn.org.hentai.jtt1078.subscriber;

import cn.org.hentai.jtt1078.codec.MP3Encoder;
import cn.org.hentai.jtt1078.flv.AudioTag;
import cn.org.hentai.jtt1078.flv.FlvAudioTagEncoder;
import cn.org.hentai.jtt1078.flv.FlvEncoder;
import cn.org.hentai.jtt1078.util.*;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

public class RTMPSubscriber extends VideoSubscriber
{
    Process process = null;
    OutputStream outputStream = null;

    long videoTimestamp = 0;
    long audioTimestamp = 0;
    long lastVideoFrameTimeOffset = 0;
    long lastAudioFrameTimeOffset = 0;
    boolean videoHeaderSent = false;

    public RTMPSubscriber(String tag)
    {
        super(tag, null);
        logger.info("RMTP publisher started...");
    }

    @Override
    public void run()
    {
        loop : while (!this.isInterrupted())
        {
            try
            {
                byte[] data = take();
                if (data == null) continue;

                if (process == null)
                {
                    String cmd = String.format("%s -analyzeduration 100 -probesize 512 -f flv -i - -c copy -f flv %s", Configs.get("ffmpeg.path"), Configs.get("rtmp.url").replaceAll("\\{TAG\\}", this.getTag()));
                    process = Runtime.getRuntime().exec(cmd);
                    outputStream = process.getOutputStream();
                    final InputStream inputStream = process.getErrorStream();
                    new Thread()
                    {
                        public void run()
                        {
                            while (true)
                            {
                                try
                                {
                                    int bufLen = inputStream.available();
                                    byte[] buf = new byte[bufLen];
                                    inputStream.read(buf, 0, bufLen);
                                    System.out.print(new String(buf, 0, bufLen, "GBK"));
                                }
                                catch(Exception ex)
                                {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }.start();
                }

                outputStream.write(data);
                outputStream.flush();
            }
            catch(Exception ex)
            {
                //销毁线程时，如果有锁wait就不会销毁线程，抛出InterruptedException异常
                if (ex instanceof InterruptedException)
                {
                    break loop;
                }
                logger.error("send failed", ex);
            }
        }
        logger.info("rtmp publisher closed");

        try { outputStream.close(); } catch(Exception e) { }
        try { process.destroy(); } catch(Exception e) { }
    }

    @Override
    public void onVideoData(long timeoffset, byte[] data, FlvEncoder flvEncoder)
    {
        if (lastVideoFrameTimeOffset == 0) lastVideoFrameTimeOffset = timeoffset;

        // 之前是不是已经发送过了？没有的话，需要补发FLV HEADER的。。。
        if (videoHeaderSent == false && flvEncoder.videoReady())
        {
            enqueue(flvEncoder.getHeader().getBytes());
            enqueue(flvEncoder.getVideoHeader().getBytes());

            // 如果第一次发送碰到的不是I祯，那就把上一个缓存的I祯发下去
            if ((data[4] & 0x1f) != 0x05)
            {
                byte[] iFrame = flvEncoder.getLastIFrame();
                if (iFrame != null)
                {
                    FLVUtils.resetTimestamp(iFrame, (int) videoTimestamp);
                    enqueue(iFrame);
                }
            }

            videoHeaderSent = true;
        }

        if (data == null) return;

        // 修改时间戳
        // System.out.println("Time: " + videoTimestamp + ", current: " + timeoffset);
        FLVUtils.resetTimestamp(data, (int) videoTimestamp);
        videoTimestamp += (int)(timeoffset - lastVideoFrameTimeOffset);
        lastVideoFrameTimeOffset = timeoffset;

        enqueue(data);
    }

    private FlvAudioTagEncoder audioEncoder = new FlvAudioTagEncoder();
    MP3Encoder mp3Encoder = new MP3Encoder();

    @Override
    public void onAudioData(long timeoffset, byte[] data, FlvEncoder flvEncoder)
    {
        byte[] mp3Data = mp3Encoder.encode(data);
        if (mp3Data.length == 0) return;
        AudioTag audioTag = new AudioTag(0, mp3Data.length + 1, AudioTag.MP3, (byte) 0, (byte)1, (byte) 0, mp3Data);
        byte[] frameData = null;
        try
        {
            ByteBuf audioBuf = audioEncoder.encode(audioTag);
            frameData = ByteBufUtils.readReadableBytes(audioBuf);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (lastAudioFrameTimeOffset == 0) lastAudioFrameTimeOffset = timeoffset;

        if (data == null) return;

        FLVUtils.resetTimestamp(frameData, (int) audioTimestamp);
        audioTimestamp += (int)(timeoffset - lastAudioFrameTimeOffset);
        lastAudioFrameTimeOffset = timeoffset;

        if (videoHeaderSent) enqueue(frameData);
    }

    @Override
    public void close()
    {
        super.close();
        mp3Encoder.close();
    }
}