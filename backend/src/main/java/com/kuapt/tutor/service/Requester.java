package com.kuapt.tutor.service;

import com.kuapt.tutor.model.RoleCode;
import com.kuapt.tutor.model.UserRecord;
import java.util.List;

public record Requester(long userId, UserRecord user, List<RoleCode> roles) {}
