/*
 * Copyright (C) 2024 by SkyWatch Space Applications Inc. http://www.skywatch.com
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.stac.database;

import org.esa.snap.stac.StacItem;
//import org.h2gis.utilities.SFSUtilities;
//import org.h2gis.utilities.SpatialResultSet;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class StacDatabase extends AbstractDatabase {

    private static final String DATABASE_NAME = "stacDB";
    private static final String ITEM_TABLE = "products";

    public static final String STAC_ID = "stac_id";
    public static final String SELF_HREF = "self_href";
    public static final String DESCRIPTION = "description";
    public static final String ACQUISITION_DATE = "acquisition_date";
    public static final String START_DATE = "start_date";
    public static final String END_DATE = "end_date";
    public static final String GEOMETRY = "geometry";

    @Override
    protected String getDatabaseName() {
        return DATABASE_NAME;
    }

    public void saveItem(final StacItem stacItem) throws SQLException {

        if (stacItem == null) {
            throw new NullPointerException("The stacItem is null.");
        }

        Integer productId = 0;
        getConnection();
        connection.setAutoCommit(false);
        try {
            productId = getId(stacItem);
            if (productId == null) {
                productId = insertProduct(stacItem);
            } else {
                updateProduct(productId, stacItem);
            }

            // commit the statements
            connection.commit();
        } catch (Exception e) {
            // rollback the statements from the transaction
            connection.rollback();
        }
    }

    private Integer getId(final StacItem stacItem) throws SQLException {
        final PreparedStatement statement = connection.prepareStatement("SELECT id FROM products WHERE " + STAC_ID + " = ?");
        statement.setString(1, stacItem.getId());
        ResultSet resultSet = statement.executeQuery();
        return resultSet.next() ? resultSet.getInt("id") : null;
    }

    private int insertProduct(final StacItem stacItem) throws Exception {
        final PreparedStatement statement = connection.prepareStatement("INSERT INTO products " +
                "(" + STAC_ID + ", " + SELF_HREF + ", " + DESCRIPTION + ", " +
                ACQUISITION_DATE + ", " + GEOMETRY + ") " +
                "VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        statement.setString(1, stacItem.getId());
        statement.setString(2, stacItem.getSelfURL());
        if (stacItem.getDescription() != null) {
            statement.setString(3, stacItem.getDescription());
        } else {
            statement.setNull(2, Types.VARCHAR);
        }

        final Date acquisitionDate = stacItem.getTime().getAsDate();
        if (acquisitionDate != null) {
            statement.setTimestamp(4, new Timestamp(acquisitionDate.getTime()));
        } else {
            statement.setNull(4, Types.TIMESTAMP);
        }
        statement.setString(5, stacItem.getGeometryAsWKT());

        int affectedRows = statement.executeUpdate();
        if (affectedRows == 0) {
            throw new SQLException("Failed to insert the product, no rows affected.");
        } else {
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Failed to get the generated product id.");
                }
            }
        }
    }

    private void updateProduct(final int productId, final StacItem stacItem) throws Exception {
        final PreparedStatement statement =
                connection.prepareStatement("UPDATE products SET " +
                        STAC_ID + " = ?, " + SELF_HREF + " = ?, " + DESCRIPTION + " = ?, " +
                        ACQUISITION_DATE + " = ?, " + GEOMETRY + " = ? " +
                        "WHERE id = ?");
        statement.setString(1, stacItem.getId());
        statement.setString(2, stacItem.getSelfURL());
        statement.setString(3, stacItem.getDescription());

        final Date acquisitionDate = stacItem.getTime().getAsDate();
        if (acquisitionDate != null) {
            statement.setTimestamp(4, new Timestamp(acquisitionDate.getTime()));
        } else {
            statement.setNull(4, Types.TIMESTAMP);
        }
        statement.setString(5, stacItem.getGeometryAsWKT());

        statement.setInt(6, productId);
        statement.executeUpdate();
    }

    public List<StacRecord> search(final Map<String, Object> parameterValues) throws SQLException {

        String sqlQuery = generateSQLQuery(parameterValues);
        System.out.println(sqlQuery);

        final List<StacRecord> recordList = new ArrayList<>();
//        final Connection wrappedConnection = SFSUtilities.wrapConnection(connection);
//
//        try (PreparedStatement prepareStatement = wrappedConnection.prepareStatement(sqlQuery)) {
//            try (SpatialResultSet resultSet = prepareStatement.executeQuery().unwrap(SpatialResultSet.class)) {
//                while (resultSet.next()) {
//                    recordList.add(new StacRecord(resultSet));
//                }
//            }
//        }
        return recordList;
    }

    private String generateSQLQuery(final Map<String, Object> parameterValues) {
        String stacID = null, selfHREF = null;
        Date startDate = null, endDate = null;
        String areaWKT = null;

        for (Map.Entry<String, Object> entry : parameterValues.entrySet()) {
            String parameterName = entry.getKey();
            Object parameterValue = entry.getValue();
            switch (parameterName) {
                case STAC_ID:
                    stacID = "'" + parameterValue + "'";
                    break;
                case SELF_HREF:
                    selfHREF = "'" + parameterValue + "'";
                    break;
                case START_DATE:
                    startDate = (Date) parameterValue;
                    if (startDate == null) {
                        throw new NullPointerException("The start date is invalid.");
                    }
                    break;
                case END_DATE:
                    endDate = (Date) parameterValue;
                    if (endDate == null) {
                        throw new NullPointerException("The end date is invalid.");
                    }
                    break;
                case GEOMETRY:
                    areaWKT = "'" + parameterValue + "'";
                    break;
                default:
                    throw new IllegalStateException("Unknown parameter '" + parameterName + "'.");
            }
        }
        if (endDate == null && startDate != null) {
            endDate = startDate;
        }

        final StringBuilder sql = new StringBuilder();
        sql.append("SELECT * ");
        sql.append("FROM products AS p");

        if (!parameterValues.isEmpty()) {
            sql.append(" WHERE ");
            if (stacID != null) {
                sql.append("p." + STAC_ID + " = ").append(stacID);
                sql.append(" AND ");
            }
            if (selfHREF != null) {
                sql.append("p." + SELF_HREF + " = ").append(selfHREF);
                sql.append(" AND ");
            }
            if (startDate != null) {
                sql.append("p.acquisition_date >= ?");
                sql.append(" AND ");
            }
            if (endDate != null) {
                sql.append("p.acquisition_date <= ?");
                sql.append(" AND ");
            }
            if (areaWKT != null) {
                sql.append(String.format("ST_Intersects(p.geometry, %s)", areaWKT));
            }
        }

        String sqlQuery = sql.toString();
        if (sqlQuery.endsWith(" AND ")) {
            sqlQuery = sqlQuery.substring(0, sqlQuery.length() - 5);
        }

        return sqlQuery;
    }
}
