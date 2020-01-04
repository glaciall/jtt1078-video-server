package cn.org.hentai.jtt1078.flv;

import cn.org.hentai.jtt1078.util.Packet;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by matrixy on 2020/1/3.
 */
public final class FlvEncoder
{
    Packet flvHeader;
    Packet SPS, PPS;
    int SPSSize, PPSSize;
    boolean writeAVCSeqHeader;
    int prevTagSize;
    int streamID;
    int videoTimeStamp;

    Packet _pAudioSpecificConfig;
    int _nAudioConfigSize;
    int _aacProfile;
    int _sampleRateIndex;
    int _channelConfig;
    int _bWriteAACSeqHeader;

    OutputStream output;
    boolean haveAudio, haveVideo;

    public FlvEncoder()
    {
        flvHeader = Packet.create(16);
    }

    public void open(OutputStream os, boolean haveVideo, boolean haveAudio) throws IOException
    {
        this.output = os;
        this.haveAudio = haveAudio;
        this.haveVideo = haveVideo;
        makeFlvHeader();
    }

    public boolean write(byte[] nalu, int nTimeStamp) throws IOException
    {
        this.videoTimeStamp = nTimeStamp;

        if (nalu == null || nalu.length <= 4) return false;

        int naluType = nalu[4] & 0x1f;
        // skip SEI
        if (naluType == 0x06) return false;
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
        if (writeAVCSeqHeader == false) return true;

        writeH264Frame(nalu, nTimeStamp);

        return true;
    }

    void makeFlvHeader() throws IOException
    {
        flvHeader.addByte((byte)'F');
        flvHeader.addByte((byte)'L');
        flvHeader.addByte((byte)'V');
        flvHeader.addByte((byte)0x01);                 // version
        flvHeader.addByte((byte)(0x00 | (haveVideo ? 0x01 : 0x00) | (haveAudio ? 0x04 : 0x00)));
        flvHeader.addInt(0x09);

        output.write(flvHeader.getBytes());
    }

    void writeH264Header(int nTimeStamp) throws IOException
    {
        writeU4(prevTagSize);

        byte cTagType = 0x09;
        write(cTagType);

        int nDataSize = 1 + 1 + 3 + 6 + 2 + (SPSSize - 4) + 1 + 2 + (PPSSize - 4);
        writeU3(nDataSize);

        writeU3(nTimeStamp);
        writeByte(nTimeStamp >> 24);

        writeU3(streamID);

        byte cVideoParam = 0x17;
        write(cVideoParam);

        byte cAVCPacketType = 0x00;
        write(cAVCPacketType);

        writeU3(0x00);

        writeByte(0x01);
        writeByte(SPS.seek(5).nextByte());
        writeByte(SPS.seek(6).nextByte());
        writeByte(SPS.seek(7).nextByte());
        writeByte(0xff);
        writeByte(0xe1);

        writeU2(SPSSize - 4);
        writeBytes(SPS.seek(4).nextBytes());
        writeByte(0x01);

        writeU2(PPSSize - 4);
        writeBytes(PPS.seek(4).nextBytes());

        prevTagSize = 11 + nDataSize;
    }

    void writeH264Frame(byte[] nalu, int nTimeStamp) throws IOException
    {
        int nNaluType = nalu[4] & 0x1f;
        if (nNaluType == 7 || nNaluType == 8) return;

        writeU4(prevTagSize);

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

        flush();

        prevTagSize = 11 + nDataSize;
    }

    void writeH264EndOfSeq() throws IOException
    {
        writeU4(prevTagSize);

        writeByte(0x09);
        int nDataSize = 1 + 1 + 3;
        writeU3(nDataSize);

        writeU3(videoTimeStamp);
        writeByte(videoTimeStamp >> 24);

        writeU3(streamID);
        writeByte(0x27);
        writeByte(0x02);
        writeU3(0x00);

        flush();
    }

    public void flush() throws IOException
    {
        output.flush();
    }

    public void close() throws IOException
    {
        if (haveVideo) writeH264EndOfSeq();
        output.close();
    }

    void write(byte u) throws IOException
    {
        output.write(u);
    }

    void writeBytes(byte[] data) throws IOException
    {
        output.write(data);
    }

    void writeBytes(byte[] data, int offset, int len) throws IOException
    {
        output.write(data, offset, len);
    }

    void writeU4(int i) throws IOException
    {
        write((byte)((i >> 24) & 0xff));
        write((byte)((i >> 16) & 0xff));
        write((byte)((i >>  8) & 0xff));
        write((byte)((i >>  0) & 0xff));
    }

    void writeU3(int i) throws IOException
    {
        write((byte)((i >> 16) & 0xff));
        write((byte)((i >>  8) & 0xff));
        write((byte)((i >>  0) & 0xff));
    }

    void writeU2(int i) throws IOException
    {
        write((byte)((i >>  8) & 0xff));
        write((byte)((i >>  0) & 0xff));
    }

    void writeByte(int i) throws IOException
    {
        write((byte)(i & 0xff));
    }
}
