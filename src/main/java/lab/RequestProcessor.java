package lab;

public final class RequestProcessor {
    public String processAuthenticated(String actor) {
        RequestContext.setActor(actor);
        try {
            return "actor=" + RequestContext.actorOrAnonymous();
        } finally {
            RequestContext.clear();
        }
    }

    public String processAnonymous() {
        return "actor=" + RequestContext.actorOrAnonymous();
    }
}
