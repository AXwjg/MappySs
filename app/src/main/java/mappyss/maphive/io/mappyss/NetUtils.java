package mappyss.maphive.io.mappyss;

import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


/**
 * Created by oldwang on 2018/4/19.
 *
 */

public class NetUtils {

    private static final String TAG = "NetUtils";

    /**
     * post请求 提交数据到服务器
     */
    public static void httpPostJSON(String json){
        Log.i(TAG, "start");
        String url = "http://www.baidu.com";
        OkHttpClient client = new OkHttpClient();//创建OkHttpClient对象。
        MediaType JSON = MediaType.parse("application/json; charset=utf-8");//数据类型为json格式，
        String jsonStr = "{\"username\":\"lisi\",\"nickname\":\"李四\"}";
        RequestBody body = RequestBody.create(JSON, jsonStr);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {

            //请求失败时调用
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(TAG, "onFailure: " + e);
            }

            //请求成功时调用
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.i(TAG, "onResponse: " + response.body().string());
                }
            }
        });

    }

    public static void postFile(View view, File file) {
        OkHttpClient client = new OkHttpClient();//创建OkHttpClient对象。
        MediaType fileType = MediaType.parse("File/*");//数据类型为json格式，
        RequestBody body = RequestBody.create(fileType , file );
        Request request = new Request.Builder()
                .url("http://www.baidu.com")
                .post(body)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i(TAG, "onFailure: "+e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i(TAG, "onResponse: "+response.body().string());
            }
        });
    }
}


