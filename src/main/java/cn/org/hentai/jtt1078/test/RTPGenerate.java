package cn.org.hentai.jtt1078.test;

import cn.org.hentai.jtt1078.flv.FlvEncoder;
import cn.org.hentai.jtt1078.server.Jtt1078Decoder;
import cn.org.hentai.jtt1078.util.ByteHolder;
import cn.org.hentai.jtt1078.util.ByteUtils;
import cn.org.hentai.jtt1078.util.Packet;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

/**
 * Created by matrixy on 2019/12/16.
 */
public class RTPGenerate
{
    public static void main(String[] args) throws Exception
    {
        InputStream input = new FileInputStream("d:\\test\\1078\\streamax-20191209.bin");
        LinkedList<Packet> packets = readPackets(input);

        for (int i = 0; i < 100; i++)
        {
            String sim = String.format("013800138%03d", i);
            int channel = 1;

            try (OutputStream output = new FileOutputStream("d:\\test\\1078\\temp\\" + sim + "-" + channel + ".bin"))
            {
                for (Packet p : packets)
                {
                    p.seek(8).putBytes(toBCD(sim));
                    p.seek(14).putByte((byte)channel);
                    output.write(p.getBytes());
                }
                System.out.println(String.format(" -> %s-%d generated...", sim, channel));
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
                System.out.println(ex);
            }
        }

        input.close();
    }

    public static LinkedList<Packet> readPackets(InputStream input) throws Exception
    {
        int len = -1;
        byte[] block = new byte[1024];
        Jtt1078Decoder decoder = new Jtt1078Decoder();

        LinkedList<Packet> packets = new LinkedList();

        while ((len = input.read(block)) > -1)
        {
            decoder.write(block, 0, len);
            while (true)
            {
                Packet p = decoder.decode();
                if (p == null) break;

                packets.add(p);
            }
        }

        return packets;
    }

    public static byte[] toBCD(String sim)
    {
        byte[] bcd = new byte[sim.length() / 2];
        for (int i = 0, k = 0, l = sim.length(); i < l; i+=2)
        {
            char a = (char)(sim.charAt(i) - '0');
            char b = (char)(sim.charAt(i + 1) - '0');
            bcd[k++] = ((byte)(a << 4 | b));
        }
        return bcd;
    }
}
