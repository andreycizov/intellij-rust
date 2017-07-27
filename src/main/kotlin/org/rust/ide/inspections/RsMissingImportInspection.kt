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
import org.rust.lang.core.psi.impl.RsUseItemImpl
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

                holder.registerProblem(o.navigationElement, "Missing import", *(o.findMatchingNamedElements().map { it.findPubPath() }.filter { it != null }.map { it!! }.map { ImportFix(it, o) }).toTypedArray())
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
    var mod: RsMod? = null
    var refMod: RsMod? = null

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

    if (this is RsMod) {
        if (this.isCrateRoot || (this.`super` != null && this.`super`!!.modName!! == "main")) {
            // Crate roots are always public
            return this
        } else {
            // Needs to have a parent
            mod = this.`super`
            refMod = this
        }
    } else if (this is RsVisibilityOwner) {
        if (this.isPublic) {
            // This may be defined in the crate root.
            mod = this.parentOfType<RsMod>()!!

            val pubUse = mod.getPubUse()

            if (pubUse != null) {
                return mod
            }

            // 1) Our parent is not a crate
            // 2) Our parent is not publicly available
            // -> Our parent needs to have a parent

            refMod = mod
            mod = mod.`super`
        } else {
            return null
        }
    } else {
        return null
    }

    refMod = refMod!!

    while (mod != null) {
        for (modChild in mod.children) {
            if (modChild is RsUseItem && modChild.isPublic) {
                val path = modChild.path
                val useItems = modChild.useGlobList

                if (path != null && path.isReferenceTo(refMod) && useItems != null) {
                    if (useItems.useGlobList.map { it.isReferenceTo(this) }.fold(false) { a, b -> a || b }) {
                        return mod
                    }
                } else if (path != null && path.isReferenceTo(refMod) && modChild.isStarImport) {
                    return mod
                } else if (path != null && path.isReferenceTo(this)) {
                    return mod
                }

            } else if (modChild is RsModItem && modChild.isPublic && modChild == refMod) {
                return mod
            } else if (modChild is RsModDeclItem && modChild.isPublic && modChild.isReferenceTo(refMod)) {
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


private fun RsNamedElement.findPubPath(): List<String>? {
    // 1) RsMod -> may be root (doesn't have a path)
    // 2) RsXXX

    // 1) This may not return the "lib" root
    // 2) This may not return the full path therefore we can not yet remove the unavailable paths.


    val supMods = this.superUsePubMods.reversed().toList()

    val r = mutableListOf<String>()

    val pkg = if (this.containingCargoPackage != null) this.containingCargoPackage!!.name else "core"

    r.add("__${pkg}")
    r.addAll(supMods.map { it.modName!! })
    r.add("${this.name!!}__")
    r.add("__${this}__")

    return r

    if (supMods.isEmpty()) {
        return null
    }

    val cargoPackageName = this.containingCargoPackage?.name
    val y = this.name!!



    r.addAll(supMods.map { it.modName!! })

    if (supMods[0].isCrateRoot) {
        r.removeAt(0)

        if (supMods[0].modName == "lib" && cargoPackageName != null) {
            r.add(0, cargoPackageName)
        } else if (cargoPackageName == null) {
            r.add(0, "PKG_UNK")
        }
    }

    r.add(y)

//    val pubUse = this.getPubUse()
//
//    val pubUseStr = if (pubUse != null) "__${pubUse.modName!!}__" else "__null__"
//    r.add(pubUseStr)

    return r
}
