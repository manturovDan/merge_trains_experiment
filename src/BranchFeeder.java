import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BranchFeeder {
    static PrintStream outputStream = System.out;
    static final Pattern branchPattern =Pattern.compile("branch_(\\d+)");
    static final double threshold = 0.8;
    static final int secInterval = 40;

    public static void main(String[] args) throws IOException, InterruptedException {
        for (int i = 1; i < 100; ++i) {
            createFakePrFromExistentBranches(i);
            TimeUnit.SECONDS.sleep(secInterval);
        }
    }

    public static void createFakePrFromExistentBranches(int newBranchNumber) throws IOException, InterruptedException {
        String monorepoPath = "/Users/Danila.Manturov/source/git-sandbox/monorepo";
        execute("pwd");
        execute("git", "-C", monorepoPath, "checkout", "main");
        execute("git", "-C", monorepoPath, "checkout", "branch_"+newBranchNumber);
        execute("git", "-C", monorepoPath, "push", "origin", "branch_"+newBranchNumber);
        execute("sh", "src/createPR.sh", String.valueOf(newBranchNumber));
    }

    public static void createFakePR() throws IOException, InterruptedException {
        String monorepoPath = "/Users/Danila.Manturov/source/git-sandbox/monorepo";
        execute("pwd");
        execute("git", "-C", monorepoPath, "checkout", "main");
        String branches = execute("git", "-C", monorepoPath, "branch");
        int newBranchNumber = getMaxBranchNumber(branches)+1; //find max N in among branch_N branches (else 0)
        execute("git", "-C", monorepoPath, "checkout", "-b", "branch_"+newBranchNumber);
        createFile(monorepoPath + "/branch_"+newBranchNumber+".txt", (Math.random() < threshold ? "1" : "0"));
        execute("git", "-C", monorepoPath, "add", ".");
        execute("git", "-C", monorepoPath, "commit", "-m", "created : " + newBranchNumber);
        execute("git", "-C", monorepoPath, "push", "origin", "branch_"+newBranchNumber);
        execute("sh", "src/createPR.sh", String.valueOf(newBranchNumber));
    }

    static String execute(String... command) throws InterruptedException, IOException {
        outputStream.println("Execution: " + Arrays.toString(command));
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command);
        Process process = processBuilder.start();
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader( new InputStreamReader(new java.io.SequenceInputStream(process.getInputStream(), process.getErrorStream())));

        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exit = process.waitFor();
        if (exit == 0) {
            outputStream.print(output);
            outputStream.println("--------");
            return output.toString();
        } else {
            throw new RuntimeException("execution error");
        }
    }

    static int getMaxBranchNumber(String branches) {
        int newBranchNum = 0;
        Matcher matcher = branchPattern.matcher(branches);
        while (matcher.find()) {
            int number = Integer.parseInt(matcher.group(1));
            newBranchNum = Math.max(number, newBranchNum);
        }

        return newBranchNum;
    }

    static void createFile(String path, String content) throws IOException {
        List<String> lines = List.of(content);
        Path file = Paths.get(path);
        Files.write(file, lines, StandardCharsets.UTF_8);
        outputStream.println("created: " + path + " with content: " + content + "\n-------");
    }
}
