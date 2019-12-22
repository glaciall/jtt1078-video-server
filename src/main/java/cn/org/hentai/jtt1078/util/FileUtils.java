package cn.org.hentai.jtt1078.util;

import java.io.*;

/**
 * Created by matrixy on 2019/8/25.
 */
public final class FileUtils
{
    public static void writeFile(File file, byte[] data)
    {
        writeFile(file, data, false);
    }

    public static void writeFile(File file, byte[] data, boolean append)
    {
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(file, append);
            fos.write(data);
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            try { fos.close(); } catch(Exception e) { }
        }
    }

    public static byte[] read(File file)
    {
        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(file);
            return read(fis);
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            try { fis.close(); } catch(Exception e) { }
        }
    }

    public static byte[] read(InputStream fis)
    {
        ByteArrayOutputStream baos = null;
        try
        {
            baos = new ByteArrayOutputStream(1024);

            int len = -1;
            byte[] block = new byte[1024];
            while ((len = fis.read(block)) > -1)
            {
                baos.write(block, 0, len);
            }

            return baos.toByteArray();
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    public static void readInto(File file, OutputStream os)
    {
        FileInputStream fis = null;
        try
        {
            int len = -1;
            byte[] block = new byte[1024];
            fis = new FileInputStream(file);
            while ((len = fis.read(block)) > -1)
            {
                os.write(block, 0, len);
                os.flush();
            }
        }
        catch(Exception ex)
        {
            throw new RuntimeException(ex);
        }
        finally
        {
            try { fis.close(); } catch(Exception e) { }
        }
    }
}
