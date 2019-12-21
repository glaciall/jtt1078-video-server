package cn.org.hentai.jtt1078.test;

import cn.org.hentai.jtt1078.codec.G711Codec;

import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Created by matrixy on 2019/12/21.
 */
public class G711ATest
{
    public static void main(String[] args) throws Exception
    {
        int len = -1;
        byte[] block = new byte[1024];
        FileInputStream fis = new FileInputStream("E:\\workspace\\enc_dec_audio\\g711\\encode_out.g711a");
        FileOutputStream fos = new FileOutputStream("E:\\test\\fuckfuckfuck111.pcm");
        G711Codec codec = new G711Codec();
        while ((len = fis.read(block)) > -1)
        {
            byte[] pcmData = null;
            if (len == 1024)
            {
                pcmData = codec.toPCM(block);
            }
            else
            {
                byte[] temp = new byte[len];
                System.arraycopy(block, 0, temp, 0, len);
                pcmData = codec.toPCM(temp);
            }
            fos.write(pcmData);
            fos.flush();
        }
        fis.close();
        fos.close();
    }
}
