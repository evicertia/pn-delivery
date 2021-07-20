package it.pagopa.pn.delivery.model.notification.address;

public class DigitalAddress {

    public enum Type {
        PEC
    }

    private Type type;

    private String address;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
