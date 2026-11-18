package foundations.e002_http_fundamentals;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FrameHeaderTest {

    @Test
    void parsesAnEmptySettingsFrameOnStreamZero() {
        // Each side's connection preface includes a SETTINGS frame (RFC 9113 §3.4). One that keeps
        // every default is empty: length 0, type 4, flags 0, and stream 0 because it applies to the connection.
        byte[] wire = {0, 0, 0, 0x04, 0x00, 0, 0, 0, 0};

        FrameHeader h = FrameHeader.parse(ByteBuffer.wrap(wire));

        assertEquals(new FrameHeader(0, FrameHeader.SETTINGS, 0, 0), h);
        assertFalse(h.isClientInitiatedStream());
    }

    @Test
    void parsesHeadersFrameWithFlagsAndThreeByteLength() {
        // Length 0x01_02_03 = 66,051 octets: needs all three length bytes, and the big-endian order.
        byte[] wire = {0x01, 0x02, 0x03, 0x01, 0x05, 0, 0, 0, 0x03};

        FrameHeader h = FrameHeader.parse(ByteBuffer.wrap(wire));

        assertEquals(66_051, h.length());
        assertEquals(FrameHeader.HEADERS, h.type());
        assertTrue(h.has(FrameHeader.END_STREAM));
        assertTrue(h.has(FrameHeader.END_HEADERS));
        assertEquals(3, h.streamId());
        assertTrue(h.isClientInitiatedStream());
    }

    @Test
    void ignoresTheReservedBit() {
        byte[] wire = {0, 0, 0x10, 0x00, 0x00, (byte) 0x80, 0, 0, 0x01};

        FrameHeader h = FrameHeader.parse(ByteBuffer.wrap(wire));

        assertEquals(1, h.streamId(), "reserved high bit must not leak into the stream id");
        assertEquals(16, h.length());
    }

    @Test
    void encodeIsTheInverseOfParse() {
        FrameHeader original = new FrameHeader(16_384, FrameHeader.DATA, FrameHeader.END_STREAM, 0x7FFF_FFFF);

        byte[] wire = original.encode();

        assertEquals(FrameHeader.SIZE, wire.length);
        assertEquals(original, FrameHeader.parse(ByteBuffer.wrap(wire)));
    }

    @Test
    void rejectsShortInputAndOutOfRangeFields() {
        assertThrows(IllegalArgumentException.class, () -> FrameHeader.parse(ByteBuffer.wrap(new byte[8])));
        assertThrows(IllegalArgumentException.class, () -> new FrameHeader(1 << 24, 0, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new FrameHeader(0, 0, 0, -1));
    }

    @Test
    void prefaceIsTwentyFourOctetsAndLooksLikeAnHttp1RequestLine() {
        assertEquals(24, FrameHeader.CLIENT_PREFACE.length);
        assertArrayEquals("PRI ".getBytes(StandardCharsets.US_ASCII), Arrays.copyOf(FrameHeader.CLIENT_PREFACE, 4));
    }
}
