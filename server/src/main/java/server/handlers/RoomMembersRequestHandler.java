package server.handlers;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import server.client.ClientListener;
import server.processing.RoomProcessing;
import server.room.Room;

import static common.Utils.buildMessage;

public class RoomMembersRequestHandler extends RequestHandler {

    public RoomMembersRequestHandler() {
    }

    @Override
    public Message handle(ClientListener clientListener, Message message) {
        return getRoomMembers(clientListener, message);
    }

    /**
     *  The method returns a string that contains enumerated clients ids of the {@code Room}
     * specified by the {@code roomId} in passed {@code message}.
     *
     * @param           message a message that contains following information:
     *                          1) {@code fromId} the {@code clientId} of the client who requests members
     *                          2) {@code roomId} the corresponding parameter of the room
     *
     * @return          an instance of {@code Message} that contains information about room members
     *                  (of {@code MessageStatus.ACCEPTED} or error message of {@code MessageStatus.ERROR}
     *                  or {@code MessageStatus.DENIED}). The message of status {@code MessageStatus.ACCEPTED}
     *                  contains ids of the clients enumerated in the {@code text} field
     *                  of the message separated by comas.
     * */
    private Message getRoomMembers(ClientListener clientListener, Message message) {
        if (clientListener.isMessageNotFromThisLoggedClient(message)) {
            return new Message(MessageStatus.DENIED).setText("Log in prior to request information");
        }
        if (message.getRoomId() == null) {
            return new Message(MessageStatus.ERROR).setText("Unspecified roomId");
        }
        int roomId = message.getRoomId();
        if (RoomProcessing.hasRoomBeenCreated(clientListener.getServer().getConfig(), roomId) == 0) {
            return new Message(MessageStatus.ERROR)
                    .setText(buildMessage("Unable to find the room (id", roomId, ')'));
        }
        Room room;
        if (!clientListener.getServer().getOnlineRooms().safe().containsKey(roomId)) {
            RoomProcessing.loadRoom(clientListener.getServer(), roomId);
        }
        room = clientListener.getServer().getOnlineRooms().safe().get(roomId);
        StringBuilder stringBuilder = new StringBuilder();
        synchronized (room.getMembers().safe()) {
            for (int clientId : room.getMembers().safe()) {
                stringBuilder.append(clientId).append(",");
            }
        }
        return new Message(MessageStatus.ACCEPTED)
                .setText(stringBuilder.substring(0, stringBuilder.length() - 1)).setRoomId(roomId);
    }
}