import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataConstants;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.actionSystem.TypedActionHandler;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.JsonUtil;

// https://intellij-support.jetbrains.com/hc/en-us/community/posts/206795775-Get-current-Project-current-file-in-editor
// https://upsource.jetbrains.com/idea-ce/file/idea-ce-e97504227f5f68c58cd623c8f317a134b6d440b5/platform/platform-api/src/com/intellij/openapi/editor/actionSystem/EditorActionHandler.java




class OwnTypedHandler extends TypedHandlerDelegate {
    String lastUrl = "";

    @Override
    public Result charTyped(char c, Project project, Editor editor, PsiFile file) {

        return Result.CONTINUE;
    }

    @NotNull
    @Override
    public Result beforeCharTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @NotNull FileType fileType) {

        VirtualFile document = FileDocumentManager.getInstance().getFile(editor.getDocument());
        String url = document.getUrl();


        if(!lastUrl.equals(url)) {
            System.out.println("Changed Dokument" + url);
            lastUrl = url;
            }


        return super.beforeCharTyped(c, project, editor, file, fileType);
    }
}