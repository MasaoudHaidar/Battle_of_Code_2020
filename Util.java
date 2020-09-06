package examplefuncsplayer;

import java.util.ArrayList;
import battlecode.common.*;

public class Util {
	static RobotController rc;
	
    static Direction[] directions = {
			Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
			Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
		};
    
	public Util(RobotController r) throws GameActionException {
		rc = r;
	}
	
    static public Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }
    
    /**
     * Attempts to move in a given direction.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
    	if(canMove(dir)) {
    		rc.move(dir);
    		return true;
		} else {
			return false;
		}
    }
    
    static boolean canMove(Direction dir) throws GameActionException {
    	if(rc.canMove(dir) && rc.canSenseLocation(rc.getLocation().add(dir)) && !rc.senseFlooding(rc.getLocation().add(dir))) {
    		return true;
    	} else {
    		return false;
    	}
    }
    

    /**
     * Attempts to build a given robot in a given direction.
     *
     * @param type The type of the robot to build
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to mine soup in a given direction.
     *
     * @param dir The intended direction of mining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    /**
     * Attempts to refine soup in a given direction.
     *
     * @param dir The intended direction of refining
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }
    
    
    public static ArrayList<Integer> computeMessageChecksum(MessageType mt, ArrayList<Integer> c) {
		ArrayList<Integer> ret = new ArrayList<Integer>();
		if(c.size() == 3) {
			if(rc.getTeam().equals(Team.A)) {
				ret.add( (mt.getVal()^c.get(1)) );
				ret.add( (mt.getVal()+c.get(1))%23 );
				ret.add( (c.get(0)+c.get(1)+c.get(2))%101 );
			} else {
				ret.add( (mt.getVal()+100*c.get(2)) % 233 );
				ret.add( (mt.getVal()+c.get(1)) % 23 );
				ret.add( (c.get(0)+c.get(1)+c.get(2))%101 );
			}
			
			return ret;
		} else {
			return null;
		}
	}
    
    public static boolean isMessageValid(Transaction trans) {
    	int[] m = trans.getMessage();
    	
    	// Get the first integer of the message, if it is not a message type then return false
    	if(!MessageType.isValid(m[0])) {
    		return false;
    	}
    
    	MessageType mt = MessageType.getMessageType(m[0]);
    	
    	// construct message into arraylist
    	ArrayList<Integer> message = new ArrayList<Integer>();
    	message.add(m[1]);
    	message.add(m[2]);
    	message.add(m[3]);
    	ArrayList<Integer> checksum = computeMessageChecksum(mt, message);
    	
    	// construct checksum into arraylist
    	ArrayList<Integer> messageCheck = new ArrayList<Integer>();
    	messageCheck.add(m[4]);
    	messageCheck.add(m[5]);
    	messageCheck.add(m[6]);
    	return checksum.equals(messageCheck);
    }
    
    // convert transaction to message without checking if it is valid
    public static Message transaction2Message(Transaction trans) {
    	int[] m = trans.getMessage();
    	MessageType mt = MessageType.getMessageType(m[0]);
    	return new Message(mt, m[1], m[2], m[3]);
    }
    
    public static void netGunCode() throws GameActionException {
    	RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
		
		// Get shootable drones
		ArrayList<RobotInfo> enemyDrones = new ArrayList<RobotInfo>();
		for(RobotInfo ri : nearbyRobots) {
			if(ri.getTeam() != rc.getTeam() && ri.getType() == RobotType.DELIVERY_DRONE) {
				// If the robot is from the other team and is a drone
				enemyDrones.add(ri);
			}
		}
		
		// Get drone that is closest to the net gun
		int minSqDist = Integer.MAX_VALUE/2;
		RobotInfo droneToBeShoot = null;
		for(RobotInfo ri : enemyDrones) {
			int dist = rc.getLocation().distanceSquaredTo(ri.location);
			if(dist < minSqDist) {
				minSqDist = dist;
				droneToBeShoot = ri;
			}
		}
		
		// Shoot drone
		if(droneToBeShoot != null) {
			if(rc.canShootUnit(droneToBeShoot.ID)) {
				rc.shootUnit(droneToBeShoot.ID);
			}
		}
    }
}
