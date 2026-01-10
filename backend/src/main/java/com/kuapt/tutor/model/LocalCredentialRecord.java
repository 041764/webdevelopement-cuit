package com.kuapt.tutor.model;

public record LocalCredentialRecord(
    long userId,
    String clientSalt,
    String clientHash,
    String serverHash) {}
