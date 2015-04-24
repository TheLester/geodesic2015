package com.dogar.geodesic.model;


public class NavDrawerItem {

    private String title;
    private int icon;
    private boolean isHeader;

    public NavDrawerItem() {
    }

    public NavDrawerItem(String title, int icon, boolean isHeader) {
        this.title = title;
        this.icon = icon;
        this.isHeader = isHeader;
    }

    public String getTitle() {
        return this.title;
    }

    public int getIcon() {
        return this.icon;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public boolean isHeader(){return isHeader;}
}
