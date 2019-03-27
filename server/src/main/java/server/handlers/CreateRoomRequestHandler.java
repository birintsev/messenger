package server.handlers;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import org.jetbrains.annotations.NotNull;
import server.client.ClientListener;
import server.room.Room;
import server.room.RoomProcessing;

import java.util.InvalidPropertiesFormatException;

import static common.Utils.buildMessage;

public class CreateRoomRequestHandler extends RequestHandler {
    public CreateRoomRequestHandler(ClientListener clientListener, Message message) {
        super(clientListener, message);
    }

    @Override
    public Message handle() {
        Message responseMessage = createRoom(message);
        return responseMessage;
    }

    /**
     *  The method {@code createRoom} handles with an input request representing by {@code Message}
     * having {@code MessageStatus.CREATE_ROOM} status
     *
     * @param           message a command that contains the {@code clientId} of a creator
     *
     * @return          an instance of {@code Message} that informs whether new room was created or not
     */
    private Message createRoom(@NotNull Message message) {
        if (clientListener.isMessageNotFromThisLoggedClient(message)) {
            return new Message(MessageStatus.DENIED).setText("Wrong passed clientId");
        }
        if (clientListener.isMessageNotFromThisLoggedClient(message)) {
            return new Message(MessageStatus.DENIED).setText("Please, log in first");
        }
        if (!MessageStatus.CREATE_ROOM.equals(message.getStatus())) {
            LOGGER.error(buildMessage("Unexpected message status. Expected"
                    , MessageStatus.CREATE_ROOM, ". But found", message.getStatus()));
            return new Message(MessageStatus.ERROR)
                    .setText(buildMessage("The message status must be "
                            , MessageStatus.CREATE_ROOM, " but found ", message.getStatus()));
        }
        /*
         *   The field toId is considered as an id of the initial room member, thus it must be valid
         * i.e. the client with such id must exists
         * */
        try {
            Room room = RoomProcessing.createRoom(clientListener.getServer(), message.getFromId());
            if (room == null) {
                return new Message(MessageStatus.ERROR).setText("Some error has occurred during the room creation");
            } else {
                clientListener.getClient().getRooms().safe().add(room.getRoomId());
                LOGGER.trace(new StringBuilder("New room (id").append(room.getRoomId()).append(") has been created"));
                return new Message(MessageStatus.ACCEPTED).setRoomId(room.getRoomId())
                        .setText(buildMessage("The room (id"
                                , room.getRoomId(), ") has been successfully created"));
            }
        } catch (InvalidPropertiesFormatException e) { // error while room creation
            LOGGER.error(e.getLocalizedMessage());
            return new Message(MessageStatus.ERROR).setText("Internal has error occurred");
        }
    }
}