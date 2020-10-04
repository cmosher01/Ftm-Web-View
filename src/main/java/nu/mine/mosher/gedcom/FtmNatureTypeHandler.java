package nu.mine.mosher.gedcom;


import org.apache.ibatis.type.*;

import java.sql.*;


public class FtmNatureTypeHandler extends BaseTypeHandler<FtmNature> {
    @Override
    public FtmNature getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return getNullableResult(rs, rs.getMetaData().getColumnName(columnIndex));
    }

    @Override
    public FtmNature getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        final int id = rs.getInt(columnName);

        if (rs.wasNull()) {
            return FtmNature.Unknown;
        }

        return FtmNature.fromId(id);
    }

    @Override
    public FtmNature getNullableResult(final CallableStatement rs, final int columnIndex) throws SQLException {
        final int id = rs.getInt(columnIndex);

        if (rs.wasNull()) {
            return FtmNature.Unknown;
        }

        return FtmNature.fromId(id);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, FtmNature parameter, JdbcType jdbcType) throws SQLException {
        throw new UnsupportedOperationException();
    }
}
