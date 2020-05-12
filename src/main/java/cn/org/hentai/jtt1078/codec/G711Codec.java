package cn.org.hentai.jtt1078.codec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * 核心转换，PCM转G711
 * Created by onlygx
 */
public class G711Codec extends AudioCodec
{
    private final static int SIGN_BIT = 0x80;
    private final static int QUANT_MASK = 0xf;
    private final static int SEG_SHIFT = 4;
    private final static int SEG_MASK = 0x70;

    static short[] seg_end = {0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF, 0x3FFF, 0x7FFF};

    static short search(short val, short[] table, short size)
    {

        for (short i = 0; i < size; i++)
        {
            if (val <= table[i])
            {
                return i;
            }
        }
        return size;
    }

    static byte linear2alaw(short pcm_val)
    {
        short mask;
        short seg;
        char aval;
        if (pcm_val >= 0)
        {
            mask = 0xD5;
        }
        else
        {
            mask = 0x55;
            pcm_val = (short) (-pcm_val - 1);
            if (pcm_val < 0)
            {
                pcm_val = 32767;
            }
        }

            /* Convert the scaled magnitude to segment number. */
        seg = search(pcm_val, seg_end, (short) 8);

            /* Combine the sign, segment, and quantization bits. */

        if (seg >= 8)       /* out of range, return maximum value. */ return (byte) (0x7F ^ mask);
        else
        {
            aval = (char) (seg << SEG_SHIFT);
            if (seg < 2) aval |= (pcm_val >> 4) & QUANT_MASK;
            else aval |= (pcm_val >> (seg + 3)) & QUANT_MASK;
            return (byte) (aval ^ mask);
        }
    }


    static short alaw2linear(byte a_val)
    {
        short t;
        short seg;

        a_val ^= 0x55;

        t = (short) ((a_val & QUANT_MASK) << 4);
        seg = (short) ((a_val & SEG_MASK) >> SEG_SHIFT);
        switch (seg)
        {
            case 0:
                t += 8;
                break;
            case 1:
                t += 0x108;
                break;
            default:
                t += 0x108;
                t <<= seg - 1;
        }
        return (a_val & SIGN_BIT) != 0 ? t : (short) -t;
    }

    // 由G.711转至PCM
    public static byte[] _toPCM(byte[] g711data)
    {
        byte[] pcmdata = new byte[g711data.length * 2];
        for (int i = 0, k = 0; i < g711data.length; i++)
        {
            short v = alaw2linear(g711data[i]);
            pcmdata[k++] = (byte) (v & 0xff);
            pcmdata[k++] = (byte)((v >> 8) & 0xff);
        }
        return pcmdata;
    }

    // 由PCM转至G.711
    public static byte[] _fromPCM(byte[] pcmdata)
    {
        byte[] g711data = new byte[pcmdata.length / 2];
        for (int i = 0, k = 0; i < pcmdata.length; i+=2,k++)
        {
            short v = (short)((pcmdata[i + 1] << 8) | (pcmdata[i]));
            g711data[k] = linear2alaw(v);
        }
        return g711data;
    }

    @Override
    public byte[] toPCM(byte[] data)
    {
        byte[] temp;
        // 如果前四字节是00 01 52 00，则是海思头，需要去掉
        if (data[0] == 0x00 && data[1] == 0x01 && (data[2] & 0xff) == (data.length - 4) / 2 && data[3] == 0x00)
        {
            temp = new byte[data.length - 4];
            System.arraycopy(data, 4, temp, 0, temp.length);
        }
        else temp = data;

        return _toPCM(temp);
    }

    @Override
    public byte[] fromPCM(byte[] data)
    {
        return _fromPCM(data);
    }

    public static void main(String[] args) throws Exception
    {
        ByteArrayInputStream bais = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 1024 * 4);

        int len = -1;
        byte[] block = new byte[512];
        FileInputStream fis = new FileInputStream("c:\\users\\houcheng\\downloads\\xxoo.pcm");
        FileOutputStream fos = new FileOutputStream("d:\\test\\g711\\hahahahahaha.g711");

        while ((len = fis.read(block)) > -1)
        {
            baos.write(block, 0, len);
        }

        byte[] xx = _fromPCM(baos.toByteArray());

        fos.write(xx);
        fos.flush();

        fis.close();
        fos.close();
    }
}