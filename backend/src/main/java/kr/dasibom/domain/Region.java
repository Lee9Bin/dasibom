package kr.dasibom.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "regions")
public class Region {
    @Id private String code;
    private String name;
    private String areaCode;
    private String areaName;
    private String tagline;
    private String theme;
    private double latitude;
    private double longitude;
    private String anchorPlace;
    protected Region() {}
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getAreaCode() { return areaCode; }
    public String getAreaName() { return areaName; }
    public String getTagline() { return tagline; }
    public String getTheme() { return theme; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public String getAnchorPlace() { return anchorPlace; }
}

