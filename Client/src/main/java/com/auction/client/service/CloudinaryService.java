package com.auction.client.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.File;
import java.util.Map;

public class CloudinaryService {
    private static Cloudinary cloudinary;

    static {
        // Lấy 3 thông tin này trên trang Dashboard khi đăng nhập vào Cloudinary của bạn
        cloudinary = new Cloudinary(ObjectUtils.asMap("cloud_name", "dpaxdzojy", "api_key", "996928454123269", "api_secret", "_Qd145vA0Ll_K-KQZS9rPe4mCdI", "secure", true));
    }

    /**
     * Truyền vào File ảnh local -> Đẩy lên Cloudinary -> Trả về chuỗi link mạng https://...
     */
    public static String uploadImage(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            // Thực hiện lệnh đẩy file lên Cloudinary
            Map uploadResult = cloudinary.uploader().upload(file, ObjectUtils.emptyMap());

            // Lấy ra đường link trực tuyến an toàn mạng trả về
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            System.err.println("Lỗi khi upload ảnh lên Cloudinary: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}