import java.util.Arrays;

import com.orbischallenge.ctz.Constants;
import com.orbischallenge.ctz.objects.ControlPoint;
import com.orbischallenge.ctz.objects.EnemyUnit;
import com.orbischallenge.ctz.objects.FriendlyUnit;
import com.orbischallenge.ctz.objects.Pickup;
import com.orbischallenge.ctz.objects.World;
import com.orbischallenge.ctz.objects.ControlPoint;
import com.orbischallenge.ctz.objects.enums.ActivateShieldResult;
import com.orbischallenge.ctz.objects.enums.ShotResult;
import com.orbischallenge.ctz.objects.enums.Direction;
import com.orbischallenge.ctz.objects.enums.PickupType;
import com.orbischallenge.ctz.objects.enums.PickupResult;
import com.orbischallenge.ctz.objects.enums.MoveResult;
import com.orbischallenge.ctz.objects.enums.Team;
import com.orbischallenge.ctz.objects.enums.WeaponType;
import com.orbischallenge.game.engine.Point;

public class PlayerAI {

	private static final int NUM_UNITS = 4;
	private static final int CP_DEFEND_SHOOT_MULTIPLIER = 10;
	private static final int CP_DEFEND_ENEMY_PROXIMITY = 3;
	private static final int CP_DEFEND_POINTS_PER_MOVE_MULTIPLIER = 100;
	private static final float MAINFRAME_DAMAGE_MULTIPLIER = 1.5f;
	private static final float MAINFRAME_DEFENSE_MULTIPLIER = 1.5f;

	// The latest state of the world.
	private World world;
	// An array of all 4 units on the enemy team. Their order won't change.
	private EnemyUnit[] enemyUnits;
	// An array of all 4 units on your team. Their order won't change.
	private FriendlyUnit[] friendlyUnits;
	// An array of enemy units to shoot
	private EnemyUnit[] enemiesToShoot = new EnemyUnit[4];

	// Stores the best direction for each friendlyUnit to travel in
	private Direction[] bestMoveDirections = new Direction[NUM_UNITS];

	// Stores the move actions that have been determined for the current turn
	// for each friendlyUnit
	private Point[] currentMoveActions = new Point[NUM_UNITS];

	// Stores the move actions that were determined for the previous turn
	// for each friendlyUnit
	private Point[] previousMoveActions = new Point[NUM_UNITS];

	// Stores the results of the previous move attempts. The difference between
	// this array and FriendlyUnit.getLastMoveResult() is that this array will
	// not have MoveResult.NO_MOVE_ATTEMPTED if the previous action was not a
	// move. Initially, it is filled with MoveResult.MOVE_COMPLETED
	private MoveResult[] previousMoveResults = new MoveResult[NUM_UNITS];

	public PlayerAI() {
		Arrays.fill(previousMoveResults, MoveResult.MOVE_COMPLETED);
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
			if (moveValid(i, d)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether given point is a part of a control point
	 * 
	 * @param p
	 *            Point we are interested in.
	 * @return True if given point is a part of CP, false otherwise.
	 */
	private boolean isOnCP(Point p) {
		// Check all control points, and check if we are within 1 block away
		ControlPoint[] controlPoints = world.getControlPoints();

		for (ControlPoint cp : controlPoints) {
			// Only consider this cp if the current direction decreases
			// the path length by 1
			if (world.getPathLength(p, cp.getPosition()) < 2)
				return true;
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
		return friendlyUnits[i].checkPickupResult() == PickupResult.PICK_UP_VALID;
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
	 * Determines if movement for a unit in a direction is valid. This considers
	 * whether the previous move attempt succeeded (only if it was to the same
	 * tile we are trying to move to now), whether there is an enemy there, and
	 * guarantees that we won't move two units to the same location or move a
	 * unit to a location that another unit won't move out of this turn.
	 * 
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 * @param d
	 *            The direction we are thinking of moving that unit in.
	 * @return Whether or not we are allowed to move the unit in that direction.
	 */
	private boolean moveValid(int i, Direction d) {
		Point movePosition = d.movePoint(friendlyUnits[i].getPosition());

		// If our last move failed and it was to the same tile we are currently
		// thinking about moving to
		if (previousMoveResults[i] != MoveResult.MOVE_COMPLETED
				&& movePosition.equals(previousMoveActions[i])) {
			return false;
		}

		// Moves onto enemy tiles are not valid
		for (EnemyUnit enemyUnit : enemyUnits) {
			if (movePosition.equals(enemyUnit.getPosition())) {
				return false;
			}
		}

		// For each friendly unit
		for (int j = 0; j < NUM_UNITS; j++) {
			if (currentMoveActions[j] != null
					&& currentMoveActions[j].equals(movePosition)) {
				// If the current unit moving in the specified direction is the
				// same position that a unit is already planning to move to,
				// then the direction is invalid
				return false;
			} else if (currentMoveActions[j] == null
					&& friendlyUnits[j].getPosition().equals(movePosition)) {
				// If the current unit is trying to move onto a location
				// occupied by a friendly unit that doesn't plan to move,
				// then the direction is invalid
				return false;
			}
		}
		return friendlyUnits[i].checkMove(d) == MoveResult.MOVE_VALID;
	}

	/**
	 * Returns the difference in path lengths between a and target and b and
	 * target.
	 * 
	 * @param a
	 *            The starting Point for the first path to target.
	 * @param b
	 *            The starting Point for the second path to target.
	 * @param target
	 *            The target Point.
	 * @return (the path length of a -> target) - (the path length of b ->
	 *         target)
	 */
	private int getDifferenceInPathLengths(Point a, Point b, Point target) {
		return world.getPathLength(a, target) - world.getPathLength(b, target);
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
		int maxPoints = Integer.MIN_VALUE;
		Direction bestDirection = null;

		// For each direction
		for (Direction d : Direction.values()) {
			// If we can actually travel in that direction
			if (moveValid(i, d)) {
				int pointsForDirection = 0;
				Point directionPoint = d.movePoint(friendlyUnits[i]
						.getPosition());

				ControlPoint[] controlPoints = world.getControlPoints();
				Pickup[] pickups = world.getPickups();

				for (ControlPoint cp : controlPoints) {
					// Only consider this cp if the current direction decreases
					// the path length by 1
					if (getDifferenceInPathLengths(
							friendlyUnits[i].getPosition(), directionPoint,
							cp.getPosition()) < 0) {
						continue;
					}
					int cpPoints = 200;
					if (cp.getControllingTeam() == friendlyUnits[i].getTeam()) {
						cpPoints = 0;
						// defend the point if there are enemies around
						for (int j = 0; j < enemyUnits.length; j++) {
							int pathLengthFromEnemy = world.getPathLength(
									enemyUnits[j].getPosition(),
									cp.getPosition());
							if (pathLengthFromEnemy <= CP_DEFEND_ENEMY_PROXIMITY) {
								if (pathLengthFromEnemy == 0)
									pathLengthFromEnemy = 1;
								cpPoints += (CP_DEFEND_ENEMY_PROXIMITY - pathLengthFromEnemy)
										* CP_DEFEND_POINTS_PER_MOVE_MULTIPLIER;
							}
						}
					} else if (cp.getControllingTeam() == Team
							.opposite(friendlyUnits[i].getTeam())) {
						// 50 extra points for neutralizing an opposing control
						// point
						if (cp.isMainframe())
							// add 400 extra points for mainframe
							cpPoints += 400;
						cpPoints += 50;
					}
					int distanceToCP = world.getPathLength(directionPoint,
							cp.getPosition());
					//any point within 1 radius counts as CP point
					if (distanceToCP == 0)
						distanceToCP++;
					// Make the points for this cp drop off with distance
					// according to x^1.5
					pointsForDirection += cpPoints
							/ Math.pow(
									distanceToCP, 1.5f);
				}

				for (Pickup p : pickups) {
					// Only consider this pickup if the current direction
					// decreases the path length by 1
					if (getDifferenceInPathLengths(
							friendlyUnits[i].getPosition(), directionPoint,
							p.getPosition()) < 0) {
						continue;
					}
					int pickupPoints = valueOfPickup(i, p.getPickupType());
					// if there is a pickup at where we are right now
					// and it's value is greater than potential pickup
					// ignore potential pickup
					if (friendlyUnits[i].checkPickupResult() == PickupResult.PICK_UP_VALID) {
						int currentPickupPoints = pointsForPickup(i);
						if (currentPickupPoints >= pickupPoints)
							continue;
					}
					// Make the points for this pickup drop off with distance
					// according to x^1.5
					pointsForDirection += pickupPoints
							/ Math.pow(
									world.getPathLength(directionPoint,
											p.getPosition()) + 1, 1.5f);
				}

				if (pointsForDirection > maxPoints) {
					maxPoints = pointsForDirection;
					bestDirection = d;
				}
			}
		}

		bestMoveDirections[i] = bestDirection;

		return maxPoints;
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
		// for each enemy, check what happens if all friendly units try to shot
		// him
		int maxPoints = Integer.MIN_VALUE;
		for (int j = 0; j < enemyUnits.length; j++) {
			// If shooting the current enemy isn't valid, skip it
			if (friendlyUnits[i].checkShotAgainstEnemy(enemyUnits[j]) != ShotResult.CAN_HIT_ENEMY) {
				continue;
			}
			int totalDamage = 0;
			int damageMultiplier = 0;
			// Calculate the total amount of damage we can do to this enemy with
			// all units
			for (int k = 0; k < friendlyUnits.length; k++) {
				if (friendlyUnits[k].checkShotAgainstEnemy(enemyUnits[j]) == ShotResult.CAN_HIT_ENEMY) {
					totalDamage += friendlyUnits[k].getCurrentWeapon()
							.getDamage();
					damageMultiplier++;
				}
			}
			int damage = totalDamage * damageMultiplier;
			int points = damage * 10;
			// if we kill the enemy, add 100 points
			if (enemyUnits[j].getHealth() <= damage) {
				points += 100;
			}
			// If the enemy doesn't have a mainframe and we do, we want to shoot
			// them more
			if (numberOfMainframesControlled(enemyUnits[j].getTeam()) == 0
					&& numberOfMainframesControlled(friendlyUnits[i].getTeam()) > 0) {
				// TODO: figure out a multiplier in case enemies have no
				// mainframes,
				// but we have mainframes
				points = (int) (points * MAINFRAME_DAMAGE_MULTIPLIER);
			}
			// Choose the enemy to shoot that maximizes our points
			if (points > maxPoints) {
				maxPoints = points;
				enemiesToShoot[i] = enemyUnits[j];
			}
		}
		if (isOnCP(friendlyUnits[i].getPosition()))
			maxPoints = maxPoints * CP_DEFEND_SHOOT_MULTIPLIER;
		return maxPoints;
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
		int amountOfDamageTaken = maximumPotentialDamage(friendlyUnits[i]
				.getPosition());
		int amountOfPoints = amountOfDamageTaken * 10;
		if (friendlyUnits[i].getHealth() < amountOfDamageTaken) {
			// unit will die, and enemy will receive additional 100 points
			amountOfPoints += 100;
		}
		if (numberOfMainframesControlled(enemyUnits[i].getTeam()) > 0
				&& numberOfMainframesControlled(friendlyUnits[i].getTeam()) == 0) {
			// if we have no mainframes, but enemy does
			amountOfPoints = (int) (amountOfPoints * MAINFRAME_DEFENSE_MULTIPLIER);
		}
		return amountOfPoints;
	}

	/**
	 * Determine the maximum potential damage that friendly unit will take next
	 * move
	 * 
	 * @param p
	 *            point that we are interested in.
	 * @return maximum damage our unit will take next turn
	 */
	private int maximumPotentialDamage(Point p) {
		int amountOfDamageTaken = 0;
		int damageMultiplier = 0;
		for (int j = 0; j < enemyUnits.length; j++) {
			// get unit's range
			int range = enemyUnits[j].getCurrentWeapon().getRange();
			if (world.canShooterShootTarget(enemyUnits[j].getPosition(), p,
					range)) {
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
	 * TODO: factor out code to use a function like valueOfPickup(Pickup pickup)
	 * which could be used in the heuristic for picking up immediately and for
	 * moving to a pickup
	 * 
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 * @return An estimate of the number of points for picking up.
	 */
	private int pointsForPickup(int i) {
		PickupType currentPickupType = world.getPickupAtPosition(
				friendlyUnits[i].getPosition()).getPickupType();

		int damageWillTake = maximumPotentialDamage(friendlyUnits[i]
				.getPosition());
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
					return valueOfPickup(i, currentPickupType);
				}
			} else {
				return valueOfPickup(i, currentPickupType);
			}
		}
		// if it is a gun:
		return valueOfPickup(i, currentPickupType);
	}

	private int valueOfPickup(int i, PickupType p) {
		int value = 0;
		WeaponType currentWeapon = friendlyUnits[i].getCurrentWeapon();
		switch (p) {
		case REPAIR_KIT:
			value = 250;
			break;
		case SHIELD:
			value = 100;
			break;
		case WEAPON_LASER_RIFLE:
			if (currentWeapon == WeaponType.LASER_RIFLE) {
				value = 50;
			} else {
				value = 150;
			}
			break;
		case WEAPON_MINI_BLASTER:
			if (currentWeapon == WeaponType.MINI_BLASTER) {
				// if we have this weapon, we will get 50 points for picking it
				// up
				value = 50;
			} else {
				// else, we have something better
				value = 0;
			}
			break;
		case WEAPON_RAIL_GUN:
			if (currentWeapon == WeaponType.RAIL_GUN) {
				value = 50;
			} else {
				// sniper rifle is the shit
				value = 200;
			}
			break;
		case WEAPON_SCATTER_GUN:
			if (currentWeapon == WeaponType.SCATTER_GUN) {
				value = 50;
			} else {
				value = 100;
			}
			break;
		default:
			value = 0;
			break;

		}
		return value;
	}

	private void performMove(int i) {
		System.out.println("      Moving to "
				+ bestMoveDirections[i].movePoint(friendlyUnits[i]
						.getPosition()));
		friendlyUnits[i].move(bestMoveDirections[i]);
		// Store the point that the current unit is planning to move to
		currentMoveActions[i] = bestMoveDirections[i]
				.movePoint(friendlyUnits[i].getPosition());

		previousMoveActions[i] = currentMoveActions[i];
	}

	private void performShoot(int i) {
		System.out.println("      Shooting at "
				+ enemiesToShoot[i].getASCIIIcon());
		friendlyUnits[i].shootAt(enemiesToShoot[i]);
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
		if (friendlyUnits[i].getLastMoveResult() != MoveResult.NO_MOVE_ATTEMPTED) {
			previousMoveResults[i] = friendlyUnits[i].getLastMoveResult();
		}

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

		System.out.println("  Unit " + friendlyUnits[i].getASCIIIcon()
				+ ": move=" + movePoints + " shoot=" + shootPoints + " shield="
				+ shieldPoints + " pickup=" + pickupPoints);

		// TODO: make this better, right now the priority is fixed - shield then
		// shoot then move then pickup
		if (canShield && shieldPoints == maxPoints) {
			System.out.println("    Performing shield");
			// If we can shield, and doing so would maximize our points
			performShield(i);
		} else if (canShoot && shootPoints == maxPoints) {
			System.out.println("    Performing shoot");
			// If we can shoot, and doing so would maximize our points
			performShoot(i);
		} else if (canMove && movePoints == maxPoints) {
			System.out.println("    Performing move");
			// If we can move, and doing so would maximize our points
			performMove(i);
		} else if (canPickup && pickupPoints == maxPoints) {
			System.out.println("    Performing pickup");
			// If we can pickup, and doing so would maximize our points
			performPickup(i);
		} else {
			System.out.println("    Standing by...");
			friendlyUnits[i].standby();
		}
	}

	int moveNumber = 0;

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

		System.out.println("Team: " + friendlyUnits[0].getTeam());
		System.out.println("Move number: " + moveNumber++);

		this.world = world;
		this.enemyUnits = enemyUnits;
		this.friendlyUnits = friendlyUnits;

		Arrays.fill(currentMoveActions, null);

		for (int i = 0; i < friendlyUnits.length; i++) {
			doMove(i);
		}
	}
}
