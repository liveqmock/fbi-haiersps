package org.fbi.sbspreserver;

import org.fbi.xplay.TcpBlockServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by zhanrui on 2014/9/23.
 * SBS ǰ�÷�����
 */
public class SpsServer {
    private static Logger logger = LoggerFactory.getLogger(SpsServer.class);

    public static void main(String[] args) throws IOException {
        int THREADS = ProjectConfigManager.getInstance().getIntProperty("server_threads");
        Executor executor = Executors.newFixedThreadPool(THREADS);

        TcpBlockServer proxy = new TcpBlockServer();

        ConcurrentLinkedQueue<String> taskLogQueue = new ConcurrentLinkedQueue<String>();
        ConcurrentLinkedQueue<String> warningTaskLogQueue = new ConcurrentLinkedQueue<String>();

        Integer port = ProjectConfigManager.getInstance().getIntProperty("server_port");

        proxy.executor(executor)
                .bind(port)
                .handler(new SpsHandler(taskLogQueue, warningTaskLogQueue));

        //���׼��
        new Thread(new TxnMonitor(taskLogQueue, warningTaskLogQueue), "Txn monitor").start();
        logger.info("Sbs pre server started...");

        //PROXY
        proxy.start();
        logger.info("Sbs pre server started...");
    }
}
