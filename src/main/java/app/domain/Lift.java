package app.domain;

import app.domain.enums.Direction;
import app.domain.enums.LiftState;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Builder
@ToString
@Getter
public final class Lift {

    private final String id;

    private final int secondsPerFloor;

    private final int tonnage;

    private final int currentFloor;

    private final boolean lightOn;

    private final LiftState state;

    private final Direction direction;

    private final LiftButtons buttons;

    public static Lift copyOf(Lift that) {
        return copyWithButtons(that, that.getButtons());
    }

    public static Lift copyWithButtons(Lift that, LiftButtons buttons) {
        return Lift.builder()
                .id(that.getId())
                .secondsPerFloor(that.getSecondsPerFloor())
                .tonnage(that.getTonnage())
                .currentFloor(that.getCurrentFloor())
                .lightOn(that.isLightOn())
                .state(that.getState())
                .direction(that.getDirection())
                .buttons(buttons)
                .build();
    }

    public static Lift copyWithDirection(Lift that, Direction direction) {
        return Lift.builder()
                .id(that.getId())
                .secondsPerFloor(that.getSecondsPerFloor())
                .tonnage(that.getTonnage())
                .currentFloor(that.getCurrentFloor())
                .lightOn(that.isLightOn())
                .state(that.getState())
                .direction(direction)
                .buttons(that.getButtons())
                .build();
    }

    public static Lift copyWithState(Lift that, LiftState state) {
        return copyWithStateAndLights(that, state, that.isLightOn());
    }

    public static Lift copyWithStateAndLights(Lift that, LiftState state, boolean lightOn) {
        return Lift.builder()
                .id(that.getId())
                .secondsPerFloor(that.getSecondsPerFloor())
                .tonnage(that.getTonnage())
                .currentFloor(that.getCurrentFloor())
                .lightOn(lightOn)
                .state(state)
                .direction(that.getDirection())
                .buttons(that.getButtons())
                .build();
    }

    public static Lift copyWithCurrentFloorAndState(Lift that, int currentFloor, LiftState state) {
        return Lift.builder()
                .id(that.getId())
                .secondsPerFloor(that.getSecondsPerFloor())
                .tonnage(that.getTonnage())
                .currentFloor(currentFloor)
                .lightOn(that.isLightOn())
                .state(state)
                .direction(that.getDirection())
                .buttons(that.getButtons())
                .build();
    }
}
