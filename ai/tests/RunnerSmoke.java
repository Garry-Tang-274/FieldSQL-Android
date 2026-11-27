import dev.fieldsql.ai.SqlPrompt;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
public final class RunnerSmoke {
    // java RunnerSmoke <llama-completion> <GGUF> <generate|fix|explain> <private work directory>
    public static void main(String[] a) throws Exception {
        Path dir=Path.of(a[3]); String mode=a[2];
        String prompt=SqlPrompt.make(mode,"SQLite","CREATE TABLE sales(city TEXT, amount REAL);",
                "fix".equals(mode)?"SELECT city, SUM(amont) FROM sales GROUP BY city;":"SELECT SUM(amount) FROM sales;",
                "no such column: amont", "generate".equals(mode)?"按城市汇总销售额，按总额从高到低排列。":"");
        Path promptFile=dir.resolve(mode+".prompt.txt");
        Files.writeString(promptFile,prompt,StandardCharsets.UTF_8);
        List<String> cmd=SqlPrompt.command(a[0],a[1],promptFile.toString(),4);
        Process p=new ProcessBuilder(cmd).redirectError(dir.resolve(mode+".diagnostics.log").toFile()).start();
        p.getOutputStream().close();
        String text=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8);
        if(p.waitFor()!=0) throw new IOException("llama-completion failed; see diagnostics");
        String cleaned=SqlPrompt.clean(text);
        if(cleaned.isBlank()||cleaned.contains("[end of text]"))throw new AssertionError("Output lifecycle failed");
        Files.writeString(dir.resolve(mode+".txt"),cleaned,StandardCharsets.UTF_8);System.out.println(cleaned);
    }
}
