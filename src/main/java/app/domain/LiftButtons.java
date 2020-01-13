package app.domain;

import java.util.*;

public final class LiftButtons {

    private final boolean[] keys;

    public LiftButtons(int floorQuantity) {
        this.keys = new boolean[floorQuantity];
    }

    public LiftButtons(boolean[] keys) {
        this.keys = keys;
    }

    public final Optional<Integer> getNextOnTheWayUp(int currentFloor) {
        for (int i = currentFloor + 1; i < keys.length; i++) {
            if (keys[i]) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public final Optional<Integer> getNextOnTheWayDown(int currentFloor) {
        for (int i = currentFloor - 1; i >= 0; i--) {
            if (keys[i]) {
                return Optional.of(i);
            }
        }
        return Optional.empty();
    }

    public final LiftButtons turnOffButtonAtFloor(int floor) {
        boolean[] newArray = Arrays.copyOf(keys, keys.length);
        newArray[floor] = false;
        return new LiftButtons(newArray);
    }

    public final LiftButtons turnOnButtonAtFloor(int floor) {
        return turnOnButtonsAtFloor(List.of(floor));
    }

    public final LiftButtons turnOnButtonsAtFloor(Collection<Integer> floors) {
        boolean[] newArray = Arrays.copyOf(keys, keys.length);
        floors.forEach(floorToSetOn -> newArray[floorToSetOn] = true);
        return new LiftButtons(newArray);
    }

    public final boolean isEveryButtonTurnedOff() {
        for (boolean key : keys) {
            if (key) {
                return false;
            }
        }
        return true;
    }

    public final boolean isButtonPressed(int floor) {
        if (floor >= keys.length || floor < 0) {
            throw new RuntimeException(String.format("invalid floor %s", floor));
        }
        return keys[floor];
    }

    @Override
    public final String toString() {
        final StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");

        for (int i = 0; i < keys.length; i++) {
            stringJoiner.add(i + "=" + ((keys[i]) ? "\u2611" :"\u25A1"));
        }
        return stringJoiner.toString();
    }
}