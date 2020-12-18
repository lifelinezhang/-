package com.blackfish.cashloan.business.handler;

public interface Handler {
    Handler next = null;

    void setNext(Handler handler);

    Handler getNext();

    <T> Object handle(T t) throws Throwable;

    <T> Object handleNext(Object result, T t) throws Throwable;
}
