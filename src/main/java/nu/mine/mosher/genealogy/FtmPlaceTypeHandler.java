package nu.mine.mosher.genealogy;


import org.apache.ibatis.type.*;

import java.sql.*;



public class FtmPlaceTypeHandler extends BaseTypeHandler<FtmPlace> {
    @Override
    public FtmPlace getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return getNullableResult(rs, rs.getMetaData().getColumnName(columnIndex));
    }

    @Override
    public FtmPlace getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        final String s = rs.getString(columnName);

        if (rs.wasNull()) {
            return FtmPlace.empty();
        }

        return FtmPlace.fromFtmPlace(s);
    }

    @Override
    public FtmPlace getNullableResult(final CallableStatement rs, final int columnIndex) throws SQLException {
        final String s = rs.getString(columnIndex);

        if (rs.wasNull()) {
            return FtmPlace.empty();
        }

        return FtmPlace.fromFtmPlace(s);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, FtmPlace parameter, JdbcType jdbcType) throws SQLException {
        throw new UnsupportedOperationException();
    }
}
