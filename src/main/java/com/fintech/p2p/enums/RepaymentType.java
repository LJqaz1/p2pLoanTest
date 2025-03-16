package com.fintech.p2p.enums;

public enum RepaymentType {
    FULL("全额还款"),
    PARTIAL("部分还款");

    private final String displayName;

    RepaymentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
