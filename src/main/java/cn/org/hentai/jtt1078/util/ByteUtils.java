package cn.org.hentai.jtt1078.util;

/**
 * Created by matrixy on 2017/8/22.
 */
public final class ByteUtils
{
    public static byte[] parse(String hexString)
    {
        String[] hexes = hexString.split(" ");
        byte[] data = new byte[hexes.length];
        for (int i = 0; i < hexes.length; i++) data[i] = (byte)(Integer.parseInt(hexes[i], 16) & 0xff);
        return data;
    }

    public static synchronized void dump(byte[] data)
    {
        for (int i = 0, l = data.length; i < l; )
        {
            String ascii = "";
            int k = 0, f = 0;
            for (; k < 16; k++)
            {
                if (k + i < l)
                {
                    f++;
                    byte d = data[i + k];
                    String hex = Integer.toHexString(d & 0xff).toUpperCase();
                    if (hex.length() == 1) hex = "0" + hex;
                    if (d >= 0x20 && d < 127) ascii += (char)d;
                    else ascii += '.';
                    System.out.print(hex);
                }
                else
                {
                    System.out.print(' ');
                    System.out.print(' ');
                }

                if (k % 4 == 3) System.out.print("   ");
                else System.out.print(' ');
            }
            i += f;
            System.out.println(ascii);
        }
    }

    public static String toString(byte[] data)
    {
        if (null == data) return "";
        return toString(data, data.length);
    }

    public static String toString(byte[] buff, int length)
    {
        StringBuffer sb = new StringBuffer(length * 2);
        for (int i = 0; i < length; i++)
        {
            if ((buff[i] & 0xff) < 0x10) sb.append('0');
            sb.append(Integer.toHexString(buff[i] & 0xff).toUpperCase());
            sb.append(' ');
        }
        return sb.toString();
    }

    public static boolean getBit(int val, int pos)
    {
        return getBit(new byte[] {
                (byte)((val >> 0) & 0xff),
                (byte)((val >> 8) & 0xff),
                (byte)((val >> 16) & 0xff),
                (byte)((val >> 24) & 0xff)
        }, pos);
    }

    public static int reverse(int val)
    {
        byte[] bytes = toBytes(val);
        byte[] ret = new byte[4];
        for (int i = 0; i < 4; i++) ret[i] = bytes[3 - i];
        return toInt(ret);
    }

    public static int toInt(byte[] bytes)
    {
        int val = 0;
        for (int i = 0; i < 4; i++) val |= (bytes[i] & 0xff) << ((3 - i) * 8);
        return val;
    }

    public static byte[] toBytes(int val)
    {
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++)
        {
            bytes[i] = (byte)(val >> ((3 - i) * 8) & 0xff);
        }
        return bytes;
    }

    public static byte[] toBytes(long val)
    {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++)
        {
            bytes[i] = (byte)(val >> ((7 - i) * 8) & 0xff);
        }
        return bytes;
    }

    public static int getInt(byte[] data, int offset, int length)
    {
        int val = 0;
        for (int i = 0; i < length; i++) val |= (data[offset + i] & 0xff) << ((length - i - 1) * 8);
        return val;
    }

    public static long getLong(byte[] data, int offset, int length)
    {
        long val = 0;
        for (int i = 0; i < length; i++) val |= ((long)data[offset + i] & 0xff) << ((length - i - 1) * 8);
        return val;
    }

    public static boolean getBit(byte[] data, int pos)
    {
        return ((data[pos / 8] >> (pos % 8)) & 0x01) == 0x01;
    }

    public static byte[] concat(byte[]...byteArrays)
    {
        int len = 0, index = 0;
        for (int i = 0; i < byteArrays.length; i++) len += byteArrays[i].length;
        byte[] buff = new byte[len];
        for (int i = 0; i < byteArrays.length; i++)
        {
            System.arraycopy(byteArrays[i], 0, buff, index, byteArrays[i].length);
            index += byteArrays[i].length;
        }
        return buff;
    }

    public static boolean compare(byte[] data1, byte[] data2)
    {
        if (data1.length != data2.length) return false;
        for (int i = 0; i < data1.length; i++)
            if ((data1[i] & 0xff) != (data2[i] & 0xff)) return false;
        return true;
    }
}
