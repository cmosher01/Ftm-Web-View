package nu.mine.mosher.gedcom;

import java.nio.ByteBuffer;
import java.util.UUID;

public class DatabaseUtil {
    public static UUID uuidOf(final byte[] rb) {
        final ByteBuffer bb = ByteBuffer.wrap(rb);

        final long high = bb.getLong();
        final long low = bb.getLong();

        return new UUID(high, low);
    }

    public static byte[] bytesOf(final UUID uuid) {
        final ByteBuffer bb = ByteBuffer.wrap(new byte[16]);

        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());

        return bb.array();
    }

    public static byte[] permute(Object s) {
        return permute((byte[])s);
    }

    public static byte[] permute(byte[] s) {
        final byte[] d = new byte[16];

        d[0x0] = s[0x3];
        d[0x1] = s[0x2];
        d[0x2] = s[0x1];
        d[0x3] = s[0x0];

        d[0x4] = s[0x5];
        d[0x5] = s[0x4];

        d[0x6] = s[0x7];
        d[0x7] = s[0x6];

        d[0x8] = s[0x8];
        d[0x9] = s[0x9];

        d[0xa] = s[0xa];
        d[0xb] = s[0xb];
        d[0xc] = s[0xc];
        d[0xd] = s[0xd];
        d[0xe] = s[0xe];
        d[0xf] = s[0xf];

        return d;
    }
}
