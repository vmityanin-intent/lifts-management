package app.service.impl;

import app.domain.ElevateRequest;
import app.domain.Lift;
import app.domain.enums.Direction;
import app.service.LiftEngineService;
import app.service.LiftRequestsDispatchingService;
import lombok.AllArgsConstructor;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

@AllArgsConstructor
public class DefaultLiftRequestsDispatchingService implements LiftRequestsDispatchingService {

    private final LiftEngineService liftEngineService;

    @Override
    public void dispatchLiftRequest(ElevateRequest request) {
        String liftId = getMostSuitableLift(request);
        liftEngineService.enqueueStopRequest(liftId, request.getFloorNumber());
    }

    private String getMostSuitableLift(ElevateRequest elevateRequest) {
        return getFirstMatchingLift(List.of(
                getClosestIdleFunction(elevateRequest),
                getClosestMovingToRequesterFunction(elevateRequest),
                getJustClosestToRequesterFunction(elevateRequest))
        );
    }

    private Function<Map<String, Lift>, Optional<String>> getClosestIdleFunction(ElevateRequest elevateRequest) {
        Predicate<Lift> closestIdle = lift -> lift.getDirection() == Direction.ANY;
        return liftState -> liftState.values().stream()
                .filter(closestIdle)
                .sorted(Comparator.comparing((Lift lift) -> Math.abs(lift.getCurrentFloor() - elevateRequest.getFloorNumber()))
                        .thenComparing(Lift::getTonnage))
                .map(Lift::getId).findAny();
    }

    private Function<Map<String, Lift>, Optional<String>> getJustClosestToRequesterFunction(ElevateRequest elevateRequest) {
        return liftsState -> liftsState.values().stream().
                sorted(Comparator.comparing((Lift lift) -> Math.abs(lift.getCurrentFloor() - elevateRequest.getFloorNumber()))
                        .thenComparing(Lift::getTonnage))
                .map(Lift::getId).findFirst();
    }


    private Function<Map<String, Lift>, Optional<String>> getClosestMovingToRequesterFunction(ElevateRequest elevateRequest) {
        return liftsState -> {
            Predicate<Lift> belowAndGoingUp = lift -> lift.getCurrentFloor() < elevateRequest.getFloorNumber() && lift.getDirection() == Direction.UP;
            Predicate<Lift> aboveAndGoingDown = lift -> lift.getCurrentFloor() > elevateRequest.getFloorNumber() && lift.getDirection() == Direction.DOWN;

            return (elevateRequest.getDirection() == Direction.UP)
                    ?
                    liftsState.values().stream()
                            .filter(belowAndGoingUp)
                            .sorted(Comparator.comparing(Lift::getCurrentFloor).reversed().thenComparing(Lift::getTonnage))
                            .map(Lift::getId).findFirst()

                    :
                    liftsState.values().stream()
                            .filter(aboveAndGoingDown)
                            .sorted(Comparator.comparing(Lift::getCurrentFloor).thenComparing(Lift::getTonnage))
                            .map(Lift::getId).findFirst();
        };
    }

    private String getFirstMatchingLift(Collection<Function<Map<String, Lift>, Optional<String>>> idCalculators) {
        return idCalculators.stream()
                .map(idCalculator -> idCalculator.apply(liftEngineService.getCurrentLiftsStateSnapshot()))
                .filter(Optional::isPresent)
                .findFirst()
                .map(Optional::get).orElseThrow(() -> new RuntimeException("No Lift Found to process request"));
    }
}
