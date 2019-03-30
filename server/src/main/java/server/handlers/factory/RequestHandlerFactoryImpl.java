package server.handlers.factory;

import common.entities.message.Message;
import common.entities.message.MessageStatus;
import server.client.ClientListener;
import server.handlers.*;

import javax.naming.OperationNotSupportedException;

import java.util.HashMap;
import java.util.Map;

import static common.Utils.buildMessage;
import static common.entities.message.MessageStatus.*;

public class RequestHandlerFactoryImpl implements RequestHandlerFactory {

    private static final Map<MessageStatus, RequestHandler> map;

    static {
        map = new HashMap<>();
        map.put(AUTH, new AuthorizationRequestHandler());
        map.put(REGISTRATION, new RegistrationRequestHandler());
        map.put(MESSAGE, new MessageSendingRequestHandler());
        map.put(CLIENT_BAN, new ClientBanRequestHandler());
        map.put(CLIENT_UNBAN, new ClientUnbanRequestHandler());
        map.put(CREATE_ROOM, new CreateRoomRequestHandler());
        map.put(DELETE_ROOM, new DeleteRoomRequestHandler());
        map.put(INVITE_CLIENT, new InviteClientRequestHandler());
        map.put(UNINVITE_CLIENT, new UninviteClientRequestHandler());
        map.put(STOP_SERVER, new StopServerRequestHandler());
        map.put(ROOM_LIST, new RoomListRequestHandler());
        map.put(RESTART_SERVER, new RestartRequestHandler());
        map.put(ROOM_MEMBERS, new RoomMembersRequestHandler());
        map.put(MESSAGE_HISTORY, new MessageHistoryRequestHandler());
        map.put(GET_CLIENT_NAME, new ClientNameRequestHandler());
    }

    public RequestHandlerFactoryImpl() {
    }

    @Override
    public RequestHandler getFor(ClientListener clientListener, Message message) throws OperationNotSupportedException {
        RequestHandler rh = map.get(message.getStatus());
        if (rh == null) {
            throw new OperationNotSupportedException(
                    buildMessage("Unable to handle a message of status:", message.getStatus()));
        }

        return rh;
    }
}