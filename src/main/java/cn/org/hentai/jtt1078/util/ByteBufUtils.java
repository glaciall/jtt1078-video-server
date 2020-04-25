package cn.org.hentai.jtt1078.util;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
/**
 * author:zhouyili (11861744@qq.com)
 */
public class ByteBufUtils {

    public static byte[] readReadableBytes(ByteBuf msg) {
       byte[] content = new byte[msg.readableBytes()];
       msg.readBytes(content);
       return content;
    }

    public static byte[] getReadableBytes(ByteBuf msg) {
        byte[] content = new byte[msg.readableBytes()];
        int start = msg.readerIndex();
        msg.getBytes(start,content);
        return content;
    }
    public static byte[] readBytes(ByteBuf buf, int length) {
        validLength(buf, length);
        byte[] content = new byte[length];
        buf.readBytes(content);
        return content;
    }

    private static void validLength(ByteBuf buf, int length) {
        int readableLength = buf.readableBytes();
        if (readableLength<length){
            throw new RuntimeException("可读数据长度小于设定值");
        }
    }

    public static String toString(ByteBuf buf, int length) {
        validLength(buf,length);
        return buf.readCharSequence(length, StandardCharsets.ISO_8859_1).toString();
    }

    public static String toString(ByteBuf buf) {
        return toString(buf,buf.readableBytes());
    }
}
