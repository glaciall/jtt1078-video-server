package cn.org.hentai.jtt1078.server;

import cn.org.hentai.jtt1078.util.ByteUtils;
import cn.org.hentai.jtt1078.util.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by matrixy on 2019/4/9.
 */
public class Jtt1078MessageDecoder extends ByteToMessageDecoder
{
    static Logger logger = LoggerFactory.getLogger(Jtt1078MessageDecoder.class);
    byte[] block = new byte[4096];
    Jtt1078Decoder decoder = new Jtt1078Decoder();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        int length = in.readableBytes();
        for (int i = 0, k = (int)Math.ceil(length / 512f); i < k; i++)
        {
            int l = i < k - 1 ? 512 : length - (i * 512);
            in.readBytes(block, 0, l);

            decoder.write(block, 0, l);

            while (true)
            {
                Packet p = decoder.decode();
                if (p == null) break;

                out.add(p);
            }
        }
    }
}
