package nu.mine.mosher.genealogy;


import org.apache.ibatis.type.*;

import java.sql.*;


public class FtmRelationshipStatusTypeHandler extends BaseTypeHandler<FtmRelationshipStatus> {
    @Override
    public FtmRelationshipStatus getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return getNullableResult(rs, rs.getMetaData().getColumnName(columnIndex));
    }

    @Override
    public FtmRelationshipStatus getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        final int id = rs.getInt(columnName);

        if (rs.wasNull()) {
            return FtmRelationshipStatus.Unknown;
        }

        return FtmRelationshipStatus.fromId(id);
    }

    @Override
    public FtmRelationshipStatus getNullableResult(final CallableStatement rs, final int columnIndex) throws SQLException {
        final int id = rs.getInt(columnIndex);

        if (rs.wasNull()) {
            return FtmRelationshipStatus.Unknown;
        }

        return FtmRelationshipStatus.fromId(id);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, FtmRelationshipStatus parameter, JdbcType jdbcType) throws SQLException {
        throw new UnsupportedOperationException();
    }
}
