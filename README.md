# DzGlobalVariable Agent

더존 코멧 웹앱의 DzGlobalVariable 초기화 문제를 해결하기 위한 자바 에이전트입니다.

## 문제 개요

스프링부트 1.5.x로 마이그레이션하는 과정에서 DzGlobalVariable의 정적 필드가 초기화되지 않아 NullPointerException이 발생하는 문제를 해결합니다.

## 동작 방식

이 에이전트는 ByteBuddy를 사용하여 다음 클래스의 메서드를 후킹합니다:

1. `com.douzone.gpd.core.DzGlobalVariable.getGlobalVariable()`
2. `com.douzone.gpd.core.template.DzAbstractGlobalVariable.containsKey()`
3. `com.douzone.gpd.bean.DzLazyBeanConfiguration.isLazyInit()`

원본 메서드 호출이 실패할 경우, 미리 정의된 기본값을 반환하여 애플리케이션이 정상적으로 작동할 수 있도록 합니다.

## 빌드 방법

```bash
mvn clean package
```

빌드 결과물은 `target/dz-global-variable-agent-1.0-SNAPSHOT.jar`에 생성됩니다.

## 사용 방법

스프링부트 애플리케이션 실행 시 다음과 같이 JVM 옵션을 추가합니다:

```bash
java -javaagent:/경로/dz-global-variable-agent-1.0-SNAPSHOT.jar -jar 애플리케이션.jar
```

또는 IDE에서 실행 시 VM 옵션에 다음을 추가합니다:

```
-javaagent:/경로/dz-global-variable-agent-1.0-SNAPSHOT.jar
```

## 주의사항

1. 자바 8 이상에서 동작합니다.
2. 이 에이전트는 임시 해결책이므로, 장기적으로는 코드 리팩토링이 필요합니다.
3. 프로덕션 환경에서 사용하기 전에 충분한 테스트가 필요합니다.