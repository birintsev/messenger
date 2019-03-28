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

public class UninviteClientRequestHandler extends RequestHandler {
    public UninviteClientRequestHandler(ClientListener clientListener, Message message) {
        super(clientListener, message);
    }

    @Override
    public Message handle() {
        Message responseMessage = kickClientFromRoom(message);
        return responseMessage;
    }

    private Message kickClientFromRoom(@NotNull Message message) {
        if (clientListener.isMessageNotFromThisLoggedClient(message)) {
            return new Message(MessageStatus.DENIED).setText("Wrong passed clientId");
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
            return new Message(MessageStatus.ERROR).setText("Missed client to be uninvited");
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
            } catch (RoomNotFoundException e) {
                errorMessage = buildMessage("Unable to find a room");
                if (LOGGER.isEnabledFor(Level.TRACE)) {
                    LOGGER.trace(buildMessage(errorMessage, "(id", message.getRoomId(), ')'));
                }
                return new Message(MessageStatus.DENIED).setText(errorMessage).setRoomId(message.getRoomId());
            }
        }
        Room room = clientListener.getServer().getOnlineRooms().safe().get(message.getRoomId());
        if (!room.getMembers().safe().contains(message.getFromId())) {
            if (LOGGER.isEnabledFor(Level.TRACE)) {
                LOGGER.trace(buildMessage("The client (id", message.getFromId()
                        , ") is not a member of the room id", message.getRoomId()));
            }
            return new Message(MessageStatus.DENIED).setText("Not a member of the room");
        }
        if (!room.getMembers().safe().contains(message.getToId())) {
            if (LOGGER.isEnabledFor(Level.TRACE)) {
                LOGGER.trace(buildMessage("Attempt to remove client (id", message.getToId()
                        , ") who is not a member of the room (id", message.getRoomId(), ')'));
            }
            return new Message(MessageStatus.DENIED).setText("This client is not a member of the room");
        }
        room.getMembers().safe().remove(message.getToId());
        Client client;
        if (clientListener.getServer().getOnlineClients().safe().containsKey(message.getToId())) {
            client = clientListener.getServer().getOnlineClients().safe().get(message.getToId()).getClient();
            clientListener.getServer().getOnlineClients().safe().get(message.getToId()).sendMessageToConnectedClient(
                    new Message(MessageStatus.UNINVITE_CLIENT).setText("You have been uninvited from the room")
                            .setRoomId(message.getRoomId()));
        } else {
            client = ClientProcessing.loadClient(clientListener.getServer().getConfig(), message.getToId());
        }
        client.getRooms().safe().remove(message.getRoomId());
        if (client.getServer() == null) {
            client.setServer(clientListener.getServer());
        }
        if (room.getServer() == null) {
            room.setServer(clientListener.getServer());
        }
        room.save();
        client.save();
        String infoString = buildMessage("Now client (id", message.getToId()
                , ") is not a member of the room (id", message.getRoomId(), ')');
        if (LOGGER.isEnabledFor(Level.TRACE)) {
            LOGGER.trace(infoString);
        }
        return new Message(MessageStatus.ACCEPTED).setText(infoString);
    }

}
