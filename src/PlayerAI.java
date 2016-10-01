import com.orbischallenge.ctz.Constants;
import com.orbischallenge.ctz.objects.EnemyUnit;
import com.orbischallenge.ctz.objects.FriendlyUnit;
import com.orbischallenge.ctz.objects.World;
import com.orbischallenge.ctz.objects.ControlPoint;
import com.orbischallenge.ctz.objects.enums.ActivateShieldResult;
import com.orbischallenge.ctz.objects.enums.ShotResult;
import com.orbischallenge.ctz.objects.enums.Direction;
import com.orbischallenge.ctz.objects.enums.PickupType;
import com.orbischallenge.ctz.objects.enums.PickupResult;
import com.orbischallenge.ctz.objects.enums.MoveResult;
import com.orbischallenge.ctz.objects.enums.Team;

public class PlayerAI {

	// The latest state of the world.
	private World world;
	// An array of all 4 units on the enemy team. Their order won't change.
	private EnemyUnit[] enemyUnits;
	// An array of all 4 units on your team. Their order won't change.
	private FriendlyUnit[] friendlyUnits;

	public PlayerAI() {
		// Any initialization code goes here.
	}

	/**
	 * Determine whether a friendlyUnit can make a move action.
	 * 
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 * @return True if the friendlyUnit can make a move action, false otherwise.
	 */
	private boolean canMove(int i) {
		// Check each direction to determine if the unit can move in that
		// direction
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
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 * @return True if the friendlyUnit can make a shoot action, false
	 *         otherwise.
	 */
	private boolean canShoot(int i) {
		for (int j = 0; j < enemyUnits.length; j++) {
			if (friendlyUnits[i].checkShotAgainstEnemy(enemyUnits[j]) == ShotResult.CAN_HIT_ENEMY) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a friendlyUnit can make a shield action.
	 * 
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 * @return True if the friendlyUnit can make a shield action, false
	 *         otherwise.
	 */
	private boolean canShield(int i) {
		return friendlyUnits[i].checkShieldActivation() == ActivateShieldResult.SHIELD_ACTIVATION_VALID;
	}

	/**
	 * Determine whether a friendlyUnit can make a pickup action.
	 * 
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 * @return True if the friendlyUnit can make a pickup action, false
	 *         otherwise.
	 */
	private boolean canPickup(int i) {
		if (friendlyUnits[i].checkPickupResult() == PickupResult.PICK_UP_VALID) {
			return true;
		}
		return false;
	}

	/**
	 * Determine number of control points controlled by a team.
	 * 
	 * @param team
	 *            Team we are interested in.
	 * @return number of control points controlled by the team
	 */
	private int numberOfControlPointsControlled(Team team) {
		ControlPoint[] controlPoints = world.getControlPoints();
		int counter = 0;
		for (int i = 0; i < controlPoints.length; i++) {
			if (controlPoints[i].getControllingTeam() == team) {
				counter++;
			}
		}
		return counter;
	}

	/**
	 * Determine number of mainframes controlled by a team.
	 * 
	 * @param team
	 *            Team we are interested in.
	 * @return number of mainframes controlled by the team
	 */
	private int numberOfMainframesControlled(Team team) {
		ControlPoint[] controlPoints = world.getControlPoints();
		int counter = 0;
		for (int i = 0; i < controlPoints.length; i++) {
			if (controlPoints[i].isMainframe()
					&& controlPoints[i].getControllingTeam() == team) {
				counter++;
			}
		}
		return counter;
	}

	/**
	 * Determine the maximum number of points we can get if we were to perform a
	 * move action for a specific friendlyUnit.
	 * 
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 * @return An estimate of the number of points for the best move action we
	 *         can take.
	 */
	private int pointsForMove(int i) {
		return 0;
	}

	/**
	 * Determine the maximum number of points we can get if we were to perform a
	 * shoot action for a specific friendlyUnit.
	 * 
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 * @return An estimate of the number of points for the best shot we can
	 *         make.
	 */
	private int pointsForShoot(int i) {
		return 0;
	}

	/**
	 * Determine the maximum number of points we can get if we were to perform a
	 * shield action for a specific friendlyUnit.
	 * 
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 * @return An estimate of the number of points for shielding.
	 */
	private int pointsForShield(int i) {
		// TODO: later on we could implement something to let us live longer if
		// the other enemy has a mainframe and we do not

		// calculate amount of damage we might take next turn
		int amountOfDamageTaken = maximumPotentialDamage(i);
		int amountOfPoints = amountOfDamageTaken * 10;
		if (friendlyUnits[i].getHealth() < amountOfDamageTaken) {
			// unit will die, and enemy will receive additional 100 points
			amountOfPoints += 100;
		}
		return amountOfPoints;
	}

	/**
	 * Determine the maximum potential damage that friendly unit will take next
	 * move
	 * 
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 * @return maximum damage our unit will take next turn
	 */
	private int maximumPotentialDamage(int i) {
		int amountOfDamageTaken = 0;
		int damageMultiplier = 0;
		for (int j = 0; j < enemyUnits.length; j++) {
			// get unit's range
			int range = enemyUnits[j].getCurrentWeapon().getRange();
			if (world.canShooterShootTarget(enemyUnits[j].getPosition(),
					friendlyUnits[i].getPosition(), range)) {
				amountOfDamageTaken += enemyUnits[j].getCurrentWeapon()
						.getDamage();
				damageMultiplier++;
			}
		}
		int amountOfDamageTakenWithMultiplier = amountOfDamageTaken
				* damageMultiplier;
		return amountOfDamageTakenWithMultiplier;
	}

	/**
	 * Determine the maximum number of points we can get if we were to perform a
	 * pickup action for a specific friendlyUnit.
	 * 
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 * @return An estimate of the number of points for picking up.
	 */
	private int pointsForPickup(int i) {
		PickupType currentPickupType = world.getPickupAtPosition(
				friendlyUnits[i].getPosition()).getPickupType();

		int damageWillTake = maximumPotentialDamage(i);
		// if pickup type is a health kit
		if (currentPickupType == PickupType.REPAIR_KIT) {
			if (damageWillTake >= 20) {
				// if we will take more than 20 dmg next move
				// picking up repair kit will not be beneficial
				return 0;
			} else {
				// net Health Gained * 10 points + 50 for pickup
				return (20 - damageWillTake) * 10 + 50;
			}
		}
		if (currentPickupType == PickupType.SHIELD) {
			if (damageWillTake >= friendlyUnits[i].getHealth()) {
				// if we will die in next move
				// picking up shield will not be beneficial
				// unless we have a mainframe
				if (numberOfMainframesControlled(friendlyUnits[i].getTeam()) == 0) {
					return 0;
				} else {
					// TODO: figure out a better value of shield
					return 100;
				}
			} else {
				// TODO: figure out a better value of shield
				return 100;
			}
		}
		return 0;
	}

	private void performMove(int i) {
		// TODO: perform the movement that we thought was best
	}

	private void performShoot(int i) {
		// TODO: perform the shot that we thought was best
	}

	private void performShield(int i) {
		friendlyUnits[i].activateShield();
	}

	private void performPickup(int i) {
		friendlyUnits[i].pickupItemAtPosition();
	}

	/**
	 * Determine the action that should be taken for the ith friendlyUnit, and
	 * take that action.
	 * 
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 */
	private void doMove(int i) {
		int movePoints = 0;
		int shootPoints = 0;
		int shieldPoints = 0;
		int pickupPoints = 0;

		boolean canMove = canMove(i);
		boolean canShoot = canShoot(i);
		boolean canShield = canShield(i);
		boolean canPickup = canPickup(i);

		// Use our heuristic functions to estimate the maximum number of points
		// we can get this turn
		int maxPoints = Integer.MIN_VALUE;
		if (canMove) {
			movePoints = pointsForMove(i);
			maxPoints = Math.max(maxPoints, movePoints);
		}
		if (canShoot) {
			shootPoints = pointsForShoot(i);
			maxPoints = Math.max(maxPoints, shootPoints);
		}
		if (canShield) {
			shieldPoints = pointsForShield(i);
			maxPoints = Math.max(maxPoints, shieldPoints);
		}
		if (canPickup) {
			pickupPoints = pointsForPickup(i);
			maxPoints = Math.max(maxPoints, pickupPoints);
		}

		// TODO: make this better, right now the priority is fixed - shield then
		// shoot then move then pickup
		if (canShield && shieldPoints == maxPoints) {
			// If we can shield, and doing so would maximize our points
			performShield(i);
		} else if (canShoot && shootPoints == maxPoints) {
			// If we can shoot, and doing so would maximize our points
			performShoot(i);
		} else if (canMove && movePoints == maxPoints) {
			// If we can move, and doing so would maximize our points
			performMove(i);
		} else if (canPickup && pickupPoints == maxPoints) {
			// If we can pickup, and doing so would maximize our points
			performPickup(i);
		}
	}

	/**
	 * This method will get called every turn.
	 * 
	 * @param world
	 *            The latest state of the world.
	 * @param enemyUnits
	 *            An array of all 4 units on the enemy team. Their order won't
	 *            change.
	 * @param friendlyUnits
	 *            An array of all 4 units on your team. Their order won't
	 *            change.
	 */
	public void doMove(World world, EnemyUnit[] enemyUnits,
			FriendlyUnit[] friendlyUnits) {
		this.world = world;
		this.enemyUnits = enemyUnits;
		this.friendlyUnits = friendlyUnits;

		for (int i = 0; i < friendlyUnits.length; i++) {
			doMove(i);
		}
	}
}
