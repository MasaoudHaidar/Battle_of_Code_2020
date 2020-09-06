package examplefuncsplayer;

import java.util.*;

public class Message {
	private MessageType messageType;
	private MessagePriority messagePriority = MessagePriority.MEDIUM;
	private ArrayList<Integer> content, checksum;
	
	public Message(MessageType mt, int x1, int x2, int x3) {
		messageType = mt;
		content = new ArrayList<Integer>();
		content.add(x1);
		content.add(x2);
		content.add(x3);
		
		// compute checksum
		checksum = Util.computeMessageChecksum(messageType, content);
	}
	
	public ArrayList<Integer> getContent() {
		return content;
	}
	
	public MessageType getType() {
		return messageType;
	}
	
	public void setPriority(MessagePriority mp) {
		messagePriority = mp; 
	}
	public MessagePriority getPriority() {
		return messagePriority;
	}
	
	public int[] toTransactionMessage() {
		int[] ret = new int[7];
		ret[0] = messageType.getVal();
		ret[1] = content.get(0);
		ret[2] = content.get(1);
		ret[3] = content.get(2);
		ret[4] = checksum.get(0);
		ret[5] = checksum.get(1);
		ret[6] = checksum.get(2);
		return ret;
	}
}
