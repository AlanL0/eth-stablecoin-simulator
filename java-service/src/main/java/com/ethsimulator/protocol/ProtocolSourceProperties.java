package com.ethsimulator.protocol;

public class ProtocolSourceProperties {

    private boolean enabled = true;
    private String address = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}