package cn.org.hentai.jtt1078.media.publisher;

import cn.org.hentai.jtt1078.media.Media;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;

/**
 * Created by matrixy on 2019/12/14.
 */
public class AudioPublisher extends MediaStreamPublisher
{
    public AudioPublisher(long channel, String tag, Process process)
    {
        super(channel, tag, process);
    }

    @Override
    public void transcodeTo(Media.Encoding mediaEncoding, byte[] data, OutputStream output) throws Exception
    {
        output.write(data);
        output.flush();

        System.out.println("audio published...");
    }
}
