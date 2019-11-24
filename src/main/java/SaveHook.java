import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
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
    private static final String mainUrl = "https://hackatum2019.herokuapp.com";
    private static final String visUrl = mainUrl + "/visual/";

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

        //retrieve information
        String repository_id = VCSInfo.getProjectId(document);
        String commit_id = VCSInfo.getCommitId(document);
        String user_id = VCSInfo.getUserId();
        String file_id = getRelativeFilepath(document);


        String repository_url = visUrl + repository_id + "/" ;
        String commit_url = repository_url + commit_id + "/";

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
        if (isOK(res) || sameUser(res, user_id)) {
            try {
                putEdit(repository_id, commit_id, file_id, user_id);

                String userString =  makeLink(user_id, commit_url);
                String fileString =  makeLink(file_id, commit_url);

                String message = "Uncommitted change of " + userString + " in " + fileString + " signed.";
                Notifications.Bus.notify(
                        new Notification("onSaveHook",
                                "Change in project base",
                                message, NotificationType.INFORMATION,
                                NotificationListener.URL_OPENING_LISTENER));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        //  Notification
        else {
            String timeString = makeLink(buildTimeString(res), commit_url);

            String fileString = makeLink(buildFileString(res),
                    commit_url);

            String userString = makeLink(buildUserString(res),
                    commit_url);

            String message = "The file " + fileString +
                    " has an uncommitted change from " + timeString + " by " +
                    userString;
            Notifications.Bus.notify(
                    new Notification(
                            "onSaveHook",
                            "Attention",
                            message,
                            NotificationType.WARNING, NotificationListener.URL_OPENING_LISTENER));
        }
    }

    private static String makeLink(String string, String url){
        return "<a href= " + url + ">" + string + "</a>";
    }


    private static String putEdit(String repository, String commit, String file, String user) throws IOException, InterruptedException {
        file = fileToUrl(file);
        String command =
                "curl -X PUT \\\n" +
                        mainUrl +
                        "/repository/" + repository + "/commit/" + commit + "/file/" + file + "/ \\\n" +
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
        file = fileToUrl(file);
        String command =
                "curl -X GET " + mainUrl + "/repository/" + repository + "/commit/" + commit + "/file/" + file + "/";
        return getString(command);
    }

    private static String inbetweenStrings(String input, String a, String b) {
        int i_a = input.indexOf(a) + a.length();
        int i_b = input.indexOf(b, i_a);
        return input.substring(i_a, i_b);
    }

    private static boolean isOK(String res){
        return inbetweenStrings(res, "\"status\": \"", "\"").equals("OK");
    }

    private static boolean sameUser(String res, String user_id){
        return inbetweenStrings(res, "\"user_id\": \"", "\"").equals(user_id);
    }

    private static String buildTimeString(String s){
        String[] splittedTime = inbetweenStrings(s, "\"timestamp\": \"", "\"").split(":");

        return splittedTime[0] + ":" + splittedTime[1];
    }

    private static String buildFileString(String s){
        return urlToFile(inbetweenStrings(s, "\"file_id\": \"", "\""));
    }

    private static String buildUserString(String s){
        return inbetweenStrings(s, "\"user_id\": \"", "\"");
    }

    private static String urlToFile(String s){
        return s.replace("___", "/");
    }

    private static String fileToUrl(String s){
        return s.replace("/","___");
    }
}