package examplefuncsplayer;

import battlecode.common.*;
import java.util.ArrayList;

public class NetGun {
	RobotController rc;
	
	public NetGun(RobotController r) throws GameActionException {
		rc = r;
	}
	
	public void run() throws GameActionException {
		Util.netGunCode();
	}
}
