package cn.org.hentai.jtt1078.server;

import cn.org.hentai.jtt1078.util.ByteHolder;
import cn.org.hentai.jtt1078.util.ByteUtils;
import cn.org.hentai.jtt1078.util.Packet;

/**
 * Created by matrixy on 2019/4/9.
 */
public class Jtt1078Decoder
{
    ByteHolder buffer = new ByteHolder(4096);

    public void write(byte[] block)
    {
        buffer.write(block);
    }

    public void write(byte[] block, int startIndex, int length)
    {
        byte[] buff = new byte[length];
        System.arraycopy(block, startIndex, buff, 0, length);
        write(buff);
    }

    public Packet decode()
    {
        if (this.buffer.size() < 30) return null;

        if ((buffer.getInt(0) & 0x7fffffff) != 0x30316364)
        {
            String header = ByteUtils.toString(buffer.array(30));
            throw new RuntimeException("invalid protocol header: " + header);
        }

        int bodyLength = this.buffer.getShort(28);
        if (this.buffer.size() < bodyLength + 30) return null;
        Packet packet = Packet.create(bodyLength + 30);
        byte[] block = new byte[bodyLength + 30];
        this.buffer.sliceInto(block, bodyLength + 30);
        return Packet.create(block);
    }
}
