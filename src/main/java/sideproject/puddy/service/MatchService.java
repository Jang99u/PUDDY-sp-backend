package sideproject.puddy.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sideproject.puddy.dto.match.response.*;
import sideproject.puddy.dto.tag.TagDto;
import sideproject.puddy.model.*;
import sideproject.puddy.repository.MatchRepository;
import sideproject.puddy.repository.PersonRepository;
import sideproject.puddy.security.util.SecurityUtil;


import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final PersonRepository personRepository;
    private final AuthService authService;
    private final DogService dogService;


    // 위치, 매칭 여부 -> (성별, 나이, 반려견 정보)
    public RandomDogDetailListResponse getMatchingByDog(int pageNum) {
        Person currentUser = authService.findById(SecurityUtil.getCurrentUserId());

        List<RandomDogDetailResponse> dogs = matchRepository.findNearPersonNotMatched(
//                        SecurityUtil.getCurrentUserId(),
                        !currentUser.isGender(),
//                        currentUser.getLongitude(),
//                        currentUser.getLatitude(),
                        PageRequest.of(pageNum, 1)
                )
                .map(person -> {
                    // 각 상대방의 main 강아지 탐색
                    log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@person: {}", person);
                    Dog mainDog = dogService.findByPersonAndMain(person);
                    log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@person: {}", mainDog);

                    RandomDogProfileDto randomDogProfileDto = new RandomDogProfileDto(
                            mainDog.getName(),
                            mainDog.getImage(),
                            mainDog.getDogType().getContent(),
                            calculateAge(mainDog.getBirth()),
                            mapTagsToDto(mainDog.getDogTagMaps())
                    );

                    return new RandomDogDetailResponse(
                            person.isGender(),
                            calculateAge(person.getBirth()),
                            person.getMainAddress(),
                            randomDogProfileDto
                    );
                })
                .toList();
        log.info("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@person: {}", dogs);
        return new RandomDogDetailListResponse(dogs);
    }

    private int calculateAge(LocalDate birthDate) {
        LocalDate currentDate = LocalDate.now();
        return Period.between(birthDate, currentDate).getYears();
    }

    @Transactional
    public void likeProfile(Long receiverId) {
        Person sender = authService.findById(SecurityUtil.getCurrentUserId());
        Person receiver = personRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        // 이미 매치된 경우에는 중복 생성하지 않도록 체크
        if (!matchRepository.existsBySenderAndReceiver(sender, receiver))
            matchRepository.save(new Match(sender, receiver));
    }

    private List<TagDto> mapTagsToDto(List<DogTagMap> dogTagMaps) {
        return dogTagMaps.stream()
                .map(dogTagMap -> new TagDto(dogTagMap.getDogTag().getContent()))
                .collect(Collectors.toList());
    }
}
