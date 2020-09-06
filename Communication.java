package examplefuncsplayer;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.LinkedList;

public class Communication {
	RobotController rc;
//	int rt;
	LinkedList<Message> queue=new LinkedList<Message>();
	
	public Communication(RobotController r) {
		rc = r;
		queue = new LinkedList<Message>();
	}
	
	void addmes(Message message) {
		queue.add(message);
	}
	
	
	// returns true if successfully sends a message
	boolean sendmes(int price) throws GameActionException {
		Message message = queue.peekFirst();
		int[] transactionMessage = message.toTransactionMessage();
        if (rc.canSubmitTransaction(transactionMessage,price)) {
        	queue.removeFirst();
            rc.submitTransaction(transactionMessage,price);
            return true;
        } else {
        	return false;
        }
    }
	
	boolean sendmes(MessagePriority priority) throws GameActionException {
		// Check transaction prices in the last round
		if(rc.getRoundNum() > 1) {
			Transaction[] ts = rc.getBlock(rc.getRoundNum()-1);
			int[] prices = new int[ts.length];
			int minPrice = Integer.MAX_VALUE/2, sum=0, maxPrice=Integer.MIN_VALUE/2;
			for(int i=0;i<ts.length;++i) {
				prices[i] = ts[i].getCost();
				if(prices[i] < minPrice) {
					minPrice = prices[i];
				}
				if(prices[i] > maxPrice) {
					maxPrice = prices[i];
				}
				sum += prices[i];
			}
			if(priority.equals(MessagePriority.LOW)) {
				return sendmes(minPrice);
			} else if(priority.equals(MessagePriority.MEDIUM)) {
				return sendmes(sum/ts.length);
			} else if(priority.equals(MessagePriority.HIGH)) {
				return sendmes(maxPrice);
			} else {
				return false;
			}
		} else {
			return sendmes(1);
		}
	}
	
	void sendallmes() throws GameActionException {
		if(queue.size() == 0) {
			return;
		}
		
		// determine prices for the messages depending on the last block
		int minPrice = Integer.MAX_VALUE/2, sum=0, maxPrice=Integer.MIN_VALUE/2, avg;
		if(rc.getRoundNum() > 1) {
			Transaction[] ts = rc.getBlock(rc.getRoundNum()-1);
			if(ts.length > 0) {
				int[] prices = new int[ts.length];
				for(int i=0;i<ts.length;++i) {
					prices[i] = ts[i].getCost();
					if(prices[i] < minPrice) {
						minPrice = prices[i];
					}
					if(prices[i] > maxPrice) {
						maxPrice = prices[i];
					}
					sum += prices[i];
				}
				avg = sum/ts.length;
			} else {
				minPrice = maxPrice = avg = 1;
			}
		} else {
			minPrice = maxPrice = avg = 1;
		}
		
		// try to send all the messages in the queue
		for(int i=0;i<queue.size();++i) {
			if(Clock.getBytecodesLeft() < Clock.getBytecodeNum()*0.7) {
				return;
			}
			Message m = queue.get(i);
			int price=1;
			switch (m.getPriority()) {
				case HIGH:
					price = (avg+maxPrice)/2;
					break;
				case MEDIUM:
					price = avg;
					break;
				case LOW:
					price = Math.max(minPrice-1, 1);
					break;
				case ONE:
					price = 1;
					break;
			}
			
			int[] transactionMessage = m.toTransactionMessage();
			if(rc.canSubmitTransaction(transactionMessage, price)) {
				queue.remove(i);
				rc.submitTransaction(transactionMessage, price);
			}
		}
	}
	
	// Search for all messages in roundnum and return all the messages that is sent by us
	ArrayList<Message> recmes(int roundnm) throws GameActionException {
		if(roundnm > rc.getRoundNum()) {
			return null;
		} else {
			ArrayList<Message> ret = new ArrayList<Message>();
			Transaction[] ts = rc.getBlock(roundnm);
			for(Transaction t: ts) {
				if(Util.isMessageValid(t)) {
					ret.add(Util.transaction2Message(t));
				}
			}
			return ret;
		}
	}
}
