package lab;

public final class RequestContext {
    private static final ThreadLocal<String> ACTOR = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void setActor(String actor) {
        ACTOR.set(actor);
    }

    public static String actorOrAnonymous() {
        String actor = ACTOR.get();
        return actor == null ? "anonymous" : actor;
    }

    public static void clear() {
        ACTOR.remove();
    }
}
