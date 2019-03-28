package server.handlers.factory;

import common.entities.message.Message;
import server.client.ClientListener;
import server.handlers.*;

import javax.naming.OperationNotSupportedException;

import static common.Utils.buildMessage;

public class RequestHandlerFacotrImpl implements RequestHandlerFactory {

    public RequestHandlerFacotrImpl() {
    }

    @Override
    public RequestHandler getFor(ClientListener clientListener, Message message) throws OperationNotSupportedException {
        switch (message.getStatus()) {
            case AUTH:
                return new AuthorizationRequestHandler(clientListener, message);
            case REGISTRATION:
                return new RegistrationRequestHandler(clientListener, message);
            case MESSAGE:
                return new MessageSendingRequestHandler(clientListener, message);
            case CLIENTBAN:
                return new ClientBanRequestHandler(clientListener, message);
            case CLIENTUNBAN:
                return new ClientUnbanRequestHandler(clientListener, message);
            case CREATE_ROOM:
                return new CreateRoomRequestHandler(clientListener, message);
            case DELETE_ROOM:
                return new DeleteRoomRequestHandler(clientListener, message);
            case INVITE_CLIENT:
                return new InviteClientRequestHandler(clientListener, message);
            case UNINVITE_CLIENT:
                return new UninviteClientRequestHandler(clientListener, message);
            case STOP_SERVER:
                return new StopServerRequestHandler(clientListener, message);
            case ROOM_LIST:
                return new RoomListRequestHandler(clientListener, message);
            case RESTART_SERVER:
                return new RestartRequestHandler(clientListener, message);
            case ROOM_MEMBERS:
                return new RoomMembersRequestHandler(clientListener, message);
            case MESSAGE_HISTORY:
                return new MessageHistoryRequestHandler(clientListener, message);
            case GET_CLIENT_NAME:
                return new ClientNameRequesHandler(clientListener, message);
                default:
                    throw new OperationNotSupportedException(buildMessage("Unable to handle a message of status", message.getStatus()));
        }
    }
}