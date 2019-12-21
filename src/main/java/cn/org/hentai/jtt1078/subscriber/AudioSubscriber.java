package cn.org.hentai.jtt1078.subscriber;

import cn.org.hentai.jtt1078.codec.AudioCodec;
import cn.org.hentai.jtt1078.entity.Media;
import cn.org.hentai.jtt1078.util.Packet;
import cn.org.hentai.jtt1078.util.WAVUtils;
import io.netty.channel.ChannelHandlerContext;
import sun.misc.BASE64Encoder;

/**
 * Created by matrixy on 2019/12/20.
 */
public class AudioSubscriber extends Subscriber
{
    private Packet packet = null;
    AudioCodec codec = null;

    public AudioSubscriber(String tag, ChannelHandlerContext ctx)
    {
        super(tag, ctx);
        this.setThreadNameTag("audio-subscriber");
        packet = Packet.create(2048);
    }

    @Override
    public void onData(ChannelHandlerContext ctx, Media media) throws Exception
    {
        if (codec == null)
        {
            codec = AudioCodec.getCodec(media.encoding);
            logger.debug("Audio Codec: {}", media.encoding);
        }
        byte[] pcmData = codec.toPCM(media.data);
        packet.reset();
        packet.addBytes(WAVUtils.createHeader(pcmData.length, 1, 8000, 16));
        packet.addBytes(pcmData);

        String audioData = new BASE64Encoder().encode(packet.getBytes()).replaceAll("[\r\n]+", "");

        ctx.writeAndFlush(String.format("%x\r\n", audioData.length() + 8).getBytes()).await();
        ctx.writeAndFlush(String.format("%08x", audioData.length()).getBytes()).await();
        ctx.writeAndFlush(audioData.getBytes()).await();
        ctx.writeAndFlush("\r\n".getBytes()).await();
    }
}
