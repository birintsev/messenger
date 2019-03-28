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
                    rh = new MessageHistoryRequestHandler(clientListener, message);
                    responseMessage = rh.handle();
                    break;
                case GET_CLIENT_NAME:
                    rh = new ClientNameRequesHandler(clientListener, message);
                    responseMessage = rh.handle();
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




}