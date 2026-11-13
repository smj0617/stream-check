package demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 자바 Stream 쇼츠 3편(stream-3-shorts, 함정) 사실 확인. 콘솔의 [3편 n] 줄을 본다.
 */
public class Part3 {

    static final List<Integer> NUMS = List.of(7, 3, 8, 1, 6, 2, 9, 4, 5, 10);

    public static void main(String[] args) throws Exception {
        System.out.println("Java " + System.getProperty("java.version"));
        fact1();
        fact2();
        fact3();
        fact4();
        fact5();
        fact6();
        System.exit(0); // 4번에서 멈추지 않는 스트림 스레드가 남아 있으므로 끝낸다
    }

    // 1. 최종 연산을 한 스트림은 다시 못 쓴다. 원본이나 Supplier 로 새로 만든다
    static void fact1() {
        System.out.println();
        System.out.println("[3편 1] 최종 연산을 한 스트림을 다시 쓰기");
        Stream<Integer> s = NUMS.stream().filter(n -> n % 2 == 0);
        System.out.println("[3편 1]   첫 count = " + s.count());
        try {
            s.count();
        } catch (IllegalStateException e) {
            System.out.println("[3편 1]   두 번째 count -> " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        System.out.println("[3편 1]   원본에서 다시: count=" + NUMS.stream().filter(n -> n % 2 == 0).count()
                + ", sum=" + NUMS.stream().filter(n -> n % 2 == 0).mapToInt(n -> n).sum());
        Supplier<Stream<Integer>> evens = () -> NUMS.stream().filter(n -> n % 2 == 0);
        System.out.println("[3편 1]   Supplier 로 다시: count=" + evens.get().count()
                + ", max=" + evens.get().max(Integer::compare).orElseThrow());
    }

    // 2. findFirst 는 필요한 만큼만 보지만, sorted 를 끼우면 전부 본다
    static void fact2() {
        System.out.println();
        System.out.println("[3편 2] filter -> findFirst 에 sorted 를 끼우면");
        AtomicInteger seenA = new AtomicInteger();
        var a = NUMS.stream().peek(n -> seenA.incrementAndGet())
                .filter(n -> n > 5).findFirst();
        System.out.println("[3편 2]   sorted 없음: 결과=" + a.orElseThrow() + ", 본 원소 수=" + seenA.get()
                + " / " + NUMS.size());

        AtomicInteger seenB = new AtomicInteger();
        var b = NUMS.stream().peek(n -> seenB.incrementAndGet())
                .sorted().filter(n -> n > 5).findFirst();
        System.out.println("[3편 2]   sorted 끼움: 결과=" + b.orElseThrow() + ", 본 원소 수=" + seenB.get()
                + " / " + NUMS.size());

        // 큰 유한 스트림(섞인 100만 개)
        List<Integer> big = new ArrayList<>(IntStream.range(0, 1_000_000).boxed().toList());
        Collections.shuffle(big, new java.util.Random(42));
        AtomicInteger seenC = new AtomicInteger();
        big.stream().peek(n -> seenC.incrementAndGet()).filter(n -> n > 500_000).findFirst();
        AtomicInteger seenD = new AtomicInteger();
        big.stream().peek(n -> seenD.incrementAndGet()).sorted().filter(n -> n > 500_000).findFirst();
        System.out.println("[3편 2]   100만 개: sorted 없음 본 수=" + seenC.get() + ", sorted 끼움 본 수=" + seenD.get());

        // limit 도 sorted 뒤에 오면 전부 본다
        AtomicInteger seenE = new AtomicInteger();
        big.stream().peek(n -> seenE.incrementAndGet()).limit(3).toList();
        AtomicInteger seenF = new AtomicInteger();
        big.stream().peek(n -> seenF.incrementAndGet()).sorted().limit(3).toList();
        System.out.println("[3편 2]   100만 개 limit(3): sorted 없음 본 수=" + seenE.get()
                + ", sorted 끼움 본 수=" + seenF.get());
    }

    // 3. distinct 도 본 것을 기억해 두는 상태 있는 연산
    static void fact3() {
        System.out.println();
        System.out.println("[3편 3] distinct 는 본 값을 기억한다(그래도 sorted 와 달리 하나씩 흘려보냄)");
        List<Integer> dup = List.of(1, 2, 1, 3, 2, 1, 4);
        List<String> log = new ArrayList<>();
        var out = dup.stream().peek(n -> log.add("in" + n)).distinct()
                .peek(n -> log.add("OUT" + n)).toList();
        System.out.println("[3편 3]   결과=" + out + ", 흐름=" + log);
        AtomicInteger seen = new AtomicInteger();
        var first = Stream.iterate(1, n -> n + 1).peek(n -> seen.incrementAndGet())
                .map(n -> n % 3).distinct().limit(3).toList();
        System.out.println("[3편 3]   끝없는 스트림 + distinct.limit(3): 결과=" + first + ", 본 수=" + seen.get()
                + " (끝남. 단 limit(4) 면 네 번째 다른 값이 없어 끝나지 않음)");
    }

    // 4. 끝없는 스트림 + sorted / distinct.limit(너무 큼) 는 끝나지 않는다(타임아웃으로 확인)
    static void fact4() throws Exception {
        System.out.println();
        System.out.println("[3편 4] 끝없는 스트림에 sorted 를 끼우면(2초 타임아웃)");
        AtomicInteger seenOk = new AtomicInteger();
        var ok = Stream.iterate(1, n -> n + 1).peek(n -> seenOk.incrementAndGet())
                .filter(n -> n > 5).findFirst();
        System.out.println("[3편 4]   sorted 없음: 결과=" + ok.orElseThrow() + ", 본 수=" + seenOk.get());

        AtomicInteger seen = new AtomicInteger();
        CompletableFuture<Integer> f = CompletableFuture.supplyAsync(() ->
                Stream.iterate(1, n -> n + 1).peek(n -> seen.incrementAndGet())
                        .sorted().filter(n -> n > 5).findFirst().orElseThrow());
        try {
            System.out.println("[3편 4]   sorted 끼움: 결과=" + f.get(2, TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            System.out.println("[3편 4]   sorted 끼움: 2초 안에 안 끝남(TimeoutException), 그동안 받아 쌓은 수=" + seen.get());
        } catch (Exception e) {
            System.out.println("[3편 4]   sorted 끼움: " + e.getCause());
        }
    }

    // 5. parallel + forEach 는 순서가 보장되지 않는다. forEachOrdered 는 보장
    static void fact5() {
        System.out.println();
        System.out.println("[3편 5] parallel 에서 forEach 순서");
        List<Integer> src = IntStream.rangeClosed(1, 12).boxed().toList();
        List<Integer> a = Collections.synchronizedList(new ArrayList<>());
        src.parallelStream().forEach(a::add);
        List<Integer> b = Collections.synchronizedList(new ArrayList<>());
        src.parallelStream().forEachOrdered(b::add);
        System.out.println("[3편 5]   forEach        = " + a);
        System.out.println("[3편 5]   forEachOrdered = " + b);
        System.out.println("[3편 5]   parallel toList = " + src.parallelStream().map(n -> n).toList()
                + " (모으기는 원래 순서 유지)");
    }

    // 6. 중간 연산만 있고 최종 연산이 없으면 아무 일도 안 일어난다(peek 로그가 안 찍힘)
    static void fact6() {
        System.out.println();
        System.out.println("[3편 6] 최종 연산을 빠뜨리면");
        AtomicInteger calls = new AtomicInteger();
        NUMS.stream().peek(n -> calls.incrementAndGet()).map(n -> n * 2);
        System.out.println("[3편 6]   최종 연산 없음: peek 실행 수=" + calls.get());
        // 참고: count 는 크기를 알 수 있으면 peek/map 을 건너뛸 수 있다(Java 9+)
        AtomicInteger calls2 = new AtomicInteger();
        long c = NUMS.stream().peek(n -> calls2.incrementAndGet()).map(n -> n * 2).count();
        System.out.println("[3편 6]   참고 count()=" + c + ", peek 실행 수=" + calls2.get()
                + " (크기를 이미 알아 건너뜀)");
    }
}
