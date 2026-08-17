package lab;

public final class RequestProcessor {
    public String processAuthenticated(String actor) {
        RequestContext.setActor(actor);
        return "actor=" + RequestContext.actorOrAnonymous();
    }

    public String processAnonymous() {
        return "actor=" + RequestContext.actorOrAnonymous();
    }
}
