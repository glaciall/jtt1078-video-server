package cn.org.hentai.jtt1078.flv;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flv 音频封装编码
 * author:zhouyili (11861744@qq.com)
 */
public class FlvAudioTagEncoder {
    public static final Logger log = LoggerFactory.getLogger(FlvAudioTagEncoder.class);

    public ByteBuf encode(AudioTag audioTag) throws Exception {
        ByteBuf buffer = Unpooled.buffer();
        if (audioTag == null) return buffer;
//        buffer.writeInt(audioTag.getPreTagSize());
        //----------------------tag header begin-------
        buffer.writeByte(8);
        buffer.writeMedium(audioTag.getTagDataSize());
        buffer.writeMedium(audioTag.getOffSetTimestamp() & 0xFFFFFF);
        buffer.writeByte(audioTag.getOffSetTimestamp() >> 24);
        buffer.writeMedium(audioTag.getStreamId());
        //---------------------tag header length 11---------
        //---------------------tag header end----------------
        byte formatAndRateAndSize = (byte) (audioTag.getFormat() << 4 | audioTag.getRate() << 2 | audioTag.getSize() << 1 | audioTag.getType());
        //-------------data begin-------
        buffer.writeByte(formatAndRateAndSize);
        buffer.writeBytes(audioTag.getData());
        //-------------data end  -------
        buffer.writeInt(buffer.writerIndex());//应该等于11+tagDataSize
        return buffer;
    }

}
