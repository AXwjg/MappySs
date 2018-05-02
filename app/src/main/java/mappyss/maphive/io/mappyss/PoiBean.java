package mappyss.maphive.io.mappyss;

import com.baidu.mapapi.model.LatLng;

/**
 * Created by oldwang on 2018/4/9.
 *
 */

public class PoiBean {

    private String name;

    private String address;

    public LatLng location;

    public PoiBean(String name, String address, LatLng location) {
        this.name = name;
        this.address = address;
        this.location = location;
    }

    public PoiBean(String name, String address) {
        this.name = name;
        this.address = address;
    }

    public PoiBean() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LatLng getLocation() {
        return location;
    }

    public void setLocation(LatLng location) {
        this.location = location;
    }
}
