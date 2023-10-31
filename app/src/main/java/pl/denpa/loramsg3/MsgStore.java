package pl.denpa.loramsg3;

import java.util.ArrayList;
import java.util.HashMap;

public class MsgStore {

    static volatile MsgStore theoneandonly = null;

    private MsgStore() {

    }

    public static MsgStore get_instance() {
        if (theoneandonly == null) {
            synchronized (MsgStore.class) {
                if (theoneandonly == null) {
                    theoneandonly = new MsgStore();
                }
            }
        }
        return theoneandonly;
    }

    HashMap<String, ArrayList<String[]>> chats = new HashMap<>();

    //called from MainActivity onNewData sorter
    public void receive(String user, String message) {
        if (!chats.containsKey(user)) {
            chats.put(user, new ArrayList<>());
        }
        chats.get(user).add(new String[]{user, message});

        //if main fragment is a chat then
        //pass forward to it to append so full reload isn't required
    }

    //called from TerminalFragment
    public void send(String user, String message) {
        if (!chats.containsKey(user)) {
            chats.put(user, new ArrayList<>());
        }
    }

    public ArrayList<String> get_messages(String user) {
        if (chats.containsKey(user)) {
            chats.get(user);
        }
        return null;
    }

}
