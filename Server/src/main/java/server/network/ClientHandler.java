package server.network;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

// XÓA BỎ DÒNG NÀY: import com.sun.net.httpserver.Request;
// THAY BẰNG IMPORT ĐÚNG LỚP CỦA BẠN:
import com.auction.client.service.Request;
import com.auction.client.service.Response;
import server.repository.UserDAO;
import server.common.model.UserDTO;

public class ClientHandler extends Thread {
    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        // Sử dụng try-with-resources để tự động đóng luồng khi kết thúc
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            while (true) {
                // 1. Nhận đối tượng Request từ Client
                Object input = in.readObject();
                if (!(input instanceof Request)) continue;

                Request req = (Request) input;
                System.out.println("Server nhận lệnh: " + req.getAction());

                Response res = null;
                UserDAO userDAO = new UserDAO();

                // 2. Phân tích lệnh (Action) dựa trên hằng số trong Request.java
                switch (req.getAction()) {
                    case "CHECK_USER_EXISTS": // Kiểm tra username có trùng không
                        String username = (String) req.getData();
                        boolean exists = userDAO.isExisted("username", username);
                        res = new Response(exists, exists ? "Username đã tồn tại" : "Có thể sử dụng", null);
                        break;

                    case "REGISTER": // Thay cho "SAVE_USER" để khớp với Request.java
                        Object[] data = (Object[]) req.getData();
                        UserDTO newUser = new UserDTO();
                        newUser.setUsername((String) data[0]);
                        newUser.setEmail((String) data[1]);
                        newUser.setPassword((String) data[2]);

                        // Lưu thẳng vào Database không cần đợi mã OTP
                        long generatedId = userDAO.insert(newUser);
                        boolean success = (generatedId != -1);

                        res = new Response(success, success ? "Đăng ký thành công!" : "Lỗi hệ thống", null);
                        break;

                    // Xóa bỏ hoàn toàn case SEND_OTP nếu có
                    default:
                        res = new Response(false, "Lệnh không xác định", null);
                }

                // 3. Gửi câu trả lời về cho Client
                out.writeObject(res);
                out.flush();
            }
        } catch (Exception e) {
            System.out.println("Client " + socket.getInetAddress() + " đã ngắt kết nối.");
        }
    }
}