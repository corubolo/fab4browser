/*
 * Copyright (c) 2009, 2010, Ken Arnold All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the myself nor the names of its contributors may be used
 * to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * @SimplxCopyright
 */

package org.simplx.mock.db;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MockResultSet implements ResultSet {
    private final MockConnection conn;
    private final List<OutParam> indexedParams = new ArrayList<OutParam>(0);
    private final Set<OutParam> namedParams = new HashSet<OutParam>(0);

    private Object[] returned;
    private boolean lastWasNull;

    MockResultSet(MockConnection conn) {
        this.conn = conn;
    }

    public boolean wasNull() throws SQLException {
        return lastWasNull;
    }

    public String getString(int parameterIndex) throws SQLException {
        Object obj = getObject(parameterIndex);
        lastWasNull = obj == null;
        if (lastWasNull)
            return null;
        else
            return obj.toString();
    }

    public boolean getBoolean(int parameterIndex) throws SQLException {
        String str = getString(parameterIndex);
        if (str == null)
            return false;
        else
            return Boolean.parseBoolean(str);
    }

    public byte getByte(int parameterIndex) throws SQLException {
        String str = getString(parameterIndex);
        if (str == null)
            return 0;
        else
            return Byte.parseByte(str);
    }

    public short getShort(int parameterIndex) throws SQLException {
        String str = getString(parameterIndex);
        if (str == null)
            return 0;
        else
            return Short.parseShort(str);
    }

    public int getInt(int parameterIndex) throws SQLException {
        String str = getString(parameterIndex);
        if (str == null)
            return 0;
        else
            return Integer.parseInt(str);
    }

    public long getLong(int parameterIndex) throws SQLException {
        String str = getString(parameterIndex);
        if (str == null)
            return 0;
        else
            return Long.parseLong(str);
    }

    public float getFloat(int parameterIndex) throws SQLException {
        String str = getString(parameterIndex);
        if (str == null)
            return 0;
        else
            return Float.parseFloat(str);
    }

    public double getDouble(int parameterIndex) throws SQLException {
        String str = getString(parameterIndex);
        if (str == null)
            return 0;
        else
            return Double.parseDouble(str);
    }

    public BigDecimal getBigDecimal(int parameterIndex) throws SQLException {
        String str = getString(parameterIndex);
        if (str == null)
            return BigDecimal.ZERO;
        else {
            return new BigDecimal(str);
        }
    }

    public BigDecimal getBigDecimal(int parameterIndex, int scale)
            throws SQLException {
        String str = getString(parameterIndex);
        if (str == null)
            return BigDecimal.ZERO;
        else {
            return new BigDecimal(str).setScale(scale);
        }
    }

    public byte[] getBytes(int parameterIndex) throws SQLException {
        String str = getString(parameterIndex);
        if (str == null)
            return new byte[0];
        else
            return str.getBytes();
    }

    public Date getDate(int parameterIndex) throws SQLException {
        //!! Handle DB default time zone
        String str = getString(parameterIndex);
        if (str == null)
            return null;
        else
            return Date.valueOf(str);
    }

    public Date getDate(int parameterIndex, Calendar cal) throws SQLException {
        //!! Handle calendar time zone
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Time getTime(int parameterIndex) throws SQLException {
        //!! Handle DB default time zone
        String str = getString(parameterIndex);
        if (str == null)
            return null;
        else
            return Time.valueOf(str);
    }

    public Time getTime(int parameterIndex, Calendar cal) throws SQLException {
        //!! Handle calendar time zone
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Timestamp getTimestamp(int parameterIndex) throws SQLException {
        //!! Handle DB default time zone
        String str = getString(parameterIndex);
        if (str == null)
            return null;
        else
            return Timestamp.valueOf(str);
    }

    public Timestamp getTimestamp(int parameterIndex, Calendar cal)
            throws SQLException {
        //!! Handle calendar time zone
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getObject(int parameterIndex) throws SQLException {
        Object obj = returned[parameterIndex];
        lastWasNull = obj == null;
        return obj;
    }

    public Object getObject(int i, Map<String, Class<?>> map)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Ref getRef(int i) throws SQLException {
        //!! Implement getRef
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Blob getBlob(int i) throws SQLException {
        byte[] bytes = getBytes(i);
        if (bytes == null)
            return null;
        else
            return new SerialBlob(bytes);
    }

    public Clob getClob(int i) throws SQLException {
        String str = getString(i);
        if (str == null)
            return null;
        else
            return new SerialClob(str.toCharArray());
    }

    public Array getArray(int i) throws SQLException {
        //!! Implement getArray
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public URL getURL(int parameterIndex) throws SQLException {
        try {
            String str = getString(parameterIndex);
            return new URL(str);
        } catch (MalformedURLException e) {
            throw MockDBUtils.sqlException(e);
        }
    }

    public void setURL(String parameterName, URL val) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setNull(String parameterName, int sqlType) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setBoolean(String parameterName, boolean x)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setByte(String parameterName, byte x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setShort(String parameterName, short x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setInt(String parameterName, int x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setLong(String parameterName, long x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setFloat(String parameterName, float x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setDouble(String parameterName, double x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setBigDecimal(String parameterName, BigDecimal x)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setString(String parameterName, String x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setBytes(String parameterName, byte[] x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setDate(String parameterName, Date x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTime(String parameterName, Time x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTimestamp(String parameterName, Timestamp x)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setAsciiStream(String parameterName, InputStream x, int length)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setBinaryStream(String parameterName, InputStream x, int length)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setObject(String parameterName, Object x, int targetSqlType,
            int scale) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setObject(String parameterName, Object x, int targetSqlType)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setObject(String parameterName, Object x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setCharacterStream(String parameterName, Reader reader,
            int length) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setDate(String parameterName, Date x, Calendar cal)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTime(String parameterName, Time x, Calendar cal)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTimestamp(String parameterName, Timestamp x, Calendar cal)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setNull(String parameterName, int sqlType, String typeName)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getString(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean getBoolean(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte getByte(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public short getShort(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getInt(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long getLong(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public float getFloat(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public double getDouble(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte[] getBytes(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date getDate(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Time getTime(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Timestamp getTimestamp(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getObject(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal getBigDecimal(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getObject(String parameterName, Map<String, Class<?>> map)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Ref getRef(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Blob getBlob(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Clob getClob(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Array getArray(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Date getDate(String parameterName, Calendar cal)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Time getTime(String parameterName, Calendar cal)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Timestamp getTimestamp(String parameterName, Calendar cal)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public URL getURL(String parameterName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean next() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InputStream getAsciiStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InputStream getUnicodeStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InputStream getBinaryStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public BigDecimal getBigDecimal(String columnName, int scale)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InputStream getAsciiStream(String columnName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InputStream getUnicodeStream(String columnName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public InputStream getBinaryStream(String columnName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public SQLWarning getWarnings() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void clearWarnings() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getCursorName() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ResultSetMetaData getMetaData() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int findColumn(String columnName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Reader getCharacterStream(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Reader getCharacterStream(String columnName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isBeforeFirst() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isAfterLast() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isFirst() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isLast() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void beforeFirst() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void afterLast() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean first() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean last() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getRow() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean absolute(int row) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean relative(int rows) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean previous() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setFetchDirection(int direction) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getFetchDirection() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setFetchSize(int rows) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getFetchSize() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getType() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getConcurrency() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean rowUpdated() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean rowInserted() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean rowDeleted() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNull(int columnIndex) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBoolean(int columnIndex, boolean x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateByte(int columnIndex, byte x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateShort(int columnIndex, short x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateInt(int columnIndex, int x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateLong(int columnIndex, long x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateFloat(int columnIndex, float x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateDouble(int columnIndex, double x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBigDecimal(int columnIndex, BigDecimal x)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateString(int columnIndex, String x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBytes(int columnIndex, byte[] x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateDate(int columnIndex, Date x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateTime(int columnIndex, Time x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateTimestamp(int columnIndex, Timestamp x)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateAsciiStream(int columnIndex, InputStream x, int length)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBinaryStream(int columnIndex, InputStream x, int length)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateCharacterStream(int columnIndex, Reader x, int length)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateObject(int columnIndex, Object x, int scale)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateObject(int columnIndex, Object x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNull(String columnName) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBoolean(String columnName, boolean x)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateByte(String columnName, byte x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateShort(String columnName, short x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateInt(String columnName, int x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateLong(String columnName, long x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateFloat(String columnName, float x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateDouble(String columnName, double x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBigDecimal(String columnName, BigDecimal x)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateString(String columnName, String x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBytes(String columnName, byte[] x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateDate(String columnName, Date x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateTime(String columnName, Time x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateTimestamp(String columnName, Timestamp x)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateAsciiStream(String columnName, InputStream x, int length)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBinaryStream(String columnName, InputStream x, int length)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateCharacterStream(String columnName, Reader reader,
            int length) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateObject(String columnName, Object x, int scale)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateObject(String columnName, Object x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void insertRow() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateRow() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void deleteRow() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void refreshRow() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void cancelRowUpdates() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void moveToInsertRow() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void moveToCurrentRow() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Statement getStatement() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateRef(int columnIndex, Ref x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateRef(String columnName, Ref x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBlob(int columnIndex, Blob x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBlob(String columnName, Blob x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateClob(int columnIndex, Clob x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateClob(String columnName, Clob x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateArray(int columnIndex, Array x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateArray(String columnName, Array x) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getHoldability() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Reader getNCharacterStream(int i) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Reader getNCharacterStream(String s) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NClob getNClob(int i) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NClob getNClob(String s) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getNString(int i) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getNString(String s) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public RowId getRowId(int i) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public RowId getRowId(String s) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public SQLXML getSQLXML(int i) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public SQLXML getSQLXML(String s) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isClosed() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateAsciiStream(int i, InputStream inputStream)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateAsciiStream(int i, InputStream inputStream, long l)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateAsciiStream(String s, InputStream inputStream)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateAsciiStream(String s, InputStream inputStream, long l)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBinaryStream(int i, InputStream inputStream)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBinaryStream(int i, InputStream inputStream, long l)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBinaryStream(String s, InputStream inputStream)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBinaryStream(String s, InputStream inputStream, long l)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBlob(int i, InputStream inputStream) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBlob(int i, InputStream inputStream, long l)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBlob(String s, InputStream inputStream)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateBlob(String s, InputStream inputStream, long l)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateCharacterStream(int i, Reader reader)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateCharacterStream(int i, Reader reader, long l)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateCharacterStream(String s, Reader reader)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateCharacterStream(String s, Reader reader, long l)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateClob(int i, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateClob(int i, Reader reader, long l) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateClob(String s, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateClob(String s, Reader reader, long l)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNCharacterStream(int i, Reader reader)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNCharacterStream(int i, Reader reader, long l)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNCharacterStream(String s, Reader reader)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNCharacterStream(String s, Reader reader, long l)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNClob(int i, NClob nClob) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNClob(int i, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNClob(int i, Reader reader, long l) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNClob(String s, NClob nClob) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNClob(String s, Reader reader) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNClob(String s, Reader reader, long l)
            throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNString(int i, String s) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateNString(String s, String s1) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateRowId(int i, RowId rowId) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateRowId(String s, RowId rowId) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateSQLXML(int i, SQLXML sqlxml) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updateSQLXML(String s, SQLXML sqlxml) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isWrapperFor(Class<?> aClass) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> T unwrap(Class<T> tClass) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }
}