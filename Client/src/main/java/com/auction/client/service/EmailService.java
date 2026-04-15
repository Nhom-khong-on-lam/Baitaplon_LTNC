package com.auction.client.service;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {
    private final String username = "your-email@gmail.com"; // Email của bạn
    private final String appPassword = "xxxx xxxx xxxx xxxx"; // Mật khẩu ứng dụng

    public void sendOTP(String toEmail, String otp) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, appPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Auction System - Registration OTP");
        message.setText("Your verification code is: " + otp + "\nThis code expires in 5 minutes.");

        Transport.send(message);
    }
}