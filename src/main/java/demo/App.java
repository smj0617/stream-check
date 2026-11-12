package demo;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** 자바 Stream 쇼츠(stream-1-shorts) 대본 사실 확인. 콘솔의 [사실 n] 줄을 본다. */
public class App {

    public static void main(String[] args) {
        System.out.println("Java " + System.getProperty("java.version"));
        fact1();
        fact2();
        fact3();
        fact4();
        fact5();
        fact6();
        fact7();
    }

    static final List<Integer> NUMS = List.of(1, 2, 3, 4, 5, 6);

    // 1. 최종 연산 전에는 중간 연산이 실행되지 않는다
    static void fact1() {
        System.out.println();
        System.out.println("[사실 1] filter, map 만 붙이고 최종 연산은 아직 안 부름");
        AtomicInteger calls = new AtomicInteger();
        Stream<Integer> s = NUMS.stream()
                .filter(n -> { calls.incrementAndGet(); System.out.println("[사실 1]   filter " + n); return n % 2 == 0; })
                .map(n -> { calls.incrementAndGet(); System.out.println("[사실 1]   map " + n); return n * 10; });
        System.out.println("[사실 1]   스트림 만든 뒤 filter/map 실행 횟수 = " + calls.get());
        System.out.println("[사실 1] 이제 collect(최종 연산) 부름");
        List<Integer> out = s.collect(Collectors.toList());
        System.out.println("[사실 1]   collect 뒤 실행 횟수 = " + calls.get() + ", 결과 = " + out);
    }

    // 2. 세로 처리 + sorted 예외
    static void fact2() {
        System.out.println();
        System.out.println("[사실 2] 원소 1,2,3,4 를 filter(짝수) -> map(x10) -> forEach");
        List.of(1, 2, 3, 4).stream()
                .filter(n -> { System.out.println("[사실 2]   filter " + n); return n % 2 == 0; })
                .map(n -> { System.out.println("[사실 2]   map " + n); return n * 10; })
                .forEach(n -> System.out.println("[사실 2]   forEach " + n));
        System.out.println("[사실 2] 원소 3,1,4,2 를 peek -> sorted (상태 연산) -> map -> forEach");
        List.of(3, 1, 4, 2).stream()
                .peek(n -> System.out.println("[사실 2]   peek(정렬 전) " + n))
                .sorted()
                .map(n -> { System.out.println("[사실 2]   map " + n); return n * 10; })
                .forEach(n -> System.out.println("[사실 2]   forEach " + n));
    }

    // 3. 단락: 100만 개 중 몇 개만 지나가는가
    static void fact3() {
        System.out.println();
        System.out.println("[사실 3] 원소 1,000,000 개 (0..999999)");
        AtomicInteger c = new AtomicInteger();
        var first = IntStream.range(0, 1_000_000).boxed()
                .peek(n -> c.incrementAndGet())
                .filter(n -> n % 7 == 3)
                .findFirst();
        System.out.println("[사실 3]   findFirst(7로 나눠 3 남는 첫 수) = " + first.get() + ", 지나간 원소 = " + c.get());

        c.set(0);
        List<Integer> five = IntStream.range(0, 1_000_000).boxed()
                .peek(n -> c.incrementAndGet())
                .filter(n -> n % 2 == 0)
                .limit(5)
                .toList();
        System.out.println("[사실 3]   filter(짝수).limit(5) = " + five + ", 지나간 원소 = " + c.get());

        c.set(0);
        boolean any = IntStream.range(0, 1_000_000).boxed()
                .peek(n -> c.incrementAndGet())
                .anyMatch(n -> n > 41);
        System.out.println("[사실 3]   anyMatch(n > 41) = " + any + ", 지나간 원소 = " + c.get());

        c.set(0);
        boolean none = IntStream.range(0, 1_000_000).boxed()
                .peek(n -> c.incrementAndGet())
                .anyMatch(n -> n < 0);
        System.out.println("[사실 3]   (예외) anyMatch(n < 0), 맞는 게 없음 = " + none + ", 지나간 원소 = " + c.get());

        c.set(0);
        var firstSorted = IntStream.range(0, 1_000_000).boxed()
                .peek(n -> c.incrementAndGet())
                .sorted()
                .findFirst();
        System.out.println("[사실 3]   (덤) 이미 정렬된 원본(range)에 sorted().findFirst() = " + firstSorted.get() + ", 지나간 원소 = " + c.get() + " (정렬 표시가 있어 sorted 를 건너뜀)");

        c.set(0);
        var firstShuffled = IntStream.range(0, 1_000_000).map(n -> (int) ((n * 7919L) % 1_000_000)).boxed()
                .peek(n -> c.incrementAndGet())
                .sorted()
                .findFirst();
        System.out.println("[사실 3]   (예외) 섞인 원본에 sorted().findFirst() = " + firstShuffled.get() + ", 지나간 원소 = " + c.get());
    }

    // 4. 재사용 불가
    static void fact4() {
        System.out.println();
        System.out.println("[사실 4] 같은 스트림에 최종 연산을 두 번");
        Stream<Integer> s = NUMS.stream().map(n -> n * 10);
        System.out.println("[사실 4]   1번째 count = " + s.count());
        try {
            System.out.println("[사실 4]   2번째 count = " + s.count());
        } catch (IllegalStateException e) {
            System.out.println("[사실 4]   2번째 -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        Stream<Integer> t = NUMS.stream();
        t.filter(n -> n > 1);
        try {
            t.map(n -> n * 2);
        } catch (IllegalStateException e) {
            System.out.println("[사실 4]   (덤) 중간 연산을 붙인 원래 스트림에 또 붙여도 -> " + e.getClass().getSimpleName());
        }
    }

    // 5. 원본 불변
    static void fact5() {
        System.out.println();
        List<Integer> src = new ArrayList<>(List.of(5, 3, 1, 4, 2));
        System.out.println("[사실 5] 원본 = " + src);
        List<Integer> out = src.stream().filter(n -> n > 2).map(n -> n * 10).sorted().toList();
        System.out.println("[사실 5]   filter/map/sorted 결과 = " + out);
        System.out.println("[사실 5]   끝난 뒤 원본 = " + src);
        List<StringBuilder> boxes = List.of(new StringBuilder("a"), new StringBuilder("b"));
        boxes.stream().forEach(sb -> sb.append("!"));
        System.out.println("[사실 5]   (예외) 원소 객체 자체를 고치면 원본 원소가 바뀜 = " + boxes);
    }

    // 6. 무한 스트림
    static void fact6() {
        System.out.println();
        AtomicInteger c = new AtomicInteger();
        List<Integer> pow = Stream.iterate(1, n -> n * 2)
                .peek(n -> c.incrementAndGet())
                .limit(10).toList();
        System.out.println("[사실 6] Stream.iterate(1, x2).limit(10) = " + pow + ", 만든 원소 = " + c.get());
        c.set(0);
        var big = Stream.iterate(1, n -> n + 1)
                .peek(n -> c.incrementAndGet())
                .filter(n -> n * n > 1000).findFirst();
        System.out.println("[사실 6] Stream.iterate(1, +1) 에서 제곱 > 1000 인 첫 수 = " + big.get() + ", 만든 원소 = " + c.get());
        c.set(0);
        List<String> g = Stream.generate(() -> "hi").peek(x -> c.incrementAndGet()).limit(3).toList();
        System.out.println("[사실 6] Stream.generate(\"hi\").limit(3) = " + g + ", 만든 원소 = " + c.get());
        System.out.println("[사실 6] (예외) limit 앞에 sorted 를 두거나 limit 없이 collect/count 하면 끝나지 않음 (돌리지 않음)");
    }

    // 7. 중간 리스트 개수
    static void fact7() {
        System.out.println();
        List<Integer> src = IntStream.rangeClosed(1, 10).boxed().toList();
        System.out.println("[사실 7] 1..10 에서 짝수만 골라 x10");
        int lists = 0;
        List<Integer> evens = new ArrayList<>(); lists++;
        for (int n : src) if (n % 2 == 0) evens.add(n);
        List<Integer> result = new ArrayList<>(); lists++;
        for (int n : evens) result.add(n * 10);
        System.out.println("[사실 7]   for 문 두 번: 결과 = " + result + ", 새로 만든 리스트 = " + lists + "개 (중간 evens " + evens.size() + "칸 + 결과)");
        List<Integer> viaStream = src.stream().filter(n -> n % 2 == 0).map(n -> n * 10).toList();
        System.out.println("[사실 7]   스트림: 결과 = " + viaStream + ", 새로 만든 리스트 = 1개 (결과만, filter 와 map 사이 목록 없음)");
        System.out.println("[사실 7]   (같은 일을 for 문 하나에 if 로 넣어도 리스트 1개: 차이는 '스트림이라서'가 아니라 '한 번에 흘려서')");
    }
}
