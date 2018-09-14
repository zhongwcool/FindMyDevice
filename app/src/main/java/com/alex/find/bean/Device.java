package com.alex.find.bean;

import java.io.Serializable;

/**
 * Created by Alex on 2017/4/11.
 */

public class Device implements Serializable {
    public String ip;
    public String sn;
    public String version;
    public String content;
    public String details;
    private String detail_base = "SN:%s Version: v%s";
    private String content_base = "Device [%s]";
    private String status;

    public Device(String device_ip, String device_sn, String firmware_version) {
        ip = device_ip;
        sn = device_sn;
        version = firmware_version;
        this.content = String.format(content_base, ip);
        this.details = String.format(detail_base, sn, version);
        this.status = "播放";
    }

    public Device(String remove_ip) {
        ip = remove_ip;
        sn = "";
        version = "";
        this.content = String.format(content_base, ip);
        this.details = String.format(detail_base, sn, version);
        this.status = "播放";
    }

    public static Device createDummy() {
        return new Device("192.168.5.173", "SN1234567", "1.0.0");
    }

    @Override
    public String toString() {
        return content;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String stat) {
        this.status = stat;
    }

    public void reset(Device dev) {
        ip = dev.ip;
        sn = dev.sn;
        version = dev.version;
        this.content = String.format(content_base, ip);
        this.details = String.format(detail_base, sn, version);
        this.status = "播放";
    }
}
