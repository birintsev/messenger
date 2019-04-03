package server.handlers;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import org.jetbrains.annotations.NotNull;
import server.client.ClientListener;

import static common.Utils.buildMessage;

public class StopServerRequestHandler extends RequestHandler {

    public StopServerRequestHandler() {
    }

    @Override
    public Message handle(ClientListener clientListener, Message message) {
        Message responseMessage = stopServer(clientListener, message);
        clientListener.sendMessageToConnectedClient(message);
        if (MessageStatus.ACCEPTED.equals(message.getStatus())) {
            clientListener.getServer().interrupt();
        }
        return responseMessage;
    }

    private Message stopServer(ClientListener clientListener, @NotNull Message message) {
        if ((!clientListener.getServer().getConfig().getProperty("serverLogin").equals(message.getLogin())
                || !clientListener.getServer().getConfig().getProperty("serverPassword")
                .equals(message.getPassword())) && !clientListener.getClient().isAdmin()) {
            return new Message(MessageStatus.DENIED).setText("Please, check your login and password");
        }
        return new Message(MessageStatus.ACCEPTED).setText("Server is going to shut down");
    }
}