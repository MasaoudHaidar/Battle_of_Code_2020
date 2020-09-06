package examplefuncsplayer;

import battlecode.common.*;


public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] directions = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST,
                                    Direction.NORTHEAST,Direction.NORTHWEST,Direction.SOUTHEAST,Direction.SOUTHWEST};
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
            RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    static int turnCount;
    static int lastdir;
    static Direction lastcordir=Direction.CENTER;
    static MapLocation hqLoc;
    static MapLocation soupLoc=null;
    static int minerCnt=0;
    static Direction hqminers=Direction.SOUTH;
    static int nxtosoup=0;

    static MapLocation mylandloc=null;
    
    static Hq hq;
    static Miner miner;
    static Refinery refinery;
    static DesignSchool design;
    static Drone drone;
    static Fulfillment fullfillment;
    static NetGun netgun;
    static Landscaper landscaper;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    //@SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        Util util = new Util(rc);
        
        turnCount = 0;
        lastdir=1;
        
        switch (rc.getType()) {
	        case HQ: {
	        	hq = new Hq(rc);
	        	break;
	        }
	        case MINER: {
	        	miner = new Miner(rc);
	        	break;
	        }
	        case REFINERY: {
	            refinery = new Refinery(rc);
	        	break;
	        }
	        case VAPORATOR: {
	        	break;
	        }
	        case DESIGN_SCHOOL: {
	        	design = new DesignSchool(rc);
	        	break;
	        }
	        case FULFILLMENT_CENTER: {
	        	fullfillment = new Fulfillment(rc);
	        	break;
	        }
	        case LANDSCAPER: {
	            landscaper = new Landscaper(rc);
	        	break;
	        }
	        case DELIVERY_DRONE: {
	        	drone = new Drone(rc);
	        	break;
	        }
	        case NET_GUN: {
	        	netgun = new NetGun(rc);
	        	break;
	        }
	        case COW: {
	        	break;
	        }
	    }

        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
                    case COW:                                        break;
                }
                
                System.out.println("Roundnum: " + rc.getRoundNum() + " Bytecode used:" + Clock.getBytecodeNum());
                
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    static void runHQ() throws GameActionException {
        hq.run();
    }
    
    static void runMiner() throws GameActionException {
    	miner.run();
    }

    static void runRefinery() throws GameActionException {
        refinery.run();
    }

    static void runVaporator() throws GameActionException {
    }

    static void runDesignSchool() throws GameActionException {
    	design.run();
    }

    static void runFulfillmentCenter() throws GameActionException {
    }

    // ---------- ---------- ---------- ---------- ---------- ---------- ----------
	static int[][] elevationData;
	
	static MapLocation getHqCoord() {
		return hqLoc;
	}
	
	static void runLandscaper() throws GameActionException {
        landscaper.run();
    }
	
	static void updateElevationData() throws GameActionException {
		// Note: max 8192 bytecode for miner
		
		// Get elevation information of the entire sensable areas
		MapLocation myLocation = rc.getLocation();
		int sensorRadiusSquared = rc.getCurrentSensorRadiusSquared();
		int sensorRadius = (int) Math.ceil(Math.sqrt(sensorRadiusSquared));
				
		for(int x=Math.max(0, myLocation.x - sensorRadius); x<=Math.min(rc.getMapWidth(), myLocation.x + sensorRadius); x++) {
			for(int y=Math.max(0, myLocation.y - sensorRadius); y<=Math.min(rc.getMapHeight(), myLocation.y + sensorRadius); y++) {
				if(Math.pow(x-myLocation.x, 2) + Math.pow(y-myLocation.y, 2) <= sensorRadiusSquared) {
					int elevation = rc.senseElevation(new MapLocation(x,y));
					elevationData[x][y] = elevation;
				}
			}
		}
	}
    static void runDeliveryDrone() throws GameActionException {
    }

    static void runNetGun() throws GameActionException {
    	netgun.run();
    }


    //Tries moving to given location
    static int goTo(MapLocation target) throws GameActionException{
        if (rc.getType()== RobotType.MINER || rc.getType()==RobotType.DELIVERY_DRONE) return goTo_miner(target);
        return 0;
    }

    static int goTo_miner(MapLocation target) throws GameActionException{

        if(!rc.isReady()) return 0;

        Direction dir = mydirto(target);
        if (lastcordir!=dir && lastcordir!=Direction.CENTER){
            lastdir=lastdir-2;
        }

        if (rc.canMove(dir))
        {
            rc.move(dir);
            lastdir=1;
            return 1;
        }
        if (lastdir!=4 && rc.canMove(dir.rotateRight())){
            rc.move(dir.rotateRight());
            lastdir=1;
            return 1;
        }
        if (lastdir!=5 && rc.canMove(dir.rotateLeft())) {
            rc.move(dir.rotateLeft());
            lastdir=1;
            return 1;
        }
        if (lastdir!=5 && lastdir!=3 && rc.canMove(dir.rotateLeft().rotateLeft())){
            rc.move(dir.rotateLeft().rotateLeft());
            lastdir=2;
            return 2;
        }
        if (lastdir!=4 && lastdir!=2 && rc.canMove(dir.rotateRight().rotateRight())){
            rc.move(dir.rotateRight().rotateRight());
            lastdir=3;
            return 3;
        }
        if (lastdir!=5 && lastdir!=3 && rc.canMove(dir.rotateLeft().rotateLeft().rotateLeft())){
            rc.move(dir.rotateLeft().rotateLeft().rotateLeft());
            lastdir=4;
            return 4;
        }
        if (lastdir!=4 && lastdir!=2 && rc.canMove(dir.rotateRight().rotateRight().rotateRight())){
            rc.move(dir.rotateRight().rotateRight().rotateRight());
            lastdir=5;
            return 5;
        }

        return lastdir=0;

    }



    static Direction mydirto(MapLocation target) throws GameActionException{
        if (target.x<rc.getLocation().x && target.y<rc.getLocation().y){
            return Direction.SOUTHWEST;
        }
        else if (target.x>rc.getLocation().x && target.y>rc.getLocation().y){
            return Direction.NORTHEAST;
        }
        else if (target.x>rc.getLocation().x && target.y<rc.getLocation().y){
            return Direction.SOUTHEAST;
        }
        else if (target.x<rc.getLocation().x && target.y>rc.getLocation().y){
            return Direction.NORTHWEST;
        }
        else if (target.x==rc.getLocation().x && target.y<rc.getLocation().y){
            return Direction.SOUTH;
        }
        else if (target.x==rc.getLocation().x && target.y>rc.getLocation().y){
            return Direction.NORTH;
        }
        else if (target.x<rc.getLocation().x && target.y==rc.getLocation().y){
            return Direction.WEST;
        }
        else if (target.x>rc.getLocation().x && target.y==rc.getLocation().y){
            return Direction.EAST;
        }
        else return Direction.CENTER;
    }

    static int getsoupLoc() throws GameActionException{
        int x0 = 0;
        if (rc.getLocation().x>=5) x0=rc.getLocation().x-5;
        int y0 = 0;
        if (rc.getLocation().y>=5) y0=rc.getLocation().y-5;

        int x1 = rc.getMapWidth()-1;
        if (rc.getLocation().x<rc.getMapWidth()-5) x1=rc.getLocation().x+5;
        int y1 = rc.getMapHeight()-1;
        if (rc.getLocation().y<rc.getMapHeight()-5) y1=rc.getLocation().y+5;

        for (int i=x0;i<=x1;i++)
            for (int j=y0;j<=y1;j++){
                MapLocation loc=new MapLocation(i,j);
                if (rc.getLocation().isWithinDistanceSquared(loc,rc.getCurrentSensorRadiusSquared()) && rc.senseSoup(loc)>0){
                    soupLoc=loc;
                    return 1;
                }
            }
        return 0;
    }

}
