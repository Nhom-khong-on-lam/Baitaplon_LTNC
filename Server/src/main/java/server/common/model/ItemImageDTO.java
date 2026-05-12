package server.common.model;

public class ItemImageDTO {
    private Long id;
    private Long itemId;
    private String imageUrl;

    // Constructor không tham số cho DAO
    public ItemImageDTO() {
    }

    // Constructor đầy đủ tham số (tiện cho việc khởi tạo nhanh)
    public ItemImageDTO(Long itemId, String imageUrl) {
        this.itemId = itemId;
        this.imageUrl = imageUrl;
    }

    // Getter và Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }
    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}