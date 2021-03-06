/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing

class RsQuoteHandlerTest : RsTypingTestBase() {
    override val dataPath = "org/rust/ide/typing/quoteHandler/fixtures"

    fun `test don't complete char quotes`() = doTestByText("""
        fn main() {
            <caret>
        }
    """, """
        fn main() {
            '<caret>
        }
    """, '\'')

    fun `test complete byte quotes`() = doTestByText("""
        fn main() {
            b<caret>
        }
    """, """
        fn main() {
            b'<caret>'
        }
    """, '\'')

    fun `test byte literal with single quote before`() = doTestByText("""
        b'<caret>
    """, """
        b''<caret>
    """, '\'')

    fun `test byte literal with single quote after`() = doTestByText("""
        b<caret>'
    """, """
        b'<caret>
    """, '\'')

    fun `test complete string quotes`() = doTestByText("""
        <caret>
    """, """
        "<caret>"
    """, '"')

    fun `test complete byte string quotes`() = doTestByText("""
        b<caret>
    """, """
        b"<caret>"
    """, '"')

    fun `test complete raw string quotes with hashes`() = doTestByText("""
        r###<caret>
    """, """
        r###"<caret>"###
    """, '"')

    fun `test complete raw string quotes no hashes`() = doTestByText("""
        r<caret>
    """, """
        r"<caret>"
    """, '"')

    fun `test complete raw byte string quotes with hashes`() = doTestByText("""
        r###<caret>
    """, """
        r###"<caret>"###
    """, '"')

    fun `test complete raw byte string quotes no hashes`() = doTestByText("""
        r<caret>
    """, """
        r"<caret>"
    """, '"')

    // https://github.com/intellij-rust/intellij-rust/issues/687
    fun `test double quote in raw string`() = doTestByText("""
        r###"Hello, <caret> World!"###
    """, """
        r###"Hello, "<caret> World!"###
    """, '"')

    fun `test single quote in raw string`() = doTestByText("""
        r###"Hello, <caret> World!"###
    """, """
        r###"Hello, '<caret> World!"###
    """, '\'')

    fun `test double quote in empty raw string`() = doTestByText("""
        r#"<caret>"#
    """, """
        r#""<caret>"#
    """, '"')

    fun `test single quote in empty raw string`() = doTestByText("""
        r#"<caret>"#
    """, """
        r#"'<caret>"#
    """, '\'')
}
