package com.example.kejun.myapplication.activity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.kejun.myapplication.R;
import com.example.kejun.myapplication.utils.PermissionsChecker;

/**
 * Created by kejun
 */
public class MainActivity extends AppCompatActivity {
    protected int requestCodePermission = 2019;

    private static String[] permission = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.INTERNET};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.sendBtn).setOnClickListener(new MyOnClickListener());
        findViewById(R.id.recvBtn).setOnClickListener(new MyOnClickListener());
    }

    @Override
    protected void onResume() {
        super.onResume();
        LacksPermissions(permission);
    }

    /**
     *注释描述:检测权限
     * @param permissions 权限数组
     */
    protected void LacksPermissions(String[] permissions){
        // 缺少权限时, 进入权限配置页面
        if (PermissionsChecker.lacksPermissions(this,permissions)) {//返回ture表示  缺少权限
            PermissionsActivity.startActivityForResult(this, requestCodePermission, permissions);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 拒绝时, 关闭页面, 缺少主要权限, 无法运行
        if (requestCode == requestCodePermission && resultCode == PermissionsActivity.PERMISSIONS_DENIED) {//没有权限
            this.finish();
        }else if (requestCode == requestCodePermission && resultCode == PermissionsActivity.PERMISSIONS_GRANTED){//有权限

        }
    }


    private final class MyOnClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (R.id.sendBtn == v.getId()) { //send file
                Intent sendFileIntent = new Intent(getBaseContext(), SendActivity.class);
                startActivity(sendFileIntent);
            } else if (R.id.recvBtn == v.getId()) {//recive file
                Intent receiveFileActivity = new Intent(getBaseContext(), ReceiveActivity.class);
                startActivity(receiveFileActivity);
            }
        }
    }


}
