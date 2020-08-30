package nu.mine.mosher.gedcom;

import org.apache.ibatis.type.*;

import java.sql.*;
import java.util.UUID;

public class FtmGuidTypeHandler extends BaseTypeHandler<UUID> {
    @Override
    public UUID getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return getNullableResult(rs, rs.getMetaData().getColumnName(columnIndex));
    }

    @Override
    public UUID getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        final byte[] rb = rs.getBytes(columnName);

        if (rs.wasNull()) {
            return null;
        }

        return DatabaseUtil.uuidOf(DatabaseUtil.permute(rb));
    }

    @Override
    public UUID getNullableResult(final CallableStatement rs, final int columnIndex) throws SQLException {
        final byte[] rb = rs.getBytes(columnIndex);

        if (rs.wasNull()) {
            return null;
        }

        return DatabaseUtil.uuidOf(DatabaseUtil.permute(rb));
    }

    @Override
    public void setNonNullParameter(final PreparedStatement ps, final int parameterIndex, final UUID parameter, final JdbcType jdbcType) throws SQLException {
        final byte[] rb = DatabaseUtil.permute(DatabaseUtil.bytesOf(parameter));
        ps.setBytes(parameterIndex, rb);
    }
}
