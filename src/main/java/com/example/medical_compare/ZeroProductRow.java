package com.example.medical_compare;

import java.util.LinkedHashMap;
import java.util.Map;

public class ZeroProductRow {

    private String name;

    // key = warehouse, value = discount
    private Map<String, String> warehouseStatus = new LinkedHashMap<>();

    // flag مهم عشان نفلتر العرض
    private boolean hasMatch;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getWarehouseStatus() {
        return warehouseStatus;
    }

    public void setWarehouseStatus(Map<String, String> warehouseStatus) {
        this.warehouseStatus = warehouseStatus;
    }

    public boolean isHasMatch() {
        return hasMatch;
    }

    public void setHasMatch(boolean hasMatch) {
        this.hasMatch = hasMatch;
    }
}