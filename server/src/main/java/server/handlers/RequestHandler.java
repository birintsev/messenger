package server.handlers;

import common.entities.message.Message;
import org.apache.log4j.Logger;
import server.client.ClientListener;

/**
 *  This class is a generalization on the all request handlers and represents a sequence of operations
 * to perform the required action
 * */
public abstract class RequestHandler {

    static final Logger LOGGER = Logger.getLogger(RequestHandler.class.getSimpleName());

    /**
     *  The method that contains logic of handling the request from client. It does not throw any exception, but if any
     * happens, returns an instance of {@code Message} with corresponding status
     *
     * @return          an instance of {@code Message} with information/result of request handling
     * */
    public abstract Message handle(ClientListener clientListener, Message message);
}