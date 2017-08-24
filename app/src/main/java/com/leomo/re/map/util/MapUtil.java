package com.leomo.re.map.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.PolylineOptions;

import java.util.List;

/**
 * Created by LeeToSun on 2017/8/3 0003
 * 封装一些地图公共方法
 */
public class MapUtil {

    /**
     * 绘制一个点标记
     *
     * @param aMap   地图对象
     * @param latLng 点标记所在经纬度
     * @param res    绘制自定义标记的所需参数
     * @param iconId 自定义标记图片
     */
    public static Marker addMarkerOptions(AMap aMap, LatLng latLng, Resources res, int iconId) {
        MarkerOptions markerOption = new MarkerOptions();
        markerOption.position(latLng);
        //markerOption.title("西安市").snippet("西安市：34.341568, 108.940174");

        //markerOption.draggable(true);//设置Marker可拖动
        markerOption.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                .decodeResource(res, iconId)));
        // 将Marker设置为贴地显示，可以双指下拉地图查看效果
        //markerOption.setFlat(true);//设置marker平贴地图效果
        return aMap.addMarker(markerOption);
    }

    /**
     * 绘制一个可点击点标记
     *
     * @param aMap   地图对象
     * @param latLng 点标记所在经纬度
     */
    public static Marker addMarkerOptions(AMap aMap, LatLng latLng, View view) {
        MarkerOptions markerOption = new MarkerOptions();
        markerOption.position(latLng);
//        markerOption.title(title).snippet(snippet);

        //markerOption.draggable(true);//设置Marker可拖动
        markerOption.icon(BitmapDescriptorFactory.fromView(view));
        // 将Marker设置为贴地显示，可以双指下拉地图查看效果
        //markerOption.setFlat(true);//设置marker平贴地图效果
        return aMap.addMarker(markerOption);
    }

    /**
     * 绘制一个可点击点标记
     *
     * @param aMap   地图对象
     * @param latLng 点标记所在经纬度
     */
    public static Marker addMarkerOptions(AMap aMap, LatLng latLng, Bitmap bitmap) {
        MarkerOptions markerOption = new MarkerOptions();
        markerOption.position(latLng);
//        markerOption.title(title).snippet(snippet);

        //markerOption.draggable(true);//设置Marker可拖动
        markerOption.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
        // 将Marker设置为贴地显示，可以双指下拉地图查看效果
        //markerOption.setFlat(true);//设置marker平贴地图效果
        return aMap.addMarker(markerOption);
    }

    /**
     * 绘制轨迹
     *
     * @param aMap       地图对象
     * @param latLngList 经纬度列表
     * @param width      轨迹宽度
     * @param context    上下文
     * @param color      轨迹线颜色
     */
    public static void addPolyline(AMap aMap, List<LatLng> latLngList, float width, Context context, int color) {
        aMap.addPolyline(new PolylineOptions().
                addAll(latLngList).width(width).color(ContextCompat.getColor(context, color)));
    }

}
