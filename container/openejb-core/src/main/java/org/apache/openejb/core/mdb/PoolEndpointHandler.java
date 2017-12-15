/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.openejb.core.mdb;

import org.apache.openejb.ApplicationException;
import org.apache.openejb.BeanContext;
import org.apache.openejb.OpenEJBException;
import org.apache.openejb.SystemException;
import org.apache.openejb.core.ThreadContext;
import org.apache.openejb.resource.activemq.jms2.DelegateMessage;
import org.apache.openejb.resource.activemq.jms2.JMS2;

import javax.ejb.EJBException;
import javax.jms.Message;
import javax.resource.spi.ApplicationServerInternalException;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.endpoint.MessageEndpoint;
import javax.transaction.xa.XAResource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;

public class PoolEndpointHandler implements InvocationHandler, MessageEndpoint {
    private volatile Boolean isAmq;

 

    private final BaseMdbContainer container;
    private final BeanContext deployment;
    private final MdbInstanceManager instanceManager;
    private final XAResource xaResource;

    private State state = State.NONE;
    private Object instance;
    private ThreadContext callContext;
    public PoolEndpointHandler(final BaseMdbContainer container, final BeanContext deployment, final MdbInstanceManager instanceManager, final XAResource xaResource) throws UnavailableException {
        this.container = container;
        this.deployment = deployment;
        this.instanceManager = instanceManager;
        this.xaResource = xaResource;
        this.callContext = ThreadContext.getThreadContext();
    }

    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
//        System.out.println("\n" +
//                "***************************************\n" +
//                "Endpoint invoked " + method + "\n" +
//                "***************************************\n\n");

        final String methodName = method.getName();
        final Class<?>[] parameterTypes = method.getParameterTypes();

        if (method.getDeclaringClass() == Object.class) {
            if ("toString".equals(methodName) && parameterTypes.length == 0) {
                return toString();
            } else if ("equals".equals(methodName) && parameterTypes.length == 1) {
                return equals(args[0]);
            } else if ("hashCode".equals(methodName) && parameterTypes.length == 0) {
                return hashCode();
            } else {
                throw new UnsupportedOperationException("Unkown method: " + method);
            }
        }

//        try {
        if ("beforeDelivery".equals(methodName) && Arrays.deepEquals(new Class[]{Method.class}, parameterTypes)) {
            beforeDelivery((Method) args[0]);
            return null;
        } else if ("afterDelivery".equals(methodName) && parameterTypes.length == 0) {
            afterDelivery();
            return null;
        } else if ("release".equals(methodName) && parameterTypes.length == 0) {
            release();
            return null;
        } else {
            final Object value = deliverMessage(method, args);
            return value;
        }
//        } finally { logTx(); }
    }

    public void beforeDelivery(final Method method) throws ApplicationServerInternalException {
        // verify current state
        switch (state) {
            case RELEASED:
                throw new IllegalStateException("Message endpoint factory has been released");
            case BEFORE_CALLED:
                throw new IllegalStateException("beforeDelivery can not be called again until message is delivered and afterDelivery is called");
            case METHOD_CALLED:
            case SYSTEM_EXCEPTION:
                throw new IllegalStateException("The last message delivery must be completed with an afterDeliver before beforeDeliver can be called again");
        }

        // call beforeDelivery on the container
        try {
            instance = instanceManager.getInstance(new ThreadContext(deployment, null));
            container.beforeDelivery(deployment, instance, method, xaResource);
        } catch (final SystemException se) {
            final Throwable throwable = se.getRootCause() != null ? se.getRootCause() : se;
            throw new ApplicationServerInternalException(throwable);
        } catch (OpenEJBException oe){
            throw new ApplicationServerInternalException(oe);
        }

        // before completed successfully we are now ready to invoke bean
        state = State.BEFORE_CALLED;
    }

    public Object deliverMessage(final Method method, final Object[] args) throws Throwable {

        boolean callBeforeAfter = false;

        // verify current state
        switch (state) {
            case NONE:
                try {
                    beforeDelivery(method);
                } catch (final ApplicationServerInternalException e) {
                    throw (EJBException) new EJBException().initCause(e.getCause());
                }
                callBeforeAfter = true;
                state = State.METHOD_CALLED;
                break;
            case BEFORE_CALLED:
                state = State.METHOD_CALLED;
                break;
            case RELEASED:
                throw new IllegalStateException("Message endpoint factory has been released");
            case METHOD_CALLED:
            case SYSTEM_EXCEPTION:
                throw new IllegalStateException("The last message delivery must be completed with an afterDeliver before another message can be delivered");
        }

        Throwable throwable = null;
        Object value = null;
        try {
            // deliver the message
            value = container.invoke(instance, method, null, wrapMessageForAmq5(args));
        } catch (final SystemException se) {
            throwable = se.getRootCause() != null ? se.getRootCause() : se;
            state = State.SYSTEM_EXCEPTION;
        } catch (final ApplicationException ae) {
            throwable = ae.getRootCause() != null ? ae.getRootCause() : ae;
        } finally {
            // if the adapter is not using before/after, we must call afterDelivery to clean up
            if (callBeforeAfter) {
                try {
                    afterDelivery();
                } catch (final ApplicationServerInternalException e) {
                    throwable = throwable == null ? e.getCause() : throwable;
                } catch (final UnavailableException e) {
                    throwable = throwable == null ? e : throwable;
                }
            }
        }

        if (throwable != null) {
            throwable.printStackTrace();
            if (isValidException(method, throwable)) {
                throw throwable;
            } else {
                throw new EJBException().initCause(throwable);
            }
        }
        return value;
    }

    // workaround for AMQ 5/JMS 2 support
    private Object[] wrapMessageForAmq5(final Object[] args) {
        if (args == null || args.length != 1 || DelegateMessage.class.isInstance(args[0])) {
            return args;
        }

        if (isAmq == null) {
            synchronized (this) {
                if (isAmq == null) {
                    isAmq = args[0].getClass().getName().startsWith("org.apache.activemq.");
                }
            }
        }
        if (isAmq) {
            args[0] = JMS2.wrap(Message.class.cast(args[0]));
        }
        return args;
    }

    public void afterDelivery() throws ApplicationServerInternalException, UnavailableException {
        // verify current state
        switch (state) {
            case RELEASED:
                throw new IllegalStateException("Message endpoint factory has been released");
            case NONE:
                throw new IllegalStateException("afterDelivery may only be called if message delivery began with a beforeDelivery call");
        }


        // call afterDelivery on the container
        boolean exceptionThrown = false;
        try {
            container.afterDelivery(instance);
        } catch (final SystemException se) {
            exceptionThrown = true;

            final Throwable throwable = se.getRootCause() != null ? se.getRootCause() : se;
            throwable.printStackTrace();
            throw new ApplicationServerInternalException(throwable);
        } finally {
            if (state == State.SYSTEM_EXCEPTION) {
//                recreateInstance(exceptionThrown);
            }
            // we are now in the default NONE state
            state = State.NONE;
        }
    }



    public void release() {
        if (state == State.RELEASED) {
            return;
        }
        state = State.RELEASED;

        // notify the container
        try {
            container.release(deployment, instance);
        } finally {
            try{
                instanceManager.poolInstance(callContext, instance);
            } catch (OpenEJBException e){
                e.printStackTrace();
            }
            instance = null;
        }
    }

    private boolean isValidException(final Method method, final Throwable throwable) {
        if (throwable instanceof RuntimeException || throwable instanceof Error) {
            return true;
        }

        final Class<?>[] exceptionTypes = method.getExceptionTypes();
        for (final Class<?> exceptionType : exceptionTypes) {
            if (exceptionType.isInstance(throwable)) {
                return true;
            }
        }
        return false;
    }
}
