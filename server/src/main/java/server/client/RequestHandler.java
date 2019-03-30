package server.client;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import server.handlers.factory.RequestHandlerFactoryImpl;
import server.handlers.factory.RequestHandlerFactory;

import javax.naming.OperationNotSupportedException;

import static common.Utils.buildMessage;

/**
 *  The {@code RequestHandler} class contains the set of methods that take client messages, operate them
 * and, in most cases return response messages
 *
 * @see         ClientListener
 * */
@SuppressWarnings("CanBeFinal")
class RequestHandler {

    private ClientListener clientListener;

    RequestHandler(ClientListener clientListener) {
        this.clientListener = clientListener;
    }
    private static Logger LOGGER = Logger.getLogger(RequestHandler.class.getSimpleName());

    void handle(Message message) {
        server.handlers.RequestHandler responseHandler;
        @SuppressWarnings("SpellCheckingInspection") RequestHandlerFactory respHandlFactory = new RequestHandlerFactoryImpl();
        Message responseMessage = new Message(MessageStatus.ERROR)
                .setText("This is a default text. If you got this message, that means that something went wrong.");
        try {
            try {
                responseHandler = respHandlFactory.getFor(clientListener, message);
                responseMessage = responseHandler.handle(clientListener, message);
            } catch (OperationNotSupportedException e) {
                if (LOGGER.isEnabledFor(Level.ERROR)) {
                    LOGGER.error(
                            buildMessage("Unable to handle the message of", message.getStatus(), "status"));
                }
            }
        } finally {
            clientListener.sendMessageToConnectedClient(responseMessage);
            LOGGER.trace("Message has been sent");
            if (MessageStatus.REGISTRATION.equals(message.getStatus())
                    && MessageStatus.ACCEPTED.equals(responseMessage.getStatus())) {
                clientListener.sendMessageToConnectedClient(new Message(MessageStatus.ACCEPTED)
                        .setText("Please, re-login on the server"));
                clientListener.interrupt();
            }
        }
    }
}