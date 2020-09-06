package examplefuncsplayer;

import java.util.*;

public enum MessageType {
	HQ_LOC (10),
	FOUND_SOUP (34),
    REF_LOC (67),
    REF_REQ (64),
    REF_ORD (22),
    NO_SOUP (20),
	LAND_POS (78),
    REF_FLOODED (89);
	
	private final int val;
	private MessageType(int val)
    {
        this.val = val;
    }
    
    // java does not support instantiating an enum given its value
    // the following is a hack to solve the problem
    // based on https://stackoverflow.com/a/27484447
    static Map<Integer, MessageType> map = new HashMap<>();

    static {
        for (MessageType messageType : MessageType.values()) {
            map.put(messageType.val, messageType);
        }
    }

    // int -> MessageType
    public static MessageType getMessageType(int val) {
        return map.get(val);
    }
    
    // MessageType -> int
    public int getVal() {
    	return val;
    }
    
    public static boolean isValid(int val) {
    	if(map.containsKey(val)) {
    		return true;
    	} else {
    		return false;
    	}
    }
}
