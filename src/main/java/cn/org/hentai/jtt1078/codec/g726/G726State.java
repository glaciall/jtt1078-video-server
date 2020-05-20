package cn.org.hentai.jtt1078.codec.g726;

public class G726State {

    /** Locked or steady state step size multiplier. */
    /*long*/int yl;
    /** Unlocked or non-steady state step size multiplier. */
    /*short*/int yu;
    /** Short term energy estimate. */
    /*short*/int dms;
    /** Long term energy estimate. */
    /*short*/int dml;
    /** Linear weighting coefficient of 'yl' and 'yu'. */
    /*short*/int ap;

    /** Coefficients of pole portion of prediction filter. */
    /*short*/int[] a;
    /** Coefficients of zero portion of prediction filter. */
    /*short*/int[] b;
    /** Signs of previous two samples of a partially
     * reconstructed signal. */
    /*short*/int[] pk;
    /** Previous 6 samples of the quantized difference
     * signal represented in an internal floating point
     * format. */
    /*short*/int[] dq;
    /** Previous 2 samples of the quantized difference
     * signal represented in an internal floating point
     * format. */
    /*short*/int[] sr;
    /* delayed tone detect, new in 1988 version */
    /*char*/int td;


    /** The first 15 values, powers of 2. */
    private static final /*short*/int[] power2 = { 1, 2, 4, 8, 0x10, 0x20, 0x40, 0x80, 0x100, 0x200, 0x400, 0x800, 0x1000, 0x2000, 0x4000 };


    /** Quantizes the input val against the table of size short integers.
     * It returns i if table[i-1]<=val<table[i].
     * <p>
     * Using linear search for simple coding. */
    private static int quan(int val, /*short*/int[] table, int size) {

        int i;
        for (i=0; i<size; i++) if (val<table[i]) break;
        return i;
    }


    /** returns the integer product of the 14-bit integer "an" and
     * "floating point" representation (4-bit exponent, 6-bit mantessa) "srn". */
    private static int fmult(int an, int srn) {

        /*short*/int anmag=(an>0)? an : ((-an)&0x1FFF);
        /*short*/int anexp=quan(anmag, power2, 15)-6;
        /*short*/int anmant=(anmag==0)? 32 :
                (anexp >= 0)? anmag>>anexp : anmag<<-anexp;
        /*short*/int wanexp=anexp + ((srn>>6)&0xF) - 13;

        /*short*/int wanmant=(anmant*(srn&077) + 0x30)>>4;
        /*short*/int retval=(wanexp>=0)? ((wanmant<<wanexp)&0x7FFF) : (wanmant>>-wanexp);

        return (((an^srn)<0)? -retval : retval);
    }


    /** Creates a new G726State. */
    public G726State() {
        a=new /*short*/int[2];
        b=new /*short*/int[6];
        pk=new /*short*/int[2];
        dq=new /*short*/int[6];
        sr=new /*short*/int[2];
        init();
    }

    /** This routine initializes and/or resets the G726State 'state'. <br>
     * All the initial state values are specified in the CCITT G.721 document. */
    private void init() {
        yl=34816;
        yu=544;
        dms=0;
        dml=0;
        ap=0;
        for (int cnta=0; cnta<2; cnta++) {
            a[cnta]=0;
            pk[cnta]=0;
            sr[cnta]=32;
        }
        for (int cnta=0; cnta<6; cnta++) {
            b[cnta]=0;
            dq[cnta]=32;
        }
        td=0;
    }

    /** computes the estimated signal from 6-zero predictor. */
    public int predictor_zero() {

        int sezi=fmult(b[0]>>2, dq[0]);
        /* ACCUM */
        for (int i=1; i<6; i++) sezi+=fmult(b[i]>>2,dq[i]);
        return sezi;
    }


    /** computes the estimated signal from 2-pole predictor. */
    public int predictor_pole() {

        return (fmult(a[1]>>2,sr[1]) + fmult(a[0]>>2,sr[0]));
    }


    /** computes the quantization step size of the adaptive quantizer. */
    public int step_size() {

        if (ap>=256) return (yu);
        else {
            int y=yl>>6;
            int dif=yu-y;
            int al=ap>>2;
            if (dif>0) y+=(dif * al)>>6;
            else
            if (dif<0) y+=(dif * al+0x3F)>>6;
            return y;
        }
    }
}
