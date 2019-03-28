package server.handlers;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import org.jetbrains.annotations.NotNull;
import server.client.ClientListener;

import static common.Utils.buildMessage;

public class StopServerRequestHandler extends RequestHandler {
    public StopServerRequestHandler(ClientListener clientListener, Message message) {
        super(clientListener, message);
    }

    @Override
    public Message handle() {
        Message responseMessage = stopServer(message);
        clientListener.sendMessageToConnectedClient(message);
        if (MessageStatus.ACCEPTED.equals(message.getStatus())) {
            clientListener.getServer().interrupt();
        }
        return responseMessage;
    }

    private Message stopServer(@NotNull Message message) {
        if (!MessageStatus.STOP_SERVER.equals(message.getStatus())) {
            String errorMessage = buildMessage("Message of status", MessageStatus.STOP_SERVER
                    , "was expected, but found", message.getStatus());
            LOGGER.warn(errorMessage);
            return new Message(MessageStatus.ERROR).setText("Internal error: ".concat(errorMessage));
        }
        if ((!clientListener.getServer().getConfig().getProperty("serverLogin").equals(message.getLogin())
                || !clientListener.getServer().getConfig().getProperty("serverPassword")
                .equals(message.getPassword())) && !clientListener.getClient().isAdmin()) {
            return new Message(MessageStatus.DENIED).setText("Please, check your login and password");
        }
        return new Message(MessageStatus.ACCEPTED).setText("Server is going to shut down");
    }
}