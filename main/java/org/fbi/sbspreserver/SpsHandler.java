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
    private int sbsTimeout = ProjectConfigManager.getInstance().getIntProperty("sbs_server_timeout"); //Ĭ�ϳ�ʱʱ�䣺ms  ���ӳ�ʱ�����ʱͳһ
    private final String ibpHost = ProjectConfigManager.getInstance().getStringProperty("ibp_server_ip");
    private final int ibpPort = ProjectConfigManager.getInstance().getIntProperty("ibp_server_port");
    private int ibpTimeout = ProjectConfigManager.getInstance().getIntProperty("ibp_server_timeout"); //Ĭ�ϳ�ʱʱ�䣺ms  ���ӳ�ʱ�����ʱͳһ

    private int warningtime = ProjectConfigManager.getInstance().getIntProperty("remote_server_txn_warning_time"); //������ʱ��Ԥ����ֵ��ms

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

    //������ ������ɺ� �ɿͻ���close connection
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

            //��CTG server��������
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
                //ת��
                ctgRequest.writeObject(dosToSbs);

                //��sbs��Ӧ
                CtgRequest sbsCtgRequest = new CtgRequest();
                sbsCtgRequest.readObject(disFromSbs);

                //���ؿͻ���
                sbsCtgRequest.writeObject(dosToClient);
                logger.debug("<<<<SBS Response ECI AREA:" + format16(sbsCtgRequest.getEciArea()));
            }
        } catch (SocketException e) {
            logger.info("�����ѹر�", e);
        } catch (Exception e) {
            logger.error("���״����쳣", e);
            //TODO
        } finally {
            try {
                //�����ر�server������
                sbsConnection.close();
            } catch (Exception e) {
                logger.error("���ӹر�ʧ��.", e);
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


            osToIbp.writeInt(ctgRequest.EYECATCODE); //CTG���ױ�ʶ
            osToIbp.write(ctgRequest.getEciArea());

            //���ձ���  (���ĳ��ȣ�int�� + ����)
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

            //���ؿͻ���
            ctgRequest.writeObject(osToClient);
            logger.debug(">>>>IBP Response ECI AREA:" + format16(eciArea));
        } finally {
            try {
                //�����ر�server������
                if (ibpConnection != null) {
                    ibpConnection.close();
                }
            } catch (Exception e) {
                logger.debug("���ӹر�ʧ��.", e);
            }
        }
    }

    //======================
    //��ʼ���м�ҵ��ƽ̨���״����б�
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
                result.append("���Ĺ����ѽض�...");
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

