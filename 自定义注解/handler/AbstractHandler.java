package com.blackfish.cashloan.business.handler;

public abstract class AbstractHandler<T> implements Comparable<AbstractHandler> {
    protected int order;

    protected AbstractHandler next;

    public AbstractHandler(int order) {
        this.order = order;
    }

    public void setNext(AbstractHandler handler) {
        this.next = handler;
    }

    public AbstractHandler getNext() {
        return this.next;
    }

    public abstract Object handle(T t) throws Throwable;

    protected Object handleNext(Object result, T t) throws Throwable {
        if (null != this.next) {
            return next.handle(t);
        } else {
            return result;
        }
    }

    public int compareTo(AbstractHandler o) {
        return this.order - o.order;
    }
}
