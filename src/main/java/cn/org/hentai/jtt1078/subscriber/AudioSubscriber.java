package cn.org.hentai.jtt1078.subscriber;

import cn.org.hentai.jtt1078.flv.FlvEncoder;
import cn.org.hentai.jtt1078.util.ByteUtils;
import cn.org.hentai.jtt1078.util.WAVUtils;
import io.netty.channel.ChannelHandlerContext;
import sun.misc.BASE64Encoder;

/**
 * Created by matrixy on 2020/1/13.
 */
public class AudioSubscriber extends Subscriber
{
    public AudioSubscriber(String tag, ChannelHandlerContext ctx)
    {
        super(tag, ctx);
    }

    @Override
    public void onData(long timeoffset, byte[] data, FlvEncoder flvEncoder)
    {
        byte[] wav = ByteUtils.concat(WAVUtils.createHeader(data.length, 1, 8000, 16), data);
        String audioData = new BASE64Encoder().encode(wav).replaceAll("[\r\n]+", "");

        byte[] output = ByteUtils.concat(
                String.format("%x\r\n", audioData.length() + 8).getBytes(),
                String.format("%08x", audioData.length()).getBytes(),
                audioData.getBytes(),
                "\r\n".getBytes()
        );

        enqueue(output);
    }
}
