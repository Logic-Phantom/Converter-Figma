package com.tomatosystem.service;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.core.io.InputStreamResource;

import java.io.IOException;
import java.io.InputStream;

public class CustomInputStreamHttpMessageConverter extends AbstractHttpMessageConverter<InputStreamResource> {

    public CustomInputStreamHttpMessageConverter() {
        super(MediaType.APPLICATION_OCTET_STREAM);  // 기본 MIME 타입은 application/octet-stream
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return InputStreamResource.class.isAssignableFrom(clazz);
    }

    @Override
    protected InputStreamResource readInternal(Class<? extends InputStreamResource> clazz, HttpInputMessage inputMessage) throws IOException {
        InputStream body = inputMessage.getBody();
        return new InputStreamResource(body);  // InputStreamResource를 반환
    }

    @Override
    protected void writeInternal(InputStreamResource inputStreamResource, HttpOutputMessage outputMessage) throws IOException {
        InputStream inputStream = inputStreamResource.getInputStream();
        // OutputStream에 데이터를 기록
        inputStream.transferTo(outputMessage.getBody());
    }
}