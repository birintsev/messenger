package server.handlers;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import server.client.Client;
import server.client.ClientListener;
import server.exceptions.RoomNotFoundException;
import server.processing.ClientProcessing;
import server.processing.RoomProcessing;
import server.room.Room;

import static common.Utils.buildMessage;

public class InviteClientRequestHandler extends RequestHandler {
    public InviteClientRequestHandler(Message message) {
        super(message);
    }

    public InviteClientRequestHandler(ClientListener clientListener, Message message) {
        super(clientListener, message);
    }

    @Override
    public Message handle() {
        Message responseMessage = addClientToRoom(message);
        return responseMessage;
    }

    private Message addClientToRoom(@NotNull Message message) {
        if (clientListener.isMessageNotFromThisLoggedClient(message)) {
            return new Message(MessageStatus.DENIED).setText("Log in prior perform any request");
        }
        if (message.getFromId() == null) {
            if (LOGGER.isEnabledFor(Level.WARN)) {
                LOGGER.warn("null fromId");
            }
            return new Message(MessageStatus.ERROR).setText("Missed addresser id");
        }
        if (message.getToId() == null) {
            if (LOGGER.isEnabledFor(Level.WARN)) {
                LOGGER.warn("null toId");
            }
            return new Message(MessageStatus.ERROR).setText("Missed client to be invited");
        }
        if (message.getRoomId() == null) {
            if (LOGGER.isEnabledFor(Level.WARN)) {
                LOGGER.warn("null roomId");
            }
            return new Message(MessageStatus.ERROR).setText("Missed roomId");
        }
        if (!clientListener.getServer().getOnlineRooms().safe().containsKey(message.getRoomId())) {
            String errorMessage;
            try {
                RoomProcessing.loadRoom(clientListener.getServer(), message.getRoomId());
            }  catch (RoomNotFoundException e) {
                errorMessage = buildMessage("Unable to find a room (id", message.getRoomId(), ')');
                if (LOGGER.isEnabledFor(Level.TRACE)) {
                    LOGGER.trace(errorMessage);
                }
                return new Message(MessageStatus.DENIED).setText(buildMessage(errorMessage));
            }
        }
        Room room = clientListener.getServer().getOnlineRooms().safe().get(message.getRoomId());
        if (!room.getMembers().safe().contains(message.getFromId())) {
            if (LOGGER.isEnabledFor(Level.TRACE)) {
                LOGGER.trace(buildMessage("The client id", message.getFromId()
                        , "is not a member of the room id", message.getRoomId()));
            }
            return new Message(MessageStatus.DENIED).setText("Not a member of the room");
        }
        if (room.getMembers().safe().contains(message.getToId())) {
            if (LOGGER.isEnabledFor(Level.TRACE)) {
                LOGGER.trace(buildMessage("Attempt to remove client (id", message.getToId()
                        , ") who is already a member of the room (id", message.getRoomId(), ')'));
            }
            return new Message(MessageStatus.DENIED).setText("This client is already a member of the room");
        }
        room.getMembers().safe().add(message.getToId());
        if (room.getServer() == null) {
            room.setServer(clientListener.getServer());
        }
        room.save();
        String infoString = buildMessage("Client (id", message.getToId()
                , ") now is a member of the room (id", message.getRoomId(), ')');
        Client client;
        if (clientListener.getServer().getOnlineClients().safe().containsKey(message.getToId())) {
            client = clientListener.getServer().getOnlineClients().safe().get(message.getToId()).getClient();
            clientListener.getServer().getOnlineClients().safe().get(message.getToId()).sendMessageToConnectedClient(
                    new Message(MessageStatus.UNINVITE_USER).setText("You have been invited to the room")
                            .setRoomId(message.getRoomId()));
        } else {
            client = ClientProcessing.loadClient(clientListener.getServer().getConfig(), message.getToId());
            client.setServer(clientListener.getServer());
        }
        client.getRooms().safe().add(message.getRoomId());
        client.save();
        if (LOGGER.isEnabledFor(Level.TRACE)) {
            LOGGER.trace(infoString);
        }
        return new Message(MessageStatus.ACCEPTED).setText(infoString);
    }
}