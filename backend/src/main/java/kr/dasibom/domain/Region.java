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
    private Double latitude;
    private Double longitude;
    private String anchorPlace;
    private String populationStatus;
    private boolean halfPrice;
    protected Region() {}
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getAreaCode() { return areaCode; }
    public String getAreaName() { return areaName; }
    public String getTagline() { return tagline; }
    public String getTheme() { return theme; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public String getAnchorPlace() { return anchorPlace; }
    public String getPopulationStatus() { return populationStatus; }
    public boolean isHalfPrice() { return halfPrice; }
}
