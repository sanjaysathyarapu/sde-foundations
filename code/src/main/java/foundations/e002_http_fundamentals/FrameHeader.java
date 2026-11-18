package foundations.e002_http_fundamentals;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * The fixed 9-octet header that starts every HTTP/2 frame (RFC 9113 §4.1).
 *
 * <p>HTTP/1.1 finds message boundaries by scanning text for CRLF. HTTP/2 instead prefixes every
 * frame with its exact length and the stream it belongs to, so a receiver never scans and frames
 * from different requests can be interleaved on one connection.
 */
public record FrameHeader(int length, int type, int flags, int streamId) {

    public static final int SIZE = 9;

    // The client sends these 24 octets first (RFC 9113 §3.4). An HTTP/1.1 server that receives
    // them sees an unknown method "PRI" and fails fast, instead of misreading binary frames.
    public static final byte[] CLIENT_PREFACE =
            "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

    // A few frame types and flags from RFC 9113 §6.
    public static final int DATA = 0x0, HEADERS = 0x1, SETTINGS = 0x4;
    public static final int END_STREAM = 0x1, END_HEADERS = 0x4;

    public FrameHeader {
        // Length is a 24-bit field; stream identifiers are 31 bits (§4.1).
        if (length < 0 || length > 0xFF_FFFF) throw new IllegalArgumentException("length " + length);
        if (streamId < 0) throw new IllegalArgumentException("stream " + streamId);
        if (type < 0 || type > 0xFF || flags < 0 || flags > 0xFF) throw new IllegalArgumentException("type/flags");
    }

    public static FrameHeader parse(ByteBuffer in) {
        if (in.remaining() < SIZE) throw new IllegalArgumentException("need 9 octets");
        // Three unsigned bytes, big-endian ("network byte order", RFC 9113 §1).
        int length = (in.get() & 0xFF) << 16 | (in.get() & 0xFF) << 8 | (in.get() & 0xFF);
        int type = in.get() & 0xFF;
        int flags = in.get() & 0xFF;
        // The top bit is reserved and MUST be ignored on receipt (§4.1), so mask it off.
        int streamId = in.getInt() & 0x7FFF_FFFF;
        return new FrameHeader(length, type, flags, streamId);
    }

    public byte[] encode() {
        return ByteBuffer.allocate(SIZE)
                .put((byte) (length >>> 16)).put((byte) (length >>> 8)).put((byte) length)
                .put((byte) type).put((byte) flags)
                .putInt(streamId) // the reserved bit is always sent as 0
                .array();
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    /** Stream 0 carries connection-level frames such as SETTINGS; odd streams are client requests (§5.1.1). */
    public boolean isClientInitiatedStream() {
        return streamId % 2 == 1;
    }
}
