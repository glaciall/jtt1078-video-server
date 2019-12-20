package cn.org.hentai.jtt1078.audio;

import cn.org.hentai.jtt1078.server.Jtt1078Decoder;
import cn.org.hentai.jtt1078.util.ByteHolder;
import cn.org.hentai.jtt1078.util.Packet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

/**
 * Created by houcheng on 2019-12-05.
 * ADPCM 和 PCM转换
 */
public final class ADPCMCodec extends AudioCodec
{
    static int[] indexTable = {
        -1, -1, -1, -1, 2, 4, 6, 8,
        -1, -1, -1, -1, 2, 4, 6, 8
    };

    static int[] stepsizeTable = {
            7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
            19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
            50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
            130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
            337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
            876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
            2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
            5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
            15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
    };

    public static class State
    {
        public short valprev;
        public byte index;
    }

    public byte[] toPCM(byte[] data)
    {
        State state = new State();
        int dlen = data.length / 2;
        byte[] temp;
        // 如果前四字节是00 01 52 00，则是海思头，需要去掉，否则就视为普通的ADPCM编码
        if (data[0] == 0x00 && data[1] == 0x01 && (data[2] & 0xff) == (data.length - 4) / 2 && data[3] == 0x00)
        {
            dlen = (data.length - 8);
            temp = new byte[data.length - 8];
            System.arraycopy(data, 8, temp, 0, temp.length);

            state.valprev = (short)(((data[5] << 8) & 0xff00) | (data[4] & 0xff));
            state.index = data[6];
        }
        else
        {
            dlen = (data.length - 4) / 2;
            temp = new byte[data.length - 4];
            System.arraycopy(data, 4, temp, 0, temp.length);

            state.valprev = (short)(((data[1] << 8) & 0xff00) | (data[0] & 0xff));
            state.index = data[2];
        }
        short[] outdata = new short[dlen * 2];
        adpcm_decoder(temp, outdata, dlen * 2, state);
        temp = new byte[dlen * 4];
        for (int i = 0, k = 0; i < outdata.length; i++)
        {
            short s = outdata[i];
            temp[k++] = (byte)(s & 0xff);
            temp[k++] = (byte)((s >> 8) & 0xff);
        }
        return temp;
    }

    public byte[] fromPCM(byte[] data)
    {
        return null;
    }

    public static void adpcm_coder(short[] indata, byte[] outdata, int len, State state)
    {
        int val;			/* Current input sample value */
        int sign;			/* Current adpcm sign bit */
        int delta;			/* Current adpcm output value */
        int diff;			/* Difference between val and valprev */
        int step;			/* Stepsize */
        int valpred;		/* Predicted output value */
        int vpdiff;			/* Current change to valpred */
        int index;			/* Current step change index */
        int outputbuffer = 0;		/* place to keep previous 4-bit value */
        int bufferstep;		/* toggle between outputbuffer/output */

        byte[] outp = outdata;
        short[] inp = indata;

        valpred = state.valprev;
        index = state.index;
        step = stepsizeTable[index];

        bufferstep = 1;

        int k = 0;
        for ( int i = 0; len > 0 ; len--, i++) {
            val = inp[i];

	        /* Step 1 - compute difference with previous value */
            diff = val - valpred;
            sign = (diff < 0) ? 8 : 0;
            if ( sign != 0) diff = (-diff);

            /* Step 2 - Divide and clamp */
            /* Note:
            ** This code *approximately* computes:
            **    delta = diff*4/step;
            **    vpdiff = (delta+0.5)*step/4;
            ** but in shift step bits are dropped. The net result of this is
            ** that even if you have fast mul/div hardware you cannot put it to
            ** good use since the fixup would be too expensive.
            */
            delta = 0;
            vpdiff = (step >> 3);

            if ( diff >= step ) {
                delta = 4;
                diff -= step;
                vpdiff += step;
            }
            step >>= 1;
            if ( diff >= step  ) {
                delta |= 2;
                diff -= step;
                vpdiff += step;
            }
            step >>= 1;
            if ( diff >= step ) {
                delta |= 1;
                vpdiff += step;
            }

	        /* Step 3 - Update previous value */
            if ( sign != 0 )
                valpred -= vpdiff;
            else
                valpred += vpdiff;

	        /* Step 4 - Clamp previous value to 16 bits */
            if ( valpred > 32767 )
                valpred = 32767;
            else if ( valpred < -32768 )
                valpred = -32768;

	        /* Step 5 - Assemble value, update index and step values */
            delta |= sign;

            index += indexTable[delta];
            if ( index < 0 ) index = 0;
            if ( index > 88 ) index = 88;
            step = stepsizeTable[index];

	        /* Step 6 - Output value */
            if ( bufferstep != 0 ) {
                outputbuffer = (delta << 4) & 0xf0;
            } else {
	            outp[k++] = (byte)((delta & 0x0f) | outputbuffer);
            }
            bufferstep = bufferstep == 0 ? 1 : 0;
        }

        /* Output last step, if needed */
        if ( bufferstep == 0 )
            outp[k++] = (byte)outputbuffer;

        state.valprev = (short)valpred;
        state.index = (byte)index;
    }


    public static void adpcm_decoder(byte[] indata, short[] outdata, int len, State state)
    {
        // signed char *inp;		/* Input buffer pointer */
	    // short *outp;		/* output buffer pointer */
        int sign;			/* Current adpcm sign bit */
        int delta;			/* Current adpcm output value */
        int step;			/* Stepsize */
        int valpred;		/* Predicted value */
        int vpdiff;			/* Current change to valpred */
        int index;			/* Current step change index */
        int inputbuffer = 0;		/* place to keep next 4-bit value */
        int bufferstep;		/* toggle between inputbuffer/input */

        short[] outp = outdata;
        byte[] inp = indata;

        valpred = state.valprev;
        index = state.index;
        if ( index < 0 ) index = 0;
        if ( index > 88 ) index = 88;
        step = stepsizeTable[index];

        bufferstep = 0;

        int k = 0;
        for ( int i = 0; len > 0 ; len-- ) {

		/* Step 1 - get the delta value */
            if ( bufferstep != 0 ) {
                delta = inputbuffer & 0xf;
            } else {
                inputbuffer = inp[i++];
                delta = (inputbuffer >> 4) & 0xf;
            }
            bufferstep = bufferstep == 0 ? 1 : 0;

		/* Step 2 - Find new index value (for later) */
            index += indexTable[delta];
            if ( index < 0 ) index = 0;
            if ( index > 88 ) index = 88;

		/* Step 3 - Separate sign and magnitude */
            sign = delta & 8;
            delta = delta & 7;

		/* Step 4 - Compute difference and new predicted value */
		/*
		** Computes 'vpdiff = (delta+0.5)*step/4', but see comment
		** in adpcm_coder.
		*/
            vpdiff = step >> 3;
            if ( (delta & 4) > 0 ) vpdiff += step;
            if ( (delta & 2) > 0 ) vpdiff += step>>1;
            if ( (delta & 1) > 0 ) vpdiff += step>>2;

            if ( sign != 0 )
                valpred -= vpdiff;
            else
                valpred += vpdiff;

		/* Step 5 - clamp output value */
            if ( valpred > 32767 )
                valpred = 32767;
            else if ( valpred < -32768 )
                valpred = -32768;

		/* Step 6 - Update step value */
            step = stepsizeTable[index];

		/* Step 7 - Output value */
		    outp[k++] = (short)valpred;
        }

        state.valprev = (short)valpred;
        state.index = (byte)index;
    }


    public static void main(String[] args) throws Exception
    {
        ByteArrayInputStream bais = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024 * 1024 * 4);

        int len = -1;
        byte[] block = new byte[512];
        FileInputStream fis = new FileInputStream("d:\\test\\g711\\streamax.bin");
        FileOutputStream fos = new FileOutputStream("d:\\test\\g711\\111111111122222222222222.pcm");

        ADPCMCodec codec = new ADPCMCodec();

        Jtt1078Decoder decoder = new Jtt1078Decoder();
        while ((len = fis.read(block)) > -1)
        {
            decoder.write(block, 0, len);
            while (true)
            {
                Packet p = decoder.decode();
                if (p == null) break;

                int lengthOffset = 28;
                int dataType = (p.seek(15).nextByte() >> 4) & 0x0f;
                // 透传数据类型：0100，没有后面的时间以及Last I Frame Interval和Last Frame Interval字段
                if (dataType == 0x04) lengthOffset = 28 - 8 - 2 - 2;
                else if (dataType == 0x03) lengthOffset = 28 - 4;

                // FFMpegManager.getInstance().feed(publisherId, packet.seek(lengthOffset + 2).nextBytes());
                if (dataType == 0x00 || dataType == 0x01 || dataType == 0x02)
                {

                }
                else
                {
                    byte[] data = p.seek(lengthOffset + 2).nextBytes();
                    fos.write(codec.toPCM(data));
                    fos.flush();
                }
            }
        }

        fos.flush();

        fis.close();
        fos.close();
    }

}
