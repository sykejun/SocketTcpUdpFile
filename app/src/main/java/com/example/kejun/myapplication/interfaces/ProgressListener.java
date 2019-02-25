package com.example.kejun.myapplication.interfaces;

/**
 * Created by kejun
 */
public interface ProgressListener {
    void updateProgress(int filePositon, long hasGot, long totalSize, int speed);
}