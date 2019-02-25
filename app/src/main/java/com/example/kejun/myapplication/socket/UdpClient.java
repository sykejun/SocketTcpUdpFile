package com.example.kejun.myapplication.socket;

import android.util.Log;

import com.example.kejun.myapplication.interfaces.SearchStateListener;
import com.example.kejun.myapplication.utils.Configuration;
import com.example.kejun.myapplication.utils.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

/**
 * Created by kejun
 */
public class UdpClient {
    private SearchStateListener updateState;
    private int                 times;
    private int                 timeout;
    private int                 port;
    private String               TAG = UdpClient.class.getName();
    public UdpClient(SearchStateListener updateState, int timeout, int times, int port) {
        this.updateState = updateState;
        this.timeout = timeout;
        this.times = times;
        this.port = port;
        TAG = UdpClient.class.getName();
    }

    public UdpClient(SearchStateListener updateState, int port) {
        this.updateState = updateState;
        this.timeout = Configuration.SEARCH_TIMOUT;
        this.times = Configuration.SEARCH_TIMES;
        this.port = port;

    }

    public UdpClient(SearchStateListener updateState) {
        this.updateState = updateState;
        this.timeout = Configuration.SEARCH_TIMOUT;
        this.times = Configuration.SEARCH_TIMES;
        this.port = Configuration.UDP_PORT;
        TAG = UdpClient.class.getName();
    }

    /**
     *注释描述:搜索地址 此方法是通过发送广播，服务器相应到广播后，返回ip地址  和端口
     */
    public HostAddress search() {
        DatagramPacket sendPacket = null;
        DatagramPacket recvPacket = null;
        DatagramSocket clientSocket = null;
        InetAddress address = null;
        String msg = "Lantrans Android UDPCLIENT" + Configuration.DELIMITER;

        byte[] recvBuf = new byte[Configuration.STRING_BUF_LEN];
        byte[] sendBuf = new byte[Configuration.STRING_BUF_LEN];
        address = Utils.getBroadcastAddr();//设置广播地址
        try {
            clientSocket = new DatagramSocket();//创建一个udpClient
            clientSocket.setBroadcast(true);//广播信息
            clientSocket.setSoTimeout(this.timeout * 1000);//如果2秒后没后得到服务器的回应, 抛出超时异常, 以便重新广播
            Log.e(TAG, "本机ip:" + Utils.getLocalHostLanIP() + " 广播地址:" + address);
            sendBuf = msg.getBytes("utf-8");
        } catch (SocketException e3) {
            e3.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        sendPacket = new DatagramPacket(sendBuf, sendBuf.length, address, port);//发送数据
        recvPacket = new DatagramPacket(recvBuf, recvBuf.length);//接收数据

        int tryTimes = 1;
        while (tryTimes <= times) {//多次尝试
            try {
                clientSocket.send(sendPacket);//向服务器发送数据包
                clientSocket.receive(recvPacket);// 从此套接字接收数据报包。
                if (recvPacket != null && new String(recvPacket.getData()).length() > 0) { //recvPacket.getData() 返回接收的数据或发送出的数据。 如果有反应，就不用继续发送
                    break;
                }
            } catch (SocketTimeoutException e) {
                e.printStackTrace();
                updateState.updateState(tryTimes, times);
                Log.e(TAG, "超时: " + tryTimes + "次" + "共:" + times + "次");
                tryTimes++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //得到对方的ip地址相应，和端口号
        if (tryTimes <= times) {
            String strPort = Utils.getMessage(recvPacket.getData()); //获取端口
            if (strPort != null && strPort.length() > 0) {
                int serverPort = Integer.parseInt(strPort);
                Log.i(TAG,"接收方的ip值："+recvPacket.getAddress()+"端口号："+serverPort);
                return new HostAddress(recvPacket.getAddress(), serverPort);//返回接收或发送此数据报文的机器的 IP 地址。
            }
        }
        return null;
    }
}
