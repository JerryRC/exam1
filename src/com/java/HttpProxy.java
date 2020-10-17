package com.java;

import java.io.IOException;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpProxy {
    ServerSocket serverSocketT;     //TCP Socket
    ExecutorService executorService; // 线程池
    final int POOL_SIZE = 4; // 单个处理器线程池工作线程数目

    /**
     * 构造函数
     *
     * @throws IOException socket创建错误
     */
    public HttpProxy() throws IOException {
        int INPort = 8000;
        serverSocketT = new ServerSocket(INPort, 10);
        // 创建线程池，Runtime的availableProcessors()方法返回当前系统可用处理器的数目，由JVM根据系统的情况来决定线程的数量
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime()
                .availableProcessors() * POOL_SIZE);
        System.out.println("服务器启动。");
    }

    /**
     * 主函数
     *
     * @throws IOException IO异常
     */
    public static void main(String[] args) throws IOException {
        HttpProxy hs = new HttpProxy();
        hs.service();   //启动多线程服务
    }

    /**
     * 通过线程池管理整个服务
     */
    public void service() {
        Socket socket;
        while (true) {
            try {
                socket = serverSocketT.accept();    //等待用户连接
                executorService.execute(new Handler(socket)); // 把执行交给线程池来维护
            } catch (IOException e) {
                System.out.println("have not got a new connection yet");
            }
        }
    }
}

