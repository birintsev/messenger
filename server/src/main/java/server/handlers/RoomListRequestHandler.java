package server.handlers;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import server.client.ClientListener;

public class RoomListRequestHandler extends RequestHandler {

    public RoomListRequestHandler() {
    }

    @Override
    public Message handle(ClientListener clientListener, Message message) {
        return getRooms(clientListener, message);
    }

    private synchronized Message getRooms(ClientListener clientListener, Message message) {
        if (clientListener.isMessageNotFromThisLoggedClient(message)) {
            return new Message(MessageStatus.ERROR).setText("Log in prior");
        }
        if (clientListener.getClient().getRooms().safe().size() == 0) {
            return new Message(MessageStatus.ROOM_LIST).setText("");
        }
        StringBuilder stringBuilder = new StringBuilder();
        synchronized (clientListener.getClient().getRooms().safe()) {
            for (int roomId : clientListener.getClient().getRooms().safe()) {
                stringBuilder.append(roomId).append(',');
            }
        }
        return new Message(MessageStatus.ROOM_LIST).setText(stringBuilder.substring(0, stringBuilder.length() - 1));
    }
}