package com.java;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * 代理服务器发送助手
 */
public class ProxyClient {

    //临时buffer
    private static final int buffer_size = 8192;
    private byte[] buffer;

    //用于代理请求
    private Socket socket = null;
    BufferedOutputStream OStream = null;
    BufferedInputStream IStream = null;

    //代理接收到的 头 和 响应
    private final StringBuffer header;
    private final StringBuffer response;

    static private final String CRLF = "\r\n";

    public ProxyClient() {
        buffer = new byte[buffer_size];
        header = new StringBuffer();
        response = new StringBuffer();
    }

    /**
     *
     * @param host 需要连接的主机地址
     * @param port 连接的端口
     * @throws Exception 包括 socket 超时等错误
     */
    public void connect(String host, int port) throws Exception {
        socket = new Socket(host, port);
        OStream = new BufferedOutputStream(socket.getOutputStream());
        IStream = new BufferedInputStream(socket.getInputStream());
    }

    /**
     *
     * @param request 用于发送GET命令
     * @throws Exception socket读写错误
     */
    public void processGetRequest(String request) throws Exception {
        request += CRLF + CRLF;
        buffer = request.getBytes();
        OStream.write(buffer, 0, request.length());
        OStream.flush();
        //清空缓存
        buffer = new byte[buffer_size];
        processResponse();
    }

    /**
     * 用于处理 头 和 响应
     * @throws Exception 读写错误
     */
    public void processResponse() throws Exception {
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

        int size;
        while ((size = IStream.read(buffer)) != -1) {
            response.append(new String(buffer, 0, size, StandardCharsets.ISO_8859_1));
            //清空缓存
            buffer = new byte[buffer_size];
        }
    }

    public String getHeader() {
        return header.toString();
    }

    public String getResponse() {
        return response.toString();
    }

    public void close() throws Exception {
        socket.close();
        IStream.close();
        OStream.close();
    }
}
