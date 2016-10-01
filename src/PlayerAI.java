import com.orbischallenge.ctz.Constants;
import com.orbischallenge.ctz.objects.EnemyUnit;
import com.orbischallenge.ctz.objects.FriendlyUnit;
import com.orbischallenge.ctz.objects.World;


public class PlayerAI {

    public PlayerAI() {
		//Any initialization code goes here.
    }
    
    /**
     * Determine whether a friendlyUnit can make a move action.
     * 
     * @param world The latest state of the world.
	 * @param enemyUnits An array of all 4 units on the enemy team. Their order won't change.
	 * @param friendlyUnits An array of all 4 units on your team. Their order won't change.
	 * @param i The index of the friendlyUnit we are interested in.
     * @return True if the friendlyUnit can make a move action, false otherwise.
     */
    private boolean canMove(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits, int i) {
    	return true;
    }
    
    /**
     * Determine whether a friendlyUnit can make a shoot action.
     * 
     * @param world The latest state of the world.
	 * @param enemyUnits An array of all 4 units on the enemy team. Their order won't change.
	 * @param friendlyUnits An array of all 4 units on your team. Their order won't change.
	 * @param i The index of the friendlyUnit we are interested in.
     * @return True if the friendlyUnit can make a shoot action, false otherwise.
     */
    private boolean canShoot(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits, int i) {
    	return true;
    }
    
    /**
     * Determine whether a friendlyUnit can make a shield action.
     * 
     * @param world The latest state of the world.
	 * @param enemyUnits An array of all 4 units on the enemy team. Their order won't change.
	 * @param friendlyUnits An array of all 4 units on your team. Their order won't change.
	 * @param i The index of the friendlyUnit we are interested in.
     * @return True if the friendlyUnit can make a shield action, false otherwise.
     */
    private boolean canShield(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits, int i) {
    	return friendlyUnits[i].checkShieldActivation() == ActivateShieldResult.SHIELD_ACTIVATION_VALID;
    }
    
    /**
     * Determine whether a friendlyUnit can make a pickup action.
     * 
     * @param world The latest state of the world.
	 * @param enemyUnits An array of all 4 units on the enemy team. Their order won't change.
	 * @param friendlyUnits An array of all 4 units on your team. Their order won't change.
	 * @param i The index of the friendlyUnit we are interested in.
     * @return True if the friendlyUnit can make a pickup action, false otherwise.
     */
    private boolean canPickup(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits, int i) {
    	return true;
    }
    
    /**
     * Determine the maximum number of points we can get if we were to perform a move action
     * for a specific friendlyUnit.
     * 
     * @param world The latest state of the world.
	 * @param enemyUnits An array of all 4 units on the enemy team. Their order won't change.
	 * @param friendlyUnits An array of all 4 units on your team. Their order won't change.
	 * @param i The index of the friendlyUnit we are interested in.
     * @return An estimate of the number of points for the best move action we can take.
     */
    private int pointsForMove(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits, int i) {
    	return 0;
    }
    
    /**
     * Determine the maximum number of points we can get if we were to perform a shoot action
     * for a specific friendlyUnit.
     * 
     * @param world The latest state of the world.
	 * @param enemyUnits An array of all 4 units on the enemy team. Their order won't change.
	 * @param friendlyUnits An array of all 4 units on your team. Their order won't change.
	 * @param i The index of the friendlyUnit we are interested in.
     * @return An estimate of the number of points for the best shot we can make.
     */
    private int pointsForShoot(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits, int i) {
    	return 0;
    }
    
    /**
     * Determine the maximum number of points we can get if we were to perform a shield action
     * for a specific friendlyUnit.
     * 
     * @param world The latest state of the world.
	 * @param enemyUnits An array of all 4 units on the enemy team. Their order won't change.
	 * @param friendlyUnits An array of all 4 units on your team. Their order won't change.
	 * @param i The index of the friendlyUnit we are interested in.
     * @return An estimate of the number of points for shielding.
     */
    private int pointsForShield(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits, int i) {
    	return 0;
    }
    
    /**
     * Determine the maximum number of points we can get if we were to perform a pickup action
     * for a specific friendlyUnit.
     * 
     * @param world The latest state of the world.
	 * @param enemyUnits An array of all 4 units on the enemy team. Their order won't change.
	 * @param friendlyUnits An array of all 4 units on your team. Their order won't change.
	 * @param i The index of the friendlyUnit we are interested in.
     * @return An estimate of the number of points for picking up.
     */
    private int pointsForPickup(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits, int i) {
    	return 0;
    }
    
    /**
     * 
     * @param world The latest state of the world.
	 * @param enemyUnits An array of all 4 units on the enemy team. Their order won't change.
	 * @param friendlyUnits An array of all 4 units on your team. Their order won't change.
	 * @param i The index of the friendlyUnit we are interested in.
     */
    private void doMove(World world, EnemyUnit[] enemyUnits, FriendlyUnit[] friendlyUnits, int i) {
    	int movePoints = pointsForMove(world, enemyUnits, friendlyUnits, i);
		int shootPoints = pointsForShoot(world, enemyUnits, friendlyUnits, i);
		int shieldPoints = pointsForShield(world, enemyUnits, friendlyUnits, i);
		int pickupPoints = pointsForPickup(world, enemyUnits, friendlyUnits, i);
		
		// If we can and should shield, do so
		if (canShield(world, enemyUnits, friendlyUnits, i) &&
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
		for (int i = 0; i < friendlyUnits.length; i++) {
			doMove(world, enemyUnits, friendlyUnits, i);
		}
    }
}
