
package cn.org.hentai.jtt1078.codec.g726;

import cn.org.hentai.jtt1078.codec.G711Codec;
import cn.org.hentai.jtt1078.codec.G711UCodec;

/** G726_32 encoder and decoder.
  * <p>
  * These routines comprise an implementation of the CCITT G.726 32kbps ADPCM
  * coding algorithm.  Essentially, this implementation is identical to
  * the bit level description except for a few deviations which
  * take advantage of work station attributes, such as hardware 2's
  * complement arithmetic and large memory.  Specifically, certain time
  * consuming operations such as multiplications are replaced
  * with lookup tables and software 2's complement operations are
  * replaced with hardware 2's complement.
  * <p>
  * The deviation from the bit level specification (lookup tables)
  * preserves the bit level performance specifications.
  * <p>
  * As outlined in the G.726 Recommendation, the algorithm is broken
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
public class G726_32 extends G726 {
	
	// ##### C-to-Java conversion: #####
	// short becomes int
	// char becomes int
	// unsigned char becomes int


	// *************************** STATIC ***************************

	static /*short*/int[] qtab_721={-124, 80, 178, 246, 300, 349, 400};
	/*
	 * Maps G726_32 code word to reconstructed scale factor normalized log
	 * magnitude values.
	 */
	static /*short*/int[] _dqlntab={-2048, 4, 135, 213, 273, 323, 373, 425, 425, 373, 323, 273, 213, 135, 4, -2048};
	
	/* Maps G726_32 code word to log of scale factor multiplier. */
	static /*short*/int[] _witab={-12, 18, 41, 64, 112, 198, 355, 1122, 1122, 355, 198, 112, 64, 41, 18, -12};

	/*
	 * Maps G726_32 code words to a set of values whose long and short
	 * term averages are computed and then compared to give an indication
	 * how stationary (steady state) the signal is.
	 */
	static /*short*/int[] _fitab={0, 0, 0, 0x200, 0x200, 0x200, 0x600, 0xE00, 0xE00, 0x600, 0x200, 0x200, 0x200, 0, 0, 0};
	
	/** Encodes the input vale of linear PCM, A-law or u-law data sl and returns
	  * the resulting code. -1 is returned for unknown input coding value. */
	public static int encode(int sl, int in_coding, G726State state) {
		
		/*short*/int sezi, se, sez; /* ACCUM */
		/*short*/int d; /* SUBTA */
		/*short*/int sr; /* ADDB */
		/*short*/int y; /* MIX */
		/*short*/int dqsez; /* ADDC */
		/*short*/int dq, i;
	
		switch (in_coding) {
			/* linearize input sample to 14-bit PCM */
			case AUDIO_ENCODING_ALAW:
				sl= G711Codec.alaw2linear((byte)sl) >> 2;
				break;
			case AUDIO_ENCODING_ULAW:
				sl= G711UCodec.ulaw2linear((byte)sl) >> 2;
				break;
			case AUDIO_ENCODING_LINEAR:
				sl >>= 2; /* 14-bit dynamic range */
				break;
			default:
				return -1;
		}
	
		sezi=state.predictor_zero();
		sez=sezi >> 1;
		se=(sezi+state.predictor_pole()) >> 1;   /* estimated signal */
	
		d=sl-se; /* estimation difference */
	
		/* quantize the prediction difference */
		y=state.step_size(); /* quantizer step size */
		i=quantize(d, y, qtab_721, 7); /* i=ADPCM code */
	
		dq=reconstruct(i & 8, _dqlntab[i], y); /* quantized est diff */
	
		sr=(dq<0)? se-(dq & 0x3FFF) : se+dq; /* reconst. signal */
	
		dqsez=sr+sez-se;        /* pole prediction diff. */
	
		update(4, y, _witab[i] << 5, _fitab[i], dq, sr, dqsez, state);
	
		return i;
	}
	
	/** Decodes a 4-bit code of G726_32 encoded data of i and
	  * returns the resulting linear PCM, A-law or u-law value.
	  * return -1 for unknown out_coding value. */
	public static int decode(int i, int out_coding, G726State state) {
		
		/*short*/int sezi, sei, sez, se; /* ACCUM */
		/*short*/int y; /* MIX */
		/*short*/int sr; /* ADDB */
		/*short*/int dq;
		/*short*/int dqsez;
	
		i &= 0x0f; /* mask to get proper bits */
		sezi=state.predictor_zero();
		sez=sezi >> 1;
		sei=sezi+state.predictor_pole();
		se=sei >> 1; /* se=estimated signal */
	
		y=state.step_size(); /* dynamic quantizer step size */
	
		dq=reconstruct(i & 0x08, _dqlntab[i], y); /* quantized diff. */
	
		sr=(dq<0)? (se-(dq & 0x3FFF)) : se+dq; /* reconst. signal */
	
		dqsez=sr-se+sez; /* pole prediction diff. */
	
		update(4, y, _witab[i] << 5, _fitab[i], dq, sr, dqsez, state);
	
		switch (out_coding) {
			
			case AUDIO_ENCODING_ALAW:
				return (tandem_adjust_alaw(sr, se, y, i, 8, qtab_721));
			case AUDIO_ENCODING_ULAW:
				return (tandem_adjust_ulaw(sr, se, y, i, 8, qtab_721));
			case AUDIO_ENCODING_LINEAR:
				return (sr << 2); /* sr was 14-bit dynamic range */
			default:
				return -1;
		}
	}


	/** Encodes the input chunk in_buff of linear PCM, A-law or u-law data and returns
	  * the G726_32 encoded chuck into out_buff. <br>
	  * It returns the actual size of the output data, or -1 in case of unknown
	  * in_coding value. */
	public static int encode(byte[] in_buff, int in_offset, int in_len, int in_coding, byte[] out_buff, int out_offset, G726State state) {
		
		if (in_coding==AUDIO_ENCODING_ALAW || in_coding==AUDIO_ENCODING_ULAW) {
			
			/*
			for (int i=0; i<in_len; i++) {
				int in_value=in_buff[in_offset+i];
				int out_value=encode(in_value,in_coding,state);
				int i_div_2=i/2;
				if (i_div_2*2==i)
					out_buff[out_offset+i_div_2]=(byte)(out_value<<4);
				else
					out_buff[out_offset+i_div_2]=(byte)(out_value+unsignedInt(out_buff[out_offset+i_div_2]));
			}
			return in_len/2;
			*/
			int len_div_2=in_len/2;
			for (int i=0; i<len_div_2; i++) {
				int in_index=in_offset+i*2;
				int in_value1=in_buff[in_index];
				int in_value2=in_buff[in_index+1];
				int out_value1=encode(in_value1,in_coding,state);        
				int out_value2=encode(in_value2,in_coding,state);      
				out_buff[out_offset+i]=(byte)((out_value1<<4) + out_value2);
			}
			return len_div_2;
		}
		else
		if (in_coding==AUDIO_ENCODING_LINEAR) {
			
			int len_div_4=in_len/4;
			for (int i=0; i<len_div_4; i++) {
				int in_index=in_offset+i*4;
				int in_value1=signedIntLittleEndian(in_buff[in_index+1],in_buff[in_index+0]);
				int in_value2=signedIntLittleEndian(in_buff[in_index+3],in_buff[in_index+2]);

				//int out_value1=encode(G711.linear2ulaw(in_value1),AUDIO_ENCODING_ULAW,state);        
				//int out_value2=encode(G711.linear2ulaw(in_value2),AUDIO_ENCODING_ULAW,state);      
				int out_value1=encode(in_value1,in_coding,state);        
				int out_value2=encode(in_value2,in_coding,state);      
				out_buff[out_offset+i]=(byte)((out_value1<<4) + out_value2);
			}
			return len_div_4;
		}
		else return -1;
	}


	/** Decodes the input chunk in_buff of G726_32 encoded data and returns
	  * the linear PCM, A-law or u-law chunk into out_buff. <br>
	  * It returns the actual size of the output data, or -1 in case of unknown
	  * out_coding value. */
	public static int decode(byte[] in_buff, int in_offset, int in_len, int out_coding, byte[] out_buff, int out_offset, G726State state) {
		
		if (out_coding==AUDIO_ENCODING_ALAW || out_coding==AUDIO_ENCODING_ULAW) {
			
			/*
			for (int i=0; i<in_len*2; i++) {
				int in_value=unsignedInt(in_buff[in_offset+i/2]);
				if ((i/2)*2==i)
					in_value>>=4;
				else
					in_value%=0x10; 
				int out_value=decode(in_value,out_coding,state);  
				out_buff[out_offset+i]=(byte)out_value;
			}
			return in_len*2;
			*/
			for (int i=0; i<in_len; i++) {
				int in_value=unsignedInt(in_buff[in_offset+i]);
				int out_value1=decode(in_value>>4,out_coding,state);  
				int out_value2=decode(in_value&0xF,out_coding,state);
				int out_index=out_offset+i*2;
				out_buff[out_index]=(byte)out_value1;
				out_buff[out_index+1]=(byte)out_value2;
			}
			return in_len*2;
		}
		else
		if (out_coding==AUDIO_ENCODING_LINEAR) {
			
			for (int i=0; i<in_len; i++) {
				int in_value=unsignedInt(in_buff[in_offset+i]);
				//int out_value1=G711.ulaw2linear(decode(in_value>>4,AUDIO_ENCODING_ULAW,state));  
				//int out_value2=G711.ulaw2linear(decode(in_value&0xF,AUDIO_ENCODING_ULAW,state));
				int out_value1=decode(in_value>>4,out_coding,state);  
				int out_value2=decode(in_value&0xF,out_coding,state);
				int out_index=out_offset+i*4;
				out_buff[out_index]=(byte)(out_value1&0xFF);
				out_buff[out_index+1]=(byte)(out_value1>>8);
				out_buff[out_index+2]=(byte)(out_value2&0xFF);
				out_buff[out_index+3]=(byte)(out_value2>>8);
			}
			return in_len*4;
		}
		else return -1;
	}


	// ************************* NON-STATIC *************************

	/** Creates a new G726_32 processor, that can be used to encode from or decode do PCM audio data. */
	public G726_32() {
		super(32000);
	}


	/** Encodes the input vale of linear PCM, A-law or u-law data sl and returns
	  * the resulting code. -1 is returned for unknown input coding value. */
	public int encode(int sl, int in_coding) {
		return encode(sl,in_coding,state);
	}

	/** Encodes the input chunk in_buff of linear PCM, A-law or u-law data and returns
	  * the G726_32 encoded chuck into out_buff. <br>
	  * It returns the actual size of the output data, or -1 in case of unknown
	  * in_coding value. */
	public int encode(byte[] in_buff, int in_offset, int in_len, int in_coding, byte[] out_buff, int out_offset) {
		return encode(in_buff,in_offset,in_len,in_coding,out_buff,out_offset,state);
	}


	/** Decodes a 4-bit code of G726_32 encoded data of i and
	  * returns the resulting linear PCM, A-law or u-law value.
	  * return -1 for unknown out_coding value. */
	public int decode(int i, int out_coding) {
		return decode(i,out_coding,state);
	}


	/** Decodes the input chunk in_buff of G726_32 encoded data and returns
	  * the linear PCM, A-law or u-law chunk into out_buff. <br>
	  * It returns the actual size of the output data, or -1 in case of unknown
	  * out_coding value. */
	public int decode(byte[] in_buff, int in_offset, int in_len, int out_coding, byte[] out_buff, int out_offset) {
		return decode(in_buff,in_offset,in_len,out_coding,out_buff,out_offset,state);
	}

}
