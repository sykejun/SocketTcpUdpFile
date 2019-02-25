package com.example.kejun.myapplication.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.kejun.myapplication.R;
import com.example.kejun.myapplication.adapter.ItemsListAdapter;
import com.example.kejun.myapplication.adapter.ViewHolder;
import com.example.kejun.myapplication.interfaces.ProgressListener;
import com.example.kejun.myapplication.interfaces.SearchStateListener;
import com.example.kejun.myapplication.socket.HostAddress;
import com.example.kejun.myapplication.socket.TcpClient;
import com.example.kejun.myapplication.socket.UdpClient;
import com.example.kejun.myapplication.utils.Configuration;
import com.example.kejun.myapplication.utils.Utils;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;


public class SendActivity extends AppCompatActivity {
    private ProgressDialog searchDialog;
    AlertDialog.Builder finishDialogBuilder;
    private       UdpClient udpClient;
    public static String    FILENAME_MK = "fileName";
    public static String    FILESIZE_MK = "fileSize";
    public static String    PROGRESS_MK = "progress";
    public static String    SPEED_MK    = "speed";
    public static String    TAG;
    private       boolean   isSending   = false;

    private Button   fileBtn;
    private Button   startSendBtn;
    private ListView listView;

    private ArrayList<File>    files           = new ArrayList<>();
    private ArrayList<Integer> progressRecords = new ArrayList<>();
    private ArrayList<Integer> speedRecords    = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sender);
        //修改发送活动的toolbar颜色, 使其与按钮一致
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.color.colorAccent));

        fileBtn = (Button) findViewById(R.id.selectFileBtn);
        startSendBtn = (Button) findViewById(R.id.startSendBtn);
        TAG = this.getClass().getName();

        listView = (ListView) findViewById(R.id.sendfileListView);
        listView.setDividerHeight(10);
        setTitle("您即将发送文件");

        fileBtn.setOnClickListener(selecfileLinstener);
        startSendBtn.setOnClickListener(startSentBtnlistener);
        Utils.showDialog(this, "如何发送文件", "1: 选择您要发送的文件\n\n2: 点右下角的按钮开始搜索接收者\n\n3: 搜到后按确认发送");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (isSending == true) {
                Utils.showDialog(SendActivity.this, "提示", "\n您有文件正在发送中...\n");
            } else {
                finish();
            }
        }
        return false;
    }

    private View.OnClickListener startSentBtnlistener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isSending == true) {
                Utils.showDialog(SendActivity.this, "提示", "您当前的发送任务正在进行中");
            } else if (Utils.getBroadcastAddr() == null) {
                Utils.showDialog(SendActivity.this, "提示", "您没有处于局域网环境中, 抱歉暂时无法使用!");
            } else if (files.size() == 0) {
                Utils.showDialog(SendActivity.this, "提示", "首先选择文件是必须的!");
            } else {
                final SearchReceiverTask searchReceiverTask = new SearchReceiverTask();//开启线程执行发送的任务
                udpClient = new UdpClient(new SearchStateListener() {
                    @Override
                    public void updateState(int tryTimes, int times) {
                        searchReceiverTask.onProgressUpdate(tryTimes, times);
                    }
                });
                searchReceiverTask.execute();//搜索完成后自动执行发送在post函数中
            }
        }
    };

    private View.OnClickListener selecfileLinstener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (isSending == true) {
                Utils.showDialog(SendActivity.this, "提示", "您有任务正在进行中, 请完成后再添加文件");
            } else {
                Intent intent = new Intent(getBaseContext(), FileSelectorActivity.class);
                intent.putExtra(FileSelectorActivity.keyClassName, SendActivity.class.getName());
                intent.putExtra(FileSelectorActivity.keyIsSelectFile, true);
                intent.putExtra(FileSelectorActivity.keyIsSingleSelector, false);
                startActivityForResult(intent, FileSelectorActivity.requestCodeSingleFile);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (data == null || data.getExtras() == null) {
            return;
        }
        if (requestCode == FileSelectorActivity.requestCodeSingleFile) {
            ArrayList<String> list = data.getStringArrayListExtra(FileSelectorActivity.keyFilePaths);
            StringBuilder builder = new StringBuilder();
            files.clear();
            speedRecords.clear();
            progressRecords.clear();
            for (int i = 0; (list != null) && (i < list.size()); i++) {
                files.add(new File(list.get(i)));//////////////////////////////将用户选择的目录或者文件放到这个list
                speedRecords.add(0);
                progressRecords.add(0);
            }
            ArrayList<HashMap<String, String>> itmeList = loadFileList(files);
            ItemsListAdapter adapter = new ItemsListAdapter(getApplicationContext(), itmeList, progressRecords, speedRecords);
            listView.setAdapter(adapter);
        }
    }

    private ArrayList<HashMap<String, String>> loadFileList(ArrayList<File> files) {
        ArrayList<HashMap<String, String>> fileList = new ArrayList<>();
        for (File file : files) {
            HashMap<String, String> item = new HashMap<>();
            item.put(FILENAME_MK, file.getName());
            item.put(FILESIZE_MK, Utils.getHumanReadableSize(file.length()));
            item.put(PROGRESS_MK, "0");
            item.put(SPEED_MK, "等待中");
            fileList.add(item);
        }
        return fileList;
    }

    /**
     *注释描述:采用udp技术跟服务器做出相应，获取其IP地址，和广播地址
     */
    class SearchReceiverTask extends AsyncTask<String, Integer, HostAddress> {
        @Override
        protected void onPreExecute() {
            searchDialog = new ProgressDialog(SendActivity.this);
            searchDialog.setTitle("提示");
            searchDialog.setMessage("正在搜索主机, 请确认接收方在线");
            searchDialog.setCancelable(false);
            searchDialog.setMax(Configuration.SEARCH_TIMES);
            searchDialog.show();
        }

        @Override
        protected HostAddress doInBackground(String... params) {
            return udpClient.search();//搜索
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            searchDialog.setProgress(values[0]);//更新UI线程
        }

        @Override
        protected void onPostExecute(final HostAddress address) {//这里的HostAddress 是对方的ip地址和 端口号
            //searchDialog.setCancelable(true);
            searchDialog.dismiss();// 直接关闭对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(SendActivity.this);// 定义弹出框
            builder.setTitle("搜索结果");// 设置标题

            if (address != null) {
                builder.setMessage("找到主机\nIP 地址:" + address.getAddress().toString().replace("/", "") + "\nTCP端口:" + address.getPort());
                builder.setPositiveButton("发送",//创建连接
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                if (address == null) {
                                    Toast.makeText(getApplicationContext(), "请先搜索主机", Toast.LENGTH_LONG).show();
                                } else {
                                    SendFileTask sendFileTask = new SendFileTask();//发送文件
                                    sendFileTask.execute(address);
                                }
                            }
                        });
                builder.setNegativeButton("取消",// 设置取消的信息
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
            } else {
                builder.setMessage("未找到主机, 请重新搜索");
            }
            builder.create().show();
        }
    }

    /**
     *注释描述:采用tcp技术发送文件
     */
    class SendFileTask extends AsyncTask<HostAddress, Integer, Integer> {
        @Override
        protected void onPreExecute() {
            isSending = true;
        }

        @Override
        protected Integer doInBackground(HostAddress... params) {
            HostAddress receiverAddr = params[0];
            if (receiverAddr == null) {
                return -1;
            }

            TcpClient tcpClient = new TcpClient(receiverAddr, new ProgressListener() {

                @Override
                public void updateProgress(int filePositon, long hasGot, long totalSize, int speed) {
                    //这里的接口是下面的发送文件
                    int progress = new Double(100.0 * (double) hasGot / (double) totalSize).intValue();
                    publishProgress(filePositon, progress, speed);
                    progressRecords.set(filePositon, progress);
                }
            });

            String fileinfo = "";
            for (int position = 0; files != null && position < files.size(); position++) {
                fileinfo += (files.get(position).getName() + Configuration.FILE_LEN_SPT + files.get(position).length() + Configuration.FILES_SPT);
            }
            fileinfo += Configuration.DELIMITER;

            String replyMsg = tcpClient.connectReceiver(fileinfo);//发送文件描述信息
            int finished = 0;
            if (replyMsg.length() > 0 && files.size() > 0) {
                finished = tcpClient.sendFile(files);//发送文件
            } else {
                return -2;//接收方没有确认
            }
            tcpClient.close();///////////////////
            Log.i(TAG,finished+"");
            return finished;
        }

        //此方法被执行，直接将进度信息更新到UI组件上。
        @Override
        protected void onProgressUpdate(Integer... values) {
            final int position = values[0];
            final int progress = values[1];
            final long speed = values[2];//多少kb每秒

            String strspeed = Utils.getHumanReadableSize(speed * 1024) + "/S";
            int firstVisiblePosition = listView.getFirstVisiblePosition();
            int lastVisiblePosition = listView.getLastVisiblePosition();
            if (position >= firstVisiblePosition && position <= lastVisiblePosition) {
                View view = listView.getChildAt(position - firstVisiblePosition);
                if (view.getTag() instanceof ViewHolder) {
                    ViewHolder vh = (ViewHolder) view.getTag();
                    vh.progressBar.setProgress(progress);
                    vh.progressText.setText(progress + "%");
                    if (progress == 100) {
                        vh.speedText.setText("已完成");
                    } else {
                        vh.speedText.setText(strspeed);
                    }
                }
            }
            setTitle("正在发送[" + (position + 1) + "/" + speedRecords.size() + "]");
        }

        //当后台操作结束时，此方法将会被调用，计算结果将做为参数传递到此方法中，直接将结果显示到UI组件上。
        @Override
        protected void onPostExecute(Integer finishedCount) {
            isSending = false;
            finishDialogBuilder = new AlertDialog.Builder(SendActivity.this);// 定义弹出框
            if (finishedCount >= 0) {
                finishDialogBuilder.setTitle("提示");// 设置标题
                finishDialogBuilder.setMessage(finishedCount + "个文件已经成功发送!");// 设置信息主体
            } else {
                finishDialogBuilder.setTitle("发生异常");// 设置标题
                finishDialogBuilder.setMessage("接收方没有发送有效确认信息!");// 设置信息主体
            }
            finishDialogBuilder.setNegativeButton("确定",// 设置取消的信息
                    new android.content.DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();// 直接关闭对话框
                        }
                    });
            finishDialogBuilder.create().show();
        }
    }
}