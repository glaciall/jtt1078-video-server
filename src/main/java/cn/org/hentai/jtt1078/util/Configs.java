package cn.org.hentai.jtt1078.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by matrixy on 2017/8/14.
 */
public final class Configs
{
    static Properties properties = new Properties();

    public static void init(String configFilePath)
    {
        try
        {
            File file = new File((configFilePath.startsWith("/") ? "." : "") + configFilePath);
            if (file.exists()) properties.load(new FileInputStream(file));
            else properties.load(Configs.class.getResourceAsStream(configFilePath));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static String get(String key)
    {
        Object val = properties.get(key);
        if (null == val) return null;
        else return String.valueOf(val).trim();
    }

    public static int getInt(String key, int defaultVal)
    {
        String val = get(key);
        if (null == val) return defaultVal;
        else return Integer.parseInt(val);
    }
}
