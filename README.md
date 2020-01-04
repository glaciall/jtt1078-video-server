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
    <li><a href="#交流讨论">交流讨论</a></li>
</ol>

## jtt1078-video-server
基于JT/T 1078协议实现的视频转播服务器，当车机服务器端主动下发**音视频实时传输控制**消息（0x9101）后，车载终端连接到此服务器后，发送指定摄像头所采集的视频流，此项目服务器完成音视频数据接收并转码，完成转播的流程，提供各平台的播放支撑。

## 分支说明
本项目目前有3个分支，都处于完善当中，各分支的情况区别如下：

|分支|说明|备注|
|---|---|---|
|master|通过ffmpeg子进程实现的纯视频RTMP推流方案|平台不限|
|fifo|通过ffmpeg子进程实现的音视频合并推流RTMP方案（此方案不稳定，已放弃）|需要linux mkfifo支持|
|multimedia|通过ffmpeg完成h264到flv封装，并直接提供HTTP-FLV支持的视频方案，音视频通过chunked分块传输到前端直接播放|平台不限|
|flv|直接使用java完成h264到flv的封装，并直接提供HTTP-FLV支持的视频方案，音频通过chunked分块传输到前端进行播放|平台不限|

### 项目说明
本项目接收来自于车载终端发过来的音视频数据，视频直接做flv的封装，音频完成G.711A、G.711U、ADPCMA到PCM的转码，项目内集成的http服务器直接提供chunked分块传输来提供FLV或WAV数据至前端网页播放。

#### 视频编码支持
目前几乎所有的终端视频，默认的视频编码都是h264，打包成flv也是非常简单的（以后有时间了自己来做封装），有个别厂家使用avs，但是我没有碰到过。本项目目前也只支持h264编码的视频。

#### 音频编码支持
|音频编码|支持|备注|
|---|---|---|
|G.711A|Y|未测试|
|G.711U|Y|未测试|
|ADPCMA|Y|完美支持，包括含海思头的（锐明的几乎都有）|
|G.726|N|尚未实现|

音频编码太多，也没那么多设备可以测试的，比较常见的就G.711A和ADPCMA这两种，G.726还没有发现哪款终端支持的，没条件测试。另外，音频播放是直接使用PCM到WAV封装的，完全没有压缩，通过BASE64编码后到前端，流量消耗很大，通常比视频还大，这里还需要另外找时间设计个压缩算法才行。

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

#### 音画同步问题
目前后端严格的控制了下发数据到前端的时间同步，但是视频的播放要比音频的稍慢（更费时间），所以音画不同步的问题还比较明显，通常是声音相当的及时，而视频画面会稍慢，暂时还没有时间去完善。

### 准备工具
项目里准备了一个测试程序（`src/main/java/cn.org.hentai.jtt1078.test.VideoPushTest.java`），以及一个数据文件（`src/main/resources/tcpdump.bin`），数据文件是通过工具采集的一段几分钟时长的车载终端发送上来的原始消息包，测试程序可以持续不断的、慢慢的发送数据文件里的内容，用来模拟车载终端发送视频流的过程。

### 测试步骤
1. 配置好服务器端，修改`app.properties`里的配置项。
2. 直接在IDE里运行`cn.org.hentai.jtt1078.app.VideoServerApp`，或对项目进行打包，执行`mvn package`，执行`java -jar jtt1078-video-server-1.0-SNAPSHOT.jar`来启动服务器端。
3. 运行`VideoPushTest.java`，开始模拟车载终端的视频推送。
4. 开始后，控制台里会输出显示**start publishing: 013800138000-1**的字样
5. 打开浏览器，输入**http://localhost:3333/test/multimedia#013800138000-1**后回车
6. 点击网页上的**play video**或**play audio**按钮，开始播放视频或音频

### 测试环境
我在我自己的VPS上搭建了一个1078音视频环境，完全使用了**multimedia**分支上的代码来创建，各位可以让终端将音视频发送到此服务器或是使用**netcat**等网络工具发送模拟数据来仿真终端，来体验音视频的效果。下面我们说一下通过**netcat**来模拟终端的方法：

|标题|说明|
|---|---|
|1078音视频服务器|103.213.245.126:10780|
|实时音视频播放页面|http://1078.hentai.org.cn/test/multimedia#SIM-CHANNEL|

1. 首先，本项目的**/src/main/resources/**下的**tcpdump.bin**即为我抓包存下来的终端音视频数据文件，通过`cat tcpdump.bin | pv -L 30k -q | nc 103.213.245.126 10780`即可以每秒30kBPS的速度，向服务器端且持续的发送数据。
2. 在浏览器里打开**http://1078.hentai.org.cn/test/multimedia#SIM-CHANNEL** （注意替换掉后面的SIM和CHANNEL，即终端的SIM卡号，不足12位前面补0，CHANNEL即为通道号），然后点击网页上的**play video**或**play audio**即可。

### 项目文件说明
```
├── doc（一些文档）
├── LICENSE
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   ├── java
│   │   │   └── cn
│   │   │       └── org
│   │   │           └── hentai
│   │   │               └── jtt1078
│   │   │                   ├── app
│   │   │                   │   └── VideoServerApp.java（主入口程序）
│   │   │                   ├── codec
│   │   │                   │   ├── ADPCMCodec.java（ADPCMA编码解码实现）
│   │   │                   │   ├── AudioCodec.java（音频编码抽象类）
│   │   │                   │   ├── G711Codec.java（G.711A编码解码实现）
│   │   │                   │   ├── G711UCodec.java（G.711U编码解码实现）
│   │   │                   │   └── RawDataCopyCodec.java（无意义的音频转码实现）
│   │   │                   ├── entity
│   │   │                   │   └── 几个实体类定义
│   │   │                   ├── http
│   │   │                   │   ├── GeneralResponseWriter.java（直接字节数组数据输出）
│   │   │                   │   └── NettyHttpServerHandler.java（基于netty的HTTP服务处理器）
│   │   │                   ├── publisher
│   │   │                   │   └── PublishManager.java（音视频数据片段发布与订阅）
│   │   │                   ├── server
│   │   │                   │   ├── Jtt1078Decoder.java（1078协议RTP消息包解码器，含粘包处理）
│   │   │                   │   ├── Jtt1078Handler.java（1078协议消息处理器）
│   │   │                   │   ├── Jtt1078MessageDecoder.java（1078协议RTP消息包解码器）
│   │   │                   │   ├── Session.java（1078连接会话数据容器）
│   │   │                   │   └── SessionManager.java（会话管理器，用于分配会话id）
│   │   │                   ├── subscriber
│   │   │                   │   ├── AudioSubscriber.java（音频数据订阅者）
│   │   │                   │   ├── Subscriber.java（音视频数据订阅者基类实现）
│   │   │                   │   └── VideoSubscriber.java（视频数据订阅者）
│   │   │                   ├── test
│   │   │                   │   └── VideoPushTest.java（终端数据发送模拟程序）
│   │   │                   ├── util
│   │   │                   │   └── 工具方法类
│   │   │                   └── video
│   │   │                       ├── FFMpegManager.java（ffmpeg子进程管理器）
│   │   │                       ├── StdoutCleaner.java（ffmpeg子进程的stderr输出缓冲数据清理）
│   │   │                       ├── VideoFeeder.java（ffmpeg子进程的视频数据提供者）
│   │   │                       └── VideoPublisher.java（ffmpeg子进程的输出处理与FLV发布）
│   │   └── resources
│   │       ├── app.properties（配置文件）
│   │       ├── audio.html（音频播放测试页面）
│   │       ├── log4j.properties（log4j配置）
│   │       ├── tcpdump.bin（锐明终端模拟数据：视频H264、音频含海思头的ADPCMA）
│   │       └── multimedia.html（音视频测试页面）
└─────────────────────────────────────────────────────────────────────────────────────────────────
```

### 项目打包说明
通过**mvn package**直接打包成jar包，通过`java -jar jtt1078-video-server-1.0-SNAPSHOT.jar`即可运行，最好把**app.properties**和**multimedia.html**一并放在同一个目录下，因为项目会优先读取文件系统中的配置文件信息。而如果没有本地测试的需求，**multimedia.html**可以不要。

### TODO
因为个人工作比较忙，还有如下遗留问题，有兴趣的朋友欢迎一起参与进来完善。

- [x] h264到flv直接封装，取消对ffmpeg的依赖
- [ ] G.726编码到PCM转码的支持
- [ ] 音画同步问题
- [ ] flv封装音频

### 致谢
本项目一开始只是个简单的示例项目，在开源、建立QQ交流群后，得到了大批的同道中人的帮助和支持，在此表示谢意。本项目尚未完全完善，非常高兴能够有更多的朋友一起加入进来，一起提出更加闪亮的想法，建设更加强大的视频监控平台！

### 致谢名单
非常感谢以下网友的帮助和支持，以及其他默默支持的朋友们！
* 不岸不名
* 故事~
* 小黄瓜要吃饭
* yedajiang44.com
* 幸福一定强
* minigps-基站定位服务
* 慢慢
* power LXC

### 交流讨论
QQ群：808432702，加入我们，群里有热心的同道中人、相关资料、测试数据、代码以及各种方案的先行者等着你。

