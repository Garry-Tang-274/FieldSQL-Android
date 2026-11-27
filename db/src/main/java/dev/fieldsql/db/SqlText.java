package dev.fieldsql.db;

import java.util.*;
/** Limit each editor action to one statement, respecting the main SQL literal/comment forms. */
final class SqlText {
    final List<String> words=new ArrayList<>(); final String single;
    SqlText(String sql,String dialect){
        boolean pg=dialect.equals("postgres"),my=dialect.equals("mysql");
        int n=sql.length(),end=-1;
        for(int i=0;i<n;){
            char c=sql.charAt(i),next=i+1<n?sql.charAt(i+1):0;
            if(Character.isWhitespace(c)){i++;continue;}
            if((c=='-'&&next=='-'&&(!my||i+2>=n||Character.isWhitespace(sql.charAt(i+2))))||(my&&c=='#')){
                while(i<n&&sql.charAt(i)!='\n'&&sql.charAt(i)!='\r')i++;continue;
            }
            if(c=='/'&&next=='*'){
                if(my&&i+2<n&&(sql.charAt(i+2)=='!'||sql.startsWith("/*M!",i)))throw new IllegalArgumentException("请直接运行语句，不支持可执行注释");
                i+=2;int depth=1;
                while(i<n&&depth>0){if(pg&&sql.startsWith("/*",i)){i+=2;depth++;}else if(sql.startsWith("*/",i)){i+=2;depth--;}else i++;}
                if(depth!=0)throw new IllegalArgumentException("注释未结束");continue;
            }
            if(c==';'){if(end<0)end=i;i++;continue;}
            if(end>=0)throw new IllegalArgumentException("每次运行一条 SQL；请选择单条语句");
            if(c=='\''||c=='"'||(c==96&&!pg)||(c=='['&&!pg&&!my)){
                char quote=c=='['?']':c;boolean escape=my||(pg&&c=='\''&&i>0&&(sql.charAt(i-1)=='E'||sql.charAt(i-1)=='e')&&(i==1||!word(sql.charAt(i-2))));
                i++;boolean closed=false;
                while(i<n){char k=sql.charAt(i++);if(escape&&k=='\\'&&i<n){i++;continue;}if(k==quote){if(i<n&&sql.charAt(i)==quote&&quote!=']'){i++;continue;}closed=true;break;}}
                if(!closed) throw new IllegalArgumentException("引号未结束");continue;
            }
            if(pg&&c=='$'){int j=i+1;while(j<n&&(Character.isLetterOrDigit(sql.charAt(j))||sql.charAt(j)=='_'))j++;
                if(j<n&&sql.charAt(j)=='$'&&(j==i+1||!Character.isDigit(sql.charAt(i+1)))){String tag=sql.substring(i,j+1);
                    int k=sql.indexOf(tag,j+1);if(k<0)throw new IllegalArgumentException("美元引号未结束");i=k+tag.length();continue;}}
            if(Character.isLetter(c)||c=='_'){int j=i+1;while(j<n&&word(sql.charAt(j)))j++;words.add(sql.substring(i,j).toUpperCase(Locale.ROOT));i=j;continue;}
            i++;
        }
        single=(end<0?sql:sql.substring(0,end)).trim();
        if(single.isEmpty()||words.isEmpty())throw new IllegalArgumentException("请输入 SQL 语句");
    }
    static boolean word(char c){return Character.isLetterOrDigit(c)||c=='_'||c=='$';}
}
