import com.android.aapt.Resources;
import com.intellij.ide.DataManager;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

public final class SaveHook implements FileDocumentManagerListener {

    public static String getRelativeFilepath(Document document) {
        VirtualFile currentFile = FileDocumentManager.getInstance().getFile(document);
        String filePathAbsolute = currentFile.getPath();

        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project activeProject = null;
        for (Project project : projects) {
            Window window = WindowManager.getInstance().suggestParentWindow(project);
            if (window != null && window.isActive()) {
                activeProject = project;
            }
        }
        String projectPathAbsolute = activeProject.getBasePath();
        String filePathRelative = filePathAbsolute.replace(projectPathAbsolute, "");
        return filePathRelative;
    }

    public void beforeDocumentSaving(@NotNull Document document) {
        String repository_id = "plugintest";
        String commit_id = "master";
        String user_id = "Christophers";
        String file_id = getRelativeFilepath(document);

        // GET request
        String res = "";
        try {
            res = getEdit(repository_id, commit_id, file_id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // PUT request
        //System.out.println(inbetweenStrings(res, "\"user_id\": \"", "\""));
        //System.out.println(inbetweenStrings(res, "\"status\": \"", "\""));
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


    public static String putEdit(String repository, String commit, String file, String user) throws IOException, InterruptedException {
        file = file.replace("/", "___");
        String command =
                "curl -X PUT \\\n" +
                        "  https://hackatum2019.herokuapp.com/repository/" + repository + "/commit/" + commit + "/file/" + file + "/ \\\n" +
                        "  -H 'cache-control: no-cache' \\\n" +
                        "  -H 'content-type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW' \\\n" +
                        "  -F user_id=" + user;
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String output = "";
        String line = "";
        while ((line = reader.readLine()) != null) {
            output += line + '\n';
        }
        return output;
    }

    public static String getEdit(String repository, String commit, String file) throws IOException, InterruptedException {
        file = file.replace("/", "___");
        String command =
                "curl -X GET http://hackatum2019.herokuapp.com/repository/" + repository + "/commit/" + commit + "/file/" + file + "/";
        Process p = Runtime.getRuntime().exec(command);
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String output = "";
        String line = "";
        while ((line = reader.readLine()) != null) {
            output += line + '\n';
        }
        return output;
    }

    public static String inbetweenStrings(String input, String a, String b) {
        int i_a = input.indexOf(a) + a.length();
        int i_b = input.indexOf(b, i_a);
        return input.substring(i_a, i_b);
    }
}