package com.mshop.web.api.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AdminOrderListRequest {
    private String status;
    private String search;
    private int days = 30;
    private int page = 0;
    private int size = 10;
    private String sort = "createdAt,desc";
}
