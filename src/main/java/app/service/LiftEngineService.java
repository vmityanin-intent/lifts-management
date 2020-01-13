package app.service;

import app.domain.Lift;

import java.util.Map;

public interface LiftEngineService {

    void enqueueStopRequest(String liftId, int floor);

    Map<String, Lift> getCurrentLiftsStateSnapshot();
}
