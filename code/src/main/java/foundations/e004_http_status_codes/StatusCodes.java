package foundations.e004_http_status_codes;

import foundations.e003_http_methods.Method;
import java.util.Set;

/**
 * How a client reads a status code it receives: first the class, then what to do next.
 *
 * <p>The class rules come from RFC 9110 §15. The retry policy in {@link #nextStep} is a common
 * client policy built on those rules, not something the RFC mandates.
 */
public final class StatusCodes {

    public enum Kind { INFORMATIONAL, SUCCESS, REDIRECTION, CLIENT_ERROR, SERVER_ERROR }

    public enum NextStep { DONE, FOLLOW_REDIRECT, FIX_REQUEST, RETRY_WITH_BACKOFF, GIVE_UP }

    // Codes that usually mean "the problem was temporary". Resending the same bytes may succeed.
    private static final Set<Integer> TRANSIENT = Set.of(408, 429, 502, 503, 504);

    private StatusCodes() {}

    /** The first digit is the class. Anything outside 100–599 isn't a valid status code (§15). */
    public static Kind kindOf(int code) {
        if (code < 100 || code > 599) throw new IllegalArgumentException("not a status code: " + code);
        return Kind.values()[code / 100 - 1];
    }

    /**
     * §15: a client must understand the class of every code and treat one it doesn't recognize as
     * the x00 code of that class. That is why inventing a code like 461 degrades to 400, not to chaos.
     */
    public static int understoodAs(int code, Set<Integer> recognized) {
        kindOf(code); // validates the range
        return recognized.contains(code) ? code : code / 100 * 100;
    }

    /** What a client should do after a final (non-1xx) response to a request sent with {@code method}. */
    public static NextStep nextStep(Method method, int code) {
        return switch (kindOf(code)) {
            case INFORMATIONAL -> throw new IllegalArgumentException("1xx is interim; wait for the final response");
            case SUCCESS -> NextStep.DONE;
            case REDIRECTION -> code == 304 ? NextStep.DONE : NextStep.FOLLOW_REDIRECT; // 304: use the cached copy
            // A temporary failure is worth repeating only if repeating can't duplicate the effect (entry 003).
            case CLIENT_ERROR, SERVER_ERROR -> TRANSIENT.contains(code)
                    ? (method.idempotent ? NextStep.RETRY_WITH_BACKOFF : NextStep.GIVE_UP)
                    : (code < 500 ? NextStep.FIX_REQUEST : NextStep.GIVE_UP); // same request, same answer
        };
    }
}
