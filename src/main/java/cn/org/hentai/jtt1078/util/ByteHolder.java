package cn.org.hentai.jtt1078.util;

import java.util.Arrays;

/**
 * Created by matrixy on 2018-06-15.
 */
public class ByteHolder
{
    int offset = 0;
    int size = 0;
    byte[] buffer = null;

    public ByteHolder(int bufferSize)
    {
        this.buffer = new byte[bufferSize];
    }

    public int size()
    {
        return this.size;
    }

    public void write(byte[] data)
    {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int length)
    {
        while (this.offset + length >= buffer.length)
            throw new RuntimeException(String.format("exceed the max buffer size, max length: %d, data length: %d", buffer.length, length));

        // 复制一下内容
        System.arraycopy(data, offset, buffer, this.offset, length);

        this.offset += length;
        this.size += length;
    }

    public byte[] array()
    {
        return array(this.size);
    }

    public byte[] array(int length)
    {
        return Arrays.copyOf(this.buffer, length);
    }

    public void write(byte b)
    {
        this.buffer[offset++] = b;
        this.size += 1;
    }

    public void sliceInto(byte[] dest, int length)
    {
        System.arraycopy(this.buffer, 0, dest, 0, length);
        // 往前挪length个位
        System.arraycopy(this.buffer, length, this.buffer, 0, this.size - length);
        this.offset -= length;
        this.size -= length;
    }

    public void slice(int length)
    {
        // 往前挪length个位
        System.arraycopy(this.buffer, length, this.buffer, 0, this.size - length);
        this.offset -= length;
        this.size -= length;
    }

    public byte get(int position)
    {
        return this.buffer[position];
    }

    public void clear()
    {
        this.offset = 0;
        this.size = 0;
    }

    public int getInt(int offset)
    {
        return ByteUtils.getInt(this.buffer, offset, 4);
    }

    public int getShort(int position)
    {
        int h = this.buffer[position] & 0xff;
        int l = this.buffer[position + 1] & 0xff;
        return ((h << 8) | l) & 0xffff;
    }
}