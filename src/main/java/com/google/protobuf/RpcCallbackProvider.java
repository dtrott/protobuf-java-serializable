package com.google.protobuf;

/**
 * Factory for wrapping RpcCallbacks
 */
public interface RpcCallbackProvider {

    /**
     * Take an {@code RpcCallback<Message>} and convert it to an
     * {@code RpcCallback} accepting a specific message type.  This is always
     * type-safe (parameter type contravariance).
     */
    public <Type extends Message> RpcCallback<Type>
    specializeCallback(final RpcCallback<Message> originalCallback);

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
        final Type defaultInstance);

    /**
     * Creates a callback which can only be called once.  This may be useful for
     * security, when passing a callback to untrusted code:  most callbacks do
     * not expect to be called more than once, so doing so may expose bugs if it
     * is not prevented.
     */
    public <ParameterType>
      RpcCallback<ParameterType> newOneTimeCallback(
        final RpcCallback<ParameterType> originalCallback) ;
}
