import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import javax.print.Doc;
import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class VCSInfo {

    static public String getCommitId(Document document){
        Process process = null;
        try {

            process = Runtime.getRuntime().exec("git --git-dir " + getProjectFilePath(document) + "/.git rev-parse HEAD");
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

        String s = null;
        while (true) {
            try {
                String temp = null;
                if ((temp = stdInput.readLine()) == null) break;
                s = temp;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println(s);
        return s;
    }


    static public String getProjectId(Document document) {

        Process process = null;
        String processString = "git config --file " + getProjectFilePath(document) + "/.git/config remote.origin.url";
        System.out.println(processString);

        try {
            process = Runtime.getRuntime().exec(processString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

        String s = null;
        while (true) {
            try {
                String temp = null;
                if ((temp = stdInput.readLine()) == null) break;
                s = temp;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        String[] splittedUrl = s.split("/");

        String projectName = splittedUrl[splittedUrl.length - 1].split("\\.")[0];

        System.out.println(projectName);

        return projectName;
    }

    static public String getUserId(Document document){

        Process process = null;
        String processString = "git config user.email";
        System.out.println(processString);

        try {
            process = Runtime.getRuntime().exec(processString);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(process.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(process.getErrorStream()));

        String s = null;
        while (true) {
            try {
                String temp = null;
                if ((temp = stdInput.readLine()) == null) break;
                s = temp;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println(s);
        return s;
    }

    static private Repository buildRepo(String path){
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        Repository repository = null;
        try {

            repositoryBuilder.setMustExist( true );
            repositoryBuilder.setGitDir(new File(path + "/.git"));
            repository = repositoryBuilder.build();
        } catch (IOException e){
            e.printStackTrace();
        }
        return repository;
    }

    private static String getProjectFilePath(Document document){
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
        return activeProject.getBasePath();
    }
}
