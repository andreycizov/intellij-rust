/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.structure

import com.intellij.ide.structureView.impl.common.PsiTreeElementBase
import org.rust.lang.core.psi.RsStructItem
import org.rust.lang.core.psi.ext.namedFields

class RsStructTreeElement(element: RsStructItem) : PsiTreeElementBase<RsStructItem>(element) {

    override fun getPresentableText() = element?.name

    override fun getChildrenBase() = (element?.namedFields ?: emptyList()).map(::RsBaseTreeElement)

}
