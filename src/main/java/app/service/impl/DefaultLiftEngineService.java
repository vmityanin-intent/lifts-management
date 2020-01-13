package app.service.impl;

import app.config.LiftsConfiguration;
import app.domain.Lift;
import app.domain.LiftButtons;
import app.domain.enums.Direction;
import app.domain.enums.LiftState;
import app.service.LiftEngineService;
import com.jcabi.log.VerboseRunnable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toConcurrentMap;
import static java.util.stream.Collectors.toUnmodifiableMap;

@Slf4j
public class DefaultLiftEngineService implements LiftEngineService {

    @Value("${lifts-quantity}")
    private int liftsQuantity;

    @Value("${floors-quantity}")
    private int floorsQuantity;

    private final LiftsConfiguration liftsConfiguration;

    private ExecutorService executor;

    private ConcurrentHashMap<String, Lift> liftsState;

    private Map<String, Pair<Lock, Condition>> requestAvailabilityConditions;

    private final Function<String, Runnable> liftEngine = (id) -> () -> {
        while (true) {
            acquireLockOnLift(id);

            while (getButtons(id).isEveryButtonTurnedOff()) {
                waitUntilAnyButtonIsPressed(id);
            }

            if (getState(id).getDirection() == Direction.UP) {
                Optional<Integer> nextFloorRequestedAbove = getButtons(id).getNextOnTheWayUp(getCurrentFloor(id));
                if (nextFloorRequestedAbove.isPresent()) {
                    log.debug("Lift {} from {} heading to {} floor.", id, getCurrentFloor(id), nextFloorRequestedAbove.get());
                    moveToTheNextFloor(id, Direction.UP, isFinalIterationToRequester(getCurrentFloor(id), nextFloorRequestedAbove.get()));
                } else {
                    setProcessingDirection(id, Direction.ANY);
                }
            }

            if (getState(id).getDirection() == Direction.DOWN) {
                Optional<Integer> nextFloorRequestedBelow = getButtons(id).getNextOnTheWayDown(getCurrentFloor(id));
                if (nextFloorRequestedBelow.isPresent()) {
                    log.debug("Lift {} from {} heading to {} floor.", id, getCurrentFloor(id), nextFloorRequestedBelow.get());
                    moveToTheNextFloor(id, Direction.DOWN, isFinalIterationToRequester(getCurrentFloor(id), nextFloorRequestedBelow.get()));
                } else {
                    setProcessingDirection(id, Direction.ANY);
                }
            }

            turnOffCurrentButtonAndCheckState(id);

            if (getState(id).getDirection() == Direction.ANY) {
                mustGoGround(id);
            }

            releaseLockOnLift(id);
        }
    };


    public DefaultLiftEngineService(LiftsConfiguration liftsConfiguration) {
        this.liftsConfiguration = liftsConfiguration;
    }

    @PostConstruct
    public void initialize() {
        liftsState = IntStream.rangeClosed(1, liftsQuantity).boxed()
                .collect(toConcurrentMap(String::valueOf,
                        k -> Lift.builder()
                                .id(String.valueOf(k))
                                .secondsPerFloor(liftsConfiguration.getSecondsPerFloor())
                                .tonnage(new Random().nextInt(1000))
                                .buttons(new LiftButtons(floorsQuantity))
                                .direction(Direction.UP)
                                .build(),
                        (a, b) -> {
                            throw new RuntimeException(String.format("Conflicting lift names %s and %s", a, b));
                        },
                        ConcurrentHashMap::new));

        requestAvailabilityConditions = IntStream.rangeClosed(1, liftsQuantity).boxed()
                .collect(toUnmodifiableMap(String::valueOf,
                        k -> {
                            Lock lock = new ReentrantLock();
                            return ImmutablePair.of(lock, lock.newCondition());
                        },
                        (a, b) -> {
                            throw new RuntimeException(String.format("Conflicting lift names %s and %s", a, b));
                        }));
        executor = Executors.newFixedThreadPool(liftsQuantity);
        liftsState.keySet().forEach(this::startLift);

    }

    @Override
    public void enqueueStopRequest(String id, int floor) {
        liftsState.compute(id, (k, v) -> Lift.copyWithButtons(v, v.getButtons().turnOnButtonAtFloor(floor)));
        notifyButtonIsPressed(id);
    }

    @Override
    public Map<String, Lift> getCurrentLiftsStateSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(liftsState));
    }

    private void startLift(String id) {
        executor.submit(new VerboseRunnable(liftEngine.apply(id)));
    }


    private void moveToTheNextFloor(String liftId, Direction direction, boolean finalIterationToRequester) {
        log.debug("About to moving lift {} from {} {}.", liftId, getState(liftId).getCurrentFloor(), direction.name());
        startAndTurnLightsOn(liftId);
        waitWhileLiftTravelling(getState(liftId), direction, liftId);
        setNextFloorAndStop(liftId, direction);
        if (finalIterationToRequester) {
            simulateButtonsPressInsideLift(liftId);
        }
    }

    private void simulateButtonsPressInsideLift(String id) {
        List<Integer> buttonsUserPressed = new Random().ints(0, floorsQuantity).limit(2).distinct().boxed().collect(Collectors.toList());
        pressNewButtonsInsideLift(id, buttonsUserPressed);
        log.debug("Lift {} reached the floor requester waited it for and new buttons were pressed inside the lift {}.", id, buttonsUserPressed);
    }

    private void waitWhileLiftTravelling(Lift lift, Direction direction, String id) {
        try {
            Thread.sleep(lift.getSecondsPerFloor() * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(String.format("ERROR moving lift %s from %s %s.", id, lift.getCurrentFloor(), direction.name()));
        }
    }

    private void turnOffCurrentButtonAndCheckState(String liftId) {
        liftsState.compute(liftId, (id, l) ->
                (l.getButtons().isButtonPressed(l.getCurrentFloor()))
                        ? Lift.copyWithButtons(l, l.getButtons().turnOffButtonAtFloor(l.getCurrentFloor()))
                        : l
        );
        liftsState.compute(liftId, (id, l) ->
                (l.getButtons().isEveryButtonTurnedOff())
                        ? Lift.copyWithDirection(l, Direction.ANY)
                        : l
        );
        log.debug("Lift {} buttons: {} ", liftId, liftsState.get(liftId).getButtons());
    }

    private void pressNewButtonsInsideLift(String id, List<Integer> buttonsUserPressed) {
        liftsState.compute(id, (k, v) -> Lift.copyWithButtons(v, v.getButtons().turnOnButtonsAtFloor(buttonsUserPressed)));
    }

    private void setProcessingDirection(String liftId, Direction direction) {
        log.debug("Lift {} reached end position and ready to go direction: {} ", liftId, direction);
        liftsState.compute(liftId, (k, v) -> Lift.copyWithDirection(v, direction));
    }

    private void stopAndTurnLightsOff(String id) {
        log.debug("Lift {} stopping and turning lights off", id);
        liftsState.compute(id, (k, v) -> Lift.copyWithStateAndLights(v, LiftState.STOPPED, false));
    }

    private void startAndTurnLightsOn(String id) {
        log.debug("Lift {} start moving with lights on", id);
        liftsState.compute(id, (k, v) -> Lift.copyWithStateAndLights(v, LiftState.MOVING, true));
    }

    private BiFunction<Direction, Integer, Integer> getDestination = (dir, currentFloor) -> (dir == Direction.UP) ? currentFloor + 1 : currentFloor - 1;

    private void setNextFloorAndStop(String liftId, Direction direction) {
        liftsState.compute(liftId, (id, l) -> Lift.copyWithCurrentFloorAndState(l, getDestination.apply(direction, l.getCurrentFloor()), LiftState.STOPPED));
        log.debug("Lift {} reached {} floor after going {}.", liftId, liftsState.get(liftId).getCurrentFloor(), liftsState.get(liftId).getDirection().name());
    }

    private Function<String, Integer> distanceToNextFloor = (id) -> {
        final Lift lift = getState(id);
        final LiftButtons buttons = lift.getButtons();
        Optional<Integer> nextFloorRequestedAbove = buttons.getNextOnTheWayUp(lift.getCurrentFloor());
        Optional<Integer> nextFloorRequestedBelow = buttons.getNextOnTheWayDown(lift.getCurrentFloor());
        final Integer possibleUp = nextFloorRequestedAbove.orElse(lift.getCurrentFloor());
        final Integer possibleDown = nextFloorRequestedBelow.orElse(lift.getCurrentFloor());
        return Math.abs(possibleUp - lift.getCurrentFloor()) - Math.abs(lift.getCurrentFloor() - possibleDown);
    };

    private void mustGoGround(String liftId) {
        if (distanceToNextFloor.apply(liftId) == 0 && !liftOnGroundFloorExists()) {
            pressNewButtonsInsideLift(liftId, List.of(0));
            log.debug("No Lift present on ground floor, activating ground floor for lift {}.", liftId);
        }
        if (distanceToNextFloor.apply(liftId) > 0) {
            setProcessingDirection(liftId, Direction.UP);
        }
        if (distanceToNextFloor.apply(liftId) < 0) {
            setProcessingDirection(liftId, Direction.DOWN);
        }
    }

    private boolean liftOnGroundFloorExists() {
        return liftsState.values().stream().anyMatch(lift -> lift.getCurrentFloor() == 0);
    }

    private void notifyButtonIsPressed(String id) {
        log.debug("Letting Lift {} know some button is pressed", id);
        acquireLockOnLift(id);
        requestAvailabilityConditions.get(id).getValue().signal();
        releaseLockOnLift(id);
    }

    private void waitUntilAnyButtonIsPressed(String id) {
        log.debug("Lift {} started waiting for some buttons pressed", id);
        stopAndTurnLightsOff(id);
        try {
            requestAvailabilityConditions.get(id).getValue().await();
        } catch (InterruptedException e) {
            throw new RuntimeException(String.format("ERROR at Lift %s started waiting for some buttons pressed", id));
        }
    }

    private void acquireLockOnLift(String id) {
        requestAvailabilityConditions.get(id).getKey().lock();
    }

    private void releaseLockOnLift(String id) {
        requestAvailabilityConditions.get(id).getKey().unlock();
    }

    private boolean isFinalIterationToRequester(int currentPosition, int requestedPosition) {
        return Math.abs(currentPosition - requestedPosition) == 1;
    }

    private Lift getState(String liftId) {
        return liftsState.get(liftId);
    }

    private LiftButtons getButtons(String liftId) {
        return liftsState.get(liftId).getButtons();
    }

    private Integer getCurrentFloor(String liftId) {
        return getState(liftId).getCurrentFloor();
    }

}
