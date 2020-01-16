package cn.org.hentai.jtt1078.flv;

import cn.org.hentai.jtt1078.util.Packet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by matrixy on 2020/1/3.
 */
public final class FlvEncoder
{
    Packet flvHeader;
    Packet videoHeader;
    Packet SPS, PPS;
    int SPSSize, PPSSize;
    boolean writeAVCSeqHeader;
    int prevTagSize;
    int streamID;
    int videoTimeStamp;
    byte[] lastIFrame;

    Packet _pAudioSpecificConfig;
    int _nAudioConfigSize;
    int _aacProfile;
    int _sampleRateIndex;
    int _channelConfig;
    int _bWriteAACSeqHeader;

    boolean haveAudio, haveVideo;

    ByteArrayOutputStream videoFrame;

    public FlvEncoder(boolean haveVideo, boolean haveAudio)
    {
        this.haveVideo = haveVideo;
        this.haveAudio = haveAudio;
        flvHeader = Packet.create(16);
        videoFrame = new ByteArrayOutputStream(4096 * 100);
        makeFlvHeader();
    }

    public Packet getHeader()
    {
        return flvHeader;
    }

    public Packet getVideoHeader()
    {
        return videoHeader;
    }

    public boolean videoReady()
    {
        return writeAVCSeqHeader;
    }

    public byte[] getLastIFrame()
    {
        return this.lastIFrame;
    }

    public byte[] write(byte[] nalu, int nTimeStamp)
    {
        this.videoTimeStamp = nTimeStamp;

        if (nalu == null || nalu.length <= 4) return null;

        int naluType = nalu[4] & 0x1f;
        // skip SEI
        if (naluType == 0x06) return null;
        if (naluType == 0x01
                || naluType == 0x02
                || naluType == 0x03
                || naluType == 0x04
                || naluType == 0x05
                || naluType == 0x07
                || naluType == 0x08) ; else return null;

        if (SPS == null && naluType == 0x07)
        {
            SPS = Packet.create(nalu);
            SPSSize = nalu.length;
        }
        if (PPS == null && naluType == 0x08)
        {
            PPS = Packet.create(nalu);
            PPSSize = nalu.length;
        }
        if (SPS != null && PPS != null && writeAVCSeqHeader == false)
        {
            writeH264Header(nTimeStamp);
            writeAVCSeqHeader = true;
        }
        if (writeAVCSeqHeader == false) return null;

        videoFrame.reset();
        writeH264Frame(nalu, nTimeStamp);

        if (videoFrame.size() == 0) return null;

        // 如果当前NAL单元为I祯，则缓存一个
        if (naluType == 0x05)
        {
            lastIFrame = videoFrame.toByteArray();
        }

        return videoFrame.toByteArray();
    }

    void makeFlvHeader()
    {
        flvHeader.addByte((byte)'F');
        flvHeader.addByte((byte)'L');
        flvHeader.addByte((byte)'V');
        flvHeader.addByte((byte)0x01);                 // version
        flvHeader.addByte((byte)(0x00 | (haveVideo ? 0x01 : 0x00) | (haveAudio ? 0x04 : 0x00)));
        flvHeader.addInt(0x09);
        flvHeader.addInt(0x00);
    }

    void writeH264Header(int nTimeStamp)
    {
        int nDataSize = 1 + 1 + 3 + 6 + 2 + (SPSSize - 4) + 1 + 2 + (PPSSize - 4);
        videoHeader = Packet.create(nDataSize + 32);

        byte cTagType = 0x09;
        videoHeader.addByte(cTagType);

        videoHeader.add3Bytes(nDataSize);

        videoHeader.add3Bytes(nTimeStamp);
        videoHeader.addByte((byte)(nTimeStamp >> 24));

        videoHeader.add3Bytes(streamID);

        byte cVideoParam = 0x17;
        videoHeader.addByte(cVideoParam);

        byte cAVCPacketType = 0x00;
        videoHeader.addByte(cAVCPacketType);

        videoHeader.add3Bytes(0x00);

        videoHeader.addByte((byte)0x01);

        videoHeader.addByte(SPS.seek(5).nextByte());
        videoHeader.addByte(SPS.seek(6).nextByte());
        videoHeader.addByte(SPS.seek(7).nextByte());
        videoHeader.addByte((byte)0xff);
        videoHeader.addByte((byte)0xe1);

        videoHeader.addShort((short)(SPSSize - 4));
        videoHeader.addBytes(SPS.seek(4).nextBytes());
        videoHeader.addByte((byte)0x01);

        videoHeader.addShort((short)(PPSSize - 4));
        videoHeader.addBytes(PPS.seek(4).nextBytes());

        prevTagSize = 11 + nDataSize;
        videoHeader.addInt(prevTagSize);
    }

    void writeH264Frame(byte[] nalu, int nTimeStamp)
    {
        int nNaluType = nalu[4] & 0x1f;
        if (nNaluType == 7 || nNaluType == 8) return;

        writeByte(0x09);

        int nDataSize = 1 + 1 + 3 + 4 + (nalu.length - 4);
        writeU3(nDataSize);

        writeU3(nTimeStamp);
        writeByte(nTimeStamp >> 24);

        writeU3(streamID);

        if (nNaluType == 5) writeByte(0x17);
        else writeByte(0x27);

        writeByte(0x01);
        writeU3(0x00);
        writeU4(nalu.length - 4);
        writeBytes(nalu, 4, nalu.length - 4);

        prevTagSize = 11 + nDataSize;

        writeU4(prevTagSize);
    }

    void write(byte u)
    {
        videoFrame.write(u);
    }

    void writeBytes(byte[] data)
    {
        videoFrame.write(data, 0, data.length);
    }

    void writeBytes(byte[] data, int offset, int len)
    {
        videoFrame.write(data, offset, len);
    }

    void writeU4(int i)
    {
        write((byte)((i >> 24) & 0xff));
        write((byte)((i >> 16) & 0xff));
        write((byte)((i >>  8) & 0xff));
        write((byte)((i >>  0) & 0xff));
    }

    void writeU3(int i)
    {
        write((byte)((i >> 16) & 0xff));
        write((byte)((i >>  8) & 0xff));
        write((byte)((i >>  0) & 0xff));
    }

    void writeU2(int i)
    {
        write((byte)((i >>  8) & 0xff));
        write((byte)((i >>  0) & 0xff));
    }

    void writeByte(int i)
    {
        write((byte)(i & 0xff));
    }
}
