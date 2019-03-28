package server.handlers;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import org.apache.log4j.Level;
import server.client.Client;
import server.client.ClientListener;
import server.processing.ClientProcessing;

import static common.Utils.buildMessage;

public class ClientUnbanRequestHandler extends RequestHandler {

    public ClientUnbanRequestHandler(ClientListener clientListener, Message message) {
        super(clientListener, message);
    }

    @Override
    public Message handle() {
        Message responseMessage = clientUnban(message);
        return responseMessage;
    }

    /**
     * The method {@code userUnban} handles with requests of unblocking a user.
     *
     * @param           message an instance of {@code Message} that represents a request about blocking a user.
     *
     *                  NOTE! It is expected that message contains following non-null fields
     *                      1) {@code fromId} - id of registered user who has admin rights
     *                          i.e. an instance of {@code Client} representing his account
     *                          has {@code isAdmin == true}
     *                      2)  {@code toId} - id of registered client who is currently banned
     *
     * @return          an instance of {@code Message} that contains info about performed (or not) operation.
     *                  It may be of the following statuses:
     *                      1) {@code MessageStatus.ACCEPTED}  -   if the specified client has been unbanned
     *                      2) {@code MessageStatus.DENIED}    -   if the sender is not an admin or specified client
     *                          is not currently banned
     *                      3) {@code MessageStatus.ERROR}     -   if an error occurred while executing the operation
     */
    private Message clientUnban(Message message) {
        String errorMessage;
        if (message == null) {
            if (LOGGER.isEnabledFor(Level.ERROR)) {
                LOGGER.error("Passed null-message to perform client unbanning");
            }
            return new Message(MessageStatus.ERROR).setText("Error occurred while unbanning (null message)");
        }
        if (message.getToId() == null) {
            errorMessage = "Attempt to unban unspecified account";
            if (LOGGER.isEnabledFor(Level.TRACE)) {
                LOGGER.trace(buildMessage(errorMessage, "from"
                        , message.getFromId() != null ? message.getFromId() : "unspecified client"));
            }
            return new Message(MessageStatus.ERROR).setText(errorMessage);
        }
        int toId = message.getToId();
        if (clientListener.isMessageNotFromThisLoggedClient(message) && !(
                clientListener.getServer().getConfig().getProperty("serverLogin").equals(message.getLogin())
                        && clientListener.getServer().getConfig().getProperty("serverPassword")
                        .equals(message.getPassword())
        )) {
            errorMessage = buildMessage("Attempt to perform an action before log-in :", message);
            return new Message(MessageStatus.ERROR).setText(errorMessage);
        }
        Integer fromId = message.getFromId();
        if (ClientProcessing.hasNotAccountBeenRegistered(clientListener.getServer().getConfig(), toId)) {
            errorMessage = buildMessage("Attempt to unban unregistered client from client (admin) (id"
                    , fromId == null ? "server admin" : fromId);
            if (LOGGER.isEnabledFor(Level.ERROR)) {
                LOGGER.error(errorMessage);
            }
            return new Message(MessageStatus.ERROR).setText(errorMessage);
        }
        boolean isAdmin = true;
        if (fromId != null) {
            isAdmin = ClientProcessing.loadClient(clientListener.getServer().getConfig(), fromId).isAdmin();
        }
        Client clientToUnban = ClientProcessing.loadClient(clientListener.getServer().getConfig(), toId);
        clientToUnban.setServer(clientListener.getServer());
        if (!isAdmin) {
            errorMessage = buildMessage("Not enough rights to perform this operation (client id"
                    , fromId, "attempts to unban client id", clientToUnban.getClientId());
            if (LOGGER.isEnabledFor(Level.ERROR)) {
                LOGGER.error(errorMessage);
            }
            return new Message(MessageStatus.ERROR).setText(errorMessage);
        }
        if (!clientToUnban.isBaned()) {
            errorMessage = buildMessage("Client", "(id", clientToUnban.getClientId(), ')', "is not banned");
            LOGGER.error(errorMessage);
            return new Message(MessageStatus.ERROR).setText(errorMessage);
        }
        clientToUnban.setBaned(false);
        clientToUnban.setIsBannedUntil(null);
        if (clientToUnban.save()) {
            String infoMessage = buildMessage("Client (id", clientToUnban.getClientId()
                    , ") has been unbanned by the admin (id ", (fromId == null ? "server admin" : fromId), ')');
            if (LOGGER.isEnabledFor(Level.INFO)) {
                LOGGER.info(infoMessage);
            }
            return new Message(MessageStatus.ACCEPTED).setText(infoMessage);
        } else {
            if (LOGGER.isEnabledFor(Level.WARN)) {
                LOGGER.warn(buildMessage("The process of unbanning client (id", toId
                        , ") has not been finished properly"));
            }
            return new Message(MessageStatus.ERROR).setText("Unknown error occurred while unbanning");
        }
    }
}