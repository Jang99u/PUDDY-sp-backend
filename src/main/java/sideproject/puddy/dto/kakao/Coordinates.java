package sideproject.puddy.dto.kakao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
@Data
@Getter
@AllArgsConstructor
public class Coordinates {
    private Double lat;
    private Double lng;
}