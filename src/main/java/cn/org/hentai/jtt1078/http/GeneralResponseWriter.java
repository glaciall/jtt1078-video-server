package cn.org.hentai.jtt1078.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Created by matrixy on 2019/11/25.
 */
public class GeneralResponseWriter extends MessageToByteEncoder<byte[]>
{
    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out) throws Exception
    {
        out.writeBytes(msg);
    }
}
