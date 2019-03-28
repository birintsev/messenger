package server.handlers;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import server.client.ClientListener;

public class RoomListRequestHandler extends RequestHandler {

    public RoomListRequestHandler(ClientListener clientListener, Message message) {
        super(clientListener, message);
    }

    @Override
    public Message handle() {
        Message responseMessage = getRooms(message);
        return responseMessage;
    }

    private synchronized Message getRooms(Message message) {
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