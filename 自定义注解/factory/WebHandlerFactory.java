package com.blackfish.cashloan.business.factory;

import com.blackfish.cashloan.business.handler.*;
import com.blackfish.cashloan.business.helper.BizHelper;

public class WebHandlerFactory implements Factory<AbstractHandler> {
    private WebHandlerFactory() {
        this.handler = createObject();
    }

    private volatile static WebHandlerFactory factory;

    private AbstractHandler handler;

    public static WebHandlerFactory getInstance() {
        if (factory == null) {
            synchronized (WebHandlerFactory.class) {
                if (factory == null) {
                    factory = new WebHandlerFactory();
                }
            }
        }
        return factory;
    }

    public AbstractHandler getObject() {
        return this.handler;
    }

    private static AbstractHandler createObject() {
        ParamsValidateHandler paramsValidateHandler = new ParamsValidateHandler(1);
        AuthorityHandler authorityHandler = new AuthorityHandler(2);
        IdempotentHandler idempotentHandler = new IdempotentHandler(3);
        TargetHandler targetHandler = new TargetHandler(4);
        return BizHelper.createHandlerChain(paramsValidateHandler, authorityHandler, idempotentHandler, targetHandler);
    }
}


