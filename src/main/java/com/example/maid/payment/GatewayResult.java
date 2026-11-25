package com.example.maid.payment;

public record GatewayResult(boolean success, String transactionRef, String message) {

    public static GatewayResult ok(String transactionRef) {
        return new GatewayResult(true, transactionRef, "OK");
    }

    public static GatewayResult failed(String message) {
        return new GatewayResult(false, null, message);
    }
}
