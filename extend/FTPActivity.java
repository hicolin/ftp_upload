package extend;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.ToastUtils;
import com.hb.dialog.myDialog.MyAlertDialog;

import org.apache.commons.net.ftp.FTP;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;

import cc.xxx.xxx.R;
import cc.xxx.xxx.entities.UploadFileEntity;
import cc.xxx.xxx.utils.DocumentUtils;
import cc.xxx.xxx.utils.FTPUtils;
import cc.xxx.xxx.utils.WebViewUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FTPActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "FileUploadActivity";

    private TextView mTitle;
    private WebView webView;

    // 文件上传
    private ValueCallback<Uri[]> uploadMessage;
    private static final int FILE_CHOOSER_RESULT_CODE = 10000;

    private FTPUtils ftpUtils;

    private final ArrayList<String> pathList = new ArrayList<>();
    private final ArrayList<UploadFileEntity> ftpFileList = new ArrayList<>();

    private String taskId;
    private String uploadId;
    private String assignId;

    // 已上传文件数量
    private int uploadCompleteCount = 0;

    private boolean isUploading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_view);

        bindView();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private void bindView() {
        RelativeLayout back = findViewById(R.id.back);
        mTitle = findViewById(R.id.top_bar_title);
        webView = findViewById(R.id.web_view);

        back.setOnClickListener(this);

        // 加载 WebView
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            String url = bundle.getString("url");

            webView.getSettings().setJavaScriptEnabled(true);
            WebViewUtils.setLocalStorageEnable(this, webView);

            if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true);
            }

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    Intent intent = new Intent();

                    if (url.contains("tel:")) {
                        intent.setAction(Intent.ACTION_DIAL);
                        Uri data = Uri.parse(url);
                        intent.setData(data);
                    } else {
                        intent.setClass(getApplicationContext(), WebViewActivity.class);
                        intent.putExtra("url", url);
                    }

                    startActivity(intent);

                    return true;
                }
            });

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                    uploadMessage = filePathCallback;
                    openFileChooser();
                    return true;
                }
            });
            webView.addJavascriptInterface(this, "app");
            webView.requestFocus(View.FOCUS_DOWN);
            webView.loadUrl(url);
        }
    }

    // 打开文件上传界面
    private void openFileChooser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "Image Chooser"), FILE_CHOOSER_RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (uploadMessage == null) return;
            Uri[] results = null;
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    // 获取文件路径
                    Uri uri = data.getData();

//                    String authority = uri.getAuthority();
                    String path = DocumentUtils.getPath(this, uri);
                    pathList.add(path);
                    Log.d(TAG, "onActivityResult: " + pathList);

                    String dataString = data.getDataString();
                    ClipData clipData = data.getClipData();
                    if (clipData != null) {
                        results = new Uri[clipData.getItemCount()];
                        for (int i = 0; i < clipData.getItemCount(); i++) {
                            ClipData.Item item = clipData.getItemAt(i);
                            results[i] = item.getUri();
                        }
                    }
                    if (dataString != null) {
                        results = new Uri[]{Uri.parse(dataString)};
                    }
                }
            }
            uploadMessage.onReceiveValue(results);
            uploadMessage = null;
        }
    }

    // 文件上传
    private void uploadFile(File file) {
        try {

            ftpUtils = FTPUtils.getInstance();

            boolean flag = ftpUtils.initFTPSetting("", 21, "", "");
            // 安卓设备的网络
//          boolean flag = ftpUtils.initFTPSetting("", 21, "", "");
            if (!flag) {
                ToastUtils.showLong("FTP 连接失败");
                return;
            }

//            Date date = new Date();
//            @SuppressLint("SimpleDateFormat") SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

            //设置存储路径
            // 文件存储格式： /newsupload/$task_id/$upload_id/
//            String dir = "/ftp/" + simpleDateFormat.format(date);
//            String dir = "/newsupload/" + taskId + "/" + uploadId + "/";
//            String dir = "/newsupload/" + simpleDateFormat.format(date) + "/";

            // 逐级创建
            String dir = "/newsupload" + "/";
            ftpUtils.ftpClient.makeDirectory(dir);
            dir += taskId + "/";
            ftpUtils.ftpClient.makeDirectory(dir);
            dir += uploadId + "/";
            ftpUtils.ftpClient.makeDirectory(dir);

            ftpUtils.ftpClient.changeWorkingDirectory(dir);
            //设置上传文件需要的一些基本信息
            // ftpUtils.ftpClient.setBufferSize(1024 * 1024 * 5); // 5MB (大文件，高带宽)
            ftpUtils.ftpClient.setBufferSize(1024 * 10);
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
            String newFileName = "UPLOAD_" + timeStamp + random + "." + suffix;

            FileInputStream fileInputStream = new FileInputStream(file);
            OutputStream outputStream = ftpUtils.ftpClient.storeFileStream(newFileName);
            while ((n = fileInputStream.read(buffer)) != -1) {
//                outputStream.write(buffer);
                outputStream.write(buffer, 0 , n);
                trans += n;
                double percent = trans * 1.0 / len * 100;
                Log.d(TAG, "文件: " + fileName + " " + "进度: " + String.format("%.2f", percent) + "%");

                updatePercent(fileName, percent);
            }

            UploadFileEntity uploadFileEntity = new UploadFileEntity();
            uploadFileEntity.setFileName(fileName);
            uploadFileEntity.setNewFileName(newFileName);
            uploadFileEntity.setFilePath(dir);
            uploadFileEntity.setFileSize(String.valueOf(len));
            uploadFileEntity.setSuffix(suffix);

            // 如果是图片，则获取宽高
            if (suffix.equalsIgnoreCase("png")
                    || suffix.equalsIgnoreCase("jpg")
                    || suffix.equalsIgnoreCase("jpeg")
                    || suffix.equalsIgnoreCase("gif")) {

                for (String path : pathList) {
                    if (path.contains(uploadFileEntity.getFileName())) {
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        BitmapFactory.decodeFile(path, options);
                        int imageWidth = options.outWidth;
                        int imageHeight = options.outHeight;

                        uploadFileEntity.setWidth(String.valueOf(imageWidth));
                        uploadFileEntity.setWidth(String.valueOf(imageHeight));
                    }
                }
            }

            ftpFileList.add(uploadFileEntity);
            Log.d(TAG, "文件: " + fileName + " " + "上传完成");

            fileInputStream.close();
            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 反馈进度
    private void updatePercent(String fileName, double percent) {
        final String name = fileName;
        final String strPercent = String.valueOf(percent);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.evaluateJavascript("updatePercent(" + "\"" + name + "\"" + "," + strPercent + ")", new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                    }
                });
            }
        });
    }

    // 提交FTP上传结果
    private void submitUploadResult(final UploadFileEntity uploadFileEntity) {

        if (uploadFileEntity.getContent() == null) {
            uploadFileEntity.setContent("");
        }
        if (uploadFileEntity.getWidth() == null) {
            uploadFileEntity.setWidth("0");
        }
        if (uploadFileEntity.getHeight() == null) {
            uploadFileEntity.setHeight("0");
        }

        FormBody formBody = new FormBody.Builder()
                .add("task_id", taskId)
                .add("upload_id", uploadId)
                .add("assign_id", assignId)
                .add("userAuth_type", String.valueOf(1))
                .add("filepath", uploadFileEntity.getFilePath())
                .add("mat_storename", uploadFileEntity.getNewFileName())
                .add("mat_name", uploadFileEntity.getFileName())
                .add("filesize", uploadFileEntity.getFileSize())
                .add("ext", uploadFileEntity.getSuffix())
                .add("content", uploadFileEntity.getContent())
                .add("width", uploadFileEntity.getWidth())
                .add("height", uploadFileEntity.getHeight())
                .build();

        final Request request = new Request.Builder()
                .url(getString(R.string.site_url) + "/newsupload/mb_newstask/ftp_uploadfile")
                .post(formBody)
                .build();

        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();
        Call call = okHttpClient.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.d(TAG, "onFailure: " + e.getMessage());
                ToastUtils.showLong(e.getMessage());

                if (uploadCompleteCount == pathList.size()) {
                    isUploading = false;
                }
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                uploadCompleteCount++;

                String jsonData = Objects.requireNonNull(response.body()).string();
                Log.d(TAG, "onResponse: " + jsonData);
                try {
                    JSONObject object = new JSONObject(jsonData);
                    String status = object.getString("status");
                    Log.d(TAG, "onResponse: status:" + status);

                    // 单文件上传失败
                    if (status.equals("0")) {
                        final JSONObject resObject = new JSONObject();
                        resObject.put("status", status);
                        resObject.put("msg", uploadFileEntity.getFileName() + " : " + "上传失败");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                webView.evaluateJavascript("uploadFail(" + resObject.toString() + ")", new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {

                                    }
                                });
                            }
                        });
                    }

                    // 全部文件上传完成
                    if (uploadCompleteCount == pathList.size()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                webView.evaluateJavascript("uploadComplete()", new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {

                                    }
                                });
                            }
                        });

                        isUploading = false;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back) {
            this.onBackPressed();
        }
    }

    @JavascriptInterface
    public void setTitle(String title) {
        mTitle.setText(title);
    }

    @JavascriptInterface
    public void goBack() {
        finish();
    }

    // 前端移除要上传的图片
    @JavascriptInterface
    public void removePath(String fileName) {
        for (String path : pathList) {
            if (path.contains(fileName)) {
                pathList.remove(path);
            }
        }
    }

    @JavascriptInterface
    public void ftpUpload(final String taskId, final String uploadId, final String assignId) {

        if (taskId.isEmpty() || uploadId.isEmpty() || assignId.isEmpty() || pathList.isEmpty()) {
            return;
        }

        this.taskId = taskId;
        this.uploadId = uploadId;
        this.assignId = assignId;

        isUploading = true;

        new Thread() {
            @Override
            public void run() {
                super.run();

                for (String path : pathList) {
                    uploadFile(new File(path));
                }

                // 处理 ftpFilePathList，通过 fileId 传递给后台接口
                Log.d(TAG, "run: " + ftpFileList);

                for (UploadFileEntity uploadFileEntity : ftpFileList) {
                    submitUploadResult(uploadFileEntity);
                }

            }
        }.start();

    }

    @Override
    public void onBackPressed() {
        if (isUploading) {
            MyAlertDialog myAlertDialog = new MyAlertDialog(this).builder()
                    .setTitle("确认吗？")
                    .setMsg("返回会中断文件上传")
                    .setPositiveButton("确认", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (ftpUtils != null && ftpUtils.ftpClient.isConnected()) {
                                try {
                                    ftpUtils.ftpClient.logout();
                                    ftpUtils.ftpClient.disconnect();
                                    ftpUtils = null;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            finish();
                        }
                    }).setNegativeButton("取消", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                        }
                    });
            myAlertDialog.show();
        } else {
            super.onBackPressed();
        }
    }



}
