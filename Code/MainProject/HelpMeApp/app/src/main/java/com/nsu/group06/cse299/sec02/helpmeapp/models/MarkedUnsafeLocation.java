package com.nsu.group06.cse299.sec02.helpmeapp.models;

import com.nsu.group06.cse299.sec02.helpmeapp.utils.TimeUtils;

import java.util.ArrayList;

/**
 * model class for unsafe locations shown in "FindSafePlacesActivity" class
 */
public class MarkedUnsafeLocation {

    // minimum distance (in meters) for HelpPost to be grouped together into a MarkedUnsafeLocation
    private static final double MINIMUM_DISTANCE = 100.00d;

    private Double latitude, longitude;
    private String description;
    private String beginTimeInterval, endTimeInterval; // format- 10 AM

    private ArrayList<HelpPost> mHelpPosts = new ArrayList<>();

    public MarkedUnsafeLocation() {
    }

    public MarkedUnsafeLocation(String description) {
        this.description = description;
    }

    public MarkedUnsafeLocation(HelpPost helpPost) {

        this.latitude = helpPost.getLatitude();
        this.longitude = helpPost.getLongitude();
        this.beginTimeInterval = TimeUtils.getHourFromTimeStamp(helpPost.getTimeStamp());
        this.endTimeInterval = TimeUtils.getHourFromTimeStamp(helpPost.getTimeStamp());
        this.description = "1 help post found nearby";
        mHelpPosts.add(helpPost);
    }

    public boolean belongsToMarkedUnsafeLocation(HelpPost helpPost) {

        if(latitude==null || longitude==null) return true;

        return distanceBetween(latitude, helpPost.getLatitude(), longitude, helpPost.getLongitude()) <= MINIMUM_DISTANCE;
    }

    public boolean hasHelpPost(HelpPost helpPost) {

        for (HelpPost h: mHelpPosts) {

            if(h.getPostId().equals(helpPost.getPostId())) return true;
        }

        return false;
    }

    public void addHelpPostToMarkedUnsafeLocation(HelpPost helpPost) {

        if(latitude==null || longitude==null){

            latitude = helpPost.getLatitude();
            longitude = helpPost.getLongitude();

            beginTimeInterval = TimeUtils.getHourFromTimeStamp(helpPost.getTimeStamp());
            endTimeInterval = TimeUtils.getHourFromTimeStamp(helpPost.getTimeStamp());

            description = "1 help post found nearby";

            mHelpPosts.add(helpPost);

            return;
        }

        else if(!belongsToMarkedUnsafeLocation(helpPost)){

            return;
        }

        int helpPostHour = getHourTimeAsInteger(TimeUtils.getHourFromTimeStamp(helpPost.getTimeStamp()));

        if(helpPostHour < getHourTimeAsInteger(beginTimeInterval)) {
            beginTimeInterval = getFormattedHour(helpPostHour);
        }

        if(helpPostHour > getHourTimeAsInteger(endTimeInterval)) {
            endTimeInterval = getFormattedHour(helpPostHour);
        }

        mHelpPosts.add(helpPost);

        description = mHelpPosts.size() + " help posts found nearby";
    }

    public void removeHelpPost(HelpPost helpPost) {

        int removeIdx = -1;

        for(HelpPost hp : mHelpPosts){

            removeIdx++;
            if(hp.getPostId().equals(helpPost.getPostId()))
                break;
        }

        if(removeIdx>=mHelpPosts.size()) return;

        mHelpPosts.remove(removeIdx);
    }

    /*
    Returns distance in meters between two latLng points
    courtesy- <https://www.geeksforgeeks.org/program-distance-two-points-earth/>
     */
    private static double distanceBetween(double lat1, double lat2, double lon1, double lon2)
    {

        // The math module contains a function
        // named toRadians which converts from
        // degrees to radians.
        lon1 = Math.toRadians(lon1);
        lon2 = Math.toRadians(lon2);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        // Haversine formula
        double dlon = lon2 - lon1;
        double dlat = lat2 - lat1;
        double a = Math.pow(Math.sin(dlat / 2), 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dlon / 2),2);

        double c = 2 * Math.asin(Math.sqrt(a));

        // Radius of earth in meters.
        double r = 6371*1000;

        // calculate the result
        return(c * r);
    }

    /**
     * get integer hour from formatted hour string
     * @param hourTime format- '10 PM'
     * @return integer value of hour '10 PM' -> 22
     */
    private int getHourTimeAsInteger(String hourTime) {

        try {

            String[] parts = beginTimeInterval.split(" ");

            int hour = Integer.parseInt(parts[0]);

            if(parts[1].equals("PM") && hour!=12) hour-=12;

            return hour;

        } catch (Exception e){
            return 0;
        }
    }

    /**
     * get formatted string hour from integer hour
     * @param hour integer hour 0-24
     * @return formatted hour 22 -> '10 PM'
     */
    private String getFormattedHour(int hour) {

        if(hour>=12){
            if(hour>12) hour-=12;

            return hour + " PM";
        }

        else return hour+" AM";
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBeginTimeInterval() {
        return beginTimeInterval;
    }

    public void setBeginTimeInterval(String beginTimeInterval) {
        this.beginTimeInterval = beginTimeInterval;
    }

    public String getEndTimeInterval() {
        return endTimeInterval;
    }

    public void setEndTimeInterval(String endTimeInterval) {
        this.endTimeInterval = endTimeInterval;
    }

    public ArrayList<HelpPost> getmHelpPosts() {
        return mHelpPosts;
    }
}
