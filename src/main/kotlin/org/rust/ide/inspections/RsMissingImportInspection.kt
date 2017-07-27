/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.stubs.StubIndex
import org.rust.ide.inspections.fixes.ImportFix
import org.rust.lang.core.psi.*
import org.rust.lang.core.psi.ext.*
import org.rust.lang.core.stubs.index.RsNamedElementIndex
import org.rust.lang.core.types.ty.TyPrimitive
import java.util.HashSet

class RsMissingImportInspection : RsLocalInspectionTool() {
    override fun getDisplayName() = "Missing import"

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        object : RsVisitor() {
            override fun visitPath(o: RsPath) {
                if (o !is RsPath) {
                    return
                }

                if (o.rootPath() != o) {
                    return
                }

                if (o.isPrimitive() || o.isResolvable()) return

                // TODO: now check if the root path is not a primitive!!!

                holder.registerProblem(o.navigationElement, "Missing import", *(o.findMatchingNamedElements().map { it.findPubPath(o) }.filter { it != null }.map { it!! }.map { ImportFix(it, o) }).toTypedArray())
            }
        }
}

private fun RsPath.isPrimitive(): Boolean {
    return TyPrimitive.fromPath(this) != null
}

private fun RsPath.isResolvable(): Boolean {
    return this.reference.resolve() != null
}

private fun RsPath.rootPath(): RsPath {
    var top_path = this
    while (top_path.path != null) {
        top_path = top_path.path!!
    }
    return top_path
}


private fun RsPath.findMatchingNamedElements(): Collection<RsNamedElement> {
    return StubIndex.getElements(
        RsNamedElementIndex.KEY,
        this.rootPath().identifier!!.text, project, null,
        RsNamedElement::class.java)
}

private fun RsNamedElement.getPubUse(): RsMod? {
    // So, cases

    // 1) we're operating on a struct
    //   1) the struct is in crate root, so it's visibility depends on it's visibility
    //   2) the struct is not in crate root, it's visibility depends on it's visibility and
    //      1) There is either a use of it's module with pub
    //      2) There is either a use of this struct in one of the parent modules
    // 2) we're operating on a mod
    //   1) the mod is in crate root, so it's visibility depends on it's visibility in crate root's definition
    //   2) the mod is not in crate root, so it's visibility depends on
    //      1) There is either a pub mod of this module
    //      2) Or there is a pub use of this module

    // Crate root may also contain items!

    // We do not differentiate between "NO PARENT" and "NOT PUBLIC"

    if (this is RsMod) {
        if (this.isCrateRoot) {
            // Crate roots are always public
            return this
        } else {
            // Needs to have a parent

            return this.findParentReferencesTo(this)
        }
    } else if (this is RsExternCrateItem) {
        val ref = this.reference.resolve()
        if (ref is RsMod) {
            return ref.getPubUse()
        }
    } else if (this is RsVisibilityOwner) {
        if (this.isPublic) {

            // This may be defined in the crate root.
            val mod = this.parentOfType<RsMod>()!!

            val pubUse = mod.getPubUse()

            if (pubUse != null) {
                return mod
            }

            // 1) Our parent is not a crate
            // 2) Our parent is not publicly available
            // -> Our parent needs to have a parent

            return mod.findParentReferencesTo(this)
        }
    }

    return null
}

private fun RsMod.findParentReferencesTo(o: RsNamedElement): RsMod? {
    var mod = this.`super`

    while (mod != null) {
        for (modChild in mod.children) {
            if (modChild is RsUseItem && modChild.isPublic) {
                val path = modChild.path
                val useItems = modChild.useGlobList

                if (path != null && path.isReferenceTo(this) && useItems != null) {
                    if (useItems.useGlobList.map { it.isReferenceTo(o) }.fold(false) { a, b -> a || b }) {
                        return mod
                    }
                } else if (path != null && path.isReferenceTo(this) && modChild.isStarImport) {
                    return mod
                } else if (path != null && path.isReferenceTo(o)) {
                    return mod
                }

            } else if (modChild is RsModItem && modChild.isPublic && modChild == this) {
                return mod
            } else if (modChild is RsModDeclItem && modChild.isPublic && modChild.isReferenceTo(o)) {
                return mod
            }
        }

        mod = mod.`super`
    }

    return null
}

private fun RsReferenceElement.isReferenceTo(x: RsNamedElement): Boolean {
    return PsiReferenceService.getService().getReferences(this, PsiReferenceService.Hints(x, null)).map {
        it.isReferenceTo(x)
    }.fold(false) { a, b -> a || b }
}

val RsNamedElement.superUsePubMods: List<RsMod> get() {
    // For malformed programs, chain of `super`s may be infinite
    // because of cycles, and we need to detect this situation.
    val visited = HashSet<RsMod>()
    return generateSequence(this.getPubUse()) { it.getPubUse() }
        .takeWhile { visited.add(it) }
        .toList()
}


private fun RsNamedElement.findPubPath(against: RsPath): List<String>? {
    val supMods = this.superUsePubMods.reversed().toList()
    val containingCargoPackageName = this.containingCargoPackage?.name

    if(this is RsExternCrateItem && containingCargoPackageName != null){
        if(containingCargoPackageName == against.containingCargoPackage!!.name) {
            return listOf(this.referenceName)
        }
    } else if(!supMods.isEmpty() && containingCargoPackageName != null) {
        val first = supMods[0]

        // mod defined in `main` receives a global project-wide scope
        if(first.isCrateRoot || (first.`super` != null && first.`super`!!.modName == "main")) {
            val r = mutableListOf<String>()
            r.add(containingCargoPackageName)
            r.addAll(supMods.drop(1).map { it.modName!! })
            r.add(this.name!!)

            return r
        }
    }

    return null
}
