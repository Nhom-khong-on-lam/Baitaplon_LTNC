
import com.auction.common.dto.User_SessionDTO;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

public class User_SessionDTOTest {
    @Test
    void testUserSessionDTO_NotExpired() {
        LocalDateTime future = LocalDateTime.now().plusHours(2);
        User_SessionDTO dto = new User_SessionDTO(1L, 10L, "TOKEN-XYZ", future, LocalDateTime.now());

        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getUserId());
        assertEquals("TOKEN-XYZ", dto.getToken());
        assertFalse(dto.isExpired());

        dto.setToken("NEW-TOKEN");
        assertEquals("NEW-TOKEN", dto.getToken());
        assertNotNull(dto.toString());
    }

    @Test
    void testUserSessionDTO_Expired() {
        LocalDateTime past = LocalDateTime.now().minusHours(2);
        User_SessionDTO dto = new User_SessionDTO();
        dto.setExpiresAt(past);

        assertTrue(dto.isExpired());
    }
}