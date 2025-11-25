package com.appautomation.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.appautomation.data.model.AutomationLog;
import com.appautomation.data.model.AutomationSession;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AutomationDao_Impl implements AutomationDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AutomationSession> __insertionAdapterOfAutomationSession;

  private final EntityInsertionAdapter<AutomationLog> __insertionAdapterOfAutomationLog;

  private final EntityDeletionOrUpdateAdapter<AutomationSession> __updateAdapterOfAutomationSession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteSession;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOldLogs;

  public AutomationDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAutomationSession = new EntityInsertionAdapter<AutomationSession>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `automation_sessions` (`id`,`startTime`,`appQueueJson`,`currentIndex`,`isActive`,`completedCount`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AutomationSession entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getStartTime());
        statement.bindString(3, entity.getAppQueueJson());
        statement.bindLong(4, entity.getCurrentIndex());
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getCompletedCount());
      }
    };
    this.__insertionAdapterOfAutomationLog = new EntityInsertionAdapter<AutomationLog>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `automation_logs` (`id`,`timestamp`,`appPackage`,`appName`,`durationMillis`,`success`,`errorMessage`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AutomationLog entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindString(3, entity.getAppPackage());
        statement.bindString(4, entity.getAppName());
        statement.bindLong(5, entity.getDurationMillis());
        final int _tmp = entity.getSuccess() ? 1 : 0;
        statement.bindLong(6, _tmp);
        if (entity.getErrorMessage() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getErrorMessage());
        }
      }
    };
    this.__updateAdapterOfAutomationSession = new EntityDeletionOrUpdateAdapter<AutomationSession>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `automation_sessions` SET `id` = ?,`startTime` = ?,`appQueueJson` = ?,`currentIndex` = ?,`isActive` = ?,`completedCount` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AutomationSession entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getStartTime());
        statement.bindString(3, entity.getAppQueueJson());
        statement.bindLong(4, entity.getCurrentIndex());
        final int _tmp = entity.isActive() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getCompletedCount());
        statement.bindLong(7, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteSession = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM automation_sessions WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteOldLogs = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM automation_logs WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertSession(final AutomationSession session,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAutomationSession.insertAndReturnId(session);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertLog(final AutomationLog log, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfAutomationLog.insert(log);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateSession(final AutomationSession session,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfAutomationSession.handle(session);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteSession(final long sessionId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteSession.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, sessionId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteSession.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOldLogs(final long beforeTimestamp,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOldLogs.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, beforeTimestamp);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteOldLogs.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getActiveSession(final Continuation<? super AutomationSession> $completion) {
    final String _sql = "SELECT * FROM automation_sessions WHERE isActive = 1 LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AutomationSession>() {
      @Override
      @Nullable
      public AutomationSession call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfAppQueueJson = CursorUtil.getColumnIndexOrThrow(_cursor, "appQueueJson");
          final int _cursorIndexOfCurrentIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "currentIndex");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfCompletedCount = CursorUtil.getColumnIndexOrThrow(_cursor, "completedCount");
          final AutomationSession _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final String _tmpAppQueueJson;
            _tmpAppQueueJson = _cursor.getString(_cursorIndexOfAppQueueJson);
            final int _tmpCurrentIndex;
            _tmpCurrentIndex = _cursor.getInt(_cursorIndexOfCurrentIndex);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final int _tmpCompletedCount;
            _tmpCompletedCount = _cursor.getInt(_cursorIndexOfCompletedCount);
            _result = new AutomationSession(_tmpId,_tmpStartTime,_tmpAppQueueJson,_tmpCurrentIndex,_tmpIsActive,_tmpCompletedCount);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AutomationSession>> getAllSessions() {
    final String _sql = "SELECT * FROM automation_sessions ORDER BY startTime DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"automation_sessions"}, new Callable<List<AutomationSession>>() {
      @Override
      @NonNull
      public List<AutomationSession> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfStartTime = CursorUtil.getColumnIndexOrThrow(_cursor, "startTime");
          final int _cursorIndexOfAppQueueJson = CursorUtil.getColumnIndexOrThrow(_cursor, "appQueueJson");
          final int _cursorIndexOfCurrentIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "currentIndex");
          final int _cursorIndexOfIsActive = CursorUtil.getColumnIndexOrThrow(_cursor, "isActive");
          final int _cursorIndexOfCompletedCount = CursorUtil.getColumnIndexOrThrow(_cursor, "completedCount");
          final List<AutomationSession> _result = new ArrayList<AutomationSession>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AutomationSession _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpStartTime;
            _tmpStartTime = _cursor.getLong(_cursorIndexOfStartTime);
            final String _tmpAppQueueJson;
            _tmpAppQueueJson = _cursor.getString(_cursorIndexOfAppQueueJson);
            final int _tmpCurrentIndex;
            _tmpCurrentIndex = _cursor.getInt(_cursorIndexOfCurrentIndex);
            final boolean _tmpIsActive;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsActive);
            _tmpIsActive = _tmp != 0;
            final int _tmpCompletedCount;
            _tmpCompletedCount = _cursor.getInt(_cursorIndexOfCompletedCount);
            _item = new AutomationSession(_tmpId,_tmpStartTime,_tmpAppQueueJson,_tmpCurrentIndex,_tmpIsActive,_tmpCompletedCount);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<AutomationLog>> getRecentLogs() {
    final String _sql = "SELECT * FROM automation_logs ORDER BY timestamp DESC LIMIT 100";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"automation_logs"}, new Callable<List<AutomationLog>>() {
      @Override
      @NonNull
      public List<AutomationLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfAppPackage = CursorUtil.getColumnIndexOrThrow(_cursor, "appPackage");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "appName");
          final int _cursorIndexOfDurationMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMillis");
          final int _cursorIndexOfSuccess = CursorUtil.getColumnIndexOrThrow(_cursor, "success");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final List<AutomationLog> _result = new ArrayList<AutomationLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AutomationLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpAppPackage;
            _tmpAppPackage = _cursor.getString(_cursorIndexOfAppPackage);
            final String _tmpAppName;
            _tmpAppName = _cursor.getString(_cursorIndexOfAppName);
            final long _tmpDurationMillis;
            _tmpDurationMillis = _cursor.getLong(_cursorIndexOfDurationMillis);
            final boolean _tmpSuccess;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSuccess);
            _tmpSuccess = _tmp != 0;
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _item = new AutomationLog(_tmpId,_tmpTimestamp,_tmpAppPackage,_tmpAppName,_tmpDurationMillis,_tmpSuccess,_tmpErrorMessage);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<AutomationLog>> getFailedLogs() {
    final String _sql = "SELECT * FROM automation_logs WHERE success = 0 ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"automation_logs"}, new Callable<List<AutomationLog>>() {
      @Override
      @NonNull
      public List<AutomationLog> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfAppPackage = CursorUtil.getColumnIndexOrThrow(_cursor, "appPackage");
          final int _cursorIndexOfAppName = CursorUtil.getColumnIndexOrThrow(_cursor, "appName");
          final int _cursorIndexOfDurationMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "durationMillis");
          final int _cursorIndexOfSuccess = CursorUtil.getColumnIndexOrThrow(_cursor, "success");
          final int _cursorIndexOfErrorMessage = CursorUtil.getColumnIndexOrThrow(_cursor, "errorMessage");
          final List<AutomationLog> _result = new ArrayList<AutomationLog>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AutomationLog _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpAppPackage;
            _tmpAppPackage = _cursor.getString(_cursorIndexOfAppPackage);
            final String _tmpAppName;
            _tmpAppName = _cursor.getString(_cursorIndexOfAppName);
            final long _tmpDurationMillis;
            _tmpDurationMillis = _cursor.getLong(_cursorIndexOfDurationMillis);
            final boolean _tmpSuccess;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSuccess);
            _tmpSuccess = _tmp != 0;
            final String _tmpErrorMessage;
            if (_cursor.isNull(_cursorIndexOfErrorMessage)) {
              _tmpErrorMessage = null;
            } else {
              _tmpErrorMessage = _cursor.getString(_cursorIndexOfErrorMessage);
            }
            _item = new AutomationLog(_tmpId,_tmpTimestamp,_tmpAppPackage,_tmpAppName,_tmpDurationMillis,_tmpSuccess,_tmpErrorMessage);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
