package com.google.protobuf;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;

/**
 * Factory that provides {@code Serializable} wrappers around RpcCallback instances.
 * This allows internal use of Java serialization even though Protocol Buffers are used for transport/storage.
 * Note: simply serializing the wrappers is not sufficent as the type descriptors are not {@code Serializable}
 * hence they must be excluded from serialization and re-constituted via reflection at callback time.
 */
public class SerializableRpcCallbackProvider implements RpcCallbackProvider {
    /**
     * Take an {@code RpcCallback<Message>} and convert it to an
     * {@code RpcCallback} accepting a specific message type.  This is always
     * type-safe (parameter type contravariance).
     */
    @SuppressWarnings("unchecked")
    public <Type extends Message> RpcCallback<Type>
    specializeCallback(final RpcCallback<Message> originalCallback) {
        return (RpcCallback<Type>) originalCallback;
    }

    /**
     * Take an {@code RpcCallback} accepting a specific message type and convert
     * it to an {@code RpcCallback<Message>}.  The generalized callback will
     * accept any message object which has the same descriptor, and will convert
     * it to the correct class before calling the original callback.  However,
     * if the generalized callback is given a message with a different descriptor,
     * an exception will be thrown.
     */
    public <Type extends Message>
    RpcCallback<Message> generalizeCallback(
        final RpcCallback<Type> originalCallback,
        final Class<Type> originalClass,
        Type defaultInstance) {

        return generalizeCallbackInternal(originalCallback, originalClass);
    }

    /**
     * Creates a callback which can only be called once.  This may be useful for
     * security, when passing a callback to untrusted code:  most callbacks do
     * not expect to be called more than once, so doing so may expose bugs if it
     * is not prevented.
     */
    public <ParameterType>
    RpcCallback<ParameterType> newOneTimeCallback(final RpcCallback<ParameterType> originalCallback) {
        return newOneTimeCallbackInternal(originalCallback);
    }

    /**
     * Using static method to avoid closure of SerializableRpcCallbackProvider instance.
     */
    @SuppressWarnings("unchecked")
    private static <Type extends Message> RpcCallback<Message> generalizeCallbackInternal(
        final RpcCallback<Type> originalCallback,
        final Class<Type> originalClass) {

        final class SerializableRpcCallback implements RpcCallback<Message>, Serializable {
            private static final long serialVersionUID = 4672335177484879721L;

            public void run(final Message parameter) {
                Type typedParameter;
                try {
                    typedParameter = originalClass.cast(parameter);
                } catch (ClassCastException ignored) {
                    typedParameter = RpcUtil.copyAsType(getDefaultInstance(), parameter);
                }
                originalCallback.run(typedParameter);
            }

            /**
             * Fetch the default instance using reflection as it is not Serializable.
             */
            public Type getDefaultInstance() {
                try {
                    return (Type) originalClass.getMethod("getDefaultInstance").invoke(null);
                } catch (NoSuchMethodException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

            }

        }
        return new SerializableRpcCallback();
    }

    /**
     * Using static method to avoid closure of SerializableRpcCallbackProvider instance.
     */
    private static <ParameterType> RpcCallback<ParameterType> newOneTimeCallbackInternal(
        final RpcCallback<ParameterType> originalCallback) {

        final class SerializableRpcCallback implements RpcCallback<ParameterType>, Serializable {
            private static final long serialVersionUID = 7819892576868101497L;
            private boolean alreadyCalled = false;

            public void run(final ParameterType parameter) {
                synchronized (this) {
                    if (alreadyCalled) {
                        throw new RpcUtil.AlreadyCalledException();
                    }
                    alreadyCalled = true;
                }
                originalCallback.run(parameter);
            }
        }

        return new SerializableRpcCallback();
    }

}
