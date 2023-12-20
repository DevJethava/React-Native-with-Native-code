package com.native_code.networkdiscovery.portauthority;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Host implements Serializable {

    private String hostname;
    private final String ip;
    private final byte[] address;
    private final String mac;
    private String vendor;

    /**
     * Constructs a host with a known IP and MAC.
     *
     * @param ip
     * @param mac
     */
    public Host(String ip, String mac) throws UnknownHostException {
        this.ip = ip;
        this.address = InetAddress.getByName(ip).getAddress();
        this.mac = mac;
    }

    /**
     * Returns this host's hostname
     *
     * @return
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Sets this host's hostname to the given value
     *
     * @param hostname Hostname for this host
     */
    public void setHostname(String hostname) {
        if (hostname != null && (hostname.isEmpty() || hostname.endsWith(".local"))) {
            hostname = hostname.substring(0, hostname.length() - 6);
        }
        this.hostname = hostname;

    }

    /**
     * Gets this host's MAC vendor.
     *
     * @return
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Returns this host's IP address
     *
     * @return
     */
    public String getIp() {
        return ip;
    }

    /**
     * Returns this host's address in byte representation.
     *
     * @return
     */
    public byte[] getAddress() {
        return this.address;
    }

    /**
     * Returns this host's MAC address
     *
     * @return
     */
    public String getMac() {
        return mac;
    }
}
