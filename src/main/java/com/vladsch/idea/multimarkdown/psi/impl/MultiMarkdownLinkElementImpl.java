/*
 * Copyright (c) 2015-2015 Vladimir Schneider <vladimir.schneider@gmail.com>, all rights reserved.
 *
 * This code is private property of the copyright holder and cannot be used without
 * having obtained a license or prior written permission of the of the copyright holder.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package com.vladsch.idea.multimarkdown.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.vladsch.idea.multimarkdown.MultiMarkdownPlugin;
import com.vladsch.idea.multimarkdown.MultiMarkdownProjectComponent;
import com.vladsch.idea.multimarkdown.psi.MultiMarkdownLinkElement;
import com.vladsch.idea.multimarkdown.psi.MultiMarkdownVisitor;
import com.vladsch.idea.multimarkdown.util.GitHubVcsRoot;
import com.vladsch.idea.multimarkdown.util.PathInfo;
import com.vladsch.idea.multimarkdown.util.StringUtilKt;
import com.vladsch.idea.multimarkdown.util.WikiLinkRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MultiMarkdownLinkElementImpl  extends ASTWrapperPsiElement implements MultiMarkdownLinkElement {
    public static String getElementLinkRefWithAnchor(@NotNull String linkRef, @Nullable String linkAnchor) {
        return linkRef.replace("#", "%23") + StringUtilKt.prefixWith(linkAnchor, '#');
    }

    public static String getElementLinkRefWithAnchor(@NotNull String linkRefWithAnchor) {
        int anchorPos = linkRefWithAnchor.indexOf('#');
        if (anchorPos == 0) {
            return getElementLinkRefWithAnchor("", linkRefWithAnchor);
        } else if (anchorPos > 0) {
            String linkRef = linkRefWithAnchor.substring(0, anchorPos);
            String linkAnchor = linkRefWithAnchor.substring(anchorPos);
            return getElementLinkRefWithAnchor(linkRef, linkAnchor);
        } else {
            return getElementLinkRefWithAnchor(linkRefWithAnchor, null);
        }
    }

    public MultiMarkdownLinkElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public String getDisplayName() {
        return getText();
    }

    @Override
    public String getLinkText() {
        return MultiMarkdownPsiImplUtil.getLinkText(this);
    }

    @Override
    public String getLinkRef() {
        return MultiMarkdownPsiImplUtil.getLinkRefText(this);
    }

    @Override
    public String getLinkTitle() {
        return MultiMarkdownPsiImplUtil.getLinkTitle(this);
    }

    @Override
    public String getLinkAnchor() {
        return MultiMarkdownPsiImplUtil.getLinkAnchor(this);
    }

    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof MultiMarkdownVisitor) visitor.visitElement(this);
        else super.accept(visitor);
    }

    @NotNull
    @Override
    public String getMissingElementNameSpace(@NotNull String prefix, boolean addLinkRef) {
        MultiMarkdownProjectComponent projectComponent = MultiMarkdownPlugin.getProjectComponent(getProject());
        PsiFile psiFile = getContainingFile();
        VirtualFile virtualFile = psiFile.getOriginalFile() != null ? psiFile.getOriginalFile().getVirtualFile() : psiFile.getVirtualFile();
        PathInfo filePathInfo = new PathInfo(virtualFile);
        GitHubVcsRoot gitHubVcsRoot = projectComponent != null ? projectComponent.getGitHubRepo(filePathInfo.getPath()) : null;
        String vcsHome = gitHubVcsRoot != null ? gitHubVcsRoot.getBasePath() + "::" : "";

        if (addLinkRef) {
            String pageRef = MultiMarkdownPsiImplUtil.getLinkRefTextWithAnchor(this);
            if (pageRef.isEmpty()) pageRef = WikiLinkRef.convertFileToLink(filePathInfo.getFileName());
            return prefix + (vcsHome.isEmpty() ? vcsHome : vcsHome + "::") + (pageRef.isEmpty() ? pageRef : pageRef + "::");
        }
        return prefix + (vcsHome.isEmpty() ? vcsHome : vcsHome + "::");
    }
}
