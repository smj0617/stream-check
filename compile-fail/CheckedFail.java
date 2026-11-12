import java.io.IOException;
import java.util.List;

// 일부러 컴파일 오류를 내는 코드(빌드에 들어가지 않음). javac 로 오류 메시지를 확인한다.
public class CheckedFail {
    static String load(String name) throws IOException {
        return name + ".txt";
    }

    public static void main(String[] args) {
        List<String> names = List.of("coffee", "book");
        List<String> files = names.stream().map(n -> load(n)).toList(); // checked 예외를 람다에서 던짐
        System.out.println(files);
    }
}
