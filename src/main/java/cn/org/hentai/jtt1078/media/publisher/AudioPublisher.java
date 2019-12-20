package cn.org.hentai.jtt1078.media.publisher;

import cn.org.hentai.jtt1078.audio.ADPCMCodec;
import cn.org.hentai.jtt1078.audio.AudioCodec;
import cn.org.hentai.jtt1078.audio.RawDataCopyCodec;
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
        AudioCodec codec = AudioCodec.getCodec(mediaEncoding);
        output.write(codec.toPCM(data));
        output.flush();
    }
}
