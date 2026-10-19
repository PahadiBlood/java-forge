package com.mshop.web.api.dto;

public class DashboardSummaryRequest {
    private int days = 30;

    public int getDays() {
        return days;
    }

    public void setDays(int days) {
        this.days = days;
    }
}
