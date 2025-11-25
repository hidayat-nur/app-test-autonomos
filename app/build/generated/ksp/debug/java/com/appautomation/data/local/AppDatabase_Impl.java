package com.appautomation.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile AutomationDao _automationDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `automation_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startTime` INTEGER NOT NULL, `appQueueJson` TEXT NOT NULL, `currentIndex` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `completedCount` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `automation_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `appPackage` TEXT NOT NULL, `appName` TEXT NOT NULL, `durationMillis` INTEGER NOT NULL, `success` INTEGER NOT NULL, `errorMessage` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'a8f46923ed3aba0b6e6bc25e3efd6fde')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `automation_sessions`");
        db.execSQL("DROP TABLE IF EXISTS `automation_logs`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsAutomationSessions = new HashMap<String, TableInfo.Column>(6);
        _columnsAutomationSessions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAutomationSessions.put("startTime", new TableInfo.Column("startTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAutomationSessions.put("appQueueJson", new TableInfo.Column("appQueueJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAutomationSessions.put("currentIndex", new TableInfo.Column("currentIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAutomationSessions.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAutomationSessions.put("completedCount", new TableInfo.Column("completedCount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAutomationSessions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAutomationSessions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAutomationSessions = new TableInfo("automation_sessions", _columnsAutomationSessions, _foreignKeysAutomationSessions, _indicesAutomationSessions);
        final TableInfo _existingAutomationSessions = TableInfo.read(db, "automation_sessions");
        if (!_infoAutomationSessions.equals(_existingAutomationSessions)) {
          return new RoomOpenHelper.ValidationResult(false, "automation_sessions(com.appautomation.data.model.AutomationSession).\n"
                  + " Expected:\n" + _infoAutomationSessions + "\n"
                  + " Found:\n" + _existingAutomationSessions);
        }
        final HashMap<String, TableInfo.Column> _columnsAutomationLogs = new HashMap<String, TableInfo.Column>(7);
        _columnsAutomationLogs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAutomationLogs.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAutomationLogs.put("appPackage", new TableInfo.Column("appPackage", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAutomationLogs.put("appName", new TableInfo.Column("appName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAutomationLogs.put("durationMillis", new TableInfo.Column("durationMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAutomationLogs.put("success", new TableInfo.Column("success", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAutomationLogs.put("errorMessage", new TableInfo.Column("errorMessage", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAutomationLogs = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAutomationLogs = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAutomationLogs = new TableInfo("automation_logs", _columnsAutomationLogs, _foreignKeysAutomationLogs, _indicesAutomationLogs);
        final TableInfo _existingAutomationLogs = TableInfo.read(db, "automation_logs");
        if (!_infoAutomationLogs.equals(_existingAutomationLogs)) {
          return new RoomOpenHelper.ValidationResult(false, "automation_logs(com.appautomation.data.model.AutomationLog).\n"
                  + " Expected:\n" + _infoAutomationLogs + "\n"
                  + " Found:\n" + _existingAutomationLogs);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "a8f46923ed3aba0b6e6bc25e3efd6fde", "f3e69ec706d0c05c31d1ce652721b4df");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "automation_sessions","automation_logs");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `automation_sessions`");
      _db.execSQL("DELETE FROM `automation_logs`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(AutomationDao.class, AutomationDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public AutomationDao automationDao() {
    if (_automationDao != null) {
      return _automationDao;
    } else {
      synchronized(this) {
        if(_automationDao == null) {
          _automationDao = new AutomationDao_Impl(this);
        }
        return _automationDao;
      }
    }
  }
}
