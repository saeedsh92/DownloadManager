package com.a7learn.downloadmanager;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private DownloadManager downloadManager;
    private long referenceId;
    private TextView progressTv;
    private DownloadTaskCompleteBroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        broadcastReceiver = new DownloadTaskCompleteBroadcastReceiver();
        registerReceiver(broadcastReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        progressTv = findViewById(R.id.tv_main_progress);
        downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        downloadFile("https://hw5.cdn.asset.aparat.com/aparat-video/16cc55bde76384049c8e4d5039b83e0011841706-144p__56036.mp4");
    }

    public void downloadFile(String url) {
        String fileName = url.substring(url.lastIndexOf("/") + 1, url.length());
        String fileFormat = url.substring(url.lastIndexOf(".") + 1, url.length());
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Downloading file");
        request.setDescription(fileName);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        referenceId = downloadManager.enqueue(request);
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean downloading = true;

                while (downloading) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(referenceId);
                    Cursor cursor = downloadManager.query(query);
                    if (cursor.moveToFirst()) {
                        long downloadBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        long totalBytes = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        final int progress = (int) ((downloadBytes * 100) / totalBytes);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressTv.setText("Downloading file... " + progress + "%");
                            }
                        });
                        int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                        switch (status) {
                            case DownloadManager.STATUS_SUCCESSFUL:
                                downloading = false;
                                break;
                            case DownloadManager.STATUS_FAILED:
                                downloading = false;
                                int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));
                                break;
                        }

                    }
                }
            }
        }).start();

        //
        downloadManager.remove(referenceId);

    }

    private void showReason(int reason) {
        switch (reason) {
            case DownloadManager.ERROR_CANNOT_RESUME:
                Toast.makeText(this, "Cannot resume downloading file", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private class DownloadTaskCompleteBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (referenceId != -1 && referenceId == MainActivity.this.referenceId)
                Toast.makeText(context, "Download Completed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }
}
