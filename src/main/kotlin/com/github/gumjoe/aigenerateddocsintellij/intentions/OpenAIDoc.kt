package com.github.gumjoe.aigenerateddocsintellij.intentions

import com.esotericsoftware.minlog.Log
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.source.PsiMethodImpl
import com.intellij.psi.impl.source.tree.java.PsiIdentifierImpl
import com.intellij.util.containers.stream
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.j2k.getContainingClass

@NonNls
class OpenAIDoc : IntentionAction, PsiElementBaseIntentionAction() {

    private val log: Logger = Logger.getInstance(OpenAIDoc::class.java)

    override fun getText(): String {
        return "Generate documentation with OpenAI";
    }

    override fun getFamilyName(): String {
        return "Documentation";
    }

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val parent = element.parent;
        // First, is the parent a method?
        if (parent != null && parent is PsiMethodImpl) {
            // Also, is there a javadoc comment?
            // If there is, do not generate!
            return parent.children.stream().noneMatch { subEl ->
                subEl is PsiComment &&
                        subEl.text.startsWith("/**")
                        && subEl.text.endsWith("*/")
            }
        }
        return false;
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        // Ok so grab method source code
        val method = element.parent as PsiMethodImpl;
        val source = method.text;

        val parent = method.containingClass
        val parentComment = parent?.docComment ?: "None provided";
        val parentName = parent?.name ?: "None provided";

        // Assemble prompt





    }

}

const val PROMPT = """
    Output in the following format: A valid, informative, factual JavaDoc comment only. Do not enclose output with backticks or place output into a codeblock. Output ONLY the documentation (do not output or repeat back any code given to you).

Enclosing Class: {{parentClass}}
Enclosing Class JavaDoc: ```
{{classComment}}
```
Method to generate JavaDoc for:
```java
{{code}}
```
"""