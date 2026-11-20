package foundations.e004_http_status_codes;

import static foundations.e004_http_status_codes.StatusCodes.NextStep.DONE;
import static foundations.e004_http_status_codes.StatusCodes.NextStep.FIX_REQUEST;
import static foundations.e004_http_status_codes.StatusCodes.NextStep.FOLLOW_REDIRECT;
import static foundations.e004_http_status_codes.StatusCodes.NextStep.GIVE_UP;
import static foundations.e004_http_status_codes.StatusCodes.NextStep.RETRY_WITH_BACKOFF;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import foundations.e003_http_methods.Method;
import foundations.e004_http_status_codes.StatusCodes.Kind;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StatusCodesTest {

    private static final Set<Integer> KNOWN = Set.of(200, 201, 204, 400, 404, 500, 503);

    @Test
    void theFirstDigitIsTheClass() {
        assertEquals(Kind.INFORMATIONAL, StatusCodes.kindOf(103));
        assertEquals(Kind.SUCCESS, StatusCodes.kindOf(201));
        assertEquals(Kind.REDIRECTION, StatusCodes.kindOf(308));
        assertEquals(Kind.CLIENT_ERROR, StatusCodes.kindOf(422));
        assertEquals(Kind.SERVER_ERROR, StatusCodes.kindOf(599));
    }

    @Test
    void codesOutsideTheRangeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> StatusCodes.kindOf(99));
        assertThrows(IllegalArgumentException.class, () -> StatusCodes.kindOf(600));
    }

    @Test
    void anUnrecognizedCodeIsTreatedAsTheX00OfItsClass() {
        assertEquals(404, StatusCodes.understoodAs(404, KNOWN));
        assertEquals(400, StatusCodes.understoodAs(461, KNOWN), "an unknown 4xx is still a client error");
        assertEquals(500, StatusCodes.understoodAs(599, KNOWN));
        assertEquals(200, StatusCodes.understoodAs(299, KNOWN));
    }

    @Test
    void successAndRedirection() {
        assertEquals(DONE, StatusCodes.nextStep(Method.POST, 201));
        assertEquals(FOLLOW_REDIRECT, StatusCodes.nextStep(Method.POST, 303));
        assertEquals(DONE, StatusCodes.nextStep(Method.GET, 304), "304 means: reuse the cached copy");
    }

    @Test
    void clientErrorsNeedADifferentRequest() {
        assertEquals(FIX_REQUEST, StatusCodes.nextStep(Method.GET, 400));
        assertEquals(FIX_REQUEST, StatusCodes.nextStep(Method.PUT, 409));
        assertEquals(FIX_REQUEST, StatusCodes.nextStep(Method.POST, 422));
    }

    @Test
    void transientFailuresAreRetriedOnlyForIdempotentMethods() {
        assertEquals(RETRY_WITH_BACKOFF, StatusCodes.nextStep(Method.GET, 503));
        assertEquals(RETRY_WITH_BACKOFF, StatusCodes.nextStep(Method.PUT, 429));
        assertEquals(RETRY_WITH_BACKOFF, StatusCodes.nextStep(Method.DELETE, 504));
        // A 504 says the gateway stopped waiting, not that the origin didn't act. A repeat could duplicate it.
        assertEquals(GIVE_UP, StatusCodes.nextStep(Method.POST, 504));
    }

    @Test
    void aPlain500IsNotRetried() {
        // Usually a bug: the same request hits the same code path and fails the same way.
        assertEquals(GIVE_UP, StatusCodes.nextStep(Method.GET, 500));
    }

    @Test
    void interimResponsesAreNotAFinalAnswer() {
        assertThrows(IllegalArgumentException.class, () -> StatusCodes.nextStep(Method.GET, 100));
    }
}
