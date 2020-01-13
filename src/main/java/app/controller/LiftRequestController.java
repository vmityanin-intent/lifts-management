package app.controller;

import app.domain.ElevateRequest;
import app.service.LiftRequestsDispatchingService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lift-requests")
@AllArgsConstructor
public class LiftRequestController {

    private final LiftRequestsDispatchingService liftRequestsDispatchingService;

    @PostMapping
    public ResponseEntity<String> enqueue(@RequestBody ElevateRequest request) {
        liftRequestsDispatchingService.dispatchLiftRequest(request);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

}
