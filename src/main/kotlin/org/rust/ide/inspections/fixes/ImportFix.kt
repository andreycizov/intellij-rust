/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.fixes

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.ext.*

class ImportFix(val suggested: List<String>, val expr: RsPath) : LocalQuickFixAndIntentionActionOnPsiElement(expr) {
    override fun getFamilyName() = text

    override fun getText(): String {
        val x = suggested.joinToString(separator = "::")
        return "Import $x"
    }

    override fun invoke(project: Project, file: PsiFile, editor: Editor?, startElement: PsiElement, endElement: PsiElement) {
        val par = expr.parentOfType<RsMod>()!!

        val my_path = suggested


        val ident2 = my_path.last()

        val useOld = par.findMatchingUse(my_path.subList(0, my_path.lastIndex))
        val itemPath = useOld?.path
        val itemPathIdentifier = useOld?.path?.identifier

        if (useOld == null || itemPath == null || itemPathIdentifier == null) {
            par.addBefore(RsPsiFactory(project).createUseItem(my_path.joinToString(separator = "::")), par.children.first())
        } else {
            val useGlobList = useOld.useGlobList
            if (useGlobList != null) {
                val oldPath = itemPath.getStringPath().joinToString("::")
                val oldIdent = useGlobList.createString()
                val useCreated = RsPsiFactory(project).createUseItem("$oldPath::{$ident2, $oldIdent}")
                useOld.replace(useCreated)
            } else {
                val ident_orig = itemPathIdentifier.text
                val path = itemPath.getStringPath()

                val itemAlias = useOld.alias?.identifier?.text

                var alias = ""
                if (itemAlias != null) {
                    alias = " as $itemAlias"
                }

                val path_shortened = path.subList(0, path.lastIndex).joinToString(separator = "::")
                useOld.replace(RsPsiFactory(project).createUseItem("$path_shortened::{$ident_orig$alias, $ident2}"))
            }

        }
    }
}

private fun RsUseGlob.createString(): String {
    val alias = this.alias?.identifier?.text
    val ident = this.identifier!!.text

    if (alias != null) {
        return "$ident as $alias"
    } else {
        return ident
    }
}

private fun RsUseGlobList.createString(): String {
    return this.useGlobList.map { it.createString() }.joinToString(", ")
}

private fun RsPath.getStringPath(): List<String> {
    var r = mutableListOf<String>()

    var top_path = this

    // identifier may be null -> is it null if the
    r.add(top_path.identifier!!.text)
    while (top_path.path != null) {
        top_path = top_path.path!!
        r.add(top_path.identifier!!.text)
    }
    return r.reversed().toList()
}

private fun RsMod.findMatchingUse(path: List<String>): RsUseItem? {
    var item: RsUseItem? = null

    for (e in this.descendantsOfType<RsUseItem>()) {
        val ePath = e.path
        if (ePath != null) {
            val ePathString = ePath.getStringPath()
            if (path == ePathString) {
                item = e
                break
            } else if (e.useGlobList == null && path == ePathString.subList(0, ePathString.lastIndex)) {
                item = e
                break
            }
        }
    }

    return item
}

