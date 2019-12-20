package cn.org.hentai.jtt1078.media.publisher;

import cn.org.hentai.jtt1078.media.Media;
import cn.org.hentai.jtt1078.media.StdoutCleaner;
import cn.org.hentai.jtt1078.util.Configs;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.LinkedList;

public class VideoPublisher extends MediaStreamPublisher
{
    public VideoPublisher(long channel, String tag, Process process)
    {
        super(channel, tag, process);
    }

    @Override
    public void transcodeTo(Media.Encoding mediaEncoding, byte[] data, OutputStream output) throws Exception
    {
        output.write(data);
        output.flush();
    }
}
