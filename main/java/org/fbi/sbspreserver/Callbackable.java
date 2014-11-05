package org.fbi.sbspreserver;

public interface Callbackable extends Runnable {
    void setResults(CtgRequest ctgRequest);
}