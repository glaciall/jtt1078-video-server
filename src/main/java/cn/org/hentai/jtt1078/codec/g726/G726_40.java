package cn.org.hentai.jtt1078.codec.g726;

import cn.org.hentai.jtt1078.codec.G711Codec;
import cn.org.hentai.jtt1078.codec.G711UCodec;

/** G726_40 encoder and decoder.
  * <p>
  * These routines comprise an implementation of the CCITT G.726 40kbps
  * ADPCM coding algorithm.  Essentially, this implementation is identical to
  * the bit level description except for a few deviations which
  * take advantage of workstation attributes, such as hardware 2's
  * complement arithmetic.
  * <p>
  * The deviation from the bit level specification (lookup tables),
  * preserves the bit level performance specifications.
  * <p>
  * As outlined in the G.723 Recommendation, the algorithm is broken
  * down into modules.  Each section of code below is preceded by
  * the name of the module which it is implementing.
  * <p>
  * This implementation is based on the ANSI-C language reference implementations
  * of the CCITT (International Telegraph and Telephone Consultative Committee)
  * G.711, G.721 and G.723 voice compressions, provided by Sun Microsystems, Inc.
  * <p>
  * Acknowledgement to Sun Microsystems, Inc. for having released the original
  * ANSI-C source code to the public domain.
  */
public class G726_40 extends G726 {
	
	// ##### C-to-Java conversion: #####
	// short becomes int
	// char becomes int
	// unsigned char becomes int
	

	// *************************** STATIC ***************************

	/*
	 * Maps G723_40 code word to ructeconstructed scale factor normalized log
	 * magnitude values.
	 */
	static /*short*/int[] _dqlntab={-2048, -66, 28, 104, 169, 224, 274, 318, 358, 395, 429, 459, 488, 514, 539, 566, 566, 539, 514, 488, 459, 429, 395, 358, 318, 274, 224, 169, 104, 28, -66, -2048};
	
	/* Maps G723_40 code word to log of scale factor multiplier. */
	static /*short*/int[] _witab={448, 448, 768, 1248, 1280, 1312, 1856, 3200, 4512, 5728, 7008, 8960, 11456, 14080, 16928, 22272, 22272, 16928, 14080, 11456, 8960, 7008, 5728, 4512, 3200, 1856, 1312, 1280, 1248, 768, 448, 448};
	
	/*
	 * Maps G723_40 code words to a set of values whose long and short
	 * term averages are computed and then compared to give an indication
	 * how stationary (steady state) the signal is.
	 */
	static /*short*/int[] _fitab={0, 0, 0, 0, 0, 0x200, 0x200, 0x200, 0x200, 0x200, 0x400, 0x600, 0x800, 0xA00, 0xC00, 0xC00, 0xC00, 0xC00, 0xA00, 0x800, 0x600, 0x400, 0x200, 0x200, 0x200, 0x200, 0x200, 0, 0, 0, 0, 0};
	
	static /*short*/int[] qtab_723_40={-122, -16, 68, 139, 198, 250, 298, 339, 378, 413, 445, 475, 502, 528, 553};
	
	/** Encodes a 16-bit linear PCM, A-law or u-law input sample and retuens
	  * the resulting 5-bit CCITT G726 40kbps code.
	  * Returns -1 if the input coding value is invalid. */
	public static int encode(int sl, int in_coding, G726State state) {
		
		/*short*/int sei, sezi, se, sez;  /* ACCUM */
		/*short*/int d; /* SUBTA */
		/*short*/int y; /* MIX */
		/*short*/int sr; /* ADDB */
		/*short*/int dqsez; /* ADDC */
		/*short*/int dq, i;
	
		switch (in_coding) {
			/* linearize input sample to 14-bit PCM */
			case AUDIO_ENCODING_ALAW:
				sl= G711Codec.alaw2linear((byte) sl) >> 2;
				break;
			case AUDIO_ENCODING_ULAW:
				sl= G711UCodec.ulaw2linear((byte)sl) >> 2;
				break;
			case AUDIO_ENCODING_LINEAR:
				sl >>= 2;      /* sl of 14-bit dynamic range */
				break;
			default:
				return (-1);
		}
	
		sezi=state.predictor_zero();
		sez=sezi >> 1;
		sei=sezi+state.predictor_pole();
		se=sei >> 1; /* se=estimated signal */
	
		d=sl-se; /* d=estimation difference */
	
		/* quantize prediction difference */
		y=state.step_size();  /* adaptive quantizer step size */
		i=quantize(d, y, qtab_723_40, 15);   /* i=ADPCM code */
	
		dq=reconstruct(i & 0x10, _dqlntab[i], y);  /* quantized diff */
	
		sr=(dq<0)? se-(dq & 0x7FFF) : se+dq; /* reconstructed signal */
	
		dqsez=sr+sez-se;     /* dqsez=pole prediction diff. */
	
		update(5, y, _witab[i], _fitab[i], dq, sr, dqsez, state);
	
		return (i);
	}

	
	/** Decodes a 5-bit CCITT G.726 40kbps code and returns
	  * the resulting 16-bit linear PCM, A-law or u-law sample value.
	  * -1 is returned if the output coding is unknown. */
	public static int decode(int i, int out_coding, G726State state) {
		
		/*short*/int sezi, sei, sez, se;  /* ACCUM */
		/*short*/int y, dif; /* MIX */
		/*short*/int sr; /* ADDB */
		/*short*/int dq;
		/*short*/int dqsez;
	
		i &= 0x1f; /* mask to get proper bits */
		sezi=state.predictor_zero();
		sez=sezi >> 1;
		sei=sezi+state.predictor_pole();
		se=sei >> 1; /* se=estimated signal */
	
		y=state.step_size();  /* adaptive quantizer step size */
		dq=reconstruct(i & 0x10, _dqlntab[i], y);  /* estimation diff. */
	
		sr=(dq<0)? (se-(dq & 0x7FFF)) : (se+dq); /* reconst. signal */
	
		dqsez=sr-se+sez; /* pole prediction diff. */
	
		update(5, y, _witab[i], _fitab[i], dq, sr, dqsez, state);
	
		switch (out_coding) {
			
			case AUDIO_ENCODING_ALAW:
				return (tandem_adjust_alaw(sr, se, y, i, 0x10, qtab_723_40));
			case AUDIO_ENCODING_ULAW:
				return (tandem_adjust_ulaw(sr, se, y, i, 0x10, qtab_723_40));
			case AUDIO_ENCODING_LINEAR:
				return (sr << 2); /* sr was of 14-bit dynamic range */
			default:
				return (-1);
		}
	}


	/** Encodes the input chunk in_buff of linear PCM, A-law or u-law data and returns
	  * the G726_40 encoded chuck into out_buff. <br>
	  * It returns the actual size of the output data, or -1 in case of unknown
	  * in_coding value. */
	public static int encode(byte[] in_buff, int in_offset, int in_len, int in_coding, byte[] out_buff, int out_offset, G726State state) {
		
		if (in_coding==AUDIO_ENCODING_ALAW || in_coding==AUDIO_ENCODING_ULAW) {
			
			int len_div_8=in_len/8;
			for (int i=0; i<len_div_8; i++) {
				long value8=0;
				int in_index=in_offset+i*8;
				for (int j=0; j<8; j++) {
					int in_value=unsignedInt(in_buff[in_index+j]);
					int out_value=encode(in_value,in_coding,state);
					value8+=((long)out_value)<<(5*(7-j));
				}
				int out_index=out_offset+i*5;
				for (int k=0; k<5; k++) {
					out_buff[out_index+k]=(byte)(value8>>(8*(4-k)));
				}
			}
			return len_div_8*5;
		}
		else
		if (in_coding==AUDIO_ENCODING_LINEAR) {
			
			int len_div_16=in_len/16;
			for (int i=0; i<len_div_16; i++) {
				long value16=0;
				int in_index=in_offset+i*16;
				for (int j=0; j<8; j++) {
					int j2=j*2;
					int in_value=signedIntLittleEndian(in_buff[in_index+j2+1],in_buff[in_index+j2]);
					int out_value=encode(in_value,in_coding,state);
					value16+=((long)out_value)<<(5*(7-j));
				}
				int out_index=out_offset+i*5;
				for (int k=0; k<5; k++) {
					out_buff[out_index+k]=(byte)(value16>>(8*(4-k)));
				}
			}
			return len_div_16*5;
		}
		else return -1;
	}


	/** Decodes the input chunk in_buff of G726_40 encoded data and returns
	  * the linear PCM, A-law or u-law chunk into out_buff. <br>
	  * It returns the actual size of the output data, or -1 in case of unknown
	  * out_coding value. */
	public static int decode(byte[] in_buff, int in_offset, int in_len, int out_coding, byte[] out_buff, int out_offset, G726State state) {
		
		if (out_coding==AUDIO_ENCODING_ALAW || out_coding==AUDIO_ENCODING_ULAW) {
			
			int len_div_5=in_len/5;
			for (int i=0; i<len_div_5; i++) {
				long value8=0;
				int in_index=in_offset+i*5;
				for (int j=0; j<5; j++) {
					value8+=(long)unsignedInt(in_buff[in_index+j])<<(8*(4-j));
				}
				int out_index=out_offset+i*8;
				for (int k=0; k<8; k++) {
					int in_value=(int)((value8>>(5*(7-k)))&0x1F);
					int out_value=decode(in_value,out_coding,state);
					out_buff[out_index+k]=(byte)out_value;
				}
			}
			return len_div_5*8;
		}
		else
		if (out_coding==AUDIO_ENCODING_LINEAR) {
			
			int len_div_5=in_len/5;
			for (int i=0; i<len_div_5; i++) {
				long value16=0;
				int in_index=in_offset+i*5;
				for (int j=0; j<5; j++) {
					value16+=(long)unsignedInt(in_buff[in_index+j])<<(8*(4-j));
				}
				int out_index=out_offset+i*16;
				for (int k=0; k<8; k++) {
					int k2=k*2;
					int in_value=(int)((value16>>(5*(7-k)))&0x1F);
					int out_value=decode(in_value,out_coding,state);
					out_buff[out_index+k2]=(byte)(out_value&0xFF);
					out_buff[out_index+k2+1]=(byte)(out_value>>8);
				}
			}
			return len_div_5*16;
		}
		else return -1;
	}


	// ************************* NON-STATIC *************************

	/** Creates a new G726_40 processor, that can be used to encode from or decode do PCM audio data. */
	public G726_40() {
		super(40000);
	}


	/** Encodes a 16-bit linear PCM, A-law or u-law input sample and retuens
	  * the resulting 5-bit CCITT G.726 40kbps code.
	  * Returns -1 if the input coding value is invalid. */
	public int encode(int sl, int in_coding) {
		return encode(sl,in_coding,state);
	}


	/** Encodes the input chunk in_buff of linear PCM, A-law or u-law data and returns
	  * the G726_40 encoded chuck into out_buff. <br>
	  * It returns the actual size of the output data, or -1 in case of unknown
	  * in_coding value. */
	public int encode(byte[] in_buff, int in_offset, int in_len, int in_coding, byte[] out_buff, int out_offset) {
		return encode(in_buff,in_offset,in_len,in_coding,out_buff,out_offset,state);
	}


	/** Decodes a 5-bit CCITT G.726 40kbps code and returns
	  * the resulting 16-bit linear PCM, A-law or u-law sample value.
	  * -1 is returned if the output coding is unknown. */
	public int decode(int i, int out_coding) {
		return decode(i,out_coding,state);
	}


	/** Decodes the input chunk in_buff of G726_40 encoded data and returns
	  * the linear PCM, A-law or u-law chunk into out_buff. <br>
	  * It returns the actual size of the output data, or -1 in case of unknown
	  * out_coding value. */
	public int decode(byte[] in_buff, int in_offset, int in_len, int out_coding, byte[] out_buff, int out_offset) {
		return decode(in_buff,in_offset,in_len,out_coding,out_buff,out_offset,state);
	}


}
