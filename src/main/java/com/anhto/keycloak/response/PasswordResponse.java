package com.anhto.keycloak.response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PasswordResponse {

    private static final Logger logger = LoggerFactory.getLogger(PasswordResponse.class);
    private static final int SALT_LENGTH = 16; // Độ dài salt (16 bytes = 128 bits)
    private static final String HASH_ALGORITHM = "SHA-256"; // Thuật toán hash
    private static final String ENCODING = "UTF-8"; // Encoding cho string

    /**
     * Hash mật khẩu với salt ngẫu nhiên
     *
     * @param rawPassword Mật khẩu gốc (plain text)
     * @return Chuỗi Base64 chứa salt + hash
     * @throws IllegalArgumentException nếu mật khẩu null hoặc rỗng
     */
    public String hashPassword(String rawPassword) {
        // Validate input
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            logger.error("Mật khẩu không được null hoặc rỗng");
            throw new IllegalArgumentException("Mật khẩu không được null hoặc rỗng");
        }

        try {
            // 1. Tạo salt ngẫu nhiên
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);

            // 2. Hash mật khẩu với salt
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt); // Thêm salt vào
            byte[] hashedPassword = md.digest(rawPassword.getBytes(ENCODING));

            // 3. Kết hợp salt và hash thành một mảng
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);

            // 4. Mã hóa thành Base64 để lưu trữ
            return Base64.getEncoder().encodeToString(combined);

        } catch (NoSuchAlgorithmException e) {
            logger.error("Thuật toán hash không khả dụng: {}", HASH_ALGORITHM, e);
            throw new RuntimeException("Thuật toán hash không khả dụng", e);
        } catch (Exception e) {
            logger.error("Lỗi khi hash mật khẩu", e);
            throw new RuntimeException("Lỗi khi hash mật khẩu", e);
        }
    }

    /**
     * Kiểm tra mật khẩu có khớp với hash đã lưu không
     *
     * @param rawPassword Mật khẩu cần kiểm tra
     * @param storedHash Hash đã lưu trong database
     * @return true nếu khớp, false nếu không khớp
     */
    public boolean verifyPassword(String rawPassword, String storedHash) {
        // Validate input
        if (rawPassword == null || storedHash == null) {
            logger.warn("Mật khẩu hoặc hash null");
            return false;
        }

        if (rawPassword.trim().isEmpty() || storedHash.trim().isEmpty()) {
            logger.warn("Mật khẩu hoặc hash rỗng");
            return false;
        }

        try {
            // 1. Giải mã Base64 để lấy lại mảng byte
            byte[] combined = Base64.getDecoder().decode(storedHash);

            // 2. Kiểm tra độ dài mảng
            if (combined.length <= SALT_LENGTH) {
                logger.warn("Hash lưu trữ không hợp lệ - quá ngắn");
                return false;
            }

            // 3. Tách salt và hash từ mảng combined
            byte[] salt = new byte[SALT_LENGTH];
            byte[] storedPasswordHash = new byte[combined.length - SALT_LENGTH];

            System.arraycopy(combined, 0, salt, 0, SALT_LENGTH);
            System.arraycopy(combined, SALT_LENGTH, storedPasswordHash, 0, storedPasswordHash.length);

            // 4. Hash mật khẩu đầu vào với salt đã lấy
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] inputPasswordHash = md.digest(rawPassword.getBytes(ENCODING));

            // 5. So sánh hai hash (dùng constant-time comparison để chống timing attack)
            return MessageDigest.isEqual(storedPasswordHash, inputPasswordHash);

        } catch (IllegalArgumentException e) {
            logger.warn("Định dạng Base64 không hợp lệ trong hash lưu trữ", e);
            return false;
        } catch (NoSuchAlgorithmException e) {
            logger.error("Thuật toán hash không khả dụng: {}", HASH_ALGORITHM, e);
            return false;
        } catch (Exception e) {
            logger.error("Lỗi khi kiểm tra mật khẩu", e);
            return false;
        }
    }

    /**
     * Tạo mật khẩu ngẫu nhiên
     *
     * @param length Độ dài mật khẩu
     * @return Mật khẩu ngẫu nhiên
     */
    public String generateRandomPassword(int length) {
        if (length < 8) {
            length = 8; // Độ dài tối thiểu
        }

        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder();

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(characters.length());
            password.append(characters.charAt(index));
        }

        return password.toString();
    }

    /**
     * Kiểm tra độ mạnh của mật khẩu
     *
     * @param password Mật khẩu cần kiểm tra
     * @return true nếu mật khẩu mạnh, false nếu yếu
     */
    public boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        // Kiểm tra có ít nhất 1 chữ hoa, 1 chữ thường, 1 số, 1 ký tự đặc biệt
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }
}