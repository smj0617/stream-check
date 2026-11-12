import java.util.List;

// 일부러 컴파일 오류를 내는 코드(빌드에 들어가지 않음). javac 로 오류 메시지를 확인한다.
public class CaptureFail {
    public static void main(String[] args) {
        List<Integer> prices = List.of(4_500, 59_000, 15_000);
        int total = 0;
        prices.forEach(p -> total += p); // 람다 안에서 바깥 지역 변수 변경
        System.out.println(total);
    }
}
