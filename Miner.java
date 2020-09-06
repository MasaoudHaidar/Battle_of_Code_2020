package examplefuncsplayer;
import battlecode.common.*;

import java.util.*;

enum minerType {
	BUILD_DESFUL, // Builder Design school & fulfillment
	BUILD_ENEMY_NETGUN, // Build netgun
	FIND_SOUP, // Find soup
}

public strictfp class Miner {
	RobotController rc;
	
	Communication comm;
	Navigation nav;

	Direction[] defaultDirections;
	minerType type;
	int turnCount = 0;
	MapLocation hqLoc;
	ArrayList<MapLocation> soupLocations; // stores soup locations
	ArrayList<MapLocation> refLocations;  // stores ref locations
	boolean hasDesign = false;
	boolean hasFulfill = false;
	boolean shouldBuild;
	MapLocation reqRefLoc;
	Direction defaultDirection;
	Direction builddir=Direction.NORTH;
	Direction lastDir = Direction.CENTER;
	Direction[] directions = new Direction[] {
			Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
			Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
	};
	int nagWait;
	LinkedList<MapLocation> recentReqs;
	public Miner(RobotController r) throws GameActionException {
		rc = r;
		comm = new Communication(rc);
		nav = new Navigation(rc);

		recentReqs = new LinkedList<MapLocation>();
		soupLocations = new ArrayList<MapLocation>();
		refLocations = new ArrayList<MapLocation>();
		shouldBuild = false;
		reqRefLoc = new MapLocation(0,0);
		nagWait = 10;
		
		
		// concrete way of knowing the hq location
		RobotInfo[] nearRobots = rc.senseNearbyRobots();
		for (RobotInfo ri : nearRobots) {
			if(ri.getType() == RobotType.HQ) {
				hqLoc = ri.getLocation();
				break;
			}
		}

		// choosing phantom direction
		defaultDirection = directions[rc.getRoundNum() % 8];
		defaultDirections = new Direction[] {defaultDirection, defaultDirection.rotateRight(),
				defaultDirection.rotateLeft(), defaultDirection.rotateLeft().rotateLeft(),
				defaultDirection.rotateRight().rotateRight(),  defaultDirection.rotateRight().rotateRight().rotateRight(),
				defaultDirection.rotateLeft().rotateLeft().rotateLeft(),
				defaultDirection.opposite()};

		
		if(rc.getRoundNum() == 2) {
			type = minerType.BUILD_DESFUL;
			MapLocation[] locs = {
				hqLoc.add(Direction.NORTH).add(Direction.NORTH),
				hqLoc.add(Direction.NORTHEAST).add(Direction.NORTHEAST),
				hqLoc.add(Direction.EAST).add(Direction.EAST),
				hqLoc.add(Direction.SOUTHEAST).add(Direction.SOUTHEAST),
				hqLoc.add(Direction.SOUTH).add(Direction.SOUTH),
				hqLoc.add(Direction.SOUTHWEST).add(Direction.SOUTHWEST),
				hqLoc.add(Direction.WEST).add(Direction.WEST),
				hqLoc.add(Direction.NORTHWEST).add(Direction.NORTHWEST),
			};
			for(MapLocation loc : locs) {
				if(rc.onTheMap(loc)) {
					desfulLocs.add(loc);
				}
			}
			builddir=rc.getLocation().directionTo(hqLoc).opposite();
		} else {
			type = minerType.FIND_SOUP;
		}
		
		run();
	}
	
	static int state = 0; // Counter to check which step the miner is in
	
	private ArrayList<MapLocation> desfulLocs = new ArrayList<MapLocation>();
	private int desfulLocsidx = 0;
	public void run() throws GameActionException{
		turnCount++;
		if(type == minerType.BUILD_DESFUL) {
			if (hasDesign == false || hasFulfill == false) {
				// Force make design schools
				if(hasDesign == false && rc.getRoundNum() > 75) {
					// Build design school as further away from hq as possible
					ArrayList<Integer> locs = new ArrayList<Integer>();
					for(int i=0;i<directions.length;++i) {
						// Concatenate distance to direction index
						locs.add(rc.getLocation().distanceSquaredTo(hqLoc)*10+i);
					}
					Collections.sort(locs);
					for(int loc : locs) {
						if(Util.tryBuild(RobotType.DESIGN_SCHOOL, directions[loc%10])) {
							hasDesign = true;
							return;
						}
					}
				}
				if (rc.getLocation().distanceSquaredTo(hqLoc)<9) {
					if (Util.tryMove(builddir)) {
						
					}
					else {
						//oops
					}
				} else {
					boolean canBuildButDoesntHaveSoup = false;
					Direction dir=builddir;
					for (int i=0;i<8;i++) {
						if (rc.getLocation().add(dir).distanceSquaredTo(hqLoc)>8) {
							// Build design school if possible
							if(hasDesign == false) {
								if(Util.tryBuild(RobotType.DESIGN_SCHOOL, dir)) {
									hasDesign = true;
									return;
								} else if(rc.getTeamSoup() < 150) {
									canBuildButDoesntHaveSoup = true;
								}
							}
							
							// Build fulfillment center if possible
							if(hasFulfill == false) {
								if(Util.tryBuild(RobotType.FULFILLMENT_CENTER, dir)) {
									hasFulfill = true;
									return;
								} else if(rc.getTeamSoup() < 150) {
									canBuildButDoesntHaveSoup = true;
								}
							}
						}
						dir.rotateRight();
					}
					if (!canBuildButDoesntHaveSoup) {
						Direction direc=builddir;
						for (int i=0;i<8;i++) {
							if (rc.getLocation().add(direc).distanceSquaredTo(hqLoc)
									<rc.getLocation().distanceSquaredTo(hqLoc)) {
								if (Util.tryMove(direc)) {
									return;
								}
							}
							direc=direc.rotateLeft();
						}
					}
				}
			} else {
				type = minerType.FIND_SOUP;
			}
		}  else if(type == minerType.FIND_SOUP) {
			if(nagWait > 0){
				nagWait -= 1;
			}
			if(nagWait == 0){
				recentReqs.clear();
			}

			this.readMessages();
			checkForEmptySoup();
			this.lookAroundForSoup();
			if(shouldBuild){
				buildRef();
			}
			if (shouldRefine()){
				return;
			}
			if (this.soupLocations.size() > 0){
				MapLocation closest = this.closestInSoupLocations();
				if (this.isNeighbour(closest)){
					for (Direction dir : directions){
						if(rc.canMineSoup(dir)){
							if(!rc.canSenseLocation(closestInRefLocations() )&& !shouldNotRequest()){
								nagWait = 10;
								recentReqs.add(closest);
								Message m = new Message(MessageType.REF_REQ, closest.x, closest.y,rc.getID());
								m.setPriority(examplefuncsplayer.MessagePriority.LOW);
								this.comm.addmes(m);
								this.comm.sendallmes();
							}
							rc.mineSoup(dir);
							return;
						}
					}
				} else {
					System.out.println("Moving to closest: " + closest);
					nav.goTo(closest);
				}
			}
			for (Direction dir :  defaultDirections ){
				if(Util.tryMove(dir)){
					return;
				}
			}
		}
	}
	private MapLocation prevClosestSoup = null;
	public  boolean shouldNotRequest(){
		for(MapLocation loc : recentReqs){
			if(rc.canSenseLocation(loc)){
				return true;
			}
		}
		return false;
	}
	public void buildRef() throws GameActionException {
		System.out.println("reqRefLoc: ");
		for (Direction dir : directions){
			if(rc.getLocation().add(dir).distanceSquaredTo(hqLoc) >= 9 &&
					rc.getLocation().add(dir).distanceSquaredTo(reqRefLoc) < 19 &&
					rc.canBuildRobot(RobotType.REFINERY, dir)){
				rc.buildRobot(RobotType.REFINERY, dir);
				shouldBuild = false;
				return;
			}
			if(rc.getLocation().distanceSquaredTo(reqRefLoc) < 3 ){
				reqRefLoc.add(rc.getLocation().directionTo(hqLoc).opposite()).add(directions[rc.getID()%directions.length]);
			}
		}
		if(rc.getLocation().distanceSquaredTo(reqRefLoc) >12 || rc.getLocation().distanceSquaredTo(hqLoc) < 9 ){
			nav.goTo(reqRefLoc);
		}
		return;

	}
	public boolean shouldRefine() throws GameActionException{
		if (rc.getSoupCarrying() == 0){
			return false;
		}
		MapLocation closestRef = closestInRefLocations();
		if (isNeighbour(closestRef)){
			for (Direction dir : directions){
				if (rc.canDepositSoup(dir)){
					rc.depositSoup(dir, rc.getSoupCarrying());
					return true;
				}
			}
		}
		if ((rc.getSoupCarrying() < RobotType.MINER.soupLimit) && soupLocations.size()>0 && rc.canSenseLocation(closestInSoupLocations())){
			return false;
		}
		if (! isNeighbour(closestRef)){
			nav.goTo(closestRef);
			return true;
		}
		return true;
	}
	public void checkForEmptySoup()throws GameActionException{
		for (int i = 0; i < soupLocations.size(); i++){
			if (rc.canSenseLocation(soupLocations.get(i))){
				if(rc.senseSoup(soupLocations.get(i)) == 0){
					Message m = new Message(MessageType.NO_SOUP, soupLocations.get(i).x, soupLocations.get(i).y,rc.getID());
					m.setPriority(examplefuncsplayer.MessagePriority.ONE);
					this.comm.addmes(m);
					this.comm.sendallmes();
					soupLocations.remove(i);
				}
			}
		}
	}
	public boolean isNeighbour(MapLocation location){
		return (rc.getLocation().distanceSquaredTo(location) <= 2);
	}


	 public void readMessages() throws GameActionException{
		ArrayList<Message> mess= this.comm.recmes(rc.getRoundNum()-1);
		for (Message i:mess){
			if (i.getType() == MessageType.FOUND_SOUP){
				MapLocation newSoupLocation = new MapLocation(i.getContent().get(0), i.getContent().get(1));
				this.addSoupLocation(newSoupLocation);
			}
			if (i.getType() == MessageType.REF_LOC){
				MapLocation newSoupLocation = new MapLocation(i.getContent().get(0), i.getContent().get(1));
				this.addRefLocation(newSoupLocation);
			}
			if ((i.getType() == MessageType.REF_ORD ) && (i.getContent().get(2) == rc.getID()) ){
				shouldBuild = true;
				reqRefLoc = new MapLocation(i.getContent().get(0), i.getContent().get(1));
			}
			if (i.getType() == MessageType.NO_SOUP ){
				MapLocation oldSoupLocation = new MapLocation(i.getContent().get(0), i.getContent().get(1));
				this.removeSoupLocation(oldSoupLocation);
			}
			if(i.getType() == MessageType.REF_FLOODED){
				MapLocation newRefLocation = new MapLocation(i.getContent().get(0), i.getContent().get(1));
				this.removeRefLocation(newRefLocation);
			}
		}
	}

	// Utility methods
	
	Direction getDirRelHq() throws GameActionException {
		// Get direction relative to hq
		MapLocation myLoc = rc.getLocation();
		for(Direction dir : directions) {
			if(myLoc.equals(hqLoc.add(dir))) {
				return dir;
			}
		}
		throw new GameActionException(GameActionExceptionType.OUT_OF_RANGE, "getDirRelHq can only be called when miner is near hq.");
	}
	
	boolean isDirAngle(Direction dir) {
		if(dir == Direction.NORTHEAST || dir == Direction.NORTHWEST || dir == Direction.SOUTHEAST || dir == Direction.SOUTHWEST) {
			return true;
		} else {
			return false;
		}
	}
	public MapLocation closestInArray(MapLocation[] locationArray) throws GameActionException{
		int minimum = 500000;
		MapLocation closest=new MapLocation(0,0);
		for (MapLocation i : locationArray){
			if (this.locationIsBad(i)){
				continue;
			}
			if(this.rc.getLocation().distanceSquaredTo(i) < minimum){
				closest = i;
				minimum = this.rc.getLocation().distanceSquaredTo(i);
			}
		}
		return closest;
	}
	public MapLocation closestInSoupLocations(){
		int minimum = 500000;
		MapLocation closest = new MapLocation(0,0);
		for (MapLocation i : this.soupLocations){
			if(this.rc.getLocation().distanceSquaredTo(i) < minimum){
				closest = i;
				minimum = this.rc.getLocation().distanceSquaredTo(i);
			}
		}
		return closest;
	}
	public MapLocation closestInRefLocations(){
		int minimum = 500000;
		MapLocation closest = new MapLocation(0,0);
		for (MapLocation i : this.refLocations){
			if(this.rc.getLocation().distanceSquaredTo(i) < minimum){
				closest = i;
				minimum = this.rc.getLocation().distanceSquaredTo(i);
			}
		}
		if (refLocations.size() == 0){
			return hqLoc;
		}
		return closest;
	}
	public void lookAroundForSoup() throws GameActionException {
		MapLocation[] nearbySoup = rc.senseNearbySoup();
		if (nearbySoup.length == 0){
			return;
		}
		MapLocation closest = this.closestInArray(nearbySoup);
		if (this.locationContainedInArrayList(this.soupLocations, closest)){
			return;
		}
		Message m = new Message(MessageType.FOUND_SOUP, closest.x, closest.y,rc.getID());
		m.setPriority(examplefuncsplayer.MessagePriority.LOW);
		this.comm.addmes(m);
		this.comm.sendallmes();
		this.soupLocations.add(closest);
	}
	public void addSoupLocation(MapLocation location){
		if (this.locationContainedInArrayList(this.soupLocations, location)){
			return;
		}
		this.soupLocations.add(location);
	}

	public void addRefLocation(MapLocation location){
		if (this.locationContainedInArrayList(this.refLocations, location)){
			return;
		}
		this.refLocations.add(location);
	}
	public void removeSoupLocation(MapLocation location){
		if (!this.locationContainedInArrayList(this.soupLocations, location)){
			return;
		}
		this.removeLocationFromArrayList(this.soupLocations, location);
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
	public boolean locationContainedInArrayList(ArrayList<MapLocation> list, MapLocation location){
		for (MapLocation loc:list){
			if(loc.equals(location)){
				return true;
			}
		}
		return false;
	}
	public boolean locationIsBad(MapLocation location) throws GameActionException{
		boolean answer = true;
		if (!rc.senseFlooding(location)){
			return false;
		}
		for (Direction dir: directions){
			MapLocation temp = location.add(dir);
			if (rc.canSenseLocation(temp)){
				if (!rc.senseFlooding(temp)){
					return false;
				}
			}
		}
	return answer;}
}