package server.handlers;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import org.apache.log4j.Level;
import server.client.ClientListener;
import server.exceptions.RoomNotFoundException;
import server.processing.RoomProcessing;
import server.room.Room;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.StringWriter;

import static common.Utils.buildMessage;

public class MessageHistoryRequestHandler extends RequestHandler {

    public MessageHistoryRequestHandler() {
    }

    @Override
    public Message handle(ClientListener clientListener, Message message) {
        return getRoomMessages(clientListener, message);
    }

    /**
     *  This method handles with the request for the list of the client rooms
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
    private synchronized Message getRoomMessages(ClientListener clientListener, Message message) {
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
}