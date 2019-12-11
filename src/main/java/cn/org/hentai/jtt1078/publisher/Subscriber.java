package cn.org.hentai.jtt1078.publisher;

/**
 * Created by houcheng on 2019-12-11.
 */
public class Subscriber
{
    // 1. 我需要什么？
    private String tag;
    // 2. 我读到哪了？
    private Long currentIndex;

    public Subscriber(String sim, int channel)
    {
        this.tag = sim + "-" + channel;
        this.currentIndex = -1L;
    }

    public String getTag()
    {
        return tag;
    }

    public void setTag(String tag)
    {
        this.tag = tag;
    }

    public Long getCurrentIndex()
    {
        return currentIndex;
    }

    public void setCurrentIndex(Long currentIndex)
    {
        this.currentIndex = currentIndex;
    }
}
