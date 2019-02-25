package com.example.kejun.myapplication.socket;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 服务器类
 * 封装服务器地址和端口
 */
public class HostAddress {
    private InetAddress address;
    private int         port;

    public HostAddress(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public HostAddress(String ip, int port) {
        try {
            this.address = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.port = port;
    }

    public InetAddress getAddress() {
        return address;
    }

    public void setAddress(InetAddress address) {
        this.address = address;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public String toString() {
        return "HostAddress{" +
                "address=" + address +
                ", port=" + port +
                '}';
    }
}
