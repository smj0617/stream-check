# stream-check

자바 Stream 쇼츠(`stream-1-shorts`) 대본에 나올 동작을 작은 코드로 확인한다. 순수 자바 25(스프링 없음), Gradle `application` 플러그인. 코드는 `src/main/java/demo/App.java` 하나.

## 확인하는 것과 결과
1. 중간 연산(filter, map)은 최종 연산 전에는 실행되지 않는다 → 확인. 스트림을 만든 뒤 실행 횟수 0, `collect` 부른 뒤 9.
2. 원소 하나가 filter → map → forEach 를 다 지난 뒤 다음 원소가 온다(세로 처리) → 확인. 단 `sorted` 는 앞 단계(peek)에서 4개를 전부 모은 뒤에야 map 이 시작된다.
3. findFirst / limit / anyMatch 는 필요한 만큼만 처리한다(단락) → 확인. 100만 개 중 findFirst 4개, filter(짝수).limit(5) 9개, anyMatch(n > 41) 43개만 지나감. 맞는 게 없는 anyMatch 는 100만 개 전부, 섞인 원본의 `sorted().findFirst()` 도 100만 개 전부(이미 정렬된 원본이면 sorted 를 건너뛰어 1개).
4. 최종 연산을 한 스트림은 다시 못 쓴다 → 확인. 두 번째 `count()` 에서 `IllegalStateException: stream has already been operated upon or closed`. 중간 연산을 이미 붙인 스트림에 또 붙여도 같은 예외.
5. 원본 리스트는 바뀌지 않는다 → 확인([5, 3, 1, 4, 2] 그대로). 단 원소 객체(StringBuilder)를 forEach 안에서 고치면 원본의 원소도 바뀐다.
6. 무한 스트림도 limit 이나 단락 연산이면 끝난다 → 확인. iterate(x2).limit(10) 10개, iterate(+1) 에서 제곱>1000 첫 수 32(32개 만듦), generate.limit(3) 3개.
7. for 문 두 번은 중간 리스트를 만들고 스트림은 만들지 않는다 → 확인. for 문 두 번 = 새 리스트 2개(중간 5칸 + 결과), 스트림 = 결과 1개. 단 for 문 하나에 if 로 넣어도 1개다.

## IntelliJ에서 돌리기
1. File > Open 으로 이 폴더를 연다(Gradle 프로젝트로 가져온다). SDK는 Java 25.
2. `src/main/java/demo/App.java` 의 `main` 옆 실행 버튼을 누른다.
3. 콘솔의 `[사실 n]` 줄을 본다. 기대 출력은 `run-expected.txt`.

터미널에서는 `gradlew.bat run` (JAVA_HOME이 Java 25를 가리켜야 한다. 한글은 build.gradle 에서 `-Dstdout.encoding=UTF-8` 로 맞춰 둠).

## 2편: 쓰임의 판단 (`src/main/java/demo/Part2.java`)
같은 일을 for 문과 스트림으로 써서 결과가 같은지, 그리고 for 문이 더 자연스러운 경우(쌓아 가는 값을 보고 멈출 때, 바로 앞 원소가 필요할 때, 바깥 변수를 바꿀 때, checked 예외를 던질 때)를 확인한다. 콘솔의 `[2편 n]` 줄을 본다.
`compile-fail/`의 두 파일은 일부러 컴파일 오류를 내는 코드다(빌드에 들어가지 않음). `javac compile-fail/CaptureFail.java`처럼 돌려 오류 메시지를 본다.
