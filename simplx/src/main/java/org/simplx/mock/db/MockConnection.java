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

import org.simplx.mock.Mocker;

import java.io.File;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;

public class MockConnection extends Mocker<String, ResultSet>
        implements Connection {

    private final File path;
    private final Properties info;

    public MockConnection(String url, Properties info) {
        if (!MockDB.validMockURL(url))
            throw new IllegalArgumentException("invalid MockDB url");
        path = new File(url.substring(MockDB.JDBC_MOCK.length() + 1));
        if (!path.exists())
            throw new IllegalArgumentException("!path.exists()");
        this.info = info;
    }

    public Statement createStatement() throws SQLException {
        return new MockStatement(this);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public CallableStatement prepareCall(String sql) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String nativeSQL(String sql) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean getAutoCommit() throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void commit() throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void rollback() throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void close() throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isClosed() throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public DatabaseMetaData getMetaData() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setReadOnly(boolean readOnly) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isReadOnly() throws SQLException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setCatalog(String catalog) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getCatalog() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTransactionIsolation(int level) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getTransactionIsolation() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public SQLWarning getWarnings() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void clearWarnings() throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Statement createStatement(int resultSetType,
            int resultSetConcurrency) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
            int resultSetConcurrency) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setHoldability(int holdability) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getHoldability() throws SQLException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Savepoint setSavepoint() throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Savepoint setSavepoint(String name) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void rollback(Savepoint savepoint) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Statement createStatement(int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public CallableStatement prepareCall(String sql, int resultSetType,
            int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys)
            throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes)
            throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames)
            throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Array createArrayOf(String s, Object[] objects) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Blob createBlob() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Clob createClob() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NClob createNClob() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public SQLXML createSQLXML() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Struct createStruct(String s, Object[] objects) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Properties getClientInfo() throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getClientInfo(String s) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isValid(int i) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setClientInfo(Properties properties)
            throws SQLClientInfoException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setClientInfo(String s, String s1)
            throws SQLClientInfoException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isWrapperFor(Class<?> aClass) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> T unwrap(Class<T> tClass) throws SQLException {
        throw new UnsupportedOperationException();  //To change body of implemented methods use File | Settings | File Templates.
    }
}