package demo;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 자바 Stream 쇼츠 2편(stream-2-shorts, 쓰임의 판단) 사실 확인. 콘솔의 [2편 n] 줄을 본다.
 * 컴파일 오류 확인용 코드는 compile-fail/ 폴더(빌드에 들어가지 않음)에 있다.
 */
public class Part2 {

    record Order(String name, int price) {}

    static final List<Order> ORDERS = List.of(
            new Order("coffee", 4_500),
            new Order("keyboard", 59_000),
            new Order("book", 15_000),
            new Order("pen", 1_200),
            new Order("headset", 32_000),
            new Order("mug", 10_000)); // 1만 원 '넘는' 것만이므로 mug(정확히 1만)는 빠져야 한다

    public static void main(String[] args) throws Exception {
        System.out.println("Java " + System.getProperty("java.version"));
        fact1();
        fact2();
        fact3();
        fact4();
        fact5();
    }

    // 1. 같은 일을 for 문과 스트림으로: 결과가 같은가
    static void fact1() {
        System.out.println();
        System.out.println("[2편 1] 1만 원 넘는 주문의 이름만 대문자로 모으기");
        List<String> byFor = new ArrayList<>();
        for (int i = 0; i < ORDERS.size(); i++) {
            Order o = ORDERS.get(i);
            if (o.price() > 10_000) {
                byFor.add(o.name().toUpperCase());
            }
        }
        List<String> byStream = ORDERS.stream()
                .filter(o -> o.price() > 10_000)
                .map(o -> o.name().toUpperCase())
                .toList();
        System.out.println("[2편 1]   for    = " + byFor);
        System.out.println("[2편 1]   stream = " + byStream);
        System.out.println("[2편 1]   같은가 = " + byFor.equals(byStream));
    }

    // 2. 바깥 지역 변수 바꾸기: 람다 안에서는 컴파일 오류(compile-fail/CaptureFail.java), 우회는 어색하다
    static void fact2() {
        System.out.println();
        System.out.println("[2편 2] 도는 중에 바깥 변수(합계, 개수)를 바꾸기");
        int total = 0;
        int count = 0;
        for (Order o : ORDERS) {
            if (o.price() > 10_000) {
                total += o.price();
                count++;
            }
        }
        System.out.println("[2편 2]   for: total=" + total + ", count=" + count);

        // 우회 1: 배열/Atomic 으로 감싸기(되긴 하지만 어색하고 병렬이면 위험)
        int[] t = {0};
        AtomicInteger c = new AtomicInteger();
        ORDERS.stream().filter(o -> o.price() > 10_000).forEach(o -> { t[0] += o.price(); c.incrementAndGet(); });
        System.out.println("[2편 2]   stream + int[]/AtomicInteger 우회: total=" + t[0] + ", count=" + c.get());

        // 우회 2: 스트림다운 방법 = 바깥 변수를 바꾸지 말고 결과를 '모으기'
        var stats = ORDERS.stream().filter(o -> o.price() > 10_000)
                .collect(Collectors.summarizingInt(Order::price));
        System.out.println("[2편 2]   stream summarizingInt: total=" + stats.getSum() + ", count=" + stats.getCount());
    }

    static String load(String name) throws IOException {
        if (name.equals("pen")) throw new IOException("pen 읽기 실패");
        return name + ".txt";
    }

    // 3. checked 예외를 던지는 일
    static void fact3() {
        System.out.println();
        System.out.println("[2편 3] checked 예외(IOException)를 던지는 메서드 부르기");
        try {
            for (Order o : ORDERS) {
                load(o.name());
            }
        } catch (IOException e) {
            System.out.println("[2편 3]   for: 그냥 try/catch 로 받음 -> " + e.getMessage());
        }
        // ORDERS.stream().map(o -> load(o.name())) 는 컴파일 오류(compile-fail/CheckedFail.java)
        try {
            ORDERS.stream().map(o -> {
                try {
                    return load(o.name());
                } catch (IOException e) {
                    throw new UncheckedIOException(e); // 람다 안에서 감싸 다시 던져야 한다
                }
            }).toList();
        } catch (UncheckedIOException e) {
            System.out.println("[2편 3]   stream: 람다 안 try/catch + UncheckedIOException 으로 감싸야 함 -> "
                    + e.getCause().getMessage());
        }
    }

    // 4. 인덱스(이전 원소)가 필요한 일: 가격이 직전 주문보다 오른 곳
    static void fact4() {
        System.out.println();
        System.out.println("[2편 4] 직전 주문보다 비싸진 주문 찾기(이전 원소 필요)");
        List<String> byFor = new ArrayList<>();
        for (int i = 1; i < ORDERS.size(); i++) {
            if (ORDERS.get(i).price() > ORDERS.get(i - 1).price()) {
                byFor.add(ORDERS.get(i - 1).name() + "->" + ORDERS.get(i).name());
            }
        }
        // 스트림에는 '이전 원소'가 없어 결국 인덱스 스트림으로 리스트를 다시 get 한다(for 문을 돌려 쓴 꼴)
        List<String> byStream = IntStream.range(1, ORDERS.size())
                .filter(i -> ORDERS.get(i).price() > ORDERS.get(i - 1).price())
                .mapToObj(i -> ORDERS.get(i - 1).name() + "->" + ORDERS.get(i).name())
                .toList();
        System.out.println("[2편 4]   for    = " + byFor);
        System.out.println("[2편 4]   stream(IntStream.range + get) = " + byStream);
        System.out.println("[2편 4]   같은가 = " + byFor.equals(byStream));
    }

    // 5. 중간에 break/return 으로 빠지기
    static void fact5() {
        System.out.println();
        System.out.println("[2편 5] 합계가 5만 원을 넘는 순간 멈추기(break)");
        int sum = 0;
        int visited = 0;
        for (Order o : ORDERS) {
            visited++;
            sum += o.price();
            if (sum > 50_000) break;
        }
        System.out.println("[2편 5]   for+break: 멈춘 합계=" + sum + ", 본 주문 수=" + visited);

        // forEach 안의 return 은 '이번 원소만 건너뛰기'(continue)일 뿐 멈추지 않는다
        AtomicInteger seen = new AtomicInteger();
        int[] s = {0};
        ORDERS.stream().forEach(o -> {
            seen.incrementAndGet();
            if (s[0] > 50_000) return; // break 처럼 보이지만 아니다
            s[0] += o.price();
        });
        System.out.println("[2편 5]   stream forEach+return: 합계=" + s[0] + ", 람다 실행 수=" + seen.get()
                + " (끝까지 다 돎)");

        // 누적 상태로 멈추려면 takeWhile + 바깥 상태 같은 어색한 조합이 된다
        int[] acc = {0};
        AtomicInteger seen2 = new AtomicInteger();
        int sum2 = ORDERS.stream()
                .peek(o -> seen2.incrementAndGet())
                .takeWhile(o -> { boolean keep = acc[0] <= 50_000; acc[0] += o.price(); return keep; })
                .mapToInt(Order::price).sum();
        System.out.println("[2편 5]   stream takeWhile+바깥 상태: 합계=" + sum2 + ", 본 주문 수=" + seen2.get()
                + " (takeWhile 은 '넘은 다음 원소'를 봐야 멈춤)");

        // 조건 하나로 찾고 멈추는 건 스트림도 깔끔하다(findFirst 는 단락 연산)
        AtomicInteger seen3 = new AtomicInteger();
        var first = ORDERS.stream().peek(o -> seen3.incrementAndGet())
                .filter(o -> o.price() > 30_000).findFirst();
        System.out.println("[2편 5]   참고 findFirst(3만 원 초과): " + first.map(Order::name).orElse("-")
                + ", 본 주문 수=" + seen3.get());
    }
}
