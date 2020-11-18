package cc.xxx.xxx.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.blankj.utilcode.util.ToastUtils;

import org.apache.commons.net.ftp.FTP;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import cc.xxx.xxx.R;
import cc.xxx.xxx.utils.FTPUtils;

public class FTPActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "FTPActivity";

    private FTPUtils ftpUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftp);

        Button ftpBtn = findViewById(R.id.ftp);
        ftpBtn.setOnClickListener(this);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.ftp) {
            new Thread() {
                @Override
                public void run() {
                    super.run();

                    if (!ftpUtils.ftpClient.isConnected()) {
                        return;
                    }

//                    文件上传
//                    File file = new File(Environment.getExternalStorageDirectory(), "camera_guzhen_avatar.jpg");
//                    boolean ftpRes = ftpUtils.uploadFile(file.getAbsolutePath(), "guzhen_avatar.jpg");
//                    boolean ftpRes = ftpUtils.uploadFile("/storage/emulated/0/browser/videocache/xuanguangui.mp4", "xuanguangui.mp4");
//                    boolean ftpRes = ftpUtils.uploadFile("/storage/emulated/0/Download/aomenfengyun3.mp4", "aomenfengyun3.mp4");
//                    boolean ftpRes = ftpUtils.uploadFile("/storage/emulated/0/hpplay_demo/local_media/kuai.mp4", "kuai.mp4");
//                    boolean ftpRes = ftpUtils.uploadFile("/storage/emulated/0/Android/data/com.tencent.mm/MicroMsg/Download/9_app-release.apk.1", "9_app-release.apk");
//                    Log.d(TAG, "onClick: " + ftpRes);

                    //  File file = new File("/storage/emulated/0/Android/data/com.tencent.mm/MicroMsg/Download/9_app-release.apk.1");
                    File file = new File(Environment.getExternalStorageDirectory(), "camera_guzhen_avatar.jpg");
                    uploadFile(file);
                }
            }.start();

        }
    }


    private void uploadFile(File file) {
        try {

            FTPUtils ftpUtils = FTPUtils.getInstance();
            boolean flag = ftpUtils.initFTPSetting("", 21, "", "");
            // 安卓设备的网络
//        boolean flag = ftpUtils.initFTPSetting("", 21, "", "");
            if (!flag) {
                ToastUtils.showLong("FTP 连接失败");
                return;
            }

            Date date = new Date();
            @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

            //设置存储路径
            String dir = "/ftp/" + simpleDateFormat.format(date);
            ftpUtils.ftpClient.makeDirectory(dir);
            ftpUtils.ftpClient.changeWorkingDirectory(dir);
            //设置上传文件需要的一些基本信息
            // ftpUtils.ftpClient.setBufferSize(1024 * 1024 * 5); // 5MB (大文件，高带宽)
            ftpUtils.ftpClient.setBufferSize(1024);
            ftpUtils.ftpClient.setControlEncoding("UTF-8");
            ftpUtils.ftpClient.enterLocalPassiveMode();
            ftpUtils.ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            int n;
            long len = file.length();
            long trans = 0;
            int bufferSize = ftpUtils.ftpClient.getBufferSize();
            byte[] buffer = new byte[bufferSize];

            long timeStamp = System.currentTimeMillis();
            int random = (int) ((Math.random() * 9 + 1) * 1000);
            String fileName = file.getName();
            String suffix = fileName.substring(fileName.lastIndexOf(".") + 1);
            String newFileName = timeStamp + String.valueOf(random) + "." + suffix;

            FileInputStream fileInputStream = new FileInputStream(file);
            OutputStream outputStream = ftpUtils.ftpClient.storeFileStream(newFileName);
            while ((n = fileInputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0 , n);
                trans += n;
                double percent = trans * 1.0 / len * 100;
                Log.d(TAG, "文件: " + fileName + " " + "进度: " + String.format("%.2f", percent) + "%");
            }
            Log.d(TAG, "文件: " + fileName + " " + "上传完成");

            fileInputStream.close();
            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
