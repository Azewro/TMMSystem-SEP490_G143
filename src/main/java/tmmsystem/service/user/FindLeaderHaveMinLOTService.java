package tmmsystem.service.user;

import org.springframework.stereotype.Service;
import tmmsystem.dto.UserDto;
import tmmsystem.mapper.UserMapper;
import tmmsystem.repository.UserRepository;
import tmmsystem.entity.User;

@Service
public class FindLeaderHaveMinLOTService {
    private final UserRepository userRepo;

    public FindLeaderHaveMinLOTService(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public UserDto getLeaderWithMinLOT() {
        User leader = userRepo.findUserHaveMinLOT().orElse(null);
        return UserMapper.toDto(leader);
    }
}
