package nu.mine.mosher.gedcom;

import org.apache.ibatis.type.*;
import org.slf4j.*;

import java.sql.*;
import java.util.UUID;

public class TimestampTypeHandler extends BaseTypeHandler<Timestamp> {
    private static final Logger LOG =  LoggerFactory.getLogger(TimestampTypeHandler.class);

    @Override
    public Timestamp getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return getNullableResult(rs, rs.getMetaData().getColumnName(columnIndex));
    }

    @Override
    public Timestamp getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        final long n = rs.getLong(columnName);

        if (rs.wasNull()) {
            return null;
        }

        return new Timestamp(n);
    }

    @Override
    public Timestamp getNullableResult(final CallableStatement rs, final int columnIndex) throws SQLException {
        final long n = rs.getLong(columnIndex);

        if (rs.wasNull()) {
            return null;
        }

        return new Timestamp(n);
    }

    private static Refn toRefn(String s) {
        try {
            return new Refn(UUID.fromString(s));
        } catch (final IllegalArgumentException e) {
            LOG.warn(e.getMessage());
            return null;
        } catch (final Throwable e) {
            LOG.warn("Unexpected exception thrown while parsing UUID string", e);
            return null;
        }
    }

    @Override
    public void setNonNullParameter(final PreparedStatement ps, final int parameterIndex, final Timestamp parameter, final JdbcType jdbcType) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
