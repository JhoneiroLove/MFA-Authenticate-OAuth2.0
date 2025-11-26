package com.security.mfaautenticate.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class MfaService {

    @Value("${mfa.issuer:MFA-Authenticate-App}")
    private String issuer;

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    public String generateSecretKey() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    public String generateQRCodeDataUri(String secret, String email) {
        try {
            String otpAuthURL = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                    issuer,
                    email,
                    new GoogleAuthenticatorKey.Builder(secret).build()
            );

            // Generar QR code
            BitMatrix matrix = new MultiFormatWriter().encode(
                    otpAuthURL,
                    BarcodeFormat.QR_CODE,
                    300,
                    300
            );

            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);

            // Convertir a Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Retornar como data URI
            return "data:image/png;base64," + base64Image;

        } catch (Exception e) {
            throw new RuntimeException("Error generando QR code", e);
        }
    }

    public boolean verifyCode(String secret, String code) {
        try {
            int codeInt = Integer.parseInt(code);
            return gAuth.authorize(secret, codeInt);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}