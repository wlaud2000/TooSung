package com.project.toosung_back.global.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.toosung_back.global.apiPayload.CustomResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.io.IOException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HttpResponseUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void setSuccessResponse(
            HttpServletResponse response,
            HttpStatus status,
            Object data
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        CustomResponse<?> customResponse = CustomResponse.success(data);
        response.getWriter().write(objectMapper.writeValueAsString(customResponse));
    }

    public static void setErrorResponse(
            HttpServletResponse response,
            HttpStatus status,
            Object data
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(objectMapper.writeValueAsString(data));
    }
}
