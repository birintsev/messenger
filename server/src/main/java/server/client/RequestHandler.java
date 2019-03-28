package server.client;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import server.exceptions.RoomNotFoundException;
import server.handlers.*;
import server.processing.ClientProcessing;
import server.processing.RestartingEnvironment;
import server.room.Room;
import server.processing.RoomProcessing;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;

import static common.Utils.buildMessage;
import static server.processing.ClientProcessing.loadClient;

/**
 *  The {@code RequestHandler} class contains the set of methods that take client messages, operate them
 * and, in most cases return response messages
 *
 * @see ClientListener
 * */
@SuppressWarnings("CanBeFinal")
public
class RequestHandler {
    private ClientListener clientListener;
    public static Logger LOGGER = Logger.getLogger(RequestHandler.class.getSimpleName());

    void handle(Message message) {
        server.handlers.RequestHandler rh;
        Message responseMessage = new Message(MessageStatus.ERROR)
                .setText("This is a default text. If you got this message, that means that something went wrong.");
        try {
            switch (message.getStatus()) {
                case AUTH:
                    rh = new AuthorizationRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case REGISTRATION:
                    rh = new RegistrationRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case MESSAGE:
                    rh = new MessageSendingRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case CLIENTBAN:
                    rh = new ClientBanRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case CREATE_ROOM:
                    rh = new CreateRoomRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case DELETE_ROOM:
                    rh = new DeleteRoomRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case INVITE_USER:
                    rh = new InviteClientRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case UNINVITE_CLIENT:
                    rh = new UninviteClientRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case STOP_SERVER:
                    rh = new StopServerRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case ROOM_LIST:
                    rh = new RoomListRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case CLIENTUNBAN:
                    rh = new ClientUnbanRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case RESTART_SERVER:
                    rh = new RestartRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case MESSAGE_HISTORY:
                    responseMessage = getRoomMessages(message);
                    break;
                case GET_CLIENT_NAME:
                    responseMessage = getClientName(message);
                    break;
                case ROOM_MEMBERS:
                    rh = new RoomMembersRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                default:
                    responseMessage = new Message(MessageStatus.ERROR)
                            .setText(buildMessage("Unknown message status", message.getStatus().toString()));
            }
        } finally {
            clientListener.sendMessageToConnectedClient(responseMessage);
            LOGGER.trace("Message has been sent");
            if (MessageStatus.REGISTRATION.equals(message.getStatus())
                    && MessageStatus.ACCEPTED.equals(responseMessage.getStatus())) {
                clientListener.sendMessageToConnectedClient(new Message(MessageStatus.KICK)
                        .setText("Please, re-login on the server"));
                clientListener.interrupt();
            }
        }
    }

    RequestHandler(ClientListener clientListener) {
        this.clientListener = clientListener;
    }

    /**
     * This method handles with the request for the list of the client rooms
     *
     * @param           message is the request message
     *                          NOTE! It is expected that message contains following non-null fields
     *                          1) {@code fromId} - an id of registered user who has logged in
     *                          2) {@code roomId} - an id of the room where the client is a member
     *
     *  NOTE! This method sends the message history by parts - message by message. The contract of the
     * method is that the caller will send the resulting message of status {@code MessageStatus.ACCEPTED}
     * to the client i.e. when the caller obtain success confirmation
     * that means that client has already received the history
     *
     * @return          an instance of {@code Message} that contains info about performed (or not) operation.
     *                  It may be of the following statuses
     *                          1) {@code MessageStatus.ACCEPTED}  -   if the history has been sent
     *                          2) {@code MessageStatus.DENIED}    -   if the request has been obtained
     *                                                                 from unlogged user
     *                          3) {@code MessageStatus.ERROR}     -   if an error occurred
     *                                                                 while executing the operation
     */
    private synchronized Message getRoomMessages(Message message) {
        if (message == null) {
            return new Message(MessageStatus.ERROR).setText("Internal error occurred. Message is null");
        }
        if (clientListener.isMessageNotFromThisLoggedClient(message)) {
            return new Message(MessageStatus.DENIED).setText("Log in first");
        }
        if (message.getRoomId() == null) {
            return new Message(MessageStatus.ERROR).setText("Unspecified room");
        }
        Room room;
        if (!clientListener.getServer().getOnlineRooms().safe().containsKey(message.getRoomId())) {
            if (RoomProcessing.hasRoomBeenCreated(clientListener.getServer().getConfig(), message.getRoomId()) != 0L) {
                try {
                    RoomProcessing.loadRoom(clientListener.getServer(), message.getRoomId());
                } catch (RoomNotFoundException e) {
                    return new Message(MessageStatus.ERROR).setText(e.getLocalizedMessage());
                }
            }
        }
        room = clientListener.getServer().getOnlineRooms().safe().get(message.getRoomId());
        if (!RoomProcessing.isMember(clientListener.getServer().getConfig(), clientListener.getClient().getClientId()
                , message.getRoomId())) {
            return new Message(MessageStatus.DENIED).setText(
                    buildMessage("You are not a member of the room (id", message.getRoomId(), ')'));
        }
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Message.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            StringWriter stringWriter;
            synchronized (room.getMessageHistory().getMessageHistory()) {
                for (Message roomMessage : room.getMessageHistory().getMessageHistory()) {
                    stringWriter = new StringWriter();
                    marshaller.marshal(roomMessage, stringWriter);
                    clientListener.getOut().safe().writeUTF(stringWriter.toString());
                    clientListener.getOut().safe().flush();
                }
            }
        } catch (JAXBException | IOException e) {
            if (LOGGER.isEnabledFor(Level.ERROR)) {
                LOGGER.error(e.getLocalizedMessage());
            }
        }
        return new Message(MessageStatus.ACCEPTED).setText("This is the end of the room message history")
                .setRoomId(message.getRoomId());
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