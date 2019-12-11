package cn.org.hentai.jtt1078.test;

import cn.org.hentai.jtt1078.server.Jtt1078Decoder;
import cn.org.hentai.jtt1078.util.Packet;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

/**
 * Created by houcheng on 2019-12-10.
 */
public class VideoServer
{
    public static void main(String[] args) throws Exception
    {
        System.out.println("started...");

        String command = ("ffmpeg -f h264 -i /opt/test/xxoo.video -f sln -ar 8000 -ac 1 -i /opt/test/xxoo.audio -vcodec copy -acodec aac -map 0:v:0 -map 1:a:0 -probesize 512 -analyzeduration 100 -f flv rtmp://localhost/live/fuck");
        Process process = Runtime.getRuntime().exec(command);
        new Reader(process.getErrorStream()).start();

        Thread.sleep(2000);

        ServerSocket server = new ServerSocket(1078, 100);
        Socket conn = server.accept();
        System.out.println("Connected from: " + conn.getRemoteSocketAddress());
        InputStream input = conn.getInputStream();

        Publisher videoPublisher = new VideoPublisher();
        Publisher audioPublisher = new AudioPublisher();

        videoPublisher.start();
        audioPublisher.start();

        int len = -1;
        byte[] block = new byte[1024];
        Jtt1078Decoder decoder = new Jtt1078Decoder();
        while (true)
        {
            len = input.read(block);
            if (len == -1) break;
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
                    videoPublisher.publish(p.seek(lengthOffset + 2).nextBytes());
                }
                else
                {
                    audioPublisher.publish(p.seek(lengthOffset + 2).nextBytes());
                }
            }
        }

        System.in.read();
    }



    static class Reader extends Thread
    {
        InputStream stdout = null;
        public Reader(InputStream is)
        {
            this.stdout = is;
        }

        public void run()
        {
            int len = -1;
            byte[] block = new byte[512];
            try
            {
                while ((len = stdout.read(block)) > -1)
                {
                    System.out.print(new String(block, 0, len));
                }
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    static class Publisher extends Thread
    {
        Object lock = new Object();
        LinkedList<byte[]> messages = new LinkedList();

        public void publish(byte[] msg)
        {
            synchronized (lock)
            {
                messages.add(msg);
                lock.notify();
            }
        }

        public byte[] peek()
        {
            byte[] msg = null;
            synchronized (lock)
            {
                while (messages.size() == 0) try { lock.wait(); } catch(Exception e) { }
                msg = messages.removeFirst();
            }
            return msg;
        }

        public FileOutputStream open(String fname)
        {
            try
            {
                return new FileOutputStream(fname);
            }
            catch(Exception ex)
            {
                throw new RuntimeException(ex);
            }
        }
    }

    static class VideoPublisher extends Publisher
    {
        public void run()
        {
            FileOutputStream fos = open("/opt/test/xxoo.video");
            while (!this.isInterrupted())
            {
                try
                {
                    byte[] msg = peek();
                    fos.write(msg);
                    fos.flush();
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    break;
                }
            }
        }
    }

    static class AudioPublisher extends Publisher
    {
        public void run()
        {
            FileOutputStream fos = open("/opt/test/xxoo.audio");
            while (!this.isInterrupted())
            {
                try
                {
                    byte[] msg = peek();
                    fos.write(msg);
                    fos.flush();
                }
                catch(Exception ex)
                {
                    ex.printStackTrace();
                    break;
                }
            }
        }
    }
}
