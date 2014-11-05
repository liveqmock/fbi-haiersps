package org.fbi.sbspreserver;

import java.io.*;
import java.util.Arrays;
import java.util.Locale;

/**
 * Created by zhanrui on 2014/10/25.
 */
public class CtgRequest {
    public static final String BASE = "BASE";
    public static final int EYECATCODE = 0x47617465;
    public static final int FLOW_REQUEST = 1;
    public static final int FLOW_CONFIRM = 2;
    public static final int FLOW_REPLY = 3;
    public static final int FLOW_ERROR = 4;
    public static final int FLOW_HANDSHAKE = 5;
    public static final int FLOW_EXCEPTION = 6;
    public static final int FLOW_PING = 7;
    public static final int FLOW_PONG = 8;
    public static final int FLOW_CONFIRM_CALLBACK = 9;
    public static final int FLOW_CLOSE = 10;
    public static final int FLOW_REQUEST_AUTHORIZED = 11;
    public static final int NULL_CLEANUP = 0;
    public static final int ADD_CLEANUP = 1;
    public static final int UPDATE_CLEANUP = 2;

    private int flowVersion;
    private String requestType;
    private int flowType;
    private int messageId;
    private int gatewayRc;
    private boolean hasSecurity;
    private int dataWhichFollows;
    private byte[] eciArea = null;

    static transient boolean bSpecifyASCII;
    private Callbackable calBack;

    public Object requestToken;
    public Locale locExchange;
    public String serverJVM;
    public String serverSecurityClass;
    public byte handshakeBuf[];
    public boolean isCloseHint;
    public String serverSideException;

    public CtgRequest() {
        flowVersion = 0x500010;
        requestType = null;
        flowType = 1;
        messageId = 0;
        gatewayRc = 0;
        hasSecurity = false;
        dataWhichFollows = 0;
        calBack = null;
        requestToken = null;
        locExchange = null;
        serverJVM = null;
        serverSecurityClass = null;
        handshakeBuf = null;
        isCloseHint = false;
        serverSideException = null;
        requestType = "BASE";
    }

    protected CtgRequest(String reqType) {
        flowVersion = 0x500010;
        requestType = null;
        flowType = 1;
        messageId = 0;
        gatewayRc = 0;
        hasSecurity = false;
        dataWhichFollows = 0;
        calBack = null;
        requestToken = null;
        locExchange = null;
        serverJVM = null;
        serverSecurityClass = null;
        handshakeBuf = null;
        isCloseHint = false;
        serverSideException = null;
        requestType = reqType;
    }

    public void setRoot(CtgRequest ctgRequest) {
        flowVersion = ctgRequest.flowVersion;
        flowType = ctgRequest.flowType;
        messageId = ctgRequest.messageId;
        gatewayRc = ctgRequest.gatewayRc;
        requestType = ctgRequest.requestType;
        hasSecurity = ctgRequest.hasSecurity;
        dataWhichFollows = ctgRequest.dataWhichFollows;
        requestToken = ctgRequest.requestToken;
        handshakeBuf = ctgRequest.handshakeBuf;
        locExchange = ctgRequest.locExchange;
        isCloseHint = ctgRequest.isCloseHint;
        serverSideException = ctgRequest.serverSideException;
        serverJVM = ctgRequest.serverJVM;
    }

    public void readObject(DataInputStream in) throws IOException {
        if (in.readInt() != EYECATCODE) throw new IOException("Eyecat code error.");
        flowVersion = in.readInt();
        flowType = in.readInt();
        messageId = in.readInt();
        gatewayRc = in.readInt();
        if (flowVersion == 0x10100) {
            requestType = readPaddedString(in, 4);
        } else {
            int j = in.readInt();
            requestType = readPaddedString(in, j);
        }
        if (flowVersion >= 0x200000)
            hasSecurity = in.readBoolean();
        dataWhichFollows = in.readInt();

        eciArea = new byte[dataWhichFollows];
        in.readFully(eciArea);
    }


    protected String readPaddedString(DataInputStream in, int i) throws IOException {
        byte abyte0[] = new byte[i];
        in.readFully(abyte0, 0, i);
        return new String(abyte0, "ASCII");
    }


    public void writeObject(DataOutputStream out) throws IOException {
        out.writeInt(EYECATCODE);
        out.writeInt(flowVersion);
        out.writeInt(flowType);
        out.writeInt(messageId);
        out.writeInt(gatewayRc);
        if (flowVersion == 0x10100) {
            out.writeBytes(toPaddedString(requestType, 4));
        } else {
            out.writeInt(requestType.length());
            out.writeBytes(toPaddedString(requestType, requestType.length()));
        }
        if (flowVersion >= 0x200000)
            out.writeBoolean(hasSecurity);
        out.writeInt(dataWhichFollows);

        out.write(eciArea, 0, dataWhichFollows);
    }


    protected String toPaddedString(String s, int i) {
        StringBuilder sb = new StringBuilder(i);
        if (s != null)
            sb.append(s);
        sb.setLength(i);
        return sb.toString();
    }


    //==============
    protected int getPasswordOffset() {
        return -1;
    }
    //===============

    public static int getFlowRequest() {
        return FLOW_REQUEST;
    }

    public static int getFlowConfirm() {
        return FLOW_CONFIRM;
    }

    public static int getFlowReply() {
        return FLOW_REPLY;
    }

    public static int getFlowError() {
        return FLOW_ERROR;
    }

    public static int getFlowHandshake() {
        return FLOW_HANDSHAKE;
    }

    public static int getFlowException() {
        return FLOW_EXCEPTION;
    }

    public static int getFlowPing() {
        return FLOW_PING;
    }

    public static int getFlowPong() {
        return FLOW_PONG;
    }

    public static int getFlowConfirmCallback() {
        return FLOW_CONFIRM_CALLBACK;
    }

    public static int getFlowClose() {
        return FLOW_CLOSE;
    }

    public static int getFlowRequestAuthorized() {
        return FLOW_REQUEST_AUTHORIZED;
    }

    public static int getNullCleanup() {
        return NULL_CLEANUP;
    }

    public static int getAddCleanup() {
        return ADD_CLEANUP;
    }

    public static int getUpdateCleanup() {
        return UPDATE_CLEANUP;
    }

    public int getFlowVersion() {
        return flowVersion;
    }

    public void setFlowVersion(int flowVersion) {
        this.flowVersion = flowVersion;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public int getFlowType() {
        return flowType;
    }

    public void setFlowType(int flowType) {
        this.flowType = flowType;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getGatewayRc() {
        return gatewayRc;
    }

    public void setGatewayRc(int gatewayRc) {
        this.gatewayRc = gatewayRc;
    }

    public boolean isHasSecurity() {
        return hasSecurity;
    }

    public void setHasSecurity(boolean hasSecurity) {
        this.hasSecurity = hasSecurity;
    }

    public int getDataWhichFollows() {
        return dataWhichFollows;
    }

    public void setDataWhichFollows(int dataWhichFollows) {
        this.dataWhichFollows = dataWhichFollows;
    }

    public static boolean isbSpecifyASCII() {
        return bSpecifyASCII;
    }

    public static void setbSpecifyASCII(boolean bSpecifyASCII) {
        CtgRequest.bSpecifyASCII = bSpecifyASCII;
    }

    public Callbackable getCalBack() {
        return calBack;
    }

    public void setCalBack(Callbackable calBack) {
        this.calBack = calBack;
    }

    public byte[] getEciArea() {
        return eciArea;
    }

    public void setEciArea(byte[] eciArea) {
        this.eciArea = eciArea;
    }

    @Override
    public String toString() {
        return "CtgRequest{" +
                "flowVersion=" + flowVersion +
                ", requestType='" + requestType + '\'' +
                ", flowType=" + flowType +
                ", messageId=" + messageId +
                ", gatewayRc=" + gatewayRc +
                ", hasSecurity=" + hasSecurity +
                ", dataWhichFollows=" + dataWhichFollows +
                ", calBack=" + calBack +
                ", requestToken=" + requestToken +
                ", locExchange=" + locExchange +
                ", serverJVM='" + serverJVM + '\'' +
                ", serverSecurityClass='" + serverSecurityClass + '\'' +
                ", handshakeBuf=" + Arrays.toString(handshakeBuf) +
                ", isCloseHint=" + isCloseHint +
                ", serverSideException='" + serverSideException + '\'' +
                '}';
    }
}
