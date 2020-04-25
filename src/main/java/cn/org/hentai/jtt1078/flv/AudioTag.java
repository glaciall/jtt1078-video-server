package cn.org.hentai.jtt1078.flv;

/**
 * flv 音频封装
 * author:zhouyili (11861744@qq.com)
 */
public class AudioTag extends FlvTag{
    public static final byte MP3 = 2;
    //UB[4]
    //0 = Linear PCM, platform endian 根据编码的平台，一般用3而不用这个
    //1 = ADPCM
    //2 = MP3
    //3 = Linear PCM, little endian
    //4 = Nellymoser 16-kHz mono
    //5 = Nellymoser 8-kHz mono
    //6 = Nellymoser
    //7 = G.711 A-law logarithmic PCM 8 = G.711 mu-law logarithmic PCM 9 = reserved
    //10 = AAC
    //11 = Speex
    //14 = MP3 8-Khz
    //15 = Device-specific sound
    private byte format;
    //0:5.5kHz,1:11kHz,2:22kHz,3:44kHz,format==10的话为3
    private byte rate;
    //0:8bit,1:16bit
    private byte size;
    //0:mono,1:stereo,format==10的话为1
    private byte type;
    //format==10是aac data,否则是对应编码后的数据
    private byte[] data;

    public AudioTag() {
    }

    public AudioTag(int offSetTimestamp, int tagDataSize, byte format, byte rate, byte size, byte type, byte[] data) {
        super(offSetTimestamp, tagDataSize);
        this.format = format;
        this.rate = rate;
        this.size = size;
        this.type = type;
        this.data = data;
    }

    public byte getFormat() {
        return format;
    }

    public void setFormat(byte format) {
        this.format = format;
    }

    public byte getRate() {
        return rate;
    }

    public void setRate(byte rate) {
        this.rate = rate;
    }

    public byte getSize() {
        return size;
    }

    public void setSize(byte size) {
        this.size = size;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }
}
