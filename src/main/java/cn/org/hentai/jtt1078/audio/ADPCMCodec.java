package cn.org.hentai.jtt1078.audio;

public final class ADPCMCodec {
    static int[] indexTable = { -1, -1, -1, -1, 2, 4, 6, 8, -1, -1, -1, -1, 2, 4, 6, 8 };

    static int[] stepsizeTable = { 7, 8, 9, 10, 11, 12, 13, 14, 16, 17, 19, 21, 23, 25, 28, 31, 34, 37, 41, 45, 50, 55,
            60, 66, 73, 80, 88, 97, 107, 118, 130, 143, 157, 173, 190, 209, 230, 253, 279, 307, 337, 371, 408, 449, 494,
            544, 598, 658, 724, 796, 876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066, 2272, 2499, 2749, 3024,
            3327, 3660, 4026, 4428, 4871, 5358, 5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899, 15289,
            16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767 };

    static class ADPCMState
    {
        public short valprev;
        public byte index;
    }

    byte[] adpcm_coder(short[] inp, int len, ADPCMState state) {
        // short *inp; /* Input buffer pointer */
        byte outp[]; /* output buffer pointer */
        int val; /* Current input sample value */
        int sign; /* Current adpcm sign bit */
        int delta; /* Current adpcm output value */
        int diff; /* Difference between val and valprev */
        int step; /* Stepsize */
        int valpred; /* Predicted output value */
        int vpdiff; /* Current change to valpred */
        int index; /* Current step change index */
        int outputbuffer = 0; /* place to keep previous 4-bit value */
        boolean bufferstep; /* toggle between outputbuffer/output */

        outp = new byte[len / 2];

        valpred = state.valprev;
        index = state.index;
        step = stepsizeTable[index];

        bufferstep = true;

        int intIndex = 0, outIndex = 0;
        for (; len > 0; len--) {
            val = inp[intIndex++];

            /* Step 1 - compute difference with previous value */
            diff = val - valpred;
            sign = (diff < 0) ? 8 : 0;
            if (sign != 0)
                diff = (-diff);

            /* Step 2 - Divide and clamp */
            /*
             * Note: This code *approximately* computes: delta = diff*4/step; vpdiff =
             * (delta+0.5)*step/4; but in shift step bits are dropped. The net result of
             * this is that even if you have fast mul/div hardware you cannot put it to good
             * use since the fixup would be too expensive.
             */
            delta = 0;
            vpdiff = (step >> 3);

            if (diff >= step) {
                delta = 4;
                diff -= step;
                vpdiff += step;
            }
            step >>= 1;
            if (diff >= step) {
                delta |= 2;
                diff -= step;
                vpdiff += step;
            }
            step >>= 1;
            if (diff >= step) {
                delta |= 1;
                vpdiff += step;
            }

            /* Step 3 - Update previous value */
            if (sign != 0)
                valpred -= vpdiff;
            else
                valpred += vpdiff;

            /* Step 4 - Clamp previous value to 16 bits */
            if (valpred > 32767)
                valpred = 32767;
            else if (valpred < -32768)
                valpred = -32768;

            /* Step 5 - Assemble value, update index and step values */
            delta |= sign;

            index += indexTable[delta];
            if (index < 0)
                index = 0;
            if (index > 88)
                index = 88;
            step = stepsizeTable[index];

            /* Step 6 - Output value */
            if (bufferstep) {
                outputbuffer = (delta << 4) & 0xf0;
            } else {
                outp[outIndex++] = (byte) ((delta & 0x0f) | outputbuffer);
            }
            bufferstep = !bufferstep;
        }

        /* Output last step, if needed */
        if (!bufferstep)
            outp[outIndex++] = (byte) outputbuffer;

        state.valprev = (short) valpred;
        state.index = (byte) index;
        return outp;
    }

    public static short[] adpcm_decoder(byte[] inp, int len, ADPCMState state) {
        // signed char *inp; /* Input buffer pointer */
        // short *outp; /* output buffer pointer */
        int sign; /* Current adpcm sign bit */
        int delta; /* Current adpcm output value */
        int step; /* Stepsize */
        int valpred; /* Predicted value */
        int vpdiff; /* Current change to valpred */
        int index; /* Current step change index */
        int inputbuffer = 0; /* place to keep next 4-bit value */
        boolean bufferstep; /* toggle between inputbuffer/input */

        short[] outp = new short[len];
        // inp = (signed char *)indata;

        valpred = state.valprev;
        index = state.index;
        step = stepsizeTable[index];

        bufferstep = false;

        for (int inIndex = 0, outIndex = 0; len > 0; len--) {

            /* Step 1 - get the delta value */
            if (bufferstep) {
                delta = inputbuffer & 0xf;
            } else {
                inputbuffer = inp[inIndex++];
                delta = (inputbuffer >> 4) & 0xf;
            }
            bufferstep = !bufferstep;

            /* Step 2 - Find new index value (for later) */
            index += indexTable[delta];
            if (index < 0)
                index = 0;
            if (index > 88)
                index = 88;

            /* Step 3 - Separate sign and magnitude */
            sign = delta & 8;
            delta = delta & 7;

            /* Step 4 - Compute difference and new predicted value */
            /*
             ** Computes 'vpdiff = (delta+0.5)*step/4', but see comment in adpcm_coder.
             */
            vpdiff = step >> 3;
            if ((delta & 4) != 0)
                vpdiff += step;
            if ((delta & 2) != 0)
                vpdiff += step >> 1;
            if ((delta & 1) != 0)
                vpdiff += step >> 2;

            if (sign != 0)
                valpred -= vpdiff;
            else
                valpred += vpdiff;

            /* Step 5 - clamp output value */
            if (valpred > 32767)
                valpred = 32767;
            else if (valpred < -32768)
                valpred = -32768;

            /* Step 6 - Update step value */
            step = stepsizeTable[index];

            /* Step 7 - Output value */
            outp[outIndex++] = (short) valpred;
        }

        state.valprev = (short) valpred;
        state.index = (byte) index;
        return outp;
    }
}
