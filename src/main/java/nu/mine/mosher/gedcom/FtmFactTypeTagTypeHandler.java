package nu.mine.mosher.gedcom;


import org.apache.ibatis.type.*;

import java.sql.*;


public class FtmFactTypeTagTypeHandler extends BaseTypeHandler<FtmFactTypeTag> {
    @Override
    public FtmFactTypeTag getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return getNullableResult(rs, rs.getMetaData().getColumnName(columnIndex));
    }

    @Override
    public FtmFactTypeTag getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        final String s = rs.getString(columnName);

        if (rs.wasNull()) {
            return null;
        }

        try {
            return FtmFactTypeTag.valueOf(s);
        } catch (final Throwable e) {
            return null;
        }
    }

    @Override
    public FtmFactTypeTag getNullableResult(final CallableStatement rs, final int columnIndex) throws SQLException {
        final String s = rs.getString(columnIndex);

        if (rs.wasNull()) {
            return null;
        }

        try {
            return FtmFactTypeTag.valueOf(s);
        } catch (final Throwable e) {
            return null;
        }
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, FtmFactTypeTag parameter, JdbcType jdbcType) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }
}
