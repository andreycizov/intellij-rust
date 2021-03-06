/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty

import com.intellij.openapi.project.Project

data class TyFunction(val paramTypes: List<Ty>, val retType: Ty) : Ty {

    override fun canUnifyWith(other: Ty, project: Project, mapping: TypeMapping?): Boolean = merge(mapping) {
        other is TyFunction && paramTypes.size == other.paramTypes.size &&
            paramTypes.zip(other.paramTypes).all { (type1, type2) -> type1.canUnifyWith(type2, project, it) } &&
            retType.canUnifyWith(other.retType, project, it)
    }

    override fun toString(): String {
        val params = paramTypes.joinToString(", ", "fn(", ")")
        return if (retType === TyUnit) params else "$params -> $retType"
    }

    override fun substitute(subst: Substitution): TyFunction =
        TyFunction(paramTypes.map { it.substitute(subst) }, retType.substitute(subst))
}
