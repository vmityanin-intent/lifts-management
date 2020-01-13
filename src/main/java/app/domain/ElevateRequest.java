package app.domain;

import app.domain.enums.Direction;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ElevateRequest {

    private int floorNumber;

    private Direction direction;


}
