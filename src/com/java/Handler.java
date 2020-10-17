package com.java;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.StringTokenizer;

import static java.lang.Math.max;

/**
 * 辅助 Handler 每个线程的实际策略
 *
 * @author JR Chan
 */
public class Handler implements Runnable {
    private final Socket socket;            //线程池传过来的参数Socket

    BufferedOutputStream OStream = null;
    BufferedInputStream IStream = null;

    private static final int buffer_size = 8192;
    private byte[] buffer;

    //http 代理请求
    private final StringBuffer header;

    static private final String CRLF = "\r\n";

    /**
     * 构造函数 创建 Handler
     *
     * @param socket 线程池传过来的Tcp socket
     * @throws SocketException socket错误
     */
    public Handler(Socket socket) throws IOException {
        this.socket = socket;
        System.out.println("<" + socket.getInetAddress() + "："
                + socket.getPort() + "> 连接成功");

        buffer = new byte[buffer_size];
        header = new StringBuffer();

        initStream();   //初始化流
    }

    /**
     * 初始化输入输出流对象方法
     *
     * @throws IOException 流错误
     */
    public void initStream() throws IOException {
        //输出流，向客户端写信息
        OStream = new BufferedOutputStream(socket.getOutputStream());
        //输入流，读取客户端信息
        IStream = new BufferedInputStream(socket.getInputStream());
    }

    /**
     * 每个线程的实际执行策略
     */
    @Override
    public void run() {
        try {
            processRequests();

            System.out.println(header);
            StringTokenizer st = new StringTokenizer(header.toString(), " ");
            String command = st.nextToken();    //第一个分段为指令名

            boolean Bad = true;
            //GET请求
            if (command.equals("GET")) {
                String hostname = st.nextToken();   //第二个分段就是地址
                String protocol = st.nextToken().trim();  //第三个分段是协议

                //只响应 HTTP/1.0
                //第三个分段若不是 HTTP/1.0 则 bad request
                if (protocol.contains("HTTP/1.0\n") || protocol.equals("HTTP/1.0")) {
                    Bad = false;
                    forwarding(hostname);
                }
            }
            //Bad
            if (Bad) {
                //http 响应头
                String response = "HTTP/1.0 400 Bad Request" + CRLF
                        + "Proxy: MyHttpProxy/1.0" + CRLF
                        + CRLF;
                sending(response);
            }
            //关闭流，客户端才会-1退出
            OStream.close();
        }
        //避免异常后直接结束服务器端
        catch (Exception e) {
//            e.printStackTrace();
            System.out.println("connection break");
        }
    }

    /**
     * 处理转发
     * 只接受 http 请求，如果接收到 https 则转换成 http 尝试
     *
     * @param hostname 转发目标位置
     * @throws IOException Socket 读写错误
     */
    private void forwarding(String hostname) throws IOException {
        //处理字符串
        if(hostname.contains("https")){
            hostname = hostname.replace("https","http");
        }
        int startSlash = hostname.indexOf("http://");
        if(startSlash != -1){
            startSlash += 7;
        } else {
            startSlash = 0;
        }
        int start = hostname.indexOf(":", startSlash);
        //如果端口号后面没有路径了，就不会出现 / ，需要直接取到尾巴
        int end = max(hostname.indexOf("/", start), hostname.length());

        try {
            //转发参数
            String host = start == -1 ?
                    hostname.substring(startSlash, end) : hostname.substring(startSlash, start);
            int OUTPort = start == -1 ?
                    80 : Integer.parseInt(hostname.substring(start + 1, end));
            String filename = end == hostname.length() ? "/" : hostname.substring(end);

            System.out.println(host + " " + OUTPort + " " + filename);

            //代理转发
            ProxyClient myClient = new ProxyClient();
            myClient.connect(host, OUTPort);
            String request = "GET " + filename + " HTTP/1.0";
            myClient.processGetRequest(request);

            //转发得到的 response 头
            sending(myClient.getHeader() + CRLF);
            //转发得到的 response 体
            sending(myClient.getResponse());

            myClient.close();
        }
        catch (Exception e){
            e.printStackTrace();
            String response = "HTTP/1.0 400 Bad Request" + CRLF
                    + "Proxy: MyHttpProxy/1.0" + CRLF
                    + CRLF;
            sending(response);
        }
    }

    /**
     * 用于转发指定内容
     *
     * @param content 要发送的内容
     * @throws IOException Socket 读写错误
     */
    private void sending(String content) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes());
        //向上取整
        int times = (content.length() + buffer_size - 1) / buffer_size;
        int size;
        for (int i = 0; i < times && (size = bis.read(buffer)) != -1; ++i) {
            OStream.write(buffer, 0, size);
            OStream.flush();
            //刷新 buffer
            buffer = new byte[buffer_size];
        }
    }

    /**
     * 用于处理 http 报文
     * 为 header 和 body 赋值
     *
     * @throws IOException 各种错误
     */
    private void processRequests() throws IOException {
        int last = 0, c;
        boolean inHeader = true; // loop control
        while (inHeader && ((c = IStream.read()) != -1)) {
            switch (c) {
                case '\r':
                    break;
                case '\n':
                    if (c == last) {
                        inHeader = false;
                        break;
                    }
                    last = c;
                    header.append("\n");
                    break;
                default:
                    last = c;
                    header.append((char) c);
            }
        }
    }

}

