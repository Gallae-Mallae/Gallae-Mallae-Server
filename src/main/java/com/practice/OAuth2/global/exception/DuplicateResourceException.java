package com.practice.OAuth2.global.exception;


// 중복 데이터 발생 시 던질 커스텀 예외
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}