package com.mshop.web.api.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DashboardSummaryRequest {
    private int days = 30;
}
