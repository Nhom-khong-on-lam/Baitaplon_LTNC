package com.auction;


import com.auction.common.model.BaseEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class BaseEntityTest {
    static class TestEntity extends BaseEntity {
        public TestEntity() { super(); }
        public TestEntity(Long id) { super(id); }
    }

    @Test
    @DisplayName("Nên khởi tạo đúng ID và thời gian khi dùng constructor có tham số")
    void shouldInitializeWithIdAndTimestamps() {
        // Given
        Long expectedId = 100L;

        // When
        TestEntity entity = new TestEntity(expectedId);

        // Then
        assertThat(entity.getId()).isEqualTo(expectedId);
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        // Kiểm tra thời gian khởi tạo phải gần với thời điểm hiện tại (sai số 1 giây)
        assertThat(entity.getCreatedAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Nên tự động khởi tạo thời gian khi dùng constructor không tham số")
    void shouldInitializeTimestampsWithNoArgConstructor() {
        // When
        TestEntity entity = new TestEntity();

        // Then
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isExactlyInstanceOf(LocalDateTime.class);
    }

    @Test
    @DisplayName("Các phương thức Getter và Setter phải hoạt động chính xác")
    void testGettersAndSetters() {
        // Given
        TestEntity entity = new TestEntity();
        LocalDateTime manualTime = LocalDateTime.of(2023, 1, 1, 10, 0);

        // When
        entity.setId(500L);
        entity.setCreatedAt(manualTime);
        entity.setUpdatedAt(manualTime);

        // Then
        assertThat(entity.getId()).isEqualTo(500L);
        assertThat(entity.getCreatedAt()).isEqualTo(manualTime);
        assertThat(entity.getUpdatedAt()).isEqualTo(manualTime);
    }
}
