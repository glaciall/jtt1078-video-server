package cn.org.hentai.jtt1078.app;

import cn.org.hentai.jtt1078.server.Jtt1078Handler;
import cn.org.hentai.jtt1078.server.Jtt1078MessageDecoder;
import cn.org.hentai.jtt1078.util.Configs;
import cn.org.hentai.jtt1078.video.PublisherManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Created by matrixy on 2019/4/9.
 */
public class VideoServerApp
{
    private static Logger logger = LoggerFactory.getLogger(VideoServerApp.class);

    private static ServerBootstrap serverBootstrap;

    private static EventLoopGroup bossGroup;
    private static EventLoopGroup workerGroup;

    public static void main(String[] args) throws Exception
    {
        Configs.init("/app.properties");
        PublisherManager.getInstance().init();

        Signal.handle(new Signal("TERM"), new SignalHandler()
        {
            @Override
            public void handle(Signal signal)
            {
                shutdown();
            }
        });

        startServer();
    }

    private static void startServer() throws Exception
    {
        serverBootstrap = new ServerBootstrap();
        serverBootstrap.option(ChannelOption.SO_BACKLOG, Configs.getInt("server.backlog", 102400));
        bossGroup = new NioEventLoopGroup(Configs.getInt("server.worker-count", Runtime.getRuntime().availableProcessors()));
        workerGroup = new NioEventLoopGroup();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(final SocketChannel channel) throws Exception {
                        ChannelPipeline p = channel.pipeline();
                        p.addLast(new Jtt1078MessageDecoder());
                        // p.addLast(new Jtt808MessageEncoder());
                        // p.addLast(new JTT808Handler());
                        p.addLast(new Jtt1078Handler());
                    }
                });

        int port = Configs.getInt("server.port", 1078);
        Channel ch = serverBootstrap.bind(port).sync().channel();
        logger.info("Server started at: {}", port);
        ch.closeFuture().sync();
    }

    private static void shutdown()
    {
        try
        {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

            PublisherManager.getInstance().shutdown();

            System.out.println("program exited...");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
