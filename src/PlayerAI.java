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
	private static final int CP_DEFEND_SHOOT_MULTIPLIER = 7;
	private static final int CP_DEFEND_ENEMY_PROXIMITY = 3;
	private static final int CP_DEFEND_POINTS_PER_MOVE_MULTIPLIER = 100;
	private static final float MAINFRAME_DAMAGE_MULTIPLIER = 2f;
	private static final float MAINFRAME_DEFENSE_MULTIPLIER = 2f;

	private static final int POINTS_PER_DAMAGE = 10;
	private static final int REPAIR_KIT_HEALTH_AMOUNT = 20;
	private static final float MOVE_DISTANCE_EXPONENT = 1.5f;
	private static final float MOVE_DISTANCE_MAINFRAME_EXPONENT = 1.25f;
	private static final int ENEMY_KILL_POINTS = 100;
	private static final int NEUTRALIZE_CONTROL_POINT_POINTS = 775;
	private static final int CAPTURE_CONTROL_POINT_POINTS = 400;
	private static final int PICKUP_POINTS = 50;
	private static final int POINTS_FOR_HELPING_OUT_FRIENDLY = 250;

	private static final int NO_MAINFRAME_MAX_TEAM_DISTANCE = 5;
	private static final int NO_MAINFRAME_GROUPING_POINTS = 250;

	private static final float MOVE_MULTIPLIER = 1.0f;
	private static final float SHOOT_MULTIPLIER = 1.0f;
	private static final float SHIELD_MULTIPLIER = 1.0f;
	private static final float PICKUP_MULTIPLIER = 3.0f;

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
			// Only prevent moving on enemy if they are alive
			if (movePosition.equals(enemyUnit.getPosition())
					&& enemyUnit.getHealth() > 0) {
				return false;
			}
		}

		// For each friendly unit
		for (int j = 0; j < NUM_UNITS; j++) {
			// If this friendly unit is dead, we can move to its position
			if (friendlyUnits[j].getHealth() <= 0) {
				continue;
			}
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
				float distanceExponent = MOVE_DISTANCE_EXPONENT;

				for (ControlPoint cp : controlPoints) {
					// Only consider this cp if the current direction decreases
					// the path length by 1
					if (getDifferenceInPathLengths(
							friendlyUnits[i].getPosition(), directionPoint,
							cp.getPosition()) != 1) {
						continue;
					}
					int cpPoints;
					if (cp.getControllingTeam() == friendlyUnits[i].getTeam()) {
						cpPoints = 0;
						// defend the point if there are enemies around
						for (int j = 0; j < enemyUnits.length; j++) {
							// ignore dead units
							if (enemyUnits[j].getHealth() <= 0)
								continue;
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
						// NEUTRALIZE_CONTROL_POINT_POINTS extra points for
						// neutralizing an opposing control
						// point
						cpPoints = NEUTRALIZE_CONTROL_POINT_POINTS;

						if (cp.isMainframe()) {
							// add 400 extra points for mainframe
							cpPoints += 400;
							distanceExponent = MOVE_DISTANCE_MAINFRAME_EXPONENT;
							// if we have no mainframes, but enemy does
							if (numberOfMainframesControlled(friendlyUnits[i]
									.getTeam()) == 0
									&& numberOfMainframesControlled(enemyUnits[i]
											.getTeam()) > 0) {
								// rush for mainframe!!!
								distanceExponent = 1;
							}
						} else {
							// Don't go to enemy cp that are guarded
							for (int j = 0; j < enemyUnits.length; j++) {
								// ignore dead enemies
								if (enemyUnits[j].getHealth() == 0)
									continue;
								int pathLengthFromEnemy = world.getPathLength(
										enemyUnits[j].getPosition(),
										cp.getPosition());
								if (pathLengthFromEnemy <= 2) {
									cpPoints = 0;
									break;
								}
							}
						}
					} else {
						cpPoints = CAPTURE_CONTROL_POINT_POINTS;
					}
					int distanceToCP = world.getPathLength(directionPoint,
							cp.getPosition());
					// any point within 1 radius counts as CP point
					if (distanceToCP == 0)
						distanceToCP++;
					// Make the points for this cp drop off with distance
					// according to x^MOVE_DISTANCE_EXPONENT
					pointsForDirection += cpPoints
							/ Math.pow(distanceToCP, distanceExponent);
				}

				for (Pickup p : pickups) {
					// Only consider this pickup if the current direction
					// decreases the path length by 1
					if (getDifferenceInPathLengths(
							friendlyUnits[i].getPosition(), directionPoint,
							p.getPosition()) != 1) {
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
					// according to x^MOVE_DISTANCE_EXPONENT
					pointsForDirection += pickupPoints
							/ Math.pow(
									world.getPathLength(directionPoint,
											p.getPosition()) + 1,
									MOVE_DISTANCE_EXPONENT);

				}

				for (int j = 0; j < NUM_UNITS; j++) {
					int pointsForEnemy = 0;
					int closestEnemy = Integer.MAX_VALUE;
					Point lastUnitLocation = friendlyUnits[j].getPosition();
					// skip yourself
					if (j == i)
						continue;
					if (friendlyUnits[j].getDamageTakenLastTurn() > 0) {
						// if a friendly took damage last turn
						// get an array of enemies that attacked that friendly
						EnemyUnit enemyUnitsWhoAttacked[] = friendlyUnits[j]
								.getEnemiesWhoShotMeLastTurn();
						// if enemies that shot friendly are within 5 range, go
						// towards them
						for (int k = 0; k < enemyUnitsWhoAttacked.length; k++) {
							if (world.getPathLength(
									friendlyUnits[i].getPosition(),
									enemyUnitsWhoAttacked[k].getPosition()) < 6
									&& world.getPathLength(friendlyUnits[i]
											.getPosition(),
											enemyUnitsWhoAttacked[k]
													.getPosition()) < closestEnemy) {
								// Only consider going there if the current
								// direction
								// decreases the path length by 1
								if (getDifferenceInPathLengths(
										friendlyUnits[i].getPosition(),
										directionPoint,
										enemyUnitsWhoAttacked[k].getPosition()) != 1) {
									continue;
								} else {
									pointsForEnemy = POINTS_FOR_HELPING_OUT_FRIENDLY;
									lastUnitLocation = enemyUnitsWhoAttacked[k]
											.getPosition();
									closestEnemy = world.getPathLength(
											friendlyUnits[i].getPosition(),
											enemyUnitsWhoAttacked[k]
													.getPosition());
								}

							}
						}
					} else {
						continue;
					}

					// Move to help out friendly unit. We are not using
					// exponential here
					// There is no point in getting real close, so anything
					// closer than 3 units is same rate
					int distanceToEnemy = world.getPathLength(directionPoint,
							lastUnitLocation);
					if (distanceToEnemy < 3)
						distanceToEnemy = 3;
					pointsForDirection += pointsForEnemy / distanceToEnemy;

				}

				int potentialDamageTakenByStaying = maximumPotentialDamageTaken(friendlyUnits[i]
						.getPosition());
				int damageTakenByStayingPoints = potentialDamageTakenByStaying
						* POINTS_PER_DAMAGE;

				if (friendlyUnits[i].getHealth() <= potentialDamageTakenByStaying) {
					damageTakenByStayingPoints += ENEMY_KILL_POINTS;
				}

				// Calculate the damage and points received by the enemy for
				// damaging us in the new position
				int potentialDamageTakenByMoving = maximumPotentialDamageTaken(directionPoint);
				int damageTakenByMovingPoints = potentialDamageTakenByMoving
						* POINTS_PER_DAMAGE;

				// If the hit will kill us then factor in the enemy gaining
				// ENEMY_KILL_POINTS
				if (friendlyUnits[i].getHealth() <= potentialDamageTakenByMoving) {
					damageTakenByMovingPoints += ENEMY_KILL_POINTS;
				}

				// Use the difference in damage between moving to the new
				// position and staying in the current position
				pointsForDirection -= damageTakenByMovingPoints
						- damageTakenByStayingPoints;
				pointsForDirection += maximumPotentialDamageDealtPoints(i,
						directionPoint)
						- maximumPotentialDamageDealtPoints(i,
								friendlyUnits[i].getPosition());

				// If no one has mainframes
				if (numberOfMainframesControlled(friendlyUnits[i].getTeam()) == 0
						&& numberOfMainframesControlled(enemyUnits[i].getTeam()) == 0) {
					// Try to gather as a group to stay alive
					for (int j = 0; j < NUM_UNITS; j++) {
						// If moving in this direction will take us to another
						// friendlyUnit
						if (i != j
								&& d.equals(world.getNextDirectionInPath(
										friendlyUnits[i].getPosition(),
										friendlyUnits[j].getPosition()))) {
							// Only move towards other friendlyUnits if the
							// distance is larger than
							// NO_MAINFRAME_MAX_TEAM_DISTANCE
							int pathLength = world.getPathLength(
									friendlyUnits[i].getPosition(),
									friendlyUnits[j].getPosition());
							if (pathLength > NO_MAINFRAME_MAX_TEAM_DISTANCE) {
								pointsForDirection += NO_MAINFRAME_GROUPING_POINTS
										/ (pathLength + 1);
							}
						}
					}
				}

				// Choose the direction that maximizes our points
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
		int minEnemyHP = Integer.MAX_VALUE;
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
			int points = damage * POINTS_PER_DAMAGE;
			// if we kill the enemy, add ENEMY_KILL_POINTS points
			if (enemyUnits[j].getHealth() <= damage) {
				points += ENEMY_KILL_POINTS;
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
				minEnemyHP = enemyUnits[j].getHealth();
			} else if (points == maxPoints) {
				// if we can shoot more than 1 guy
				if (enemyUnits[j].getHealth() < minEnemyHP) {
					// we pick guy with least health
					maxPoints = points;
					enemiesToShoot[i] = enemyUnits[j];
					minEnemyHP = enemyUnits[j].getHealth();
				}
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
		int amountOfDamageTaken = maximumPotentialDamageTaken(friendlyUnits[i]
				.getPosition());
		int amountOfPoints = amountOfDamageTaken * POINTS_PER_DAMAGE;
		if (friendlyUnits[i].getHealth() < amountOfDamageTaken) {
			// unit will die, and enemy will receive additional
			// ENEMY_KILL_POINTS points
			amountOfPoints += ENEMY_KILL_POINTS;
		}
		if (numberOfMainframesControlled(enemyUnits[i].getTeam()) > 0
				&& numberOfMainframesControlled(friendlyUnits[i].getTeam()) == 0) {
			// if we have no mainframes, but enemy does
			amountOfPoints = (int) (amountOfPoints * MAINFRAME_DEFENSE_MULTIPLIER);
		}
		return amountOfPoints;
	}

	/**
	 * Determine the maximum potential damage that enemies can deal to a certain
	 * point.
	 * 
	 * @param p
	 *            point that we are interested in.
	 * @return maximum damage that enemies can deal to that location next turn
	 */
	private int maximumPotentialDamageTaken(Point p) {
		int amountOfDamageTaken = 0;
		int damageMultiplier = 0;
		for (int j = 0; j < enemyUnits.length; j++) {
			// get unit's range
			int range = enemyUnits[j].getCurrentWeapon().getRange();
			// If the enemy can shoot the target and the enemy is alive
			if (world.canShooterShootTarget(enemyUnits[j].getPosition(), p,
					range) && enemyUnits[j].getHealth() > 0) {
				amountOfDamageTaken += enemyUnits[j].getCurrentWeapon()
						.getDamage();
				damageMultiplier++;
			}
		}
		return amountOfDamageTaken * damageMultiplier;
	}

	/**
	 * Determine the maximum potential damage that we can deal to enemies if
	 * friendlyUnit i moves to point p.
	 * 
	 * @param i
	 *            The index of the friendlyUnit we are interested in.
	 * @param p
	 *            The point we are interested in.
	 * @return The maximum damage that we can deal to enemies if friendlyUnit i
	 *         moves to Point p.
	 */
	private int maximumPotentialDamageDealtPoints(int i, Point p) {
		int maxPoints = 0;

		// For each enemyUnit
		for (int j = 0; j < NUM_UNITS; j++) {
			// If shooting the current enemy isn't valid, skip it
			if (!world.canShooterShootTarget(p, enemyUnits[j].getPosition(),
					friendlyUnits[i].getCurrentWeapon().getRange())
					&& enemyUnits[j].getHealth() > 0) {
				continue;
			}

			int totalDamage = 0;
			int damageMultiplier = 0;

			// For each friendlyUnit
			for (int k = 0; k < NUM_UNITS; k++) {
				// Determine the position of the friendlyUnit next turn
				Point nextFriendlyPosition = currentMoveActions[k] != null ? currentMoveActions[k]
						: friendlyUnits[k].getPosition();
				// If the current friendlyUnit is the ith friendlyUnit, we need
				// to consider that it's moving to point p
				if (i == k) {
					nextFriendlyPosition = p;
				}

				// If the friendlyUnit can shoot the target and it's alive
				if (world.canShooterShootTarget(nextFriendlyPosition,
						enemyUnits[j].getPosition(), friendlyUnits[k]
								.getCurrentWeapon().getRange())
						&& enemyUnits[j].getHealth() > 0) {
					totalDamage += friendlyUnits[k].getCurrentWeapon()
							.getDamage();
					damageMultiplier++;
				}
			}

			int damage = totalDamage * damageMultiplier;
			int points = damage * POINTS_PER_DAMAGE;
			// if we kill the enemy, add ENEMY_KILL_POINTS points
			if (enemyUnits[j].getHealth() <= damage) {
				points += ENEMY_KILL_POINTS;
			}

			if (points > maxPoints) {
				maxPoints = points;
			}
		}
		return maxPoints;
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

		int damageWillTake = maximumPotentialDamageTaken(friendlyUnits[i]
				.getPosition());
		// if pickup type is a health kit
		if (currentPickupType == PickupType.REPAIR_KIT) {
			if (damageWillTake >= REPAIR_KIT_HEALTH_AMOUNT) {
				// if we will take more than REPAIR_KIT_HEALTH_AMOUNT dmg next
				// move picking up repair kit will not be beneficial
				return 0;
			} else {
				// net Health Gained * POINTS_PER_DAMAGE points + PICKUP_POINTS
				// for pickup
				return (REPAIR_KIT_HEALTH_AMOUNT - damageWillTake)
						* POINTS_PER_DAMAGE + PICKUP_POINTS;
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
			value = 200;
			break;
		case WEAPON_LASER_RIFLE:
			switch (currentWeapon) {
			case LASER_RIFLE:
				value = PICKUP_POINTS;
				break;
			case MINI_BLASTER:
				value = 100;
				break;
			case RAIL_GUN:
				value = 0;
				break;
			case SCATTER_GUN:
				value = 0;
				break;
			}
			break;
		case WEAPON_MINI_BLASTER:
			switch (currentWeapon) {
			case LASER_RIFLE:
				value = 0;
				break;
			case MINI_BLASTER:
				value = PICKUP_POINTS;
				break;
			case RAIL_GUN:
				value = 0;
				break;
			case SCATTER_GUN:
				value = 0;
				break;
			}
			break;
		case WEAPON_RAIL_GUN:
			switch (currentWeapon) {
			case LASER_RIFLE:
				value = 200;
				break;
			case MINI_BLASTER:
				value = 200;
				break;
			case RAIL_GUN:
				value = PICKUP_POINTS;
				break;
			case SCATTER_GUN:
				value = 200;
				break;
			}
			break;
		case WEAPON_SCATTER_GUN:
			switch (currentWeapon) {
			case LASER_RIFLE:
				value = 150;
				break;
			case MINI_BLASTER:
				value = 150;
				break;
			case RAIL_GUN:
				value = 0;
				break;
			case SCATTER_GUN:
				value = PICKUP_POINTS;
				break;
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
			movePoints = (int) (pointsForMove(i) * MOVE_MULTIPLIER);
			maxPoints = Math.max(maxPoints, movePoints);
		}
		if (canShoot) {
			shootPoints = (int) (pointsForShoot(i) * SHOOT_MULTIPLIER);
			maxPoints = Math.max(maxPoints, shootPoints);
		}
		if (canShield) {
			shieldPoints = (int) (pointsForShield(i) * SHIELD_MULTIPLIER);
			maxPoints = Math.max(maxPoints, shieldPoints);
		}
		if (canPickup) {
			pickupPoints = (int) (pointsForPickup(i) * PICKUP_MULTIPLIER);
			maxPoints = Math.max(maxPoints, pickupPoints);
		}

		System.out.println("  Unit " + friendlyUnits[i].getASCIIIcon()
				+ ": move=" + movePoints + " shoot=" + shootPoints + " shield="
				+ shieldPoints + " pickup=" + pickupPoints);

		if (canShield && shieldPoints == maxPoints) {
			System.out.println("    Performing shield");
			// If we can shield, and doing so would maximize our points
			performShield(i);
		} else if (canShoot && shootPoints == maxPoints) {
			System.out.println("    Performing shoot");
			// If we can shoot, and doing so would maximize our points
			performShoot(i);
		} else if (canPickup && pickupPoints == maxPoints) {
			System.out.println("    Performing pickup");
			// If we can pickup, and doing so would maximize our points
			performPickup(i);
		} else if (canMove && movePoints == maxPoints) {
			System.out.println("    Performing move");
			// If we can move, and doing so would maximize our points
			performMove(i);
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
