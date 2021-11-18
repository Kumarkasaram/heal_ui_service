package com.heal.dashboard.service.enums;

public enum ComponentType {

    app_service("Application Server"),
    db_service ("Database Server"),
    micro_service ("Services"),
    unknown("unknown"),
    queue("queue"),
    web_service("Web Server"),
    user("user"),
    host("Host");

    protected  String type;

    ComponentType(String type){
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public static ComponentType findByType(String abbr) {
        for (ComponentType v : values()) {
            if (v.getType().equals(abbr)) {
                return v;
            }
        }
        return unknown;
    }
}
