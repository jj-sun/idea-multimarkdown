/*
 * Copyright (c) 2015-2015 Vladimir Schneider <vladimir.schneider@gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.vladsch.idea.multimarkdown.language;

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import com.intellij.util.NullableFunction;
import com.vladsch.idea.multimarkdown.MultiMarkdownBundle;
import com.vladsch.idea.multimarkdown.MultiMarkdownIcons;
import com.vladsch.idea.multimarkdown.psi.MultiMarkdownFile;
import com.vladsch.idea.multimarkdown.psi.MultiMarkdownWikiPageRef;
import com.vladsch.idea.multimarkdown.psi.impl.MultiMarkdownReferenceWikiPageRef;
import com.vladsch.idea.multimarkdown.util.FilePathInfo;
import com.vladsch.idea.multimarkdown.util.FileReference;
import com.vladsch.idea.multimarkdown.util.FileReferenceLink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;

public class MultiMarkdownLineMarkerProvider extends RelatedItemLineMarkerProvider {
    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, Collection<? super RelatedItemLineMarkerInfo> result) {
        if (element instanceof MultiMarkdownWikiPageRef) {
            PsiReference psiReference = element.getReference();
            //MultiMarkdownFile[] markdownFiles = MultiMarkdownPlugin.getProjectComponent(element.getProject()).getFileReferenceList().query()
            //        .matchWikiRef((MultiMarkdownWikiPageRef) element)
            //        .accessibleWikiPageFiles()
            //        ;

            ResolveResult[] results = ((MultiMarkdownReferenceWikiPageRef) psiReference) != null ? ((MultiMarkdownReferenceWikiPageRef) psiReference).multiResolve(false) : null;
            if (results != null && results.length > 0) {
                final PsiFile containingFile = element.getContainingFile();

                NullableFunction<PsiElement, String> namer = new NullableFunction<PsiElement, String>() {
                    @Nullable
                    @Override
                    public String fun(PsiElement element) {
                        return new FileReferenceLink(containingFile, (PsiFile) element).getLinkRef();
                    }
                };

                final Project project = element.getProject();
                final String basePath = project.getBasePath() == null ? "/" : project.getBasePath();
                boolean showWikiHome = false;
                String lastWikiHome = null;

                if (results.length > 0) {
                    ArrayList<MultiMarkdownFile> markdownTargets = new ArrayList<MultiMarkdownFile>();
                    Icon icon = null;
                    for (ResolveResult resolveResult : results) {
                        if (resolveResult.getElement() instanceof MultiMarkdownFile && resolveResult.getElement() != containingFile) {
                            MultiMarkdownFile file = (MultiMarkdownFile) resolveResult.getElement();
                            if (icon == null) {
                                icon = file.isWikiPage() ? MultiMarkdownIcons.WIKI : MultiMarkdownIcons.FILE;
                            }

                            FilePathInfo pathInfo = new FilePathInfo(file.getVirtualFile());
                            if (lastWikiHome == null) {
                                lastWikiHome = pathInfo.getWikiHome();
                            } else if (!showWikiHome && !lastWikiHome.equals(pathInfo.getWikiHome())) {
                                showWikiHome = true;
                            }
                            markdownTargets.add(file);
                        }
                    }

                    if (markdownTargets.size() > 0) {
                        final boolean showContainer = showWikiHome;
                        PsiElementListCellRenderer cellRenderer = new PsiElementListCellRenderer() {
                            @Override
                            public String getElementText(PsiElement element) {
                                if (element instanceof MultiMarkdownFile) {
                                    FileReferenceLink referenceLink = new FileReferenceLink(containingFile, (PsiFile) element);
                                    return referenceLink.getLinkRef();
                                }

                                return "<unknown>";
                            }

                            @Nullable
                            @Override
                            protected String getContainerText(PsiElement element, String name) {
                                if (showContainer && element instanceof MultiMarkdownFile && ((MultiMarkdownFile) element).isWikiPage()) {
                                    FileReference fileReference = new FileReference((PsiFile) element);
                                    String wikiHome = fileReference.getWikiHome();
                                    FileReferenceLink referenceLink = new FileReferenceLink(basePath + "/dummy", wikiHome, project);
                                    return referenceLink.getLinkRef();
                                }
                                return null;
                            }
                            @Override
                            protected int getIconFlags() {
                                return 0;
                            }
                        };

                        if (icon == null) icon = MultiMarkdownIcons.FILE;

                        NavigationGutterIconBuilder<PsiElement> builder =
                                NavigationGutterIconBuilder.create(icon)
                                        .setCellRenderer(cellRenderer)
                                        .setTargets(markdownTargets)
                                        .setNamer(namer)
                                        .setTooltipText(MultiMarkdownBundle.message("linemarker.navigate-to"));

                        result.add(builder.createLineMarkerInfo(element));
                    }
                }
            }
        }
    }
}