package server.handlers;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import server.client.ClientListener;
import server.processing.RestartingEnvironment;

public class RestartRequestHandler extends RequestHandler {

    public RestartRequestHandler(ClientListener clientListener, Message message) {
        super(clientListener, message);
    }

    @Override
    public Message handle() {
        Message responseMessage = restartServer(message);
        return responseMessage;
    }

    private Message restartServer(Message message) {
        if ((clientListener.isMessageNotFromThisLoggedClient(message))
                && !message.getLogin().equals(clientListener.getServer().getConfig().getProperty("serverLogin"))
                && !message.getPassword().equals(
                clientListener.getServer().getConfig().getProperty("serverPassword"))) {
            return new Message(MessageStatus.DENIED).setText("Log in first");
        }
        if (clientListener.isLogged() && !clientListener.getClient().isAdmin()) {
            return new Message(MessageStatus.DENIED).setText("Not enough rights to perform the restart");
        }
        RestartingEnvironment restartingEnvironment = new RestartingEnvironment(clientListener.getServer());
        restartingEnvironment.start();
        return new Message(MessageStatus.ACCEPTED).setText("The server is going to stop the work");
    }
}