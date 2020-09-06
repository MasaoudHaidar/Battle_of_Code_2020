package examplefuncsplayer;

import java.util.ArrayList;

import battlecode.common.*;

public class DesignSchool {
	RobotController rc;
	Communication comm;
	MapLocation hqLoc = null;
	
	MapLocation[] landposes={null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null};
	
	public DesignSchool(RobotController r) throws GameActionException {
		rc=r;
		comm = new Communication(rc);
		
		// Get hq location
		for(int i=1;i<=10;i++){
            ArrayList<Message> mess= comm.recmes(i);
            for (Message j:mess){
                if (j.getType()==MessageType.HQ_LOC){
                    hqLoc=new MapLocation(j.getContent().get(0),j.getContent().get(1));
                }
            }
        }
		
		landposes[4]=hqLoc.add(Direction.SOUTH);
        landposes[0]=hqLoc.add(Direction.SOUTH).add(Direction.EAST);
        landposes[3]=hqLoc.add(Direction.SOUTH).add(Direction.WEST);
        landposes[6]=hqLoc.add(Direction.EAST);
        landposes[7]=hqLoc.add(Direction.WEST);
        landposes[1]=hqLoc.add(Direction.NORTH).add(Direction.EAST);
        landposes[2]=hqLoc.add(Direction.NORTH).add(Direction.WEST);
        landposes[5]=hqLoc.add(Direction.NORTH);
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

	}
	
	static int numLandscapers = 0;
    static int mycool=0;
    static Direction des_sch_build = Direction.NORTH;
    
	public void run() throws GameActionException {
		System.out.println(hqLoc);
    	if(numLandscapers < 20) {
    	    while(!rc.onTheMap(landposes[numLandscapers])){
    	        numLandscapers++;
            }
    	    if (numLandscapers>=20) return;
            if (mycool==0 && rc.getTeamSoup()>=RobotType.LANDSCAPER.cost+1 && Util.tryBuild(RobotType.LANDSCAPER,des_sch_build)) {
                numLandscapers++;
                //Communication comm=new Communication(rc);
                Message mess=new Message(MessageType.LAND_POS,landposes[numLandscapers-1].x,landposes[numLandscapers-1].y,0);
                System.out.println("Send message: " + landposes[numLandscapers-1]);
                comm.addmes(mess);
                comm.sendallmes();
                mycool=20;
                return;
            }
            else{
                des_sch_build=des_sch_build.rotateLeft();
            }
            if (mycool>0) mycool--;
    	}
    	comm.sendallmes();
	}
}
