package server.handlers;

import common.entities.Shell;
import common.entities.message.Message;
import common.entities.message.MessageStatus;
import org.apache.log4j.Level;
import org.jetbrains.annotations.NotNull;
import server.Server;
import server.client.Client;
import server.client.ClientListener;
import server.exceptions.ClientNotFoundException;
import server.processing.ClientProcessing;
import server.room.Room;
import server.processing.RoomProcessing;

import java.util.Map;
import java.util.Set;

import static common.Utils.buildMessage;

/**
 *  This {@code RequestHandler} implementation handles with clients requests of removing the rooms
 * */
public class DeleteRoomRequestHandler extends RequestHandler {

    public DeleteRoomRequestHandler() {
    }

    @Override
    public Message handle(ClientListener clientListener, Message message) {
        return deleteRoom(clientListener, message);
    }

    private Message deleteRoom(ClientListener clientListener, Message message) {
        if (clientListener.isMessageNotFromThisLoggedClient(message)) {
            return new Message(MessageStatus.DENIED).setText("Please, log in prior room deleting");
        }
        if (message.getRoomId() == null) {
            return new Message(MessageStatus.ERROR).setText("Unspecified roomId");
        }
        int roomId = message.getRoomId();
        if (RoomProcessing.hasRoomBeenCreated(clientListener.getServer().getConfig(), roomId) == 0) {
            return new Message(MessageStatus.ERROR).setText(
                    buildMessage("Unable to find room (id", roomId, ')'));
        }
        if (!clientListener.getServer().getOnlineRooms().safe().containsKey(roomId)) {
            RoomProcessing.loadRoom(clientListener.getServer(), roomId);
        }
        Room room = clientListener.getServer().getOnlineRooms().safe().get(roomId);
        if (room.getAdminId() != message.getFromId()) {
            return new Message(MessageStatus.DENIED)
                    .setText("Not enough rights to perform room deleting action").setRoomId(roomId);
        }
        removeRoomFromClientsRoomLists(clientListener.getServer(), roomId);
        RoomProcessing.permanentRemoveRoom(clientListener.getServer(), roomId);
        informClientsAboutRoomDeleting(clientListener.getServer().getOnlineClients(), roomId);
        clientListener.getServer().getOnlineRooms().safe().remove(roomId);
        if (RoomProcessing.hasRoomBeenCreated(clientListener.getServer().getConfig(), roomId) == 0) {
            return new Message(MessageStatus.ACCEPTED)
                    .setText("The room has been successfully deleted").setRoomId(roomId);
        } else {
            return new Message(MessageStatus.ERROR)
                    .setText("An error occurred while deleting the room").setRoomId(roomId);
        }
    }

    private void informClientsAboutRoomDeleting(@NotNull Shell<Map<Integer, ClientListener>> roomMembers, int roomId) {
        Map<Integer, ClientListener> rm = roomMembers.safe();
        Message info = new Message(MessageStatus.DELETE_ROOM).setRoomId(roomId)
                .setText("Room has been deleted by admin");
        for (Map.Entry<Integer, ClientListener> entry : rm.entrySet()) {
            synchronized (entry.getValue()) {
                if (entry.getValue().getClient().getRooms().safe().contains(roomId)) {
                    entry.getValue().sendMessageToConnectedClient(info);
                }
            }
        }
    }

    private void removeRoomFromClientsRoomLists(@NotNull Server server, int roomId) {
        Room room;
        if (!server.getOnlineRooms().safe().containsKey(roomId)) {
            RoomProcessing.loadRoom(server, roomId);
        }
        room = server.getOnlineRooms().safe().get(roomId);
        Set<Integer> roomMembers = room.getMembers().safe();
        synchronized (server.getOnlineClients().safe()) {
            for (int clientId : roomMembers) {
                try {
                    Client client;
                    if (server.getOnlineClients().safe().containsKey(roomId)) {
                        client = server.getOnlineClients().safe().get(clientId).getClient();
                    } else {
                        client = ClientProcessing.loadClient(server.getConfig(), clientId);
                    }
                    client.getRooms().safe().remove(roomId);
                    client.setServer(server);
                    client.save();
                } catch (ClientNotFoundException e) {
                    if (LOGGER.isEnabledFor(Level.WARN)) {
                        LOGGER.warn(buildMessage("Unable to find client (id", clientId, ')'));
                    }
                }
            }
        }
    }
}