package de.worldOneo.spiJetAPI.sql;

import com.google.gson.Gson;
import com.zaxxer.hikari.HikariDataSource;
import de.worldOneo.spiJetAPI.utils.AsyncExecutor;
import de.worldOneo.spiJetAPI.utils.SpiJetBuilder;
import lombok.Getter;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Future;

public class JsonSQLStorage extends AsyncExecutor {
    private final SQLExecutor<SQLQueryBuilder> sqlExecutor;
    private final String tableName;
    private final String databaseName;
    private static final Gson GSON = new Gson();

    @Getter
    private enum SQLStrings {
        TABLE_CREATION_STRING("CREATE TABLE IF NOT EXISTS `%s` (" +
                "`uuid` TINYTEXT NOT NULL," +
                "`jsonDocument` LONGTEXT NOT NULL COLLATE 'utf8mb4_bin'," +
                "UNIQUE INDEX `UniqueID` (`uuid`(40))" +
                ");"),
        GETTER_STRING("SELECT * from `%s` WHERE uuid='%s';"),
        SETTER_STRING("INSERT INTO `%s` (uuid, jsonDocument) VALUES ('%s', '%3$s') ON DUPLICATE KEY UPDATE jsonDocument='%3$s';");
        final String string;

        SQLStrings(String string) {
            this.string = string;
        }
    }

    public JsonSQLStorage(SQLExecutor<SQLQueryBuilder> sqlExecutor, String tableName, String databaseName) throws SQLException {
        this.sqlExecutor = sqlExecutor;
        this.tableName = tableName;
        this.databaseName = databaseName;
        setup();
    }

    public JsonSQLStorage(SpiJetBuilder<HikariDataSource> dataSourceBuilder, String tableName, String databaseName) throws SQLException {
        this(new QuerySQLManager(dataSourceBuilder), tableName, databaseName);
    }

    public JsonSQLStorage(HikariDataSource hikariDataSource, String tableName, String databaseName) throws SQLException {
        this(new QuerySQLManager(hikariDataSource), tableName, databaseName);
    }

    private void setup() throws SQLException {
        String formattedCreationString = String.format(SQLStrings.TABLE_CREATION_STRING.getString(), tableName);
        SQLQueryBuilder sqlQueryBuilder = new SQLQueryBuilder(databaseName, formattedCreationString);
        sqlExecutor.executeUpdate(sqlQueryBuilder);
    }

    public <T> T getData(UUID uuid, Class<T> classOfT) throws SQLException {
        String formattedString = String.format(SQLStrings.SETTER_STRING.getString(), tableName, uuid.toString());
        SQLQueryBuilder sqlQueryBuilder = new SQLQueryBuilder(databaseName, formattedString);
        CachedRowSet cachedRowSet = sqlExecutor.executeQuery(sqlQueryBuilder);
        if (cachedRowSet == null) {
            return null;
        }
        return GSON.fromJson(cachedRowSet.getString("jsonDocument"), classOfT);
    }

    public boolean setData(UUID uuid, Object dataObject) throws SQLException {
        String data = GSON.toJson(dataObject);
        String format = String.format(SQLStrings.SETTER_STRING.getString(), tableName, uuid.toString(), data);
        SQLQueryBuilder sqlQueryBuilder = new SQLQueryBuilder(databaseName, format);
        CachedRowSet cachedRowSet = sqlExecutor.executeUpdate(sqlQueryBuilder);
        return cachedRowSet != null;
    }

    public Future<Boolean> setDataAsync(UUID uuid, Object dataObject) {
        return getThreadPoolExecutor().submit(() -> setData(uuid, dataObject));
    }

    public <T> Future<T> getDataAsync(UUID uuid, Class<T> classOfT) {
        return getThreadPoolExecutor().submit(() -> getData(uuid, classOfT));
    }
}