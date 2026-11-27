package dev.fieldsql.db;

import java.util.*;
/** One live session. Confine all calls (including close) to one serial background executor. */
public interface Database extends AutoCloseable {
    final class Result {
        public final List<String> columns;
        /** SQL NULL is Java null. Blobs are marked previews; oversized scalar values raise an error rather than changing meaning. */
        public final List<List<String>> rows;
        /** Optional, parallel to rows: N=numeric SQL storage/type, B=boolean, S=all others.
         * The original string in rows stays authoritative (including precision and SQL NULL). */
        public final List<List<String>> cellTypes;
        /** -1 when this statement has no meaningful update count. */
        public final long affected;
        public final boolean truncated;
        Result(List<String> c,List<List<String>> r,long n,boolean more){this(c,r,n,more,Collections.emptyList());}
        Result(List<String> c,List<List<String>> r,long n,boolean more,List<List<String>> types){columns=c;rows=r;affected=n;truncated=more;cellTypes=types;}
        static Result count(long n){return new Result(Collections.emptyList(),Collections.emptyList(),n,false);}
    }
    List<String> tables() throws Exception;
    /** Current schema. kind=column/index; primary is the 1-based primary-key ordinal, or null. */
    Result describe(String table) throws Exception;
    Result browse(String table,int offset,int limit) throws Exception;
    default Result execute(String sql) throws Exception {return execute(sql,500);}
    Result execute(String sql,int maxRows) throws Exception;
    /** Query deadline, not TCP idle timeout; available only for remote JDBC sessions. */
    default void setQueryTimeoutSeconds(int seconds) throws Exception{throw new UnsupportedOperationException("本地 SQLite 不提供查询限时");}
    /** Prepared single-row update; key must contain every primary-key component, with non-null values. */
    int update(String table,Map<String,String> values,Map<String,String> key) throws Exception;
    void close() throws Exception;
}




