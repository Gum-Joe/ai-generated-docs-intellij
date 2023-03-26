package com.github.gumjoe.aigenerateddocsintellij.intentions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.PsiMethodImpl
import com.intellij.util.containers.stream
import com.theokanning.openai.completion.chat.ChatCompletionRequest
import com.theokanning.openai.completion.chat.ChatMessage
import com.theokanning.openai.service.OpenAiService
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.psi.KtNamedFunction
import javax.swing.SwingUtilities

const val OPENAI_MODEL = "gpt-3.5-turbo"
const val OPENAI_TEMPERATURE = 0.1

@NonNls
class OpenAIDoc : IntentionAction, PsiElementBaseIntentionAction() {

    private val log: Logger = Logger.getInstance(OpenAIDoc::class.java)

    override fun getText(): String {
        return "Generate documentation with OpenAI"
    }

    override fun getFamilyName(): String {
        return "Documentation"
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    /**
    * Determines if a javadoc comment can be generated for the given element.
    *
    * @param project the current project
    * @param editor the current editor (can be null)
    * @param element the element to check for a parent method and existing javadoc comment
    * @return true if a javadoc comment can be generated, false otherwise
    */
    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val parent = element.parent
        log.info(element::class.java.name)
        log.info(parent::class.java.name)
        // First, is the parent a method?
        if (parent != null && (parent is PsiMethodImpl || parent is KtNamedFunction)) {
            // Also, is there a javadoc comment?
            // If there is, do not generate!
            return parent.children.stream().noneMatch { subEl ->
                subEl is PsiComment &&
                        subEl.text.startsWith("/**")
                        && subEl.text.endsWith("*/")
            }
        }
        return false
    }

    /**
    * Generates JavaDoc documentation for a given method using OpenAI's ChatGPT model.
    *
    * @param project the current project
    * @param editor the current editor (can be null)
    * @param element the PsiElement representing the method to generate documentation for
    *
    * The method first grabs the source code of the given method and the parent class's name and documentation comment.
    * It then assembles a prompt using this information and sends it to OpenAI's ChatGPT model to generate documentation.
    * The generated documentation is then validated and added to the method as a JavaDoc comment.
    *
    * If the OpenAI API key is not specified in the environment variable OPENAI_API_KEY, an error dialog is shown.
    * If OpenAI does not return a response or the response is invalid, an error dialog is shown.
    */
    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        // Setup - grab method (which was already checked) and source code

        val method = MethodContainer(element.parent)
        val sourceCode = method.text

        // Grab parent for OpenAI to use as context!
        val parent = method.getContainingClass()
        val parentComment = parent?.docComment?.text ?: "None provided"
        val parentName = parent?.name ?: "None provided"

        // Assemble prompt
        val prompt = getPrompt(parentName, parentComment, sourceCode)

        // Load OpenAI
        val openAiToken = System.getenv("OPENAI_API_KEY")
        if (openAiToken == null) {
            Messages.showErrorDialog(
                "Please specify your OpenAI API key in the environment variable OPENAI_API_KEY",
                "Plugin Error"
            )
            return
        }
        val service = OpenAiService(openAiToken)

        // Create request to OpenAI. We use ChatGPT as our responder.
        val completionRequest = ChatCompletionRequest.builder()
            .temperature(OPENAI_TEMPERATURE)
            .model(OPENAI_MODEL)
            .messages(
                listOf(
                    ChatMessage("user", prompt)
                )
            )
            .build()

        // Run our request to OpenAI in a progress manager
        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(project, "Generating documentation", true) {
                override fun run(progressIndicator: ProgressIndicator) {
                    progressIndicator.isIndeterminate = true
                    progressIndicator.text = "Generating documentation..."

                    log.debug("Requesting documentation completion from OpenAI...")
                    // Actually get the documentation
                    var response =
                        service.createChatCompletion(completionRequest).choices[0]?.message?.content?.toString()
                    if (response == null) {
                        SwingUtilities.invokeLater {
                            Messages.showErrorDialog(
                                "OpenAI did not return a response!",
                                "OpenAI Error"
                            )
                        }
                        return
                    }

                    // If response enclosed within backticks, extract it!
                    if ("^\\s*```(.*)```\\s*$".toRegex(RegexOption.DOT_MATCHES_ALL) matches response) {
                        response = response.trim().drop(3).dropLast(3)
                    }

                    // validate - did we actually get valid JavaDoc?
                    if (!("^\\s*/\\*\\*(.*)\\*/\\s*$".toRegex(RegexOption.DOT_MATCHES_ALL) matches response)) {
                        SwingUtilities.invokeLater {
                            Messages.showErrorDialog(
                                "OpenAI response was invalid. Got back $response",
                                "OpenAI Error"
                            )
                        }
                        return
                    }

                    // Have to wrap like this so that we can actually write to the source code.
                    WriteCommandAction.runWriteCommandAction(project) {
                        val factory = JavaPsiFacade.getElementFactory(project)
                        val docComment = factory.createDocCommentFromText(response)
                        method.addBefore(
                            docComment,
                            method.firstChild
                        )
                    }

                    // DONE!
                    progressIndicator.fraction = 1.0
                }
            })


        // Done!


    }

    /**
     * The actual prompt we use for ChatGPT
     */
    private fun getPrompt(parentClass: String, classComment: String, code: String) = run {
        """Output in the following format: A valid, informative, factual JavaDoc comment only. Do not enclose output with backticks or place output into a codeblock. Output ONLY the documentation (do not output or repeat back any code given to you).

Enclosing Class: $parentClass
Enclosing Class JavaDoc: ```
$classComment
```
Method to generate JavaDoc for:
```
$code
```
"""
    }

}