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
    static final int secInterval = 15;

    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length < 1) {
            throw new RuntimeException("No repo specified!");
        }

        int newRepoBrNum = -1;
        if (args.length > 1) {
            newRepoBrNum = Integer.parseInt(args[1]);
        }

        System.out.println(args[0]);
        for (int i = 0; i < 100; ++i) {
            createFakePR(args[0], newRepoBrNum + i);
            TimeUnit.SECONDS.sleep(secInterval);
        }
    }


    public static void createFakePR(String repoPath, int newCounter) throws IOException, InterruptedException {
        execute("pwd");
        execute("git", "-C", repoPath, "checkout", "main");
        String branches = execute("git", "-C", repoPath, "branch");
        int newBranchNumber = newCounter;
        if (newBranchNumber == -1) {
            newBranchNumber = getMaxBranchNumber(branches) + 1; //find max N in among branch_N branches (else 0)
        }
        execute("git", "-C", repoPath, "checkout", "-b", "branch_"+newBranchNumber);
        String result = (Math.random() < threshold ? "1" : "0");
        createFile(repoPath + "/branch_"+newBranchNumber+".txt", result);
        execute("git", "-C", repoPath, "add", ".");
        execute("git", "-C", repoPath, "commit", "-m", "created : " + newBranchNumber);
        execute("git", "-C", repoPath, "push", "origin", "branch_"+newBranchNumber);
        execute("sh", "src/createMR_gitlab.sh", String.valueOf(newBranchNumber));
        System.err.println("Created: PR-" + newBranchNumber + ": " + result);
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
