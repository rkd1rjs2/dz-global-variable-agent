package com.douzone.agent;

import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.instrument.Instrumentation;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class DzGlobalVariableAgent {
    
    public static void premain(String arguments, Instrumentation instrumentation) {
        System.out.println("DzGlobalVariableAgent: 에이전트 시작");
        
        // 1. DzGlobalVariable 클래스의 getGlobalVariable 메서드를 후킹
        new AgentBuilder.Default()
            .type(ElementMatchers.named("com.douzone.gpd.core.DzGlobalVariable"))
            .transform((builder, type, classLoader, module) -> 
                builder.method(ElementMatchers.named("getGlobalVariable"))
                      .intercept(MethodDelegation.to(VariableInterceptor.class))
            )
            .installOn(instrumentation);
        
        // 2. DzAbstractGlobalVariable 클래스의 containsKey 메서드를 후킹
        new AgentBuilder.Default()
            .type(ElementMatchers.named("com.douzone.gpd.core.template.DzAbstractGlobalVariable"))
            .transform((builder, type, classLoader, module) -> 
                builder.method(ElementMatchers.named("containsKey"))
                      .intercept(MethodDelegation.to(ContainsKeyInterceptor.class))
            )
            .installOn(instrumentation);
        
        // 3. DzLazyBeanConfiguration 클래스의 isLazyInit 메서드를 후킹
        new AgentBuilder.Default()
            .type(ElementMatchers.named("com.douzone.gpd.bean.DzLazyBeanConfiguration"))
            .transform((builder, type, classLoader, module) -> 
                builder.method(ElementMatchers.named("isLazyInit"))
                      .intercept(MethodDelegation.to(LazyInitInterceptor.class))
            )
            .installOn(instrumentation);
        
        System.out.println("DzGlobalVariableAgent: 메서드 후킹 완료");
    }
    
    public static class VariableInterceptor {
        private static final Map<String, Object> PROXY_MAP = new HashMap<>();
        
        static {
            // 기본값 설정
            PROXY_MAP.put("db.jdbc.connectionTimeout", "30000");
            PROXY_MAP.put("db.jdbc.maxTotal", "60");
            PROXY_MAP.put("db.jdbc.maxWaitMillis", "30000");
            PROXY_MAP.put("db.jdbc.minIdle", "25");
            PROXY_MAP.put("bean.lazyinit", "false");
            PROXY_MAP.put("property.config", "true");
            PROXY_MAP.put("douzone.config.dir", "./dews-web/config");
            PROXY_MAP.put("quartz.enabled", "false");
            
            System.out.println("DzGlobalVariableAgent: 프록시 맵 초기화 완료, 크기: " + PROXY_MAP.size());
        }
        
        public static Map<String, Object> intercept(@SuperCall Callable<Map<String, Object>> zuper) {
            try {
                Map<String, Object> originalMap = zuper.call();
                if (originalMap != null && !originalMap.isEmpty()) {
                    System.out.println("DzGlobalVariableAgent: 원본 맵 사용 (크기: " + originalMap.size() + ")");
                    return originalMap;
                }
            } catch (Exception e) {
                System.out.println("DzGlobalVariableAgent: 원본 맵 호출 실패: " + e.getMessage());
            }
            System.out.println("DzGlobalVariableAgent: getGlobalVariable 프록시 맵 반환");
            return PROXY_MAP;
        }
    }
    
    public static class ContainsKeyInterceptor {
        public static boolean intercept(@SuperCall Callable<Boolean> zuper, String key) {
            try {
                return zuper.call();
            } catch (Exception e) {
                // Map이 null이거나 다른 예외가 발생하면 VariableInterceptor의 맵 확인
                boolean exists = VariableInterceptor.PROXY_MAP.containsKey(key);
                if (exists) {
                    System.out.println("DzGlobalVariableAgent: 프록시 맵에서 키 찾음: " + key);
                }
                return exists;
            }
        }
    }
    
    public static class LazyInitInterceptor {
        public static boolean intercept(@SuperCall Callable<Boolean> zuper) {
            try {
                boolean original = zuper.call();
                System.out.println("DzGlobalVariableAgent: 원본 LazyInit 값: " + original);
                return original;
            } catch (Exception e) {
                // 원래 호출이 실패하면 기본값 반환
                System.out.println("DzGlobalVariableAgent: LazyInitInterceptor 사용됨");
                return false; // 대부분의 빈을 즉시 로드
            }
        }
    }
}