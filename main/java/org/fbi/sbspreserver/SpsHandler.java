package org.fbi.sbspreserver;

import org.fbi.xplay.ChannelContext;
import org.fbi.xplay.ChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by zhanrui on 2014/9/23.
 */
public class SpsHandler implements ChannelHandler {
    private final String sbsHost = ProjectConfigManager.getInstance().getStringProperty("sbs_server_ip");
    private final int sbsPort = ProjectConfigManager.getInstance().getIntProperty("sbs_server_port");
    private int sbsTimeout = ProjectConfigManager.getInstance().getIntProperty("sbs_server_timeout"); //默认超时时间：ms  连接超时与读超时统一
    private final String ibpHost = ProjectConfigManager.getInstance().getStringProperty("ibp_server_ip");
    private final int ibpPort = ProjectConfigManager.getInstance().getIntProperty("ibp_server_port");
    private int ibpTimeout = ProjectConfigManager.getInstance().getIntProperty("ibp_server_timeout"); //默认超时时间：ms  连接超时与读超时统一

    private int warningtime = ProjectConfigManager.getInstance().getIntProperty("remote_server_txn_warning_time"); //长交易时间预警阈值：ms

    HashMap<String, String> ccbRouterConfig = new HashMap<>();
    List<String> ibpTxncodes = new ArrayList<>();

    private ConcurrentLinkedQueue<String> taskLogQueue;
    private ConcurrentLinkedQueue<String> warningTaskLogQueue;

    private Logger logger = LoggerFactory.getLogger(this.getClass());


    public SpsHandler(ConcurrentLinkedQueue<String> taskLogQueue,
                      ConcurrentLinkedQueue<String> warningTaskLogQueue) {
        this.taskLogQueue = taskLogQueue;
        this.warningTaskLogQueue = warningTaskLogQueue;

        initIbpTxncodeList();
    }

    //短链接 处理完成后 由客户端close connection
    @Override
    public void onRead(ChannelContext ctx) throws IOException {
        Socket clntConnection = ctx.connection();
        SocketAddress clientAddress = clntConnection.getRemoteSocketAddress();

        //CTG Server
        InetAddress addr = InetAddress.getByName(sbsHost);
        Socket sbsConnection = new Socket();
        sbsConnection.connect(new InetSocketAddress(addr, sbsPort), sbsTimeout);
        sbsConnection.setSoTimeout(sbsTimeout);

        logger.debug("Client[ " + clientAddress + "]  CTG server[" + sbsHost + ":" + sbsPort + "]");

        try {
            DataInputStream disFromClient = new DataInputStream(clntConnection.getInputStream());
            DataOutputStream dosToClient = new DataOutputStream(clntConnection.getOutputStream());

            //与CTG server建立连接
            DataOutputStream dosToSbs = new DataOutputStream(sbsConnection.getOutputStream());
            DataInputStream disFromSbs = new DataInputStream(sbsConnection.getInputStream());

            for (; ; ) {
                CtgRequest ctgRequest = new CtgRequest();
                ctgRequest.readObject(disFromClient);
                logger.debug(">>>>SBS Request:" + format16(ctgRequest.getEciArea()));

                int flowType = ctgRequest.getFlowType();
                if (flowType == 1) {
                    CtgEciRequest eciRequest = new CtgEciRequest();
                    eciRequest.readObject(new DataInputStream(new ByteArrayInputStream(ctgRequest.getEciArea())));
                    if (eciRequest.Call_Type == 1) {
                        CtgSif txnHeader = new CtgSif(eciRequest.Commarea);
                        logger.debug(">>>>" + txnHeader);
//                        logger.debug(">>>>SBS Request ECI AREA:" + format16(eciRequest.Commarea));

                        String txnCode = txnHeader.getTxnCode();
                        String termId = txnHeader.getTermId();
                        MDC.put("txnCode", txnCode);

                        if (isLocalTxncode(txnCode)) {
                            processIbpTxn(ctgRequest, eciRequest, dosToClient);
                            continue;
                        }
                    }
                }
                //转发
                ctgRequest.writeObject(dosToSbs);

                //收sbs响应
                CtgRequest sbsCtgRequest = new CtgRequest();
                sbsCtgRequest.readObject(disFromSbs);

                //返回客户端
                sbsCtgRequest.writeObject(dosToClient);
                logger.debug("<<<<SBS Response ECI AREA:" + format16(sbsCtgRequest.getEciArea()));
            }
        } catch (SocketException e) {
            logger.info("连接已关闭", e);
        } catch (Exception e) {
            logger.error("交易处理异常", e);
            //TODO
        } finally {
            try {
                //主动关闭server端连接
                sbsConnection.close();
            } catch (Exception e) {
                logger.error("连接关闭失败.", e);
            }
            MDC.remove("txnCode");
        }
    }

    private void processIbpTxn(CtgRequest ctgRequest, CtgEciRequest eciRequest, DataOutputStream osToClient) throws IOException {
        Socket ibpConnection = null;
        try {
            InetAddress addrIbp = InetAddress.getByName(ibpHost);
            ibpConnection = new Socket();
            ibpConnection.connect(new InetSocketAddress(addrIbp, ibpPort), sbsTimeout);
            ibpConnection.setSoTimeout(ibpTimeout);
            DataOutputStream osToIbp = new DataOutputStream(ibpConnection.getOutputStream());
            DataInputStream osFromIbp = new DataInputStream(ibpConnection.getInputStream());


            osToIbp.writeInt(ctgRequest.EYECATCODE); //CTG交易标识
            osToIbp.write(ctgRequest.getEciArea());

            //接收报文  (报文长度（int） + 报文)
            int msgLen = osFromIbp.readInt();
            byte[] msgBuf = new byte[msgLen]; //commarea
            osFromIbp.readFully(msgBuf);

            ctgRequest.setFlowType(3);
            eciRequest.Commarea_Length = msgLen;
            eciRequest.Commarea = msgBuf;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            eciRequest.writeObject(dos);
            byte[] eciArea = baos.toByteArray();
            ctgRequest.setEciArea(eciArea);
            ctgRequest.setDataWhichFollows(eciArea.length);

            //返回客户端
            ctgRequest.writeObject(osToClient);
            logger.debug(">>>>IBP Response ECI AREA:" + format16(eciArea));
        } finally {
            try {
                //主动关闭server端连接
                if (ibpConnection != null) {
                    ibpConnection.close();
                }
            } catch (Exception e) {
                logger.debug("连接关闭失败.", e);
            }
        }
    }

    //======================
    //初始化中间业务平台交易处理列表
    private void initIbpTxncodeList() {
        String ibpTxncode = ProjectConfigManager.getInstance().getStringProperty("ibp_txncode");
        this.ibpTxncodes = Arrays.asList(ibpTxncode.split(","));
    }

    private boolean isLocalTxncode(String txnCode) {
        for (String txn : ibpTxncodes) {
            if (txnCode.equalsIgnoreCase(txn)) {
                return true;
            }
        }
        return false;
    }

    private String format16(byte[] buffer) {
        StringBuilder result = new StringBuilder();
        result.append("\n");
        int n = 0;
        byte[] lineBuffer = new byte[16];
        for (byte b : buffer) {
            if (n % 16 == 0) {
                result.append(String.format("%05x: ", n));
                lineBuffer = new byte[16];
            }
            result.append(String.format("%02x ", b));
            lineBuffer[n % 16] = b;
            n++;
            if (n % 16 == 0) {
                result.append(new String(lineBuffer));
                result.append("\n");
            }


//            if (n >= 2048) {
            if (n >= 128) {
                result.append("报文过大，已截断...");
                break;
            }


        }
        for (int k = 0; k < (16 - n % 16); k++) {
            result.append("   ");
        }
        result.append(new String(lineBuffer));
        result.append("\n");
        return result.toString();
    }
}

