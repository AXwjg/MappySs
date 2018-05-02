package mappyss.maphive.io.mappyss;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public class PermissionsActivity extends AppCompatActivity {

    public static final int PERMISSIONS_GRANTED = 0; // 权限授权
    public static final int PERMISSIONS_DENIED = 1; // 权限拒绝

    private static final int PERMISSION_REQUEST_CODE = 0; // 系统权限管理页面的参数

    private static final String EXTRA_PERMISSIONS = "mappyss.maphive.io.mappyss.permission";

    private static final String PACKAGE_URL_SCHEME = "package:"; // 方案

    private boolean isRequireCheck; // 是否需要系统权限检测, 防止和系统提示框重叠

    // 启动当前权限页面的公开接口
    public static void startActivityForResult(Activity activity, int requestCode, String... permissions) {
        Intent intent = new Intent(activity, PermissionsActivity.class);
        intent.putExtra(EXTRA_PERMISSIONS, permissions);
        ActivityCompat.startActivityForResult(activity, intent, requestCode, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getIntent() == null || !getIntent().hasExtra(EXTRA_PERMISSIONS)) {
            throw new RuntimeException("PermissionsActivity must start by calling startActivityForResult!");
        }
        setContentView(R.layout.activity_permissions);
        isRequireCheck = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isRequireCheck) {
            String[] permissions = getPermissions();
            for (String permission : permissions) {
                Log.i("-----", permission);
            }
            if (lacksPermissions(permissions)) {
                ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE); //缺少则弹出权限请求
            } else {
               allPermissionGranted();
            }
        } else {
            isRequireCheck = true;
        }
    }

    // 返回需要检测的权限
    private String[] getPermissions() {
        return getIntent().getStringArrayExtra(EXTRA_PERMISSIONS);
    }

    private boolean lacksPermissions(String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(PermissionsActivity.this, permission) ==
                    PackageManager.PERMISSION_DENIED) {
                return true; //缺失某些权限，6.0及以上能检测出，6.0以下在全部为false(均需在manifest声明)
            }
        }
        return false;
    }

    private void allPermissionGranted() {
        setResult(PERMISSIONS_GRANTED); //所有权限已授权
        finish();
    }

    /**
     * 用户权限处理
     * 权限请求没有被拒绝则结束
     * 被拒绝则跳转到app的设置界面开启权限
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE && hasAllPermissionsGranted(grantResults)) {
            //所有权限请求均被允许
            isRequireCheck = true;
            allPermissionGranted();
        } else {
            isRequireCheck = false;
            showMissingPermissionDialog();
        }
    }

    // 含有全部的权限
    private boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    //权限缺失提示
    private void showMissingPermissionDialog() {
        new AlertDialog.Builder(PermissionsActivity.this).setTitle("注意")
                .setMessage(R.string.lacks_permission)
                .setNegativeButton("退出", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(PermissionsActivity.this, WelcomeActivity.class);
                        intent.putExtra(WelcomeActivity.EXIT_TAG, true);
                        setResult(WelcomeActivity.REQUEST_CODE, intent);
                        PermissionsActivity.this.finish();
                    }
                }).setPositiveButton("设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startAppSettings();
            }
        }).setCancelable(false).show();
    }

    // 启动应用的设置
    private void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse(PACKAGE_URL_SCHEME + getPackageName()));
        startActivity(intent);
    }
}
