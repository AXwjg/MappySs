package mappyss.maphive.io.mappyss;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapBaseIndoorMapInfo;
import com.baidu.mapapi.map.MapBaseIndoorMapInfo.SwitchFloorError;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.UiSettings;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.CityInfo;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * oldWang
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MapView mMapView;
    private UiSettings mUiSettings;
    private BaiduMap mBaiduMap;
    private PoiSearch mPoiSearch;
    private MapBaseIndoorMapInfo mMapBaseIndoorMapInfo;

    private EditText mEtSearch;
    private ImageView mIvDeleted;
    private EditText mEtSearchCity;
    private RecyclerView mRecyclerView;
    private StripListView stripListView;                // 楼层切换器

    private SearchAdapter searchAdapter;                // 搜索POI列表
    private BaseStripAdapter mFloorListAdapter;         // 楼层切换器适配器

    private Context mContext;
    private InputMethodManager inputMethodManager;      // 键盘

    private boolean isFocus = false;                    // 是否显示个性化地图
    private PoiBean currentPoiBean;                     // 当前的建筑信息
    private String currentFocusedID;                    // 当前到建筑ID
    private int currentFloorIndex = 0;                  // 当前楼层到索引值

    // 对话框内容
    private String buildingName = null;
    private String country = null;
    private String city = null;
    private String buildingId = null;
    private boolean isSs = false;

    private List<PoiBean> mPoiBeans = new ArrayList<>(); // 搜索到到POI
    private List<String> floorList = new ArrayList<>(); // 原始楼层名字
    private List<String> diyFloors = new ArrayList<>(); // 修改后到楼层

    public static int DEFAULT_POINT_1_X = 300;
    public static int DEFAULT_POINT_1_Y = 500;
    public static int DEFAULT_POINT_2_X = 800;
    public static int DEFAULT_POINT_2_Y = 1000;

    List<MPoint> mPoints = new ArrayList<>();   // 实例化
    MImage mimage = new MImage();

    private double midlat = 0f;     // 中间点
    private double midlon = 0f;

    private final int THE_FIRST = 0;
    private final int SUCCESS = 1;

    private long curTime;

    private String TAG = "MainActivity";

    private String dirName = Environment.getExternalStorageDirectory().toString() + "/" + "baidushot" + "/";

    private final static String PATH = "custom_config_dark.json";

//    public static final String BASE_SERVER = "http://lbscutmap.cherishi.com:8080";
    public static final String BASE_SERVER = "http://10.0.10.48:8080";

    public static String IMAGEURL = BASE_SERVER + "/uploadImage";
    public static String ATTACHURL = BASE_SERVER + "/uploadAttach";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 注意该方法要再setContentView方法之前实现  样式地图
        MapView.setCustomMapStylePath(StringUtils.setMapCustomFile(this, PATH));
        SDKInitializer.initialize(getApplicationContext());
//        SDKInitializer.setCoordType(CoordType.GCJ02); // 转换成国测坐标
        setContentView(R.layout.activity_main);

        mContext = this;
        MapView.setMapCustomEnable(false);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        initView();
        initEvent();


    }

    private void initView() {
        RelativeLayout allView = findViewById(R.id.rl_all);
        //获取地图控件引用
        mMapView = findViewById(R.id.bmapView);
        mEtSearch = findViewById(R.id.et_search);
        mIvDeleted = findViewById(R.id.iv_deleted);
        mEtSearchCity = findViewById(R.id.et_search_city);
        mRecyclerView = findViewById(R.id.rl_search);
        stripListView = new StripListView(mContext);
        allView.addView(stripListView);


        stripListView.bringToFront();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.VERTICAL, false));

        searchAdapter = new SearchAdapter(mContext, mPoiBeans);
        mRecyclerView.setAdapter(searchAdapter);

        RelativeLayout rl = findViewById(R.id.search_rl);
        rl.bringToFront();
        mRecyclerView.bringToFront();

        mBaiduMap = mMapView.getMap();
        mUiSettings = mBaiduMap.getUiSettings();

        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL); // 2D地图
        mBaiduMap.setIndoorEnable(true);                // 打开室内地图
        mMapView.showScaleControl(false);            // 比例尺关闭
        mMapView.showZoomControls(false);            // 缩放按钮
        mUiSettings.setCompassEnabled(true);            // 指南针
        mUiSettings.setOverlookingGesturesEnabled(false);      // 地图俯视（3D）
        mBaiduMap.setCompassPosition(new Point(100, 300));

        //23.0393805243,113.1942142084
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(23.0393805243, 113.1942142084)));
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(18));

        mFloorListAdapter = new BaseStripAdapter(mContext);
        initGooglePlaces();
    }

    private void initGooglePlaces() {
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        LinearLayout ll = findViewById(R.id.ll);
        ll.bringToFront();
        ll.setVisibility(View.GONE);
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                if (place == null) {
                    return;
                }
                LatLng latLng = new LatLng(place.getLatLng().latitude, place.getLatLng().longitude);
                LatLng tolatLng = new CoordinateConverter().from(CoordinateConverter.CoordType.COMMON).coord(latLng).convert();

                currentPoiBean = new PoiBean(place.getName().toString(), place.getAddress().toString(), tolatLng);
                mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(tolatLng.latitude, tolatLng.longitude)));
                mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(19));

            }

            @Override
            public void onError(Status status) {
                Log.e("----google place error", "An error occurred: " + status);
            }
        });
    }

    private void initEvent() {
        EventBus.getDefault().register(this);
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(poiListener);

        mEtSearch.addTextChangedListener(mSearchEditTextWatcher);
        mIvDeleted.setOnClickListener(this);
        findViewById(R.id.focus_btn).setOnClickListener(this);
        findViewById(R.id.screenshot_btn).setOnClickListener(this);

        mEtSearch.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    Log.i("点击EditText","--");
                } else {
                    if (inputMethodManager.isActive()) {
                        inputMethodManager.hideSoftInputFromWindow(mEtSearch.getWindowToken(), 0);//隐藏
                    }
                    mEtSearch.setText("");
                    searchAdapter.setmPoiBeanList(new ArrayList<PoiBean>());
                }
            }
        });

        // 进入室内地图监听
        mBaiduMap.setOnBaseIndoorMapListener(new BaiduMap.OnBaseIndoorMapListener() {
            @Override
            public void onBaseIndoorMapMode(boolean b, MapBaseIndoorMapInfo mapBaseIndoorMapInfo) {
                if (b && mapBaseIndoorMapInfo != null) {
                    if (mMapBaseIndoorMapInfo != null && !TextUtils.isEmpty(mMapBaseIndoorMapInfo.getID())
                                    && mMapBaseIndoorMapInfo.getID().equals(mapBaseIndoorMapInfo.getID())) {
                        if (stripListView.getVisibility() != View.VISIBLE) {
                            stripListView.setVisibility(View.VISIBLE);
                        }
                        return;
                    }
                    mFloorListAdapter.setmFloorList(mapBaseIndoorMapInfo.getFloors());
                    Log.i("----getFloors", mapBaseIndoorMapInfo.getFloors().toString());
                    stripListView.setVisibility(View.VISIBLE);
                    stripListView.setStripAdapter(mFloorListAdapter);
                    mMapBaseIndoorMapInfo = mapBaseIndoorMapInfo;
                    // 进入室内图
                    currentFocusedID = mMapBaseIndoorMapInfo.getID();
                    floorList = mMapBaseIndoorMapInfo.getFloors();
                    SwitchFloorError switchFloorError = mBaiduMap.switchBaseIndoorMapFloor(floorList.get(0), currentFocusedID);
                    if (switchFloorError.toString().equals(SwitchFloorError.SWITCH_OK.toString())) {
                        mFloorListAdapter.setSelectedPostion(floorList.size() - 1 - currentFloorIndex);
                        mFloorListAdapter.notifyDataSetChanged();
                    }
                } else {
                    // 移除室内图
                    stripListView.setVisibility(View.GONE);
                }
            }
        });

        // 楼层切换器的监听
        stripListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                if (mMapBaseIndoorMapInfo == null) {
                    return;
                }
                String floor = (String) mFloorListAdapter.getItem(position);
                mBaiduMap.switchBaseIndoorMapFloor(floor, mMapBaseIndoorMapInfo.getID());
                mFloorListAdapter.setSelectedPostion(position);
                mFloorListAdapter.notifyDataSetInvalidated();
            }
        });

        // 地图改变监听，方便获取地图中心位置
        mBaiduMap.setOnMapStatusChangeListener(onMapStatusChangeListener);

    }

    // Poi搜索结果监听
    OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener() {
        @Override
        public void onGetPoiResult(PoiResult poiResult) {
            if (poiResult == null || poiResult.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
                return;
            }
            // 获取POI检索结果
            mPoiBeans = new ArrayList<>();
            if (null != poiResult.getAllPoi()) {
                for (PoiInfo poiInfo : poiResult.getAllPoi()) {
                    PoiBean poiBean = new PoiBean(poiInfo.name, poiInfo.address, poiInfo.location);
                    mPoiBeans.add(poiBean);
                }
                mRecyclerView.setVisibility(View.VISIBLE);
                searchAdapter.setmPoiBeanList(mPoiBeans);
            } else {
                if (poiResult.getSuggestCityList() != null) {
                    for (CityInfo cityInfo : poiResult.getSuggestCityList()) {
                        Log.i("搜索结果 ", "百度本市搜索无结果" + cityInfo.city);
                    }
                } else {
                    Log.i("搜索结果 ", "百度搜索无结果");
                }
            }
        }

        @Override
        public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {
            if (poiDetailResult.error != SearchResult.ERRORNO.NO_ERROR) {
                showToast("抱歉，未找到结果");
            } else {
                showToast("poiDetailResult.getName() + \": \" + poiDetailResult.getAddress()");
            }

        }

        @Override
        public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {
        }
    };

    // 地图状态改变
    BaiduMap.OnMapStatusChangeListener onMapStatusChangeListener = new BaiduMap.OnMapStatusChangeListener() {
        @Override
        public void onMapStatusChangeStart(MapStatus mapStatus) {

        }

        @Override
        public void onMapStatusChangeStart(MapStatus mapStatus, int i) {

        }

        @Override
        public void onMapStatusChange(MapStatus mapStatus) {

        }

        @Override
        public void onMapStatusChangeFinish(MapStatus mapStatus) {
            LatLng mCenterLatLng = mapStatus.target;
//            double[] doubles = CoordinateTransformUtil.gcj02towgs84(mCenterLatLng.longitude, mCenterLatLng.latitude);
//            midlon = doubles[1];
//            midlat = doubles[0];
            double[] doubles = CoordinateTransformUtil.bd09towgs84(mCenterLatLng.longitude, mCenterLatLng.latitude);
            midlon = doubles[1];
            midlat = doubles[0];
//            midlon = mCenterLatLng.longitude;
//            midlat = mCenterLatLng.latitude;

        }
    };

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventPoi(PoiBean poiBean) {
        // 搜索结果点击返回
        currentPoiBean = poiBean;
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newLatLng(new LatLng(poiBean.location.latitude, poiBean.location.longitude)));
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.zoomTo(20));
        mRecyclerView.setVisibility(View.GONE);
        mEtSearchCity.clearFocus();
        mEtSearch.clearFocus();

        currentFocusedID = mMapBaseIndoorMapInfo.getID();
        floorList = mMapBaseIndoorMapInfo.getFloors();

    }

    private TextWatcher mSearchEditTextWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            // 搜索的城市
            String searchCity = mEtSearchCity.getText().toString().trim();
            // 输入关键字搜索
            String search = mEtSearch.getText().toString().trim();
            if (search.equals("")) {
                mRecyclerView.setVisibility(View.GONE);
            }
            mPoiSearch.searchInCity((new PoiCitySearchOption())
                    .city(searchCity)
                    .keyword(search)
                    .pageNum(0));
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            Rect poiList = new Rect();
            mRecyclerView.getGlobalVisibleRect(poiList);
            if (!((mRecyclerView.getVisibility() == View.VISIBLE
                    && poiList.contains((int) ev.getRawX(), (int) ev.getRawY())))) {
                View v = getCurrentFocus();
                if (StringUtils.isShouldHideInput(v, ev)) {
                    if (inputMethodManager != null) {
                        inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    mRecyclerView.setVisibility(View.GONE);
                }


            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //在activity执行onDestroy时执行mMapView.onDestroy()，实现地图生命周期管理
        mPoiSearch.destroy();
        mMapView.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mMapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mMapView.onPause();
    }

    @Override
    public void onClick(View view) {
        if (isSs) {
            showToast("正在截图...");
            return;
        }
        switch (view.getId()) {
            case R.id.iv_deleted:   // 删除输入
                mEtSearch.setText("");
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);//隐藏
                break;

            case R.id.focus_btn:    // 个性化地图开启
                if (isFocus) {
                    isShowOtherStyle(false);
                } else {
                    isShowOtherStyle(true);
                }
                break;

            case R.id.screenshot_btn:   // 截图
                if (currentPoiBean == null || floorList == null) {
                    showToast("请先搜索位置!");
                    return;
                }
                //截图前先将其他的影响的style去掉
                isShowOtherStyle(true);
                LayoutInflater factory = LayoutInflater.from(MainActivity.this);
                final View textEntryView = factory.inflate(R.layout.input_screenshot, null);
                final TextView editTextBuilding = textEntryView.findViewById(R.id.editTextBuilding);
                final TextView editTextCountry = textEntryView.findViewById(R.id.editTextCountry);
                final TextView editTextCity = textEntryView.findViewById(R.id.editTextCity);
                final EditText editTextFloors = textEntryView.findViewById(R.id.floor_et);
                StringBuilder diyBuilder = new StringBuilder();
                for (int y = 0; y < floorList.size(); y++) {
                    diyBuilder.append(floorList.get(y));
                    if (y != floorList.size()-1){
                        diyBuilder.append(",");
                    }
                }
                editTextFloors.setText(diyBuilder.toString());
                final AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                editTextBuilding.setText(currentPoiBean.getName());
                dialog.setTitle("截图信息确认");
                dialog.setView(textEntryView);
                dialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        if (editTextBuilding.getText().toString().equals("")) {
                            new AlertDialog.Builder(MainActivity.this).setTitle("请输入buildingName").setPositiveButton("确定", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            }).show();
                            return;
                        } else {
                            buildingName = editTextBuilding.getText().toString();
                        }
                        isSs = true;
                        country = editTextCountry.getText().toString();
                        city = editTextCity.getText().toString();
                        // 当不修改时获取默认值
                        if (country.equals("")) {
                            country = getResources().getString(R.string.default_country);
                        }
                        if (city.equals("")) {
                            city = getResources().getString(R.string.default_city);
                        }

                        buildingId = StringUtils.generateBuildingId(buildingName, city);
                        diyFloors = StringUtils.splitString(editTextFloors.getText().toString().trim());
                        curTime = System.currentTimeMillis();
                        Log.i("DiyFloors", diyFloors.toString());

                        dialogInterface.dismiss();
                        View view = MainActivity.this.getCurrentFocus();
                        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);//隐藏

                        SwitchFloorError switchFloorError = mBaiduMap.switchBaseIndoorMapFloor(floorList.get(currentFloorIndex), currentFocusedID);
                        mMapBaseIndoorMapInfo = mBaiduMap.getFocusedBaseIndoorMapInfo();
                        mFloorListAdapter.setSelectedPostion(floorList.size() -1 - currentFloorIndex);
                        mFloorListAdapter.notifyDataSetChanged();

                        if (switchFloorError.toString().equals(SwitchFloorError.SWITCH_OK.toString())) {
                            Message message = Message.obtain();
                            message.what = THE_FIRST;
                            message.obj = "null";
                            handler.sendMessageDelayed(message, 3000);
                        } else if (switchFloorError.toString().equals(SwitchFloorError.FLOOR_INFO_ERROR.toString())) {
                            showToast("室内ID信息错误");
                        } else if (switchFloorError.toString().equals(SwitchFloorError.FLOOR_OVERLFLOW.toString())) {
                            showToast("即当前室内图不存在该楼层");
                        } else if (switchFloorError.toString().equals(SwitchFloorError.FOCUSED_ID_ERROR.toString())) {
                            showToast("切换楼层室内ID与当前聚焦室内ID不匹配");
                        } else if (switchFloorError.toString().equals(SwitchFloorError.SWITCH_ERROR.toString())) {
                            showToast("楼层切换失败，");
                        } else {
                            showToast("截图失败！");
                        }

                    }
                });
                dialog.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                dialog.setCancelable(false);
                dialog.show();

                break;

            default:
                break;
        }
    }

    /** 截图
     *
     * @param floor 截图截的当前的楼
     * */
    private void screenshot(final String floor) {
        // 截图，在SnapshotReadyCallback中保存图片到 sd 卡
        mBaiduMap.snapshot(new BaiduMap.SnapshotReadyCallback() {
            public void onSnapshotReady(Bitmap snapshot) {
                File dir = new File(dirName);
                // 判断文件夹是否存在，不存在则创建
                if (!dir.exists()) {
                    dir.mkdir();
                }
                String bName = buildingName.replace(" ", "#s#");
                String fileFolderPath = dirName + bName + "/";
                File buildingDir = new File(fileFolderPath);
                // 判断文件夹是否存在，不存在则创建
                if (!buildingDir.exists()) {
                    buildingDir.mkdir();
                }
                final File file = new File(fileFolderPath + StringUtils.removeSpace(bName, floor, curTime) + ".png");
                FileOutputStream out;
                try {
                    out = new FileOutputStream(file);
                    if (snapshot.compress(
                            Bitmap.CompressFormat.PNG, 100, out)) {
                        out.flush();
                        out.close();
                    }
                    uploadFile(fileFolderPath, file.getName(), "image");
                    Log.i("snapshot SUCCESS file", file.toString());
                    currentFloorIndex++;
                    String cfname = StringUtils.removeSpace(bName, floor, curTime) + ".json";
                    // 初始Json数据
                    buildConfigureFile(fileFolderPath, cfname, snapshot.getWidth(), snapshot.getHeight());
                    uploadFile(fileFolderPath, cfname, "attach");
                    SwitchFloorError switchFloorError;
                    if (currentFloorIndex >= floorList.size()) {
                        // 最后一次层楼层
                        currentFloorIndex = 0;
                        switchFloorError =
                                mBaiduMap.switchBaseIndoorMapFloor(floorList.get(floorList.size() -1), mMapBaseIndoorMapInfo.getID());
                        mMapBaseIndoorMapInfo = mBaiduMap.getFocusedBaseIndoorMapInfo();
                    } else {
                        switchFloorError =
                                mBaiduMap.switchBaseIndoorMapFloor(floorList.get(currentFloorIndex), mMapBaseIndoorMapInfo.getID());
                        mMapBaseIndoorMapInfo = mBaiduMap.getFocusedBaseIndoorMapInfo();
                        mFloorListAdapter.setSelectedPostion(floorList.size() - 1 - currentFloorIndex);
                        mFloorListAdapter.notifyDataSetChanged();
                    }
                    if (switchFloorError.toString().equals(SwitchFloorError.SWITCH_OK.toString())) {
                        Message message = Message.obtain();
                        message.what = SUCCESS;
                        message.obj = mMapBaseIndoorMapInfo.getCurFloor();
                        handler.sendMessageDelayed(message, 3000);
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        showToast("正在截取屏幕图片...");

    }

    private void isShowOtherStyle(boolean isShow) {
        if (isShow) {
            mUiSettings.setRotateGesturesEnabled(false);
            MapView.setMapCustomEnable(true);
            mBaiduMap.showMapPoi(false);
        } else {
            mUiSettings.setRotateGesturesEnabled(true);
            MapView.setMapCustomEnable(false);
            mBaiduMap.showMapPoi(true);
        }
        isFocus = !isFocus;
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SUCCESS:
                    if (currentFloorIndex != 0) {
//                        mFloorListAdapter.setSelectedPostion(floorList.size() - 1 - currentFloorIndex);
//                        mFloorListAdapter.notifyDataSetChanged();
                        screenshot(diyFloors.get(currentFloorIndex));
                    } else {
                        isSs = false;
                        showToast("完成截图");
                        Log.i("截图完成","-");
                    }
                    break;

                case THE_FIRST:
                    mMapBaseIndoorMapInfo = mBaiduMap.getFocusedBaseIndoorMapInfo();
                    Log.e("switchFloor 第一次", mMapBaseIndoorMapInfo.getCurFloor());
                    screenshot(diyFloors.get(currentFloorIndex));

                    break;
                default:
                    break;
            }
        }
    };

    private void buildConfigureFile(String dirName, String fileName, int width, int height) throws IOException {
        Point point1 = new Point(DEFAULT_POINT_1_X, DEFAULT_POINT_1_Y);
        Point point2 = new Point(DEFAULT_POINT_2_X, DEFAULT_POINT_2_Y);

        LatLng latLng1 = mBaiduMap.getProjection().fromScreenLocation(point1);
        LatLng latLng2 = mBaiduMap.getProjection().fromScreenLocation(point2);
//        double[] doubles1 = CoordinateTransformUtil.gcj02towgs84(latLng1.longitude, latLng1.latitude);
//        double[] doubles2 = CoordinateTransformUtil.gcj02towgs84(latLng2.longitude, latLng2.latitude);
        // 写入全局变量
        double[] doubles1 = CoordinateTransformUtil.bd09towgs84(latLng1.longitude, latLng1.latitude);
        double[] doubles2 = CoordinateTransformUtil.bd09towgs84(latLng2.longitude, latLng2.latitude);
        MPoint mpoint1 = new MPoint();
        mpoint1.setX(DEFAULT_POINT_1_X);
        mpoint1.setY(DEFAULT_POINT_1_Y);
        mpoint1.setLat(doubles1[1]);
        mpoint1.setLon(doubles1[0]);
//        mpoint1.setLat(latLng1.latitude);
//        mpoint1.setLon(latLng1.longitude);

        MPoint mpoint2 = new MPoint();
        mpoint2.setX(DEFAULT_POINT_2_X);
        mpoint2.setY(DEFAULT_POINT_2_Y);
        mpoint2.setLat(doubles2[1]);
        mpoint2.setLon(doubles2[0]);
//        mpoint2.setLat(latLng2.latitude);
//        mpoint2.setLon(latLng2.longitude);
        mPoints.clear();
        mPoints.add(mpoint1);
        mPoints.add(mpoint2);

        mimage.setHeight(height);
        mimage.setWidth(width);

        if (Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED)) {
            File dir = new File(dirName);
            // 判断文件夹是否存在，不存在则创建
            if (!dir.exists()) {
                dir.mkdir();
            }

            File file = new File(dirName + fileName);
            // 判断文件是否存在，不存在则创建
            if (!file.exists()) {
                file.createNewFile();
            }

            try {
                FileWriter fileWriter = new FileWriter(file, true);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                // 写Json文件
                bufferedWriter.write(writeJson().toString());
                bufferedWriter.close();
                fileWriter.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    //生成 attach.json字段
    private JSONObject writeJson() throws Exception {
        JSONObject jsonObject = new JSONObject();
        JSONArray jPoints = new JSONArray();
        for (MPoint mPoint : mPoints) {
            JSONObject jPoint = new JSONObject();
            jPoint.put("x", mPoint.getX());
            jPoint.put("y", mPoint.getY());
            jPoint.put("lat", mPoint.getLat());
            jPoint.put("lon", mPoint.getLon());
            jPoints.put(jPoint);
        }

        jsonObject.put("point", jPoints);
        JSONObject jImage = new JSONObject();
        jImage.put("width", mimage.getWidth());
        jImage.put("height", mimage.getHeight());
        jsonObject.put("image", jImage);

        JSONArray jFloors = new JSONArray();
        for (int i = 0; i < diyFloors.size(); i++)
            jFloors.put(diyFloors.get(i));
        jsonObject.put("floor", jFloors);

        JSONObject jAddr = new JSONObject();
        jAddr.put("country", country);
        jAddr.put("city", city);
        jsonObject.put("addr", jAddr);

        JSONObject jMidlatlon = new JSONObject();
        jMidlatlon.put("midlat", midlat);
        jMidlatlon.put("midlon", midlon);
        jsonObject.put("midlatlon", jMidlatlon);

        jsonObject.put("buildingName", buildingName);
        jsonObject.put("buildingId", buildingId);
        jsonObject.put("mapType", "BAIDU");
        return jsonObject;
    }

    /**
     * @param dirName       文件夹根目录
     * @param fileName      上传文件名
     * @param type     "image" or "attach"
     */
    private void uploadFile(final String dirName, final String fileName, final String type) {
        try {
            //得到文件的目录
            final File uploadFile = new File(dirName + fileName);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (type.equals("image")) {
                            UploadUtil.uploadFile(uploadFile, IMAGEURL);
                        } else {
                            UploadUtil.uploadFile(uploadFile, ATTACHURL);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "upload file error" + fileName);
                        Log.e(TAG, e.toString());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showToast("Upload file error: " + fileName);
                            }
                        });
//                        isHandling = false;
                    }
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "read file error when upload" + fileName);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    showToast("read file error when upload: " + fileName);
                }
            });
            Log.e(TAG, e.toString());
//            isHandling = false;
        }
    }

    private Toast toast;

    public void showToast(String str) {
        if (toast == null) {
            toast = Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT);
        } else {
            toast.setText(str);
            toast.setDuration(Toast.LENGTH_SHORT);
        }
        toast.show();
    }

    private long exitTime = 0;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)) {
            if ((System.currentTimeMillis() - exitTime) > 2000) {
                showToast("再次点击退出");
                exitTime = System.currentTimeMillis();
            } else {
                finish();
                System.exit(0);
            }
            return false;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            showToast("横屏模式");
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
            showToast("竖屏模式");
        }
    }

}
