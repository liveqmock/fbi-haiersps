package org.fbi.sbspreserver;

/**
 * Created by zhanrui on 2014/10/28.
 */
public class CtgSif {
    private String txnFlagStr; //TPEI
    private String txnCode;
    private String branchId;
    private String termId;
    private String tellerId;
    private String authTlr;
    private String authTlrPwd;
    private String reserve;
    private String lineNo;
    private String status;

    private byte[] sifData;

    public CtgSif(byte[] buf){
        int offset = 0;
        int len = 4;
        txnFlagStr = new String(buf, offset, len).trim();
        offset += len;
        len = 6;
        txnCode = new String(buf, offset, len).trim();
        offset += len;
        len = 10;
        branchId = new String(buf, offset, len).trim();
        offset += len;
        len = 4;
        termId = new String(buf, offset, len).trim();
        offset += len;
        len = 4;
        tellerId = new String(buf, offset, len).trim();
        offset += len;
        len = 4;
        authTlrPwd = new String(buf, offset, len).trim();
        offset += len;
        len = 8;
        authTlrPwd = new String(buf, offset, len).trim();
        offset += len;
        len = 8;
        reserve = new String(buf, offset, len).trim();
        offset += len;
        len = 1;
        lineNo = new String(buf, offset, len).trim();
        offset += len;
        len = 2;
        status = new String(buf, offset, len).trim();

        offset = 51; //固定从51开始
        sifData = new byte[buf.length - offset];
        System.arraycopy(buf, offset, sifData, 0, sifData.length);
    }
    //===========
    public String getTxnFlagStr() {
        return txnFlagStr;
    }

    public void setTxnFlagStr(String txnFlagStr) {
        this.txnFlagStr = txnFlagStr;
    }

    public String getTxnCode() {
        return txnCode;
    }

    public void setTxnCode(String txnCode) {
        this.txnCode = txnCode;
    }

    public String getBranchId() {
        return branchId;
    }

    public void setBranchId(String branchId) {
        this.branchId = branchId;
    }

    public String getTermId() {
        return termId;
    }

    public void setTermId(String termId) {
        this.termId = termId;
    }

    public String getTellerId() {
        return tellerId;
    }

    public void setTellerId(String tellerId) {
        this.tellerId = tellerId;
    }

    public String getAuthTlr() {
        return authTlr;
    }

    public void setAuthTlr(String authTlr) {
        this.authTlr = authTlr;
    }

    public String getAuthTlrPwd() {
        return authTlrPwd;
    }

    public void setAuthTlrPwd(String authTlrPwd) {
        this.authTlrPwd = authTlrPwd;
    }

    public String getReserve() {
        return reserve;
    }

    public void setReserve(String reserve) {
        this.reserve = reserve;
    }

    public byte[] getSifData() {
        return sifData;
    }

    public void setSifData(byte[] sifData) {
        this.sifData = sifData;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLineNo() {
        return lineNo;
    }

    public void setLineNo(String lineNo) {
        this.lineNo = lineNo;
    }

    @Override
    public String toString() {
        return "CtgSifHeader{" +
                "txnFlagStr='" + txnFlagStr + '\'' +
                ", txnCode='" + txnCode + '\'' +
                ", branchId='" + branchId + '\'' +
                ", termId='" + termId + '\'' +
                ", tellerId='" + tellerId + '\'' +
                ", authTlr='" + authTlr + '\'' +
                ", authTlrPwd='" + authTlrPwd + '\'' +
                ", reserve='" + reserve + '\'' +
                ", lineNo='" + lineNo + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
