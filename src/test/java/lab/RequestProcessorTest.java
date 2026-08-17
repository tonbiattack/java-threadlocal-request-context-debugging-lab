package lab;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class RequestProcessorTest {
    public static void main(String[] args) throws Exception {
        authenticatedRequestKeepsItsOwnActor();
        anonymousRequestDoesNotInheritPreviousActorsContext();
        System.out.println("PASS: all tests");
    }

    static void authenticatedRequestKeepsItsOwnActor() {
        RequestContext.clear();
        RequestProcessor processor = new RequestProcessor();

        String actual = processor.processAuthenticated("alice");

        assertEquals("actor=alice", actual, "認証済みリクエストは自分のactorを監査出力する");
        RequestContext.clear();
    }

    static void anonymousRequestDoesNotInheritPreviousActorsContext() throws Exception {
        RequestProcessor processor = new RequestProcessor();
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<String> authenticated = executor.submit(() -> processor.processAuthenticated("alice"));
            assertEquals("actor=alice", authenticated.get(), "最初のリクエストはaliceとして処理される");

            Future<String> anonymous = executor.submit(processor::processAnonymous);
            String actual = anonymous.get();

            assertEquals("actor=anonymous", actual,
                    "同じワーカースレッド上の匿名リクエストは前リクエストのactorを引き継がない");
        } finally {
            RequestContext.clear();
        }
    }

    private static void assertEquals(String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
        }
    }
}
