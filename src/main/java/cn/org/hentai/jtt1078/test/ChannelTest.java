package cn.org.hentai.jtt1078.test;

import cn.org.hentai.jtt1078.util.ByteHolder;
import cn.org.hentai.jtt1078.util.ByteUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * Created by matrixy on 2020/1/9.
 */
public class ChannelTest implements ByteChannel
{
    byte[] temp = new byte[4];
    ByteHolder buffer = new ByteHolder(1024);

    // 读出，存入dst
    @Override
    public int read(ByteBuffer dst) throws IOException
    {
        dst.flip();
        int len = Math.min(4, buffer.size());
        if (dst.remaining() > len)
        {
            buffer.sliceInto(temp, len);
            dst.put(temp, 0, len);
        }
        else
        {
            // 丢掉？？？
        }
        dst.flip();
        return len;
    }

    // 从src读出，写入进来
    @Override
    public int write(ByteBuffer src) throws IOException
    {
        int len = -1;
        // src.flip();
        len = Math.min(4, src.limit());
        src.get(temp, 0, len);
        buffer.write(temp, 0, len);
        // src.flip();
        System.out.println("write: " + len);
        return len;
    }

    @Override
    public boolean isOpen()
    {
        return true;
    }

    @Override
    public void close() throws IOException
    {

    }

    public byte[] array()
    {
        return buffer.array();
    }

    public static void main(String[] args) throws Exception
    {
        ChannelTest chl = new ChannelTest();
        ByteBuffer buffer = ByteBuffer.allocate(4);
        java.nio.ByteBuffer xx;
        System.out.println(buffer.getClass().getName());
        for (int i = 0; i < 4096; i++)
            buffer.put((byte)'f');
        /*
        buffer.putLong(0x1122334455667788L);
        buffer.flip();
        // flip太迷惑了
        buffer.isReadOnly();
        int len = chl.write(buffer);
        len = chl.write(buffer);
        ByteUtils.dump(chl.array());
        */
    }

    static final class ByteBufferWrapper
    {
        boolean writeMode;
        ByteBuffer buffer;

        private ByteBufferWrapper(int size)
        {
            this.buffer = ByteBuffer.allocate(size);
        }

        // 控制写入，代理过来
        public void write()
        {

        }

        // 写出就无所谓了

        public static ByteBufferWrapper create(int size)
        {
            return new ByteBufferWrapper(size);
        }
    }
}
