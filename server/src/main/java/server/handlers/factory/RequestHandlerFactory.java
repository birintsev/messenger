package server.handlers.factory;

import common.entities.message.Message;
import server.client.ClientListener;

import javax.naming.OperationNotSupportedException;

/**
 *  An implementation of the interface must provide an ability to create
 * an instance of {@code server.handlers.RequestHandler} that will operate the passed request.
 * */
public interface RequestHandlerFactory {
    /**
     *  The method {@code getFor} provides a caller with a handler corresponding to the specific of the message status
     *
     * @param           clientListener the client who sent the request
     *
     * @param           message the request represented by message
     *
     * @throws          OperationNotSupportedException if the implementation can not create a handler
     * */
    server.handlers.RequestHandler getFor(ClientListener clientListener, Message message)
            throws OperationNotSupportedException;
}