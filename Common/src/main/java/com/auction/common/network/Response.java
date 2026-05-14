package com.auction.common.network;

import java.io.Serializable;

/**
 * Object được serialize trả về từ Server cho Client.
 */
public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String  message;
    private Object  data;
    private String  token;
    public Response() {}

    public Response(boolean success, String message, Object data) {
        this.success = success;
        this.message = message;
        this.data    = data;
    }

    // Factory helpers
    public static Response ok(Object data) {
        return new Response(true, "OK", data);
    }

    public static Response error(String message) {
        return new Response(false, message, null);
    }

    // Getters / Setters
    public boolean isSuccess()  { return success; }
    public String  getMessage() { return message; }
    public Object  getData()    { return data;    }
    public String  getToken()   { return token;   }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message)  { this.message = message; }
    public void setData(Object data)        { this.data    = data;    }
    public void setToken(String token)      { this.token = token;     }

    @Override
    public String toString() {
        return "Response {success=" + success + ", message='" + message + "', token='" + token + "'}";
    }
}