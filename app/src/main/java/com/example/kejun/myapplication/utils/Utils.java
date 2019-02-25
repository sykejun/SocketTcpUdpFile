package com.example.kejun.myapplication.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

/**
 * Created by kejun
 */
public class Utils {
    public static String getMessage(byte[] buffer) {
        try {
            String msg = new String(buffer, "utf-8");
            int eof = msg.indexOf(Configuration.EOF);
            if (eof > 0) {
                return msg.substring(0, eof);
            } else {
                return msg;
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }



    public static String getHumanReadableSize(final long size) {
        String[] units = {"Byte", "KB", "MB", "GB", "TB", "PB"};
        int pos = 0;
        double dsize = size;
        while (dsize > 1024) {
            dsize /= 1024;
            pos++;
        }
        return (int) (dsize * 100) / 100.0 + units[pos];
    }

    /**
     *注释描述: 获取本机在局域网中的IP
     */
    public static InetAddress getLocalHostLanIP() {
        InetAddress IP = null;
        try {
            for (Enumeration<NetworkInterface> enNetI = NetworkInterface
                    .getNetworkInterfaces(); enNetI.hasMoreElements(); ) {
                NetworkInterface netI = enNetI.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = netI
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                        return inetAddress;
                    }
                }
            }

        } catch (SocketException e) {
            e.printStackTrace();
        }
        return IP;
    }



    public static InetAddress getBroadcastAddr(InetAddress address) {
        //获取本机在局域网中的广播地址
        if (address == null) {
            return null;
        }
        InetAddress broadcastAddr = null;
        NetworkInterface networkInterface;
        try {
            networkInterface = NetworkInterface.getByInetAddress(address);
            for (InterfaceAddress taddr : networkInterface.getInterfaceAddresses()) {
                //获取指定ip的广播地址
                if (taddr.getAddress().getHostAddress().equals(address.getHostAddress())) {
                    broadcastAddr = taddr.getBroadcast();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return broadcastAddr;
    }

    public static InetAddress getBroadcastAddr() {
        return getBroadcastAddr(getLocalHostLanIP());
    }

    public static void showDialog(Activity activity, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);// 定义弹出框
        builder.setTitle(title);// 设置标题
        builder.setMessage(message);// 设置信息主体
        builder.setPositiveButton("知道了",// 设置确定键显示的内容及点击后的操作
                new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();// 直接关闭对话框
                    }
                });

        builder.create().show();
    }
}
