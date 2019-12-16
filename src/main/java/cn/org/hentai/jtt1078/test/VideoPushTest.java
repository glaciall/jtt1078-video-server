package cn.org.hentai.jtt1078.test;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by matrixy on 2019/4/10.
 */
public class VideoPushTest
{
    public static void main(String[] args) throws Exception
    {
        Socket conn = new Socket("localhost", 1078);
        OutputStream os = conn.getOutputStream();

        // InputStream fis = new FileInputStream("e:\\workspace\\enc_dec_audio\\streamax.bin");
        InputStream fis = new FileInputStream("e:\\test\\streaming.hex");
        int len = -1;
        byte[] block = new byte[512];
        while ((len = fis.read(block)) > -1)
        {
            os.write(block, 0, len);
            os.flush();
            Thread.sleep(20);
            System.out.println("sending...");
        }
        os.close();
        fis.close();
        conn.close();
    }
}
