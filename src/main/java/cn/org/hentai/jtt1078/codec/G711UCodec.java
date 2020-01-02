package cn.org.hentai.jtt1078.codec;

import cn.org.hentai.jtt1078.util.ByteUtils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
/*
 * This source code is a product of Sun Microsystems, Inc. and is provided
 * for unrestricted use.  Users may copy or modify this source code without
 * charge.
 *
 * SUN SOURCE CODE IS PROVIDED AS IS WITH NO WARRANTIES OF ANY KIND INCLUDING
 * THE WARRANTIES OF DESIGN, MERCHANTIBILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE, OR ARISING FROM A COURSE OF DEALING, USAGE OR TRADE PRACTICE.
 *
 * Sun source code is provided with no support and without any obligation on
 * the part of Sun Microsystems, Inc. to assist in its use, correction,
 * modification or enhancement.
 *
 * SUN MICROSYSTEMS, INC. SHALL HAVE NO LIABILITY WITH RESPECT TO THE
 * INFRINGEMENT OF COPYRIGHTS, TRADE SECRETS OR ANY PATENTS BY THIS SOFTWARE
 * OR ANY PART THEREOF.
 *
 * In no event will Sun Microsystems, Inc. be liable for any lost revenue
 * or profits or other special, indirect and consequential damages, even if
 * Sun has been advised of the possibility of such damages.
 *
 * Sun Microsystems, Inc.
 * 2550 Garcia Avenue
 * Mountain View, California  94043
 */

/**
 * Created by houcheng on 2019-12-11.
 */
public class G711UCodec extends AudioCodec
{
    static final int ULAW = 1;
    static final int ALAW = 2;

    /* 16384 entries per table (16 bit) */
    static byte[] linear_to_ulaw = new byte[65536];

    /* 16384 entries per table (8 bit) */
    static short[] ulaw_to_linear = new short[256];

    static final int SIGN_BIT = 0x80;
    static final int QUANT_MASK = 0x0f;
    static final int NSEGS = 0x08;
    static final int SEG_SHIFT = 0x04;
    static final int SEG_MASK = 0x70;

    static final int BIAS = 0x84;
    static final int CLIP = 8159;

    static short[] seg_aend = { 0x1F, 0x3F, 0x7F, 0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF };
    static short[] seg_uend = { 0x3F, 0x7F, 0xFF, 0x1FF, 0x3FF, 0x7FF, 0xFFF, 0x1FFF };

    int[] _u2a = {			/* u- to A-law conversions */
            1,	1,	2,	2,	3,	3,	4,	4,
            5,	5,	6,	6,	7,	7,	8,	8,
            9,	10,	11,	12,	13,	14,	15,	16,
            17,	18,	19,	20,	21,	22,	23,	24,
            25,	27,	29,	31,	33,	34,	35,	36,
            37,	38,	39,	40,	41,	42,	43,	44,
            46,	48,	49,	50,	51,	52,	53,	54,
            55,	56,	57,	58,	59,	60,	61,	62,
            64,	65,	66,	67,	68,	69,	70,	71,
            72,	73,	74,	75,	76,	77,	78,	79,
            /* corrected:
                81,	82,	83,	84,	85,	86,	87,	88,
               should be: */
            80,	82,	83,	84,	85,	86,	87,	88,
            89,	90,	91,	92,	93,	94,	95,	96,
            97,	98,	99,	100,	101,	102,	103,	104,
            105,	106,	107,	108,	109,	110,	111,	112,
            113,	114,	115,	116,	117,	118,	119,	120,
            121,	122,	123,	124,	125,	126,	127,	128};

    int[] _a2u = {			/* A- to u-law conversions */
            1,	3,	5,	7,	9,	11,	13,	15,
            16,	17,	18,	19,	20,	21,	22,	23,
            24,	25,	26,	27,	28,	29,	30,	31,
            32,	32,	33,	33,	34,	34,	35,	35,
            36,	37,	38,	39,	40,	41,	42,	43,
            44,	45,	46,	47,	48,	48,	49,	49,
            50,	51,	52,	53,	54,	55,	56,	57,
            58,	59,	60,	61,	62,	63,	64,	64,
            65,	66,	67,	68,	69,	70,	71,	72,
            /* corrected:
                73,	74,	75,	76,	77,	78,	79,	79,
               should be: */
            73,	74,	75,	76,	77,	78,	79,	80,
            80,	81,	82,	83,	84,	85,	86,	87,
            88,	89,	90,	91,	92,	93,	94,	95,
            96,	97,	98,	99,	100,	101,	102,	103,
            104,	105,	106,	107,	108,	109,	110,	111,
            112,	113,	114,	115,	116,	117,	118,	119,
            120,	121,	122,	123,	124,	125,	126,	127};

    static
    {
        // 初始化ulaw表
        for (int i = 0; i < 256; i++) ulaw_to_linear[i] = ulaw2linear((byte)i);
        // 初始化ulaw2linear表
        for (int i = 0; i < 65535; i++) linear_to_ulaw[i] = linear2ulaw((short)i);
    }

    static short ulaw2linear(byte u_val)
    {
        short t;
        u_val = (byte)(~u_val);
        t = (short)(((u_val & QUANT_MASK) << 3) + BIAS);
        t <<= (u_val & SEG_MASK) >>> SEG_SHIFT;

        return ((u_val & SIGN_BIT) > 0 ? (short)(BIAS - t) : (short)(t - BIAS));
    }

    static byte linear2ulaw(short pcm_val)
    {
        short mask;
        short seg;
        byte uval;

        pcm_val = (short)(pcm_val >> 2);
        if (pcm_val < 0)
        {
            pcm_val = (short)(-pcm_val);
            mask = 0x7f;
        }
        else
        {
            mask = 0xff;
        }

        if (pcm_val > CLIP) pcm_val = CLIP;
        pcm_val += (BIAS >> 2);

        seg = search(pcm_val, seg_uend, (short)8);

        if (seg >= 8)
        {
            return (byte)(0x7f ^ mask);
        }
        else
        {
            uval = (byte) ((seg << 4) | ((pcm_val >> (seg + 1)) & 0xF));
            return (byte)(uval ^ mask);
        }
    }

    static short search(short val, short[] table, short size)
    {
        for (short i = 0; i < size; i++)
        {
            if (val <= table[i]) return i;
        }
        return size;
    }

    static void ulaw_to_pcm16(int src_length, byte[] src_samples, byte[] dst_samples)
    {
        for (int i = 0, k = 0; i < src_length; i++)
        {
            short s = ulaw_to_linear[src_samples[i] & 0xff];
            dst_samples[k++] = (byte)(s & 0xff);
            dst_samples[k++] = (byte)((s >> 8) & 0xff);
        }
    }

    static void pcm16_to_ulaw(int src_length, byte[] src_samples, byte[] dst_samples)
    {
        short[] s_samples = ByteUtils.toShortArray(src_samples);
        for (int i = 0, k = 0; i < s_samples.length; i++)
        {
            dst_samples[k++] = linear2ulaw(s_samples[i]);
        }
    }

    @Override
    public byte[] toPCM(byte[] data)
    {
        byte[] dest = new byte[data.length * 2];
        ulaw_to_pcm16(data.length, data, dest);
        return dest;
    }

    @Override
    public byte[] fromPCM(byte[] data)
    {
        byte[] dest = new byte[data.length / 2];
        pcm16_to_ulaw(data.length, data, dest);
        return dest;
    }

    public static void main(String[] args) throws Exception
    {
        FileInputStream fis = new FileInputStream("d:\\fuck121212121.pcm");
        int len = -1;
        byte[] buff = new byte[320];
        AudioCodec codec = new G711UCodec();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096 * 1024);
        while ((len = fis.read(buff)) > -1)
        {
            baos.write(buff, 0, len);
        }
        new FileOutputStream("D:\\temp\\fuckfuckfuck.g711u").write(codec.fromPCM(baos.toByteArray()));
    }
}
