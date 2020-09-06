package examplefuncsplayer;
import battlecode.common.*;


import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.ArrayList;



public class Landscaper {
    RobotController rc;

    Communication comm;
    Navigation nav;

    final int giveupround=800;
    final int rat_in_out=10;

    int turnCount = 0;
    MapLocation hqLoc=new MapLocation(200,200);
    Direction lastDir = Direction.CENTER;

    MapLocation mylandloc=null;
    static MapLocation[] landposes=new MapLocation[20];//{null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null};

    Direction[] directions = {
            Direction.NORTH, Direction.NORTHEAST, Direction.EAST, Direction.SOUTHEAST,
            Direction.SOUTH, Direction.SOUTHWEST, Direction.WEST, Direction.NORTHWEST
    };

    public Landscaper (RobotController r) throws GameActionException{
        rc=r;
        comm=new Communication(r);
        nav=new Navigation(r);

        for (int i=1;i<=10;i++){
            procmes(i);
        }

        landposes[0]=hqLoc.add(Direction.SOUTH);
        landposes[1]=hqLoc.add(Direction.SOUTH).add(Direction.EAST);
        landposes[2]=hqLoc.add(Direction.SOUTH).add(Direction.WEST);
        landposes[3]=hqLoc.add(Direction.EAST);
        landposes[4]=hqLoc.add(Direction.WEST);
        landposes[5]=hqLoc.add(Direction.NORTH).add(Direction.EAST);
        landposes[6]=hqLoc.add(Direction.NORTH).add(Direction.WEST);
        landposes[7]=hqLoc.add(Direction.NORTH);
        landposes[8]=hqLoc.add(Direction.SOUTH).add(Direction.SOUTH).add(Direction.EAST);
        landposes[9]=hqLoc.add(Direction.SOUTH).add(Direction.WEST).add(Direction.WEST);

        landposes[10]=hqLoc.add(Direction.NORTH).add(Direction.EAST).add(Direction.EAST);
        landposes[11]=hqLoc.add(Direction.WEST).add(Direction.NORTH).add(Direction.NORTH);
        landposes[12]=hqLoc.add(Direction.SOUTH).add(Direction.EAST).add(Direction.EAST);
        landposes[13]=hqLoc.add(Direction.SOUTH).add(Direction.SOUTH).add(Direction.WEST);
        landposes[14]=hqLoc.add(Direction.NORTH).add(Direction.WEST).add(Direction.WEST);
        landposes[15]=hqLoc.add(Direction.NORTH).add(Direction.NORTH).add(Direction.EAST);
        landposes[16]=hqLoc.add(Direction.SOUTH).add(Direction.SOUTH).add(Direction.EAST).add(Direction.EAST);
        landposes[17]=hqLoc.add(Direction.SOUTH).add(Direction.SOUTH).add(Direction.WEST).add(Direction.WEST);
        landposes[18]=hqLoc.add(Direction.NORTH).add(Direction.NORTH).add(Direction.WEST).add(Direction.WEST);////
        landposes[19]=hqLoc.add(Direction.NORTH).add(Direction.NORTH).add(Direction.EAST).add(Direction.EAST);////

        procmes(rc.getRoundNum()-3);
        procmes(rc.getRoundNum()-2);
        run();

    }

    public void run() throws GameActionException{
	System.out.println("working:" + rat_in_out);
        procmes(rc.getRoundNum()-1);
        System.out.println(hqLoc);
        System.out.println(mylandloc);

        if (mylandloc==null) return;

        if (rc.getLocation().distanceSquaredTo(hqLoc)<=2){
            for (Direction i:directions){
                if (rc.getLocation().add(i).equals(hqLoc) && rc.canDigDirt(i)){
                    rc.digDirt(i);
                    return;
                }
            }
        }

        if(rc.getLocation().distanceSquaredTo(mylandloc)>0){
            nav.goTo_landscaper(mylandloc,hqLoc);
            return;
        }


        // Get a place to dig dirt

        // Get a place to deposit dirt
        //Direction placeToDeposit = getLowestElevationNearHq();
        Direction placeToDeposit = Direction.CENTER;

        if (rc.getRoundNum()>giveupround){
            int minelev=rc.senseElevation(rc.getLocation());
            if (rc.getLocation().distanceSquaredTo(hqLoc)>2){
                minelev*=rat_in_out;
            }
            Direction mindir=Direction.CENTER;
            for (Direction i:directions){
                if (rc.getLocation().add(i).distanceSquaredTo(hqLoc)>2) continue;
                if (rc.getLocation().add(i).equals(hqLoc)) continue;
                if (rc.canDepositDirt(i) && rc.senseElevation(rc.getLocation().add(i))<minelev){
                    minelev=rc.senseElevation(rc.getLocation().add(i));
                    mindir=i;
                }
            }
            placeToDeposit=mindir;
        }
        else{
            for (Direction dir:directions){
                for (MapLocation m:landposes){
                    if (rc.getLocation().add(dir).equals(m)
                            && rc.senseElevation(rc.getLocation().add(dir))<=GameConstants.getWaterLevel(rc.getRoundNum())
                            && rc.canDepositDirt(dir)){
                        placeToDeposit=dir;
                    }
                }
            }
        }


        Direction placeTodig = null;
        for (Direction i:directions){
            boolean isfine=true;
            for (int m=0;m<20;m++){
                if (rc.getLocation().add(i).equals(landposes[m])) {isfine=false;break;}
            }
            if (!rc.canDigDirt(i) || rc.senseRobotAtLocation(rc.getLocation().add(i))!=null) isfine=false;
            if (isfine) {
                placeTodig=i;
                break;
            }
        }
        //System.out.println("placeToDeposit: " + placeToDeposit);

        if (rc.getDirtCarrying()>0){///
            if (rc.canDepositDirt(placeToDeposit)) {
                rc.depositDirt(placeToDeposit);
                return;
            }
        }
        if (placeTodig!=null) {
            rc.digDirt(placeTodig);
            return;
        }
    }

    public void procmes (int roundnm) throws GameActionException {
        ArrayList<Message> mess=comm.recmes(roundnm);
        for (Message i:mess){
            if (i.getType()==MessageType.HQ_LOC){
                hqLoc=new MapLocation(i.getContent().get(0),i.getContent().get(1));

            }
            else if (i.getType()==MessageType.LAND_POS && mylandloc==null){
                mylandloc=new MapLocation(i.getContent().get(0),i.getContent().get(1));
            }
        }

    }
}
