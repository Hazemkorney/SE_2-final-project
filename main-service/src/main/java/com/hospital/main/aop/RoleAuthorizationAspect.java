package com.hospital.main.aop;

import com.hospital.main.security.RequestUser;
import com.hospital.main.security.RequestUserHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class RoleAuthorizationAspect {
    @Around("@annotation(requiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint, RequiresRole requiresRole) throws Throwable {
        RequestUser requestUser = RequestUserHolder.get();
        if (requestUser == null || Arrays.stream(requiresRole.value()).noneMatch(v -> v.equals(requestUser.role()))) {
            throw new AccessDeniedException("Access denied for role");
        }
        return joinPoint.proceed();
    }
}
