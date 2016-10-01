import com.orbischallenge.ctz.Constants;
import com.orbischallenge.ctz.objects.EnemyUnit;
import com.orbischallenge.ctz.objects.FriendlyUnit;
import com.orbischallenge.ctz.objects.World;
import com.orbischallenge.ctz.objects.enums.ActivateShieldResult;
import com.orbischallenge.ctz.objects.enums.ShotResult;
import com.orbischallenge.ctz.objects.enums.Direction;
import com.orbischallenge.ctz.objects.enums.PickupType;
import com.orbischallenge.ctz.objects.enums.MoveResult;


public class PlayerAI {

	// The latest state of the world.
	private World world;
	// An array of all 4 units on the enemy team. Their order won't change.
	private EnemyUnit[] enemyUnits;
	// An array of all 4 units on your team. Their order won't change.
	private FriendlyUnit[] friendlyUnits;
	
    public PlayerAI() {
		//Any initialization code goes here.
    }
    
    /**
     * Determine whether a friendlyUnit can make a move action.
     * 
	 * @param i The index of the friendlyUnit we are interested in.
     * @return True if the friendlyUnit can make a move action, false otherwise.
     */
    private boolean canMove(int i) {
    	// Check each direction to determine if the unit can move in that direction
    	for (Direction d : Direction.values()) {
    		if (friendlyUnits[i].checkMove(d) == MoveResult.MOVE_VALID) {
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * Determine whether a friendlyUnit can make a shoot action.
     * 
	 * @param i The index of the friendlyUnit we are interested in.
     * @return True if the friendlyUnit can make a shoot action, false otherwise.
     */
    private boolean canShoot(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits, int i) {
    	for (int j = 0; j < enemyUnits.length; j++){
    		if (friendlyUnits[i].checkShotAgainstEnemy(enemyUnits[j]) == ShotResult.CAN_HIT_ENEMY){
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * Determine whether a friendlyUnit can make a shield action.
     * 
	 * @param i The index of the friendlyUnit we are interested in.
     * @return True if the friendlyUnit can make a shield action, false otherwise.
     */
    private boolean canShield(int i) {
    	return friendlyUnits[i].checkShieldActivation() == ActivateShieldResult.SHIELD_ACTIVATION_VALID;
    }
    
    /**
     * Determine whether a friendlyUnit can make a pickup action.
     * 
	 * @param i The index of the friendlyUnit we are interested in.
     * @return True if the friendlyUnit can make a pickup action, false otherwise.
     */
    private boolean canPickup(int i) {
    	return true;
    }
    
    /**
     * Determine the maximum number of points we can get if we were to perform a move action
     * for a specific friendlyUnit.
     * 
	 * @param i The index of the friendlyUnit we are interested in.
     * @return An estimate of the number of points for the best move action we can take.
     */
    private int pointsForMove(int i) {
    	return 0;
    }
    
    /**
     * Determine the maximum number of points we can get if we were to perform a shoot action
     * for a specific friendlyUnit.
     * 
	 * @param i The index of the friendlyUnit we are interested in.
     * @return An estimate of the number of points for the best shot we can make.
     */
    private int pointsForShoot(int i) {
    	return 0;
    }
    
    /**
     * Determine the maximum number of points we can get if we were to perform a shield action
     * for a specific friendlyUnit.
     * 
	 * @param i The index of the friendlyUnit we are interested in.
     * @return An estimate of the number of points for shielding.
     */
    private int pointsForShield(int i) {
    	return 0;
    }
    
    /**
     * Determine the maximum number of points we can get if we were to perform a pickup action
     * for a specific friendlyUnit.
     * 
	 * @param i The index of the friendlyUnit we are interested in.
     * @return An estimate of the number of points for picking up.
     */
    private int pointsForPickup(int i) {
    	return 0;
    }
    
    /**
     * 
	 * @param i The index of the friendlyUnit we are interested in.
     */
    private void doMove(int i) {
    	int movePoints = pointsForMove(i);
		int shootPoints = pointsForShoot(i);
		int shieldPoints = pointsForShield(i);
		int pickupPoints = pointsForPickup(i);
		
		// If we can and should shield, do so
		if (canShield(i) &&
				shieldPoints >= movePoints &&
				shieldPoints >= shootPoints &&
				shieldPoints >= pickupPoints) {
			
		}
    }

	/**
	 * This method will get called every turn.
	 *
	 * @param world The latest state of the world.
	 * @param enemyUnits An array of all 4 units on the enemy team. Their order won't change.
	 * @param friendlyUnits An array of all 4 units on your team. Their order won't change.
	 */
    public void doMove(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits) {
    	this.world = world;
    	this.enemyUnits = enemyUnits;
    	this.friendlyUnits = friendlyUnits;
		
    	for (int i = 0; i < friendlyUnits.length; i++) {
			doMove(i);
    	}
    }
}
