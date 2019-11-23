import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.*;

public final class SaveHook implements FileDocumentManagerListener {

    private boolean debug = false;

    private static String getRelativeFilepath(Document document) {
        VirtualFile currentFile = FileDocumentManager.getInstance().getFile(document);
        assert currentFile != null;
        String filePathAbsolute = currentFile.getPath();

        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project activeProject = null;
        for (Project project : projects) {
            Window window = WindowManager.getInstance().suggestParentWindow(project);
            if (window != null && window.isActive()) {
                activeProject = project;
            }
        }
        assert activeProject != null;
        String projectPathAbsolute = activeProject.getBasePath();
        assert projectPathAbsolute != null;
        return filePathAbsolute.replace(projectPathAbsolute, "");
    }

    public void beforeDocumentSaving(@NotNull Document document) {
        String repository_id = VCSInfo.getProjectId(document);
        String commit_id = VCSInfo.getCommitId(document);
        String user_id = VCSInfo.getUserId();
        String file_id = getRelativeFilepath(document);

        // GET request
        String res = "";
        try {
            res = getEdit(repository_id, commit_id, file_id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // PUT request

        if (debug) {
            System.out.println(inbetweenStrings(res, "\"user_id\": \"", "\""));
            System.out.println(inbetweenStrings(res, "\"status\": \"", "\""));
        }
        if (inbetweenStrings(res, "\"status\": \"", "\"").equals("OK") || inbetweenStrings(res, "\"user_id\": \"", "\"").equals(user_id)) {
            try {
                res = putEdit(repository_id, commit_id, file_id, user_id);
                String message = "Uncommitted change of " + user_id + " in " + file_id + " signed.";
                Notifications.Bus.notify(new Notification("onSaveHook", "Change in project base", message, NotificationType.INFORMATION));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //  Notification
        else {
            String message = "The file " + inbetweenStrings(res, "\"file_id\": \"", "\"").replace("___", "/") +
                    " has an uncommitted change from " + inbetweenStrings(res, "\"timestamp\": \"", "\"") + " by " +
                    inbetweenStrings(res, "\"user_id\": \"", "\"");
            Notifications.Bus.notify(
                    new Notification(
                            "onSaveHook",
                            "Attention",
                            message,
                            NotificationType.WARNING));
        }
    }


    private static String putEdit(String repository, String commit, String file, String user) throws IOException, InterruptedException {
        file = file.replace("/", "___");
        String command =
                "curl -X PUT \\\n" +
                        "  https://hackatum2019.herokuapp.com/repository/" + repository + "/commit/" + commit + "/file/" + file + "/ \\\n" +
                        "  -H 'cache-control: no-cache' \\\n" +
                        "  -H 'content-type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW' \\\n" +
                        "  -F user_id=" + user;
        return getString(command);
    }

    @NotNull
    private static String getString(String command) throws IOException, InterruptedException {
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append('\n');
        }
        return output.toString();
    }

    private static String getEdit(String repository, String commit, String file) throws IOException, InterruptedException {
        file = file.replace("/", "___");
        String command =
                "curl -X GET http://hackatum2019.herokuapp.com/repository/" + repository + "/commit/" + commit + "/file/" + file + "/";
        return getString(command);
    }

    private static String inbetweenStrings(String input, String a, String b) {
        int i_a = input.indexOf(a) + a.length();
        int i_b = input.indexOf(b, i_a);
        return input.substring(i_a, i_b);
    }
}