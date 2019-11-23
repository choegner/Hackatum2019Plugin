import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;

import com.intellij.openapi.wm.WindowManager;


import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class VCSInfo {

    static String getCommitId(Document document){
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("git --git-dir " + getProjectFilePath(document) + "/.git rev-parse HEAD");
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert process != null;
        return processReading(process);
    }

    private static String processReading(Process process) {
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

        String s = null;
        while (true) {
            try {
                String temp;
                if ((temp = stdInput.readLine()) == null) break;
                s = temp;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return s;
    }


    static String getProjectId(Document document) {

        Process process = null;
        String processString = "git config --file " + getProjectFilePath(document) + "/.git/config remote.origin.url";

        try {
            process = Runtime.getRuntime().exec(processString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert process != null;
        String s = processReading(process);


        assert s != null;
        String[] splittedUrl = s.split("/");

       return splittedUrl[splittedUrl.length - 1].split("\\.")[0];
    }

    static String getUserId(){

        Process process = null;
        String processString = "git config user.email";

        try {
            process = Runtime.getRuntime().exec(processString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert process != null;

        return processReading(process);
    }

    private static String getProjectFilePath(Document document){

        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project activeProject = null;
        for (Project project : projects) {
            Window window = WindowManager.getInstance().suggestParentWindow(project);
            if (window != null && window.isActive()) {
                activeProject = project;
            }
        }
        assert activeProject != null;
        return activeProject.getBasePath();
    }
}
