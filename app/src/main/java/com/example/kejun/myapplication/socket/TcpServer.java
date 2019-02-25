package com.example.kejun.myapplication.socket;

import android.util.Log;

import com.example.kejun.myapplication.interfaces.ProgressListener;
import com.example.kejun.myapplication.utils.Configuration;
import com.example.kejun.myapplication.utils.FileDesc;
import com.example.kejun.myapplication.utils.Utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

/**
 * Created by kejun
 */
public class TcpServer {
    private ProgressListener     progressListener;
    private int                  port;
    private String               TAG;
    private ServerSocket         serverSocket;
    private Socket               channel;
    private BufferedInputStream  bufferedInputStream;
    private BufferedOutputStream bufferedOutputStream;

    public TcpServer(int port, ProgressListener progressListener) {
        this.progressListener = progressListener;
        this.port = port;
        TAG = TcpServer.class.getName();
    }

    public TcpServer(ProgressListener progressListener) {
        this.progressListener = progressListener;
        this.port = Configuration.TCP_PORT; //偷懒就使用直接使用65500端口
        TAG = TcpServer.class.getName();
    }

    /**

     *注释描述:等待与发送方建立Tcp连接
     */
    public ArrayList<FileDesc> waitSenderConnect() {
        ArrayList<FileDesc> files = null;
        String fileInfo = "";
        byte[] inputBuf = new byte[Configuration.STRING_BUF_LEN];

        try {
            serverSocket = new ServerSocket(this.port);//创建tcp服务器, 接收文件
            serverSocket.setSoTimeout(Configuration.WAITING_TIME * 1000);//设置等待连接时长
            Log.e(TAG, "tcp server is waiting");
            channel = serverSocket.accept();//建立链接  接受客户端所发送的请求，同时创建一个Socket对象
            channel.setKeepAlive(Boolean.TRUE);

            //获取socket的输入输出流
            bufferedInputStream = new BufferedInputStream(channel.getInputStream());  // 得到该Socket对象的输入流
            bufferedOutputStream = new BufferedOutputStream(channel.getOutputStream());

            bufferedInputStream.read(inputBuf);//读取要接收文件的描述信息
            fileInfo = Utils.getMessage(inputBuf);


            //分离字符串形式的文件描述信息, 保存到arraylist
            files = new ArrayList<>();
            for (String file : fileInfo.split(Configuration.FILES_SPT)) {
                String[] fd = file.split(Configuration.FILE_LEN_SPT);
                files.add(new FileDesc(fd[0], Long.parseLong(fd[1])));
            }
            bufferedOutputStream.write((Utils.getMessage(inputBuf) + Configuration.DELIMITER).getBytes("utf-8"));//将客户端发送的信息原封回复, 表示可以开始传输文件
            bufferedOutputStream.flush();
        } catch (SocketTimeoutException e) {//表示文件的进度传输失败
            progressListener.updateProgress(-3, 100, 100, 999);//更新UI界面
            try {
                serverSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return files;
    }

    /**
     *注释描述:接收文件
     */
    public int recieveFile(ArrayList<FileDesc> files, String savePath) {
        byte[] recvBuf = new byte[Configuration.STRING_BUF_LEN];
        int filePosition = 0;
        String msg = "";

        for (; filePosition < files.size(); filePosition++) {
            try {
                long hasRecieve = 0;
                long lastimeRecv = 0;
                long startTime = 0;
                long endTime = 0;
                double speed = 0L;
                int actualLen;
                FileDesc fileDesc = files.get(filePosition);

                FileOutputStream fileOutputStream = null;
                File newFile = new File(savePath, fileDesc.getName());
                newFile.createNewFile();
                newFile.setWritable(true);
                fileOutputStream = new FileOutputStream(newFile);

                bufferedInputStream.read(recvBuf);//接收即将要发送的文件<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
                msg = Utils.getMessage(recvBuf);
                msg += Configuration.DELIMITER;

                bufferedOutputStream.write(msg.getBytes("utf-8"));//发送准备接收的确认>>>>>>>>>>>>>>>>>>>>>>>>>>>
                bufferedOutputStream.flush();

                startTime = System.nanoTime();
                if (fileDesc.getLength() == 0) { //接收空文件处理
                    progressListener.updateProgress(filePosition, 100, 100, 888);
                    continue;
                }

                //从网络中读取文件字节流
                byte[] fileBuf = new byte[Configuration.FILE_IO_BUF_LEN];
                while ((actualLen = bufferedInputStream.read(fileBuf, 0, Configuration.FILE_IO_BUF_LEN)) > 0) {
                    //将网络中的字节流写入本地文件
                    fileOutputStream.write(fileBuf, 0, actualLen);
                    endTime = System.nanoTime();
                    hasRecieve += actualLen;

                    long diffTime = endTime - startTime;
                    if (diffTime >= 500000000) {//计算传输速度0.5秒一次更新
                        long diffSize = hasRecieve - lastimeRecv;
                        speed = ((double) diffSize / (double) diffTime) * (1000000000.0 / 1024.0);
                        lastimeRecv = hasRecieve;
                        startTime = endTime;
                    }

                    progressListener.updateProgress(filePosition, hasRecieve, fileDesc.getLength(), new Double(speed).intValue());
                    // recieve all part of file
                    if (hasRecieve == fileDesc.getLength()) {
                        bufferedOutputStream.flush();
                        break;
                    }
                }

                if (hasRecieve == fileDesc.getLength()) {
                    String sizeAck = hasRecieve + Configuration.DELIMITER;
                    bufferedOutputStream.write(sizeAck.getBytes("utf-8"));
                    bufferedOutputStream.flush();
                }

            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
                Log.e(TAG, "EXCEPTION RAISED:" + e1.getMessage());
                continue;
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, e.getMessage());
            }
        }
        return filePosition;
    }

    public void close() {
        try {
            bufferedInputStream.close();
            bufferedOutputStream.close();
            serverSocket.close();
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
