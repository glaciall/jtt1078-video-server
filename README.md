## 目录
<ol>
	<li><a href="#jtt1078-video-server">简介说明</a></li>
    <li><a href="#分支说明">分支说明</a></li>
    <li><a href="#项目说明">项目说明</a></li>
    <li><a href="#准备工具">准备工具</a></li>
    <li><a href="#测试步骤">测试步骤</a></li>
    <li><a href="#测试环境">测试环境</a></li>
    <li><a href="#TODO">TODO</a></li>
    <li><a href="#致谢">致谢</a></li>
    <li><a href="#推荐群友项目">推荐群友项目</a></li>
    <li><a href="#交流讨论">交流讨论</a></li>
</ol>

<div align="center"><img src="./doc/1078.png" /></div>

<hr />

## jtt1078-video-server
基于JT/T 1078协议实现的视频转播服务器，当车机服务器端主动下发**音视频实时传输控制**消息（0x9101）后，车载终端连接到此服务器后，发送指定摄像头所采集的视频流，此项目服务器完成音视频数据接收并转码，完成转播的流程，提供各平台的播放支撑。

> 非常感谢 **孤峰赏月/hx（[github/jelycom](https://github.com/jelycom)）** 提供的mp3音频支持。

## 分支说明
本项目目前有5个分支，flv分支最完善，各分支的情况区别如下：

|分支|说明|跨平台？|优势|劣势|
|---|---|---|---|---|
|master|通过ffmpeg子进程实现的纯视频RTMP推流方案|平台不限|简单，首屏时间最长|不支持音频，低并发|
|fifo|通过ffmpeg子进程实现的音视频合并推流RTMP方案|需要linux mkfifo支持|不稳定，需要掌握ffmpeg|可以支持各种视频编码，低并发|
|multimedia|通过ffmpeg完成h264到flv封装，并直接提供HTTP-FLV支持的视频方案，音视频通过chunked分块传输到前端直接播放|平台不限|-|需要ffmpeg支持，低并发|
|flv|直接使用java完成音视频到flv的封装，并直接提供HTTP-FLV支持的视频方案|平台不限|首屏时间最短|不支持其它形式的输出|

使用了ffmpeg子进程模式的，都可以想办法同时输出多个目标，比如到RTMP，RTMP还可以转为HLS等。

> 有其它语言的开发者，可以参考我的“[JTT/1078音视频传输协议开发指南](https://www.hentai.org.cn/article?id=8)”，我所知道的官方文档里的错误或是缺陷以及坑，我全部写了下来，希望对你有帮助。

### 项目说明
本项目接收来自于车载终端发过来的音视频数据，视频直接做flv的封装，音频完成G.711A、G.711U、ADPCMA到PCM的转码，项目内集成的http服务器直接提供chunked分块传输来提供FLV至前端网页播放。

#### 视频编码支持
目前几乎所有的终端视频，默认的视频编码都是h264，打包成flv也是非常简单的，有个别厂家使用avs，但是我没有碰到过。本项目目前也只支持h264编码的视频。

#### 音频编码支持
|音频编码|支持|备注|
|---|---|---|
|G.711A|Y|支持|
|G.711U|Y|支持|
|ADPCMA|Y|支持|
|G.726|N|尚未实现|

音频编码太多，也没那么多设备可以测试的，比较常见的就G.711A和ADPCMA这两种，G.726音频的编解码代码的翻译工作不大顺利，暂时不支持，本程序对于不支持的音频，将作静音处理。

#### 音频编码转码扩展实现
继承并实现`AudioCodec`类的抽象方法，完成任意音频到PCM编码的转码过程，并且补充`AudioCodec.getCodec()`工厂方法即可。`AudioCodec`抽象类原型如下：
```java
public abstract class AudioCodec
{
	// 转换至PCM
    public abstract byte[] toPCM(byte[] data);
    // 由PCM转为当前编码，可以留空，反正又没有调用
    public abstract byte[] fromPCM(byte[] data);
}
```

### 准备工具
项目里准备了一个测试程序（`src/main/java/cn.org.hentai.jtt1078.test.VideoPushTest.java`），以及一个数据文件（`src/main/resources/tcpdump.bin`），数据文件是通过工具采集的一段几分钟时长的车载终端发送上来的原始消息包，测试程序可以持续不断的、慢慢的发送数据文件里的内容，用来模拟车载终端发送视频流的过程。

### 测试步骤
1. 配置好服务器端，修改`app.properties`里的配置项。
2. 直接在IDE里运行`cn.org.hentai.jtt1078.app.VideoServerApp`，或对项目进行打包，执行`mvn package`，执行`java -jar jtt1078-video-server-1.0-SNAPSHOT.jar`来启动服务器端。
3. 运行`VideoPushTest.java`，开始模拟车载终端的视频推送。
4. 开始后，控制台里会输出显示**start publishing: 013800138000-1**的字样
5. 打开浏览器，输入 **http://localhost:3333/test/multimedia#013800138000-1** 后回车
6. 点击网页上的**play video**，开始播放视频

### 测试环境
我在我自己的VPS上搭建了一个1078音视频环境，完全使用了**flv**分支上的代码来创建，各位可以让终端将音视频发送到此服务器或是使用**netcat**等网络工具发送模拟数据来仿真终端，来体验音视频的效果。下面我们说一下通过**netcat**来模拟终端的方法：

|标题|说明|
|---|---|
|1078音视频服务器|103.213.245.126:10780|
|实时音视频播放页面|http://1078.hentai.org.cn/test/multimedia#SIM-CHANNEL|

1. 首先，本项目的 **/src/main/resources/** 下的 **tcpdump.bin** 即为我抓包存下来的终端音视频数据文件，通过`cat tcpdump.bin | pv -L 40k -q | nc 103.213.245.126 10780`即可以每秒40kBPS的速度，向服务器端持续的发送数据。
2. 在浏览器里打开**http://1078.hentai.org.cn/test/multimedia#SIM-CHANNEL** （注意替换掉后面的SIM和CHANNEL，即终端的SIM卡号，不足12位前面补0，CHANNEL即为通道号），然后点击网页上的**play video**即可。

### 项目文件说明
```

├── doc
├── LICENSE
├── pom.xml
├── README.md
├── src
│   └── main
│       ├── java
│       │   └── cn
│       │       └── org
│       │           └── hentai
│       │               └── jtt1078
│       │                   ├── app
│       │                   │   └── VideoServerApp.java（主入口程序）
│       │                   ├── codec
│       │                   │   ├── ADPCMCodec.java（ADPCM编解码器）
│       │                   │   ├── AudioCodec.java（音频编解码器抽象类）
│       │                   │   ├── G711Codec.java（G711A编解码器）
│       │                   │   ├── G711UCodec.java（G711U编解码器）
│       │                   │   └── RawDataCopyCodec.java
│       │                   ├── entity
│       │                   │   ├── Audio.java
│       │                   │   ├── MediaEncoding.java
│       │                   │   ├── Media.java
│       │                   │   └── Video.java
│       │                   ├── flv
│       │                   │   └── FlvEncoder.java（H264到FLV封装编码器）
│       │                   ├── http（内置HTTP服务，提供HTTP-CHUNKED传输支持）
│       │                   │   ├── GeneralResponseWriter.java
│       │                   │   └── NettyHttpServerHandler.java
│       │                   ├── publisher
│       │                   │   ├── Channel.java（一个通道一个Channel实例，Subscriber订阅Channel上的音频或视频）
│       │                   │   └── PublishManager.java（管理Channel和Subscriber）
│       │                   ├── server（负责完成1078 RTP消息包的接收和解码）
│       │                   │   ├── Jtt1078Decoder.java
│       │                   │   ├── Jtt1078Handler.java
│       │                   │   ├── Jtt1078MessageDecoder.java
│       │                   │   └── Session.java
│       │                   ├── subscriber
│       │                   │   ├── AudioSubscriber.java（音频数据封装与订阅）
│       │                   │   ├── Subscriber.java
│       │                   │   └── VideoSubscriber.java（视频数据封装与订阅）
│       │                   ├── test（一些测试代码）
│       │                   │   ├── AudioTest.java
│       │                   │   ├── ChannelTest.java
│       │                   │   ├── G711ATest.java
│       │                   │   ├── RTPTest.java
│       │                   │   ├── VideoPushTest.java（使用数据文件模拟终端发送音视频数据）
│       │                   │   ├── VideoServer.java
│       │                   │   └── WAVTest.java
│       │                   └── util
│       │                       ├── ByteHolder.java
│       │                       ├── ByteUtils.java
│       │                       ├── Configs.java
│       │                       ├── FileUtils.java
│       │                       ├── FLVUtils.java
│       │                       ├── HttpChunk.java
│       │                       ├── Packet.java
│       │                       └── WAVUtils.java
│       └── resources
│           ├── app.properties（主配置文件）
│           ├── audio.html
│           ├── log4j.properties
│           ├── multimedia.html（测试用音视频播放页面）
│           ├── tcpdump.bin（测试用数据文件，音频ADPCM含海思头，视频H264）
│           ├── test.html
│           └── video.html
└─────────────────────────────────────────────────────────────────────────────────────────────────
```

### 项目打包说明
通过**mvn package**直接打包成jar包，通过`java -jar jtt1078-video-server-1.0-SNAPSHOT.jar`即可运行，最好把**app.properties**和**multimedia.html**一并放在同一个目录下，因为项目会优先读取文件系统中的配置文件信息。而如果没有本地测试的需求，**multimedia.html**可以不要。

### 注意事项
1. 本项目为JT 1078协议的流媒体服务器部分的实现，不包括1078协议的控制消息交互部分，就是在0x9101指令下发后，终端连接到的音视频服务器的实现。
2. 在一般的浏览器里，比如Chrome下，浏览器限制了对于同一个域名的连接最多只能够有6个并发，所以如果要同时播放多路视频，需要准备多个域名或是端口，通过轮循分配的方式，把视频的传输连接，分配到不同的URL上去。

### TODO
因为个人工作比较忙，还有如下遗留问题，有兴趣的朋友欢迎一起参与进来完善。

- [x] h264到flv直接封装，取消对ffmpeg的依赖
- [ ] G.726编码到PCM转码的支持
- [x] 音画同步问题
- [x] flv封装音频

### 致谢
本项目一开始只是个简单的示例项目，在开源、建立QQ交流群后，得到了大批的同道中人的帮助和支持，在此表示谢意。本项目尚未完全完善，非常高兴能够有更多的朋友一起加入进来，一起提出更加闪亮的想法，建设更加强大的视频监控平台！

### 致谢名单
非常感谢以下网友的帮助和支持，以及其他默默支持的朋友们！
* 不岸不名
* 故事~
* 小黄瓜要吃饭
* yedajiang44.com（[github.com/yedajiang44](https://github.com/yedajiang44)）
* 幸福一定强
* minigps-基站定位服务
* 慢慢
* power LXC
* 奎杜
* 孤峰赏月/hx（[github/jelycom](https://github.com/jelycom)）
* 洛奇（[cuiyaonan](https://gitee.com/cuiyaonan2000)）

### 推荐群友项目
|项目|URL|作者|说明|
|---|---|---|---|
|JT1078|https://github.com/yedajiang44/JT1078|SmallChi/[yedajiang44](https://github.com/yedajiang44)|C#，支持音视频，通过websocket传输flv到前端|

### 交流讨论
QQ群：808432702，加入我们，群里有热心的同道中人、相关资料、测试数据、代码以及各种方案的先行者等着你。

