package com.github.gumjoe.aigenerateddocsintellij.intentions

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.PsiMethodImpl
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElement

/** Contains a PsiMethodImpl or KtNamedFunction so that we can operate on it in a language neutral manner */
class MethodContainer(private val method: PsiElement) {

    fun getContainingClass(): UClass? {
        if (method is PsiMethodImpl) {
            return method.containingClass.toUElement(UClass::class.java)
        } else if (method is KtNamedFunction) {
            return method.containingClass().toUElement(UClass::class.java)
        }

        throw UnsupportedOperationException("Expected a PsiMethodImpl or KtNamedFunction!")
    }

    val text: String
        get() = method.text

    val addBefore = method::addBefore
    val firstChild: PsiElement = method.firstChild

    init {
        if (!(method is PsiMethodImpl || method is KtNamedFunction) ) {
            throw UnsupportedOperationException("Expected a PsiMethodImpl or KtNamedFunction!")
        }
    }

}