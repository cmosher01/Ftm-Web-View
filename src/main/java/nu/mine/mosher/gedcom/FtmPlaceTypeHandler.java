package nu.mine.mosher.gedcom;



import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;



public class FtmPlaceTypeHandler extends BaseTypeHandler<Place> {
    @Override
    public Place getNullableResult(final ResultSet rs, final int columnIndex) throws SQLException {
        return getNullableResult(rs, rs.getMetaData().getColumnName(columnIndex));
    }

    @Override
    public Place getNullableResult(final ResultSet rs, final String columnName) throws SQLException {
        final String s = rs.getString(columnName);

        if (rs.wasNull()) {
            return Place.empty();
        }

        return Place.fromFtmPlace(s);
    }

    @Override
    public Place getNullableResult(final CallableStatement rs, final int columnIndex) throws SQLException {
        final String s = rs.getString(columnIndex);

        if (rs.wasNull()) {
            return Place.empty();
        }

        return Place.fromFtmPlace(s);
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Place parameter, JdbcType jdbcType) throws SQLException {
        throw new UnsupportedOperationException();
    }
}
