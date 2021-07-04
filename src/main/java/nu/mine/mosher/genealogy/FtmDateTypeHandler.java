package nu.mine.mosher.genealogy;


import org.apache.ibatis.type.*;

import java.sql.*;



public class FtmDateTypeHandler extends BaseTypeHandler<Day> {
    @Override
    public Day getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return getNullableResult(rs, rs.getMetaData().getColumnName(columnIndex));
    }

    @Override
    public Day getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        final String s = rs.getString(columnName);

        if (rs.wasNull()) {
            return Day.UNKNOWN;
        }

        return Day.fromFtmFactDate(s);
    }

    @Override
    public Day getNullableResult(final CallableStatement rs, final int columnIndex) throws SQLException {
        final String s = rs.getString(columnIndex);

        if (rs.wasNull()) {
            return Day.UNKNOWN;
        }

        return Day.fromFtmFactDate(s);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Day parameter, JdbcType jdbcType) {
        throw new UnsupportedOperationException();
    }
}
