package nu.mine.mosher.genealogy;

import org.apache.ibatis.type.*;
import org.slf4j.*;

import java.sql.*;
import java.util.UUID;

public class RefnTypeHandler extends BaseTypeHandler<Refn> {
    private static final Logger LOG =  LoggerFactory.getLogger(RefnTypeHandler.class);

    @Override
    public Refn getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return getNullableResult(rs, rs.getMetaData().getColumnName(columnIndex));
    }

    @Override
    public Refn getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        final String s = rs.getString(columnName);

        if (rs.wasNull()) {
            return null;
        }

        return toRefn(s);
    }

    @Override
    public Refn getNullableResult(final CallableStatement rs, final int columnIndex) throws SQLException {
        final String s = rs.getString(columnIndex);

        if (rs.wasNull()) {
            return null;
        }

        return toRefn(s);
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
    public void setNonNullParameter(final PreparedStatement ps, final int parameterIndex, final Refn parameter, final JdbcType jdbcType) throws SQLException {
        ps.setString(parameterIndex, parameter.getId());
    }
}
