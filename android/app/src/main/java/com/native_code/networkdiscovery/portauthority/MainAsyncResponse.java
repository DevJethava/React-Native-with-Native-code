package com.native_code.networkdiscovery.portauthority;

import java.util.concurrent.atomic.AtomicInteger;

public interface MainAsyncResponse extends ErrorAsyncResponse {

    /**
     * Delegate to handle Host + AtomicInteger outputs
     *
     * @param h
     * @param i
     */
    void processFinish(Host h, AtomicInteger i);

    /**
     * Delegate to handle integer outputs
     *
     * @param output
     */
    void processFinish(int output);

    /**
     * Delegate to handle string outputs
     *
     * @param output
     */
    void processFinish(String output);

    /**
     * Delegate to handle boolean outputs
     *
     * @param output
     */
    void processFinish(boolean output);
}
