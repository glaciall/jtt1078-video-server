package cn.org.hentai.jtt1078.flv;

//由于flv格式是header+body,而body是preTagSize+currentTagData.......循环形式,并且第一个的preTagSize始终为0,
// 与其这样不如采用(header+preTag0Size)+（currentTagData+currentTagSize），这样就避免每次都要记录以下上一个tag的数据大小。
/**
 * author:zhouyili (11861744@qq.com)
 */
public class FlvTag {
    public static final int AUDIO = 8;
    public static final int VIDEO = 9;
    public static final int SCRIPT = 18;//0x12
    private int preTagSize;
    private byte tagType;
    /**3个字节 streamId以后的数据长度*/
    private int tagDataSize;
    //低3个字节写入后再写高位字节,相对于第一帧的时间偏移量，单位ms
    private int offSetTimestamp;
    //3个字节，一般总是0
    private int streamId;

    public FlvTag() {
    }

    public FlvTag(int offSetTimestamp, int tagDataSize) {
        this.tagDataSize = tagDataSize;
        this.offSetTimestamp = offSetTimestamp;
    }

    public int getPreTagSize() {
        return preTagSize;
    }

    public void setPreTagSize(int preTagSize) {
        this.preTagSize = preTagSize;
    }

    public byte getTagType() {
        return tagType;
    }

    public void setTagType(byte tagType) {
        this.tagType = tagType;
    }

    public int getTagDataSize() {
        return tagDataSize;
    }

    public void setTagDataSize(int tagDataSize) {
        this.tagDataSize = tagDataSize;
    }

    public int getOffSetTimestamp() {
        return offSetTimestamp;
    }

    public void setOffSetTimestamp(int offSetTimestamp) {
        this.offSetTimestamp = offSetTimestamp;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }
}
