package com.example.demo;

import java.util.Date;

public class WeatherForecast
{
    public java.util.Date date;

    public int temperatureC;

    public int temperatureF;

    public String summary;

    @Override
    public String toString() {
        return "Weather forecast{" +
                "date=" + date.toString() +
                ", temperatureC='" + temperatureC + '\'' +
                ", temperatureF='" + temperatureF + '\'' +
                ", summary='" + summary + '\'' +
                '}';
    }
}