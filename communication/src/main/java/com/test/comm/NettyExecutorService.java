package com.test.comm;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import com.test.comm.server.FileServer;


@Service
public class NettyExecutorService {
	
	public NettyExecutorService() {
		
		//为了颜色一致，我们用管理Err输出
		System.err.println("---------- Spring自动加载         ---------- ");
		System.err.println("---------- 启动Netty线程池       ---------- ");
		
		FileServer server = new FileServer();
		//线程池
		ExecutorService es = Executors.newCachedThreadPool();
		//启动线程池
		es.execute(server);
		
	}
	
}
