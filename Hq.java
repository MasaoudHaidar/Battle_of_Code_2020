package examplefuncsplayer;

import battlecode.common.*;

import java.util.*;
public class Hq {
	RobotController rc;
	Communication comm;
	
	ArrayList<Integer> minerIds;

	ArrayList<MapLocation> refLocations;
	ArrayList<MapLocation> approvedRefReqs;
	ArrayList<Integer> approvedIDs;
	LinkedList<MapLocation> sentOrds;
	int wait;
	int defaultWait = 100;
	
	Direction[] directions = new Direction[] {
			Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
			Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
	};
	int minerCnt = 0;
	
	public Hq(RobotController r) throws GameActionException {
		rc = r;
		int wait = 0;
		comm = new Communication(rc);

		refLocations = new ArrayList<MapLocation>();
		approvedRefReqs = new ArrayList<MapLocation>();
		sentOrds = new LinkedList<MapLocation>();
		approvedIDs = new ArrayList<Integer>();
		minerIds = new ArrayList<Integer>();

		// Broadcast hq location
        Message mess=new Message(MessageType.HQ_LOC,rc.getLocation().x,rc.getLocation().y,0);
        comm.addmes(mess);
        
		run();
	}
	

	public void run() throws GameActionException {
		Util.netGunCode();
		if(wait >0){
			wait -= 1;
		}
		if(wait == 0){
			wait = defaultWait;
			if (sentOrds.size() > 0){
				sentOrds.removeFirst();
			}
		}
		if (rc.getRoundNum() == 1){
			MapLocation [] nearbySoup = rc.senseNearbySoup();
			if (nearbySoup.length <= 6){
				for (MapLocation loc : nearbySoup){
					Message m = new Message(MessageType.FOUND_SOUP,
							loc.x,
							loc.y,
							121);
					m.setPriority(MessagePriority.ONE);
					this.comm.addmes(m);
					this.comm.sendallmes();
				}
			} else {
				ArrayList<MapLocation> soupToBroadcast = new ArrayList<MapLocation>();
				for (MapLocation loc: nearbySoup){
					boolean addToSoupToBroadcast = true;
					for (MapLocation chosenLoc : soupToBroadcast){
						if (closeEnough(chosenLoc, loc)){
							addToSoupToBroadcast = false;
						}
					}
					if(addToSoupToBroadcast){
						soupToBroadcast.add(loc);
					}
				}

				for (MapLocation loc : soupToBroadcast){
					Message m = new Message(MessageType.FOUND_SOUP,
							loc.x,
							loc.y,
							121);
					m.setPriority(MessagePriority.ONE);
					this.comm.addmes(m);
					this.comm.sendallmes();
				}


			}

		}
		comm.sendallmes();
		if (rc.getRoundNum()>1)
			readMessages();
		sendOrds();

		if (minerCnt<7){
            if (rc.getTeamSoup()>=70){
            	if (rc.getRoundNum()>1) {
            		for (Direction dir:directions) {
            			if (Util.tryBuild(RobotType.MINER, dir)) {
            				minerIds.add(rc.senseRobotAtLocation(rc.getLocation().add(dir)).ID);
            				minerCnt++;
            				return;
            			}
            		}
            	}
            	else {
            		for (Direction dir:directions) {
            			MapLocation m=rc.getLocation();
            			boolean isgood=true;
            			for (int i=0;isgood & i<3;i++) {
            				if(!rc.canSenseLocation(m) || !rc.canSenseLocation(m.add(dir))) {
            					isgood = false;
            				} else {
                				int x1=rc.senseElevation(m);
                				int x2=rc.senseElevation(m.add(dir));
                				if (Math.abs(x1-x2)>3 || rc.senseFlooding(m.add(dir))) {
                					isgood=false;
                				} else {
                    				m=m.add(dir);                					
                				}
            				}
            			}
            			if (isgood && rc.canBuildRobot(RobotType.MINER, dir)) {
            				rc.buildRobot(RobotType.MINER, dir);
            				minerCnt++;
            				return;
            			}
            		}
            		//if we reach here, we might be in trouble
            		for (Direction dir:directions) {
            			if (Util.tryBuild(RobotType.MINER, dir)) {
            				minerCnt++;
            				return;
            			}
            		}
            		
            	}
                
            }
        } else{

        }
        return;
	}

	public void readMessages() throws  GameActionException{
		if(rc.getRoundNum() == 1) {
			return;
		}
		ArrayList<Message> mess= this.comm.recmes(rc.getRoundNum()-1);
		for (Message i:mess){
			if ((i.getType() == MessageType.REF_REQ ) ){
				MapLocation loc = new MapLocation(i.getContent().get(0), i.getContent().get(1));
				boolean old = false;
				for (MapLocation j : refLocations){
					if (closeEnough(j, loc)) {
						old = true;
						break;
					}
					
				}
				for (MapLocation j : approvedRefReqs){
					if (closeEnough(j, loc)) {
						old = true;
						break;
					}
				}
				for (MapLocation j : sentOrds){
					if (closeEnough(j, loc)) {
						old = true;
						break;
					}
				}
				if(old){
					continue;
				}
				while (rc.getLocation().distanceSquaredTo(loc) < 25){
					Direction dir = rc.getLocation().directionTo(loc);
					loc.add(dir);
				}
				approvedRefReqs.add(loc);
				approvedIDs.add(i.getContent().get(2));
			}
			if(i.getType() == MessageType.REF_LOC){
				MapLocation newRefLocation = new MapLocation(i.getContent().get(0), i.getContent().get(1));
				this.addRefLocation(newRefLocation);
			}
			if(i.getType() == MessageType.REF_FLOODED){
				MapLocation newRefLocation = new MapLocation(i.getContent().get(0), i.getContent().get(1));
				this.removeRefLocation(newRefLocation);
			}
		}
	}
	public void removeRefLocation(MapLocation location){
		if (!this.locationContainedInArrayList(this.refLocations, location)){
			return;
		}
		this.removeLocationFromArrayList(this.refLocations, location);
	}
	public void removeLocationFromArrayList(ArrayList<MapLocation> list, MapLocation location){
		for (MapLocation loc:list){
			if(loc.equals(location)){
				list.remove(loc);
				break;
			}
		}
	}
	public void sendOrds() throws  GameActionException{
		while(approvedRefReqs.size() > 0){
			Message m = new Message(MessageType.REF_ORD,
					approvedRefReqs.get(approvedRefReqs.size()-1).x,
					approvedRefReqs.get(approvedRefReqs.size()-1).y,
					approvedIDs.get(approvedIDs.size()-1));
			m.setPriority(MessagePriority.MEDIUM);
			this.comm.addmes(m);
			this.comm.sendallmes();
			sentOrds.add(approvedRefReqs.get(approvedRefReqs.size()-1));
			approvedRefReqs.remove(approvedRefReqs.size()-1);
			approvedIDs.remove(approvedIDs.size()-1);
			if (wait == 0){
				wait = defaultWait;
			}
		}

	}
	public boolean closeEnough(MapLocation A, MapLocation B){
		return (A.distanceSquaredTo(B) <= 34);
	}
	public void addRefLocation(MapLocation location){
		if (this.locationContainedInArrayList(this.refLocations, location)){
			return;
		}
		this.refLocations.add(location);
	}
	public boolean locationContainedInArrayList(ArrayList<MapLocation> list, MapLocation location){
		for (MapLocation loc:list){
			if(loc.equals(location)){
				return true;
			}
		}
		return false;
	}
}
