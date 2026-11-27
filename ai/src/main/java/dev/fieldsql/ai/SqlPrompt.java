package dev.fieldsql.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure-Java wire contract used by Android and the native smoke test. */
public final class SqlPrompt {
    private SqlPrompt() {}
    public static String make(String mode, String dialect, String schema, String sql, String error, String request) {
        return make(mode,dialect,schema,sql,error,request,"Chinese");
    }
    public static String make(String mode,String dialect,String schema,String sql,String error,String request,String language){
        String instruction;
        if ("explain".equals(mode)) instruction = "Explain the SQL in "+language+". Describe the result, grouping, filters, and any side effects accurately. Do not execute anything.";
        else if ("fix".equals(mode)) instruction = "Fix the given SQL for the database dialect. Use the error if present. Return only the corrected SQL, no explanation.";
        else instruction = "Write one SQL statement matching the user's request. Use the provided schema and SQL dialect. Return only SQL, no explanation.";
        return instruction + "\nDialect: " + dialect +
            (schema.isEmpty() ? "" : "\nDatabase schema:\n" + schema) +
            ("generate".equals(mode) || sql.isEmpty() ? "" : "\nSQL:\n" + sql) +
            (!"fix".equals(mode) || error.isEmpty() ? "" : "\nDatabase error:\n" + error) +
            (request.isEmpty() ? "" : "\nUser request:\n" + request);
    }
    public static List<String> command(String exe, String model, String promptFile, int threads) {
        return new ArrayList<>(Arrays.asList(exe, "-m",model,"--file",promptFile,"-c","3072","-n","384",
            "-t",Integer.toString(threads),"--temp","0","--seed","42","-ngl","0","--no-warmup","--no-perf",
            "-cnv","-st","--no-display-prompt","--color","off","--simple-io"));
    }
    public static String clean(String output) {
        String s = output.trim();
        if (s.endsWith("[end of text]")) s = s.substring(0,s.length()-13).trim();
        // Strip only a single enclosing fence; never silently truncate generated SQL.
        Matcher m = Pattern.compile("(?is)^" + ((char)96) + "{3}(?:sql|sqlite|postgresql|postgres|mysql|mariadb)?[ \\t]*\\r?\\n(.*)\\r?\\n?" + ((char)96) + "{3}\\s*$").matcher(s);
        return m.matches() ? m.group(1).trim() : s;
    }
}
