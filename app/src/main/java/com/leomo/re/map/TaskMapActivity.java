package com.leomo.re.map;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.leomo.re.map.bean.TrackBean;
import com.leomo.re.map.service.LocationService;
import com.leomo.re.map.util.MapUtil;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by LeeToSun on 2017/7/25 0025
 * 轨迹
 */
public class TaskMapActivity extends AppCompatActivity implements LocationSource, AMap.OnCameraChangeListener,
                                                                  GeocodeSearch.OnGeocodeSearchListener, AMap.OnMapLoadedListener {

    /**
     * 视图注入
     */
    private MapView mapView;
    private ImageView location;

    /**
     * 高德地图所需变量定义
     */
    private AMap aMap;                                  // 定义AMap 地图对象的操作方法与接口。
    private OnLocationChangedListener mListener;        // 位置改变的监听接口。
    private UiSettings mUiSettings;                     // 设置用户界面的一个AMap，地图ui控制器
    private AMapLocation amapLocation;                  // 定位信息类。定位完成后的位置信息。
    private LatLonPoint mCenterLatLonPoint = null;      // MapView中央对于的屏幕坐标
    private GeocodeSearch geocodeSearch;                // 地理编码与逆地理编码类

    /**
     * 其他自定义地图变量
     */
    private boolean isFirstLoc = true;                  // 是否首次定位
    private double lat;                                 // 纬度
    private double lng;                                 // 经度

    /**
     * 常量定义
     */
    public static final float MAP_ZOOM = 16;            // 地图缩放等级

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_task_map_activity);
        mapView = (MapView) findViewById(R.id.map_view);
        location = (ImageView) findViewById(R.id.iv_location);
        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                isFirstLoc = true;
                Toast.makeText(TaskMapActivity.this, "正在定位中...", Toast.LENGTH_SHORT).show();
                startService(new Intent(TaskMapActivity.this, LocationService.class));
            }
        });
        // 透明状态栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.BLACK);
            window.setNavigationBarColor(Color.BLACK);
        }
        initView(savedInstanceState);
        netGetTrack();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 在activity执行onResume时执行mMapView. onResume ()，实现地图生命周期管理
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        // 在activity执行onPause时执行mMapView. onPause ()，实现地图生命周期管理
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        // 解绑广播
        if (mItemViewListClickReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mItemViewListClickReceiver);
        }
        stopService(new Intent(TaskMapActivity.this, LocationService.class));
    }

    /**
     * 监听地图定位
     */
    BroadcastReceiver mItemViewListClickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            amapLocation = intent.getParcelableExtra("location");
            if (mListener != null && amapLocation != null) {
                if (amapLocation.getErrorCode() == 0 && aMap != null) {
                    amapLocation.setAccuracy(0);
                    mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
                    if (isFirstLoc) {
                        isFirstLoc = false;
                        // 设置中心点
                        aMap.animateCamera(CameraUpdateFactory
                                .changeLatLng(new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude())));
                        aMap.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                        new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude()), MAP_ZOOM),
                                500, null);
                    }
                } else {
                    String errText = "定位失败," + amapLocation.getErrorCode() + ": " + amapLocation.getErrorInfo();
                    Log.e("AmapErr", errText);
                }
            }
        }
    };

    private void initView(Bundle savedInstanceState) {

        /**
         * 标题设置
         */
        setToolbar(">> 轨迹 <<", R.color.text_black);

        setLeftView("", R.drawable.ic_arrow_left_24, new OnClickListener() {
            @Override
            public void onNoDoubleClick(View v) {
                finish();
            }
        });

        // 在activity执行onCreate时执行mMapView.onCreate(savedInstanceState)，创建地图
        mapView.onCreate(savedInstanceState);

        /**
         * 初始化地图控制器对象, 添加相应配置
         */
        if (aMap == null) {
            aMap = mapView.getMap();
        }
        aMap.setLocationSource(this);// 通过aMap对象设置定位数据源的监听
        aMap.setOnCameraChangeListener(this);// 设置地图状态的监听接口
        aMap.setMyLocationEnabled(true);// 可触发定位并显示当前位置(设置为true表示启动显示定位蓝点)
        aMap.setOnMapLoadedListener(this);// 地图加载监听

        /**
         * 获取地图ui控制器
         */
        mUiSettings = aMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(false);// 是否允许显示缩放按钮
        mUiSettings.setMyLocationButtonEnabled(false); // 是否显示默认的定位按钮

        /**
         * 自定义小蓝点(当前位置)样式
         */
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationIcon(BitmapDescriptorFactory.fromResource(R.drawable.gps_point));// 设置小蓝点的图标
        myLocationStyle.strokeColor(Color.argb(0, 0, 0, 0));// 设置圆形的边框颜色
        myLocationStyle.radiusFillColor(Color.argb(0, 0, 0, 0));// 设置圆形的填充颜色
        myLocationStyle.strokeWidth(0f);// 设置圆形的边框粗细
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATE);// 定位的类型，设置为定位一次，且将视角移动到地图中心点
        aMap.setMyLocationStyle(myLocationStyle);

        /**
         * 地理编码与逆地理编码
         */
        geocodeSearch = new GeocodeSearch(this);
        geocodeSearch.setOnGeocodeSearchListener(this);

        /**
         * 注册广播
         */
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.location");
        LocalBroadcastManager.getInstance(this).registerReceiver(mItemViewListClickReceiver, intentFilter);

    }

    /**
     * 仅标题
     *
     * @param text            中间文字
     * @param centerTextColor 文字颜色
     */
    protected void setToolbar(String text, int centerTextColor) {
        TextView tv_center = (TextView) findViewById(R.id.tv_center);
        tv_center.setText(text);
        tv_center.setTextColor(this.getResources().getColor(centerTextColor));
    }

    /**
     * 左侧文字+左侧图标+自定义点击事件
     *
     * @param text     左侧文字
     * @param icon     左侧图标
     * @param listener 点击事件
     */
    protected void setLeftView(String text, int icon, OnClickListener listener) {
        TextView left = (TextView) findViewById(R.id.tv_left);
        left.setText(text);
        RelativeLayout rl_left_toolbar = (RelativeLayout) findViewById(R.id.rl_left_toolbar);
        if (icon != 0) {
            Drawable drawable = getResources().getDrawable(icon);
            drawable.setBounds(0, 0, drawable.getMinimumWidth(), drawable.getMinimumHeight()); // 设置边界
            left.setCompoundDrawables(drawable, null, null, null);// 画在左边
        }
        rl_left_toolbar.setOnClickListener(listener);
    }

    /**
     * 获取轨迹
     */
    public void netGetTrack() {
//        RequestCall call = OkHttpUtils.get().url("").build();
//        call.execute(new BaseCallBack() {
//            @Override
//            public void onResponse(String response, int id) {
//                super.onResponse(response, id);
                Gson gson = new Gson();
                Type type = new TypeToken<List<TrackBean>>() {
                }.getType();
                List<TrackBean> list = gson.fromJson(json, type);

                List<LatLng> allLatLng = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    if (null == list.get(i)) {
                        continue;
                    }
                    //设置轨迹起点
                    LatLng startLatLng = new LatLng(Double.valueOf(list.get(0).getLat()),
                            Double.valueOf(list.get(0).getLng()));
                    MapUtil.addMarkerOptions(aMap, startLatLng, getResources(), R.drawable.ic_beginning_point);


                    //设置轨迹终点
                    int last = list.size() - 1;
                    LatLng endLatLng = new LatLng(Double.valueOf(list.get(last).getLat()),
                            Double.valueOf(list.get(last).getLng()));
                    MapUtil.addMarkerOptions(aMap, endLatLng, getResources(), R.drawable.ic_finishing_point);

                    //轨迹线条
                    List<LatLng> latLngs = new ArrayList<>();
                    for (TrackBean listBean : list) {
                        latLngs.add(new LatLng(Double.valueOf(listBean.getLat()), Double.valueOf(listBean.getLng())));
                        allLatLng.add(new LatLng(Double.valueOf(listBean.getLat()), Double.valueOf(listBean.getLng())));
                    }
                    MapUtil.addPolyline(aMap, latLngs, 20, R.drawable.arrow_track);
                }

                //视角移动
                LatLngBounds.Builder builder = LatLngBounds.builder();

                for (int i = 0; i < allLatLng.size(); i++) {
                    builder.include(new LatLng(allLatLng.get(i).latitude, allLatLng.get(i).longitude));
                }
                aMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 5));
//            }
//        });

    }

    /**
     * 激活位置接口。
     * 定位程序将通过将此接口将主线程广播定位信息，直到用户关闭此通知。
     */
    @Override
    public void activate(OnLocationChangedListener onLocationChangedListener) {
        mListener = onLocationChangedListener;
    }

    /**
     * 处理定位更新的接口。
     */
    @Override
    public void deactivate() {
        mListener = null;
    }

    /**
     * 可视范围改变时回调此方法。
     * 这个方法必须在主线程中调用。
     */
    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    /**
     * 用户对地图做出一系列改变地图可视区域的操作（如拖动、动画滑动、缩放）完成之后回调此方法。
     * 这个方法必须在主线程中调用。
     */
    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        lat = cameraPosition.target.latitude;
        lng = cameraPosition.target.longitude;
        mCenterLatLonPoint = new LatLonPoint(lat, lng);
        // 第一个参数表示一个Latlng，第二参数表示范围多少米，第三个参数表示是火系坐标系还是GPS原生坐标系
        RegeocodeQuery query = new RegeocodeQuery(mCenterLatLonPoint, 200, GeocodeSearch.AMAP);
        geocodeSearch.getFromLocationAsyn(query);
    }

    /**
     * 根据给定的经纬度和最大结果数返回逆地理编码的结果列表。
     * 逆地理编码兴趣点返回结果最大返回数目为10，道路和交叉路口返回最大数目为3。
     */
    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {

    }

    /**
     * 根据给定的地理名称和查询城市，返回地理编码的结果列表。
     */
    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    @Override
    public void onMapLoaded() {
        // TODO 加载时
    }

    String json = "[\n" +
            "  {\n" +
            "    \"lng\": \"125.307361\",\n" +
            "    \"lat\": \"43.822424\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.307548\",\n" +
            "    \"lat\": \"43.822252\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.307766\",\n" +
            "    \"lat\": \"43.821808\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.307815\",\n" +
            "    \"lat\": \"43.821616\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.307815\",\n" +
            "    \"lat\": \"43.821616\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.308037\",\n" +
            "    \"lat\": \"43.821261\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.308147\",\n" +
            "    \"lat\": \"43.820993\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.308321\",\n" +
            "    \"lat\": \"43.820838\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.308321\",\n" +
            "    \"lat\": \"43.820838\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.309604\",\n" +
            "    \"lat\": \"43.820609\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.309351\",\n" +
            "    \"lat\": \"43.820266\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.308928\",\n" +
            "    \"lat\": \"43.819746\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.308532\",\n" +
            "    \"lat\": \"43.819378\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.308453\",\n" +
            "    \"lat\": \"43.819191\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.308343\",\n" +
            "    \"lat\": \"43.819065\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.30821\",\n" +
            "    \"lat\": \"43.81886\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.307869\",\n" +
            "    \"lat\": \"43.818321\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.307551\",\n" +
            "    \"lat\": \"43.818056\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.307685\",\n" +
            "    \"lat\": \"43.818061\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.30817\",\n" +
            "    \"lat\": \"43.817786\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.30817\",\n" +
            "    \"lat\": \"43.817786\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.308674\",\n" +
            "    \"lat\": \"43.817619\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.309454\",\n" +
            "    \"lat\": \"43.817393\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.309751\",\n" +
            "    \"lat\": \"43.817331\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.31021\",\n" +
            "    \"lat\": \"43.817129\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.310379\",\n" +
            "    \"lat\": \"43.817061\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.310747\",\n" +
            "    \"lat\": \"43.816971\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.311001\",\n" +
            "    \"lat\": \"43.816887\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.311791\",\n" +
            "    \"lat\": \"43.816925\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.311791\",\n" +
            "    \"lat\": \"43.816925\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.31199\",\n" +
            "    \"lat\": \"43.817263\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.312222\",\n" +
            "    \"lat\": \"43.817466\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.312236\",\n" +
            "    \"lat\": \"43.817587\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.312801\",\n" +
            "    \"lat\": \"43.818409\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"lng\": \"125.313181\",\n" +
            "    \"lat\": \"43.818873\"\n" +
            "  }\n" +
            "]";

}
