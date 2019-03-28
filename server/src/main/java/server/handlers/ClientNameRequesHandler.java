package server.handlers;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import server.client.Client;
import server.client.ClientListener;
import server.processing.ClientProcessing;

import static common.Utils.buildMessage;

public class ClientNameRequesHandler extends RequestHandler {

    public ClientNameRequesHandler(ClientListener clientListener, Message message) {
        super(clientListener, message);
    }

    @Override
    public Message handle() {
        Message responseMessage = getClientName(message);
        return responseMessage;
    }

    private Message getClientName(Message message) {
        if (clientListener.isMessageNotFromThisLoggedClient(message)) {
            return new Message(MessageStatus.DENIED)
                    .setText("Log in prior to request information");
        }
        if (message.getToId() == null) {
            return new Message(MessageStatus.ERROR).setText("Unspecified client id");
        }
        int clientId = message.getToId();
        if (ClientProcessing.hasNotAccountBeenRegistered(clientListener.getServer().getConfig(), clientId)) {
            return new Message(MessageStatus.DENIED)
                    .setText(buildMessage("Unable to find client id", clientId));
        }
        Client client;
        if (clientListener.getServer().getOnlineClients().safe().containsKey(clientId)) {
            client = clientListener.getServer().getOnlineClients().safe().get(clientId).getClient();
        } else {
            client = ClientProcessing.loadClient(clientListener.getServer().getConfig(), clientId);
        }
        return new Message(MessageStatus.ACCEPTED).setFromId(clientId).setText(client.getLogin());
    }
}