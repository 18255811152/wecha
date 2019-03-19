package com.sk.weichat.audio;

public interface RecordListener {
    public void onRecordStart();

    public void onRecordCancel();

    public void onRecordSuccess(String filePath, int timeLen);
}
