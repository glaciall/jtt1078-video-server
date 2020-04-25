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

    public ByteBuf encode(FlvTag flvTag) throws Exception {
        ByteBuf buffer = Unpooled.buffer();
        if (flvTag == null) return buffer;
//        buffer.writeInt(flvTag.getPreTagSize());
        //----------------------tag header begin-------
        buffer.writeByte(8);
        buffer.writeMedium(flvTag.getTagDataSize());
        buffer.writeMedium(flvTag.getOffSetTimestamp() & 0xFFFFFF);
        buffer.writeByte(flvTag.getOffSetTimestamp() >> 24);
        buffer.writeMedium(flvTag.getStreamId());
        //---------------------tag header length 11---------
        //---------------------tag header end----------------
         if (flvTag instanceof AudioTag) {
            AudioTag audioTag = (AudioTag) flvTag;
            byte formatAndRateAndSize = (byte) (audioTag.getFormat() << 4 | audioTag.getRate() << 2 | audioTag.getSize() << 1 | audioTag.getType());
            //-------------data begin-------
            buffer.writeByte(formatAndRateAndSize);
            buffer.writeBytes(audioTag.getData());
            //-------------data end  -------
        }
        buffer.writeInt(buffer.writerIndex());//应该等于11+tagDataSize
        return buffer;
    }

}
