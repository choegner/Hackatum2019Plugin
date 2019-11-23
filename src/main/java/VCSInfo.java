
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.io.IOException;

public class VCSInfo {

    static public String getCommitId() throws IOException {
        Repository repository = buildRepo();

        return repository.getAllRefs().get("HEAD").getObjectId().toString().split("[\\[||\\]]")[1];
    }

    static public String getProjectId(){
        Repository repository = buildRepo();

        String url = repository.getConfig().getString("remote","origin", "url");
        String[] splittedUrl = url.split("/");

        return splittedUrl[splittedUrl.length-1];
    }

    static public String getUserId(){
        Repository repository = buildRepo();
        Config config = repository.getConfig();
        String email = config.getString("user", null, "email");

        return email;
    }

    static private Repository buildRepo(){
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
        Repository repository = null;
        try {
            repository = repositoryBuilder.setGitDir(new File(".git")).readEnvironment().findGitDir().setMustExist(true).build();
        } catch (IOException e){
            e.printStackTrace();
        }
        return repository;
    }
}
