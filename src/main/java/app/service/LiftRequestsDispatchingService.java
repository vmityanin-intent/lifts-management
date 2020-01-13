package app.service;

import app.domain.ElevateRequest;

public interface LiftRequestsDispatchingService {

    void dispatchLiftRequest(ElevateRequest request);

}
