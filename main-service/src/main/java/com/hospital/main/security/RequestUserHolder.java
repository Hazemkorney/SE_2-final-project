package com.hospital.main.security;

public final class RequestUserHolder {
    private static final ThreadLocal<RequestUser> HOLDER = new ThreadLocal<>();

    private RequestUserHolder() {}

    public static void set(RequestUser requestUser) { HOLDER.set(requestUser); }
    public static RequestUser get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
}
