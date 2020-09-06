package examplefuncsplayer;

import battlecode.common.*;

public class Refinery {
    RobotController rc;
    Communication comm;
    Direction[] directions = new Direction[] {
            Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
            Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
    };
    boolean sent = false;

    public Refinery(RobotController r) throws GameActionException{
        rc = r;
        comm = new Communication(rc);
        Message m = new Message(MessageType.REF_LOC, rc.getLocation().x, rc.getLocation().y,rc.getID());
        m.setPriority(MessagePriority.MEDIUM);
        this.comm.addmes(m);
        this.comm.sendallmes();
        run();
    }
    public void run() throws GameActionException{
        for (Direction dir : directions){
            if (rc.canSenseLocation(rc.getLocation().add(dir))){
                if (rc.senseFlooding(rc.getLocation().add(dir))){
                    if ((rc.senseElevation(rc.getLocation()) - GameConstants.getWaterLevel(rc.getRoundNum()) <= 3) && (!sent)){
                        Message m = new Message(MessageType.REF_FLOODED, rc.getLocation().x, rc.getLocation().y,rc.getID());
                        m.setPriority(MessagePriority.MEDIUM);
                        this.comm.addmes(m);
                        this.comm.sendallmes();
                        sent = true;
                    }
                }
            }
        }
    }
}
