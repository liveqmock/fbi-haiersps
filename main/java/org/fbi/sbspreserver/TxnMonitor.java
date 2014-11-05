package org.fbi.sbspreserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by zhanrui on 2014/9/17.
 * 交易监控及预警
 */
public class TxnMonitor implements Runnable {
    private ConcurrentLinkedQueue<String> taskLogQueue;
    private ConcurrentLinkedQueue<String> warningLogQueue;

    private FileOutputStream taskLogOut;
    private FileOutputStream warningLogOut;
    private int count = 0;
    private int warningCount = 0;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public TxnMonitor(ConcurrentLinkedQueue<String> taskLogQueue, ConcurrentLinkedQueue<String> warningLogQueue) {
        try {
            String prjName = "/haiersps/";
            String logDir = getPrjRootDit(prjName);
            this.taskLogOut = new FileOutputStream(new File(logDir + "txnstats.txt"), true);
            this.warningLogOut = new FileOutputStream(new File(logDir + "txnstats_warning.txt"), true);
        } catch (Exception e) {
            throw new RuntimeException("Init txnMonitor error", e);
        }
        this.taskLogQueue = taskLogQueue;
        this.warningLogQueue = warningLogQueue;
    }

    private String getPrjRootDit(String prjName) {
        String prj_root_dir = null;
        try {
            prj_root_dir = this.getClass().getResource("/").toURI().getPath();
            int index = prj_root_dir.indexOf(prjName);
            prj_root_dir = prj_root_dir.substring(0, index + prjName.length());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Get prj root path error", e);
        }
        return prj_root_dir;
    }

    @Override
    public void run() {
        while (true) {
            //long start = System.nanoTime();
            boolean isEmpty = true;
            if (!taskLogQueue.isEmpty()) {
                try {
                    isEmpty = false;
                    count++;
                    taskLogOut.write(("" + count + "|").getBytes());
                    taskLogOut.write(taskLogQueue.poll().getBytes());
                    taskLogOut.write("\r\n".getBytes());
                } catch (IOException e) {
                    logger.error("TaskMonitor 队列读取失败.", e);
                }
            }

            if (!warningLogQueue.isEmpty()) {
                try {
                    isEmpty = false;
                    warningCount++;
                    warningLogOut.write(("" + warningCount + "|").getBytes());
                    warningLogOut.write(warningLogQueue.poll().getBytes());
                    warningLogOut.write("\r\n".getBytes());
                } catch (IOException e) {
                    logger.error("TaskMonitor 队列读取失败.", e);
                }

            }

            if (isEmpty) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    logger.error("TaskMonitor 写文件失败.", e);
                }
            }
        }
    }

    public void addTaskLogToQueue(String log) {
        this.taskLogQueue.add(log);
    }

    public void addWarningLogToQueue(String log) {
        this.warningLogQueue.add(log);
    }
}
