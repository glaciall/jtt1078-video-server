package cn.org.hentai.jtt1078.util;

/**
 * Created by matrixy on 2019/12/18.
 */
public final class WAVUtils
{
    /**
     * 创建WAV头，仅返回WAV头部字节数组信息
     * @param dataLength PCM数据字节总
     * @param channels 通道数，通常为1
     * @param sampleRate 采样率，通常为8000
     * @param sampleBits 样本比特位数，通常为16
     * @return
     */
    public static byte[] createHeader(int dataLength, int channels, int sampleRate, int sampleBits)
    {
        Packet p = Packet.create(44);
        p.addBytes("RIFF".getBytes())
                .addBytes(ByteUtils.toLEBytes(dataLength + 36))
                .addBytes("WAVE".getBytes())                                    // wave type
                .addBytes("fmt ".getBytes())                                    // fmt id
                .addInt(0x10000000)                                             // fmt chunk size
                .addShort((short)0x0100)                                        // format: 1 -> PCM
                .addBytes(ByteUtils.toLEBytes((short)channels))                 // channels: 1
                .addBytes(ByteUtils.toLEBytes(sampleRate))                      // samples per second
                .addBytes(ByteUtils.toLEBytes(1 * sampleRate * sampleBits / 8)) // BPSecond
                .addBytes(ByteUtils.toLEBytes((short)(1 * sampleBits / 8)))     // BPSample
                .addBytes(ByteUtils.toLEBytes((short)(1 * sampleBits)))         // bPSecond
                .addBytes("data".getBytes())                                    // data id
                .addBytes(ByteUtils.toLEBytes(dataLength));                     // data chunk size

        return p.getBytes();
    }
}
