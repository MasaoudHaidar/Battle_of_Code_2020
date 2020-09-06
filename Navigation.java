package examplefuncsplayer;
import battlecode.common.*;
import com.sun.xml.internal.xsom.impl.scd.Iterators;

import java.util.*;

public class Navigation {
	RobotController rc;
	Direction[] directions;
	
	public Navigation(RobotController r) {
		rc = r;
		
		if(((int)(Math.random()*1000))%2 == 0) {
			// circle clockwise
			directions = new Direction[] {
					Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
					Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
			};
		} else {
			// circle counter clockwise
			directions = new Direction[] {
					Direction.NORTH, Direction.NORTHWEST, Direction.WEST, Direction.SOUTHWEST,
					Direction.SOUTH, Direction.SOUTHEAST, Direction.EAST, Direction.NORTHEAST
			};
		}
		bugNavReset();
	}
	
	// return direction to go with bugNav algorithm
	// Note that it is assumed that the robot is ready to move
	private int bugNavMinDist;
	private int bugNavWaitCnt;
	private boolean bugNavIsFollowingWall;
	private int bugNavHandDirIdx;
	// The wall the hand is on bugNavHandLoc
	private int bugNavMaxWaitCnt;
	private final int bugNavHistory = 3;
	private LinkedList<MapLocation> pastLoc = new LinkedList<MapLocation>();
	private void bugNavReset() {
		System.out.println("Nav reseted");
		bugNavMinDist = Integer.MAX_VALUE/2;
		bugNavWaitCnt = 0;
		bugNavIsFollowingWall = false;
		bugNavHandDirIdx = -1;
		pastLoc.clear();
		bugNavMaxWaitCnt = ((int)(Math.random()*1000))%5+1; // Random # 1-5
	}
	public Direction bugNav(MapLocation target) throws GameActionException {
		MapLocation myLoc = rc.getLocation();
		
		if(myLoc.distanceSquaredTo(target) < bugNavMinDist) {
			bugNavMinDist = myLoc.distanceSquaredTo(target);
		}
		
		// Check if I reached target
		for(Direction dir : Direction.allDirections()) {
			MapLocation loc = myLoc.add(dir);
			if(loc.equals(target)) {
				// Reached target
				bugNavReset();
				if(Util.canMove(dir)) {
					return dir;
				} else {
					return Direction.CENTER;
				}
			}
		}
		
		// Check if there is a direction I can move forward to
		int minDist = bugNavMinDist;
		Direction minDir = Direction.CENTER;
		for(Direction dir : directions) {
			MapLocation loc = myLoc.add(dir);
			int dist = loc.distanceSquaredTo(target);
			if(dist < minDist) {
				// If that location gets me closer to the target
				if(Util.canMove(dir)) {
					// I can move there
					minDist = dist;
					minDir = dir;
				} else {
					// I can't move there
					if(bugNavWaitCnt < bugNavMaxWaitCnt && rc.canSenseLocation(loc)) {
						RobotInfo robot = rc.senseRobotAtLocation(loc);
						if(robot != null) {
							if(robot.getType().canMove()) {
								// If that location is a movable robot
								bugNavWaitCnt++;
								bugNavIsFollowingWall=false;
								bugNavHandDirIdx=-1;
								return Direction.CENTER;
							}
						}
					}
				}
			}
		}
		if(!minDir.equals(Direction.CENTER)) {
			bugNavWaitCnt = 0;
			bugNavIsFollowingWall = false;
			return minDir;
		}
		
		if(!bugNavIsFollowingWall) {
			// Start following the wall
			bugNavIsFollowingWall = true;
			// Get the first wall
			for(int i=0; i<directions.length; ++i) {
				if(!Util.canMove(directions[i])) {
					bugNavHandDirIdx = i;
					break;
				}
			}
			// If there is no one that is blocking my way
			if(bugNavHandDirIdx == -1) {
				bugNavReset();
				return Direction.CENTER;
			}
		}
		
		// Follow the wall until you get somewhere you can move
		for(int i=0;i<directions.length; ++i) {
			Direction dir = directions[(bugNavHandDirIdx+i)%directions.length];
			if(Util.canMove(dir)) {
				// If I can move in the direction
				bugNavWaitCnt = 0;
				
				// Set new hand direction
				int di = i-3;
				if(di < 0) {
					di+=directions.length;
				}
				bugNavHandDirIdx += di;
				bugNavHandDirIdx %= directions.length;
				return dir;
			} else {
				// I can't move
//				System.out.println("Checking moving robots (follow wall)");
				MapLocation loc = myLoc.add(dir);
				if(bugNavWaitCnt < bugNavMaxWaitCnt && rc.canSenseLocation(loc)) {
					// check if the place is a moveable robot
					RobotInfo robot = rc.senseRobotAtLocation(loc);
					if(robot != null) {
						if(robot.getType().canMove()) {
							// If that location is a movable robot
							bugNavWaitCnt++;
							bugNavIsFollowingWall=false;
							bugNavHandDirIdx=-1;
							return Direction.CENTER;
						}
					}
				}
			}
		}
		
		
		// If this code runs, this means that I am surrounded by walls and cannot move
		return Direction.CENTER;
	}

	private MapLocation prevTarget = null;
	public void goTo(MapLocation target) throws GameActionException {
		if(rc.isReady()) {
		    if(prevTarget == null || !prevTarget.equals(target)) {
		    	prevTarget = target;
		        bugNavReset();
            }
			Direction dir = bugNav(target);
			if(bugNavHandDirIdx == -1) {
				System.out.println(-1);
			} else {
				System.out.println(directions[bugNavHandDirIdx]);
			}
			System.out.println(dir);
			Util.tryMove(dir);
		}
	}

	//Landscaper functions:
	Direction lastcordir=null;
	int lastdir=1;

    public int goTo_landscaper(MapLocation target, MapLocation hqloc) throws GameActionException{
        if(!rc.isReady()) return 0;

        Direction dir = mydirto(target);
        if (lastcordir!=dir){
            lastdir=lastdir-2;
        }

        if (rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir)))
        {
            rc.move(dir);
            lastdir=1;
            return 1;
        }
        if (lastdir!=4 && rc.canMove(dir.rotateRight()) && !rc.senseFlooding(rc.getLocation().add(dir))){
            rc.move(dir.rotateRight());
            lastdir=1;
            return 1;
        }
        if (lastdir!=5 && rc.canMove(dir.rotateLeft()) && !rc.senseFlooding(rc.getLocation().add(dir))) {
            rc.move(dir.rotateLeft());
            lastdir=1;
            return 1;
        }

        if (!rc.senseFlooding(rc.getLocation().add(dir)) && gotoviolent(dir,1,hqloc)){
            lastdir=1;
            return 1;
        }
        if (!rc.senseFlooding(rc.getLocation().add(dir)) && lastdir!=4 && gotoviolent(dir.rotateRight(),0,hqloc)){
            lastdir=1;
            return 1;
        }
        if (!rc.senseFlooding(rc.getLocation().add(dir)) && lastdir!=5 && gotoviolent(dir.rotateLeft(),0,hqloc)) {
            lastdir=1;
            return 1;
        }

        if (lastdir!=5 && lastdir!=3 && rc.canMove(dir.rotateLeft().rotateLeft()) && !rc.senseFlooding(rc.getLocation().add(dir))){
            rc.move(dir.rotateLeft().rotateLeft());
            lastdir=2;
            return 2;
        }
        if (lastdir!=4 && lastdir!=2 && rc.canMove(dir.rotateRight().rotateRight()) && !rc.senseFlooding(rc.getLocation().add(dir))){
            rc.move(dir.rotateRight().rotateRight());
            lastdir=3;
            return 3;
        }
        if (lastdir!=5 && lastdir!=3 && rc.canMove(dir.rotateLeft().rotateLeft().rotateLeft()) && !rc.senseFlooding(rc.getLocation().add(dir))){
            rc.move(dir.rotateLeft().rotateLeft().rotateLeft());
            lastdir=4;
            return 4;
        }
        if (lastdir!=4 && lastdir!=2 && rc.canMove(dir.rotateRight().rotateRight().rotateRight()) && !rc.senseFlooding(rc.getLocation().add(dir))){
            rc.move(dir.rotateRight().rotateRight().rotateRight());
            lastdir=5;
            return 5;
        }
        return lastdir=0;
    }

    static boolean washere=false;

    public boolean gotoviolent(Direction dir, int maindir, MapLocation hqLoc) throws GameActionException{
        if(rc.senseRobotAtLocation(rc.getLocation().add(dir))!=null){
            if(maindir==0) return false;
            if(!washere){
                washere=true;
                return true;
            }
            washere=false;
            return false;
        }
        washere=false;
        if (rc.senseFlooding(rc.getLocation().add(dir))){
            if(rc.getDirtCarrying()>0){
                rc.depositDirt(dir);
                return true;
            }
            else{
                for (Direction i:directions){
                    if (i==dir) continue;
                    if (rc.canDigDirt(i)){
                        rc.digDirt(i);
                        return true;
                    }
                }
            }
        }
        else if(rc.senseElevation(rc.getLocation())<rc.senseElevation(rc.getLocation().add(dir))) {
            if(rc.canDigDirt(dir)){
                rc.digDirt(dir);
                return true;
            }
            else if (rc.getDirtCarrying()>0 && rc.canDepositDirt(Direction.CENTER)){
                rc.depositDirt(dir);
                return true;
            }
            else if(rc.getDirtCarrying()==0){
                for (Direction i:directions){
                    if (rc.canDigDirt(i)){
                        rc.digDirt(i);
                        return true;
                    }
                }
            }
        }
        else if(rc.senseElevation(rc.getLocation())>rc.senseElevation(rc.getLocation().add(dir))){
            if (rc.getLocation().add(dir).distanceSquaredTo(hqLoc)<18) return false;
            if(rc.canDigDirt(Direction.CENTER)){
                rc.digDirt(dir);
                return true;
            }
            else if (rc.getDirtCarrying()>0 && rc.canDepositDirt(dir)){
                rc.depositDirt(dir);
                return true;
            }
            else if(rc.getDirtCarrying()==0){
                for (Direction i:Direction.allDirections()){
                    if (i==dir) continue;
                    if (rc.canDigDirt(i)){
                        rc.digDirt(i);
                        return true;
                    }
                }
            }
        }
        return false;
    }


    public Direction mydirto(MapLocation target) throws GameActionException{
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

}
