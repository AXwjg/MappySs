package mappyss.maphive.io.mappyss;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by oldwang on 2018/4/9.
 *
 */

public class WelcomeActivity extends AppCompatActivity {

    public static final String EXIT_TAG = "TAG EXIT";

    public static final int REQUEST_CODE = -1;

    private boolean lacksPermission = false;

    private String[] permissions = {
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welocome);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        boolean isExit = getIntent().getBooleanExtra(EXIT_TAG, false);
        if (isExit) {
            WelcomeActivity.this.finish(); //退出整个app的
        } else {
            if (lacksPermissions(permissions)) { //缺少权限
                PermissionsActivity.startActivityForResult(this, REQUEST_CODE, permissions);
                lacksPermission = true;
            } else {
//                initLogback();
                verifyAndJump();
            }
        }
    }

    private boolean lacksPermissions(String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(WelcomeActivity.this, permission) ==
                    PackageManager.PERMISSION_DENIED) {
                return true; //缺失某些权限，6.0及以上能检测出，6.0以下在全部为false(均需在manifest声明)
            }
        }
        return false;
    }

    private void verifyAndJump() {
        //等待过程中访问网络是否开启
        if (isNetworkConnected()) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        if (!lacksPermission) {
                            Thread.sleep(800);
                        }
                        startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                        finish();
                    } catch (InterruptedException e) {
                        Log.e("",e.getLocalizedMessage());
                    }

                }
            }.start();
        } else {
        }
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = cm.getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (data.getBooleanExtra(EXIT_TAG, false)) {
                finish();
            }
        }
    }

}
