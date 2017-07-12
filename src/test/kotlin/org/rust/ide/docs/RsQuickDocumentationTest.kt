/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.docs

import org.intellij.lang.annotations.Language

class RsQuickDocumentationTest : RsDocumentationProviderTest() {

    fun `test fn`() = doTest("""
        /// Adds one to the number given.
        ///
        /// # Examples
        ///
        /// ```
        /// let five = 5;
        ///
        /// assert_eq!(6, add_one(5));
        /// ```
        fn add_one(x: i32) -> i32 {
            x + 1
        }

        fn main() {
            add_one(91)
             //^
        }
    """, """
        <pre>test_package</pre>
        <pre>fn <b>add_one</b>(x: i32) -&gt; i32</pre>
        <p>Adds one to the number given.</p><h1>Examples</h1><pre><code>let five = 5;

        assert_eq!(6, add_one(5));
        </code></pre>
    """)

    fun `test pub fn`() = doTest("""
        pub fn foo() {}
              //^
    """, """
        <pre>test_package</pre>
        <pre>pub fn <b>foo</b>()</pre>
    """)

    fun `test const fn`() = doTest("""
        const fn foo() {}
                 //^
    """, """
        <pre>test_package</pre>
        <pre>const fn <b>foo</b>()</pre>
    """)

    fun `test unsafe fn`() = doTest("""
        unsafe fn foo() {}
                  //^
    """, """
        <pre>test_package</pre>
        <pre>unsafe fn <b>foo</b>()</pre>
    """)

    fun `test fn in extern block`() = doTest("""
        extern {
            fn foo();
              //^
        }
    """, """
        <pre>test_package</pre>
        <pre>extern fn <b>foo</b>()</pre>
    """)

    fun `test fn in extern block with abi name`() = doTest("""
        extern "C" {
            fn foo();
              //^
        }
    """, """
        <pre>test_package</pre>
        <pre>extern "C" fn <b>foo</b>()</pre>
    """)

    fun `test extern fn`() = doTest("""
        extern fn foo() {}
                 //^
    """, """
        <pre>test_package</pre>
        <pre>extern fn <b>foo</b>()</pre>
    """)

    fun `test extern fn with abi name`() = doTest("""
        extern "C" fn foo() {}
                      //^
    """, """
        <pre>test_package</pre>
        <pre>extern "C" fn <b>foo</b>()</pre>
    """)

    fun `test generic fn`() = doTest("""
        fn foo<T: Into<String>>(t: T) {}
          //^
    """, """
        <pre>test_package</pre>
        <pre>fn <b>foo</b>&lt;T: Into&lt;String&gt;&gt;(t: T)</pre>
    """)

    fun `test generic fn with where clause`() = doTest("""
        fn foo<T>(t: T) where T: Into<String> {}
          //^
    """, """
        <pre>test_package</pre>
        <pre>fn <b>foo</b>&lt;T&gt;(t: T) where T: Into&lt;String&gt;</pre>
    """)

    fun `test complex fn`() = doTest("""
        /// Docs
        #[cfg(test)]
        /// More Docs
        pub const unsafe extern "C" fn foo<T: Into<String>, F>(t: T, f: F) where F: Ord {}
                                      //^
    """, """
        <pre>test_package</pre>
        <pre>pub const unsafe extern "C" fn <b>foo</b>&lt;T: Into&lt;String&gt;, F&gt;(t: T, f: F) where F: Ord</pre>
        <p>Docs
        More Docs</p>
    """)

    fun `test method`() = doTest("""
        struct Foo;

        impl Foo {
            pub fn foo(&self) {}
                  //^
        }
    """, """
        <pre>test_package</pre>
        <pre>impl Foo</pre>
        <pre>pub fn <b>foo</b>(&amp;self)</pre>
    """)

    fun `test generic struct method`() = doTest("""
        struct Foo<T>(T);

        impl<T> Foo<T> {
            pub fn foo(&self) {}
                  //^
        }
    """, """
        <pre>test_package</pre>
        <pre>impl&lt;T&gt; Foo&lt;T&gt;</pre>
        <pre>pub fn <b>foo</b>(&amp;self)</pre>
    """)

    fun `test generic struct method with where clause`() = doTest("""
        struct Foo<T, F>(T, F);

        impl<T, F> Foo<T, F> where T: Ord, F: Into<String> {
            pub fn foo(&self) {}
                  //^
        }
    """, """
        <pre>test_package</pre>
        <pre>impl&lt;T, F&gt; Foo&lt;T, F&gt;<br>where<br>&nbsp;&nbsp;&nbsp;&nbsp;T: Ord,<br>&nbsp;&nbsp;&nbsp;&nbsp;F: Into&lt;String&gt;,</pre>
        <pre>pub fn <b>foo</b>(&amp;self)</pre>
    """)

    fun testDifferentComments() = doTest("""
        /// Outer comment
        /// 111
        #[doc = "outer attribute"]
        /// Second outer comment
        /// 222
        #[doc = r###"second outer attribute"###]
        fn overly_documented() {
         //^
            #![doc = "inner attribute"]
            //! Inner comment
        }
    """, """
        <pre>test_package</pre>
        <pre>fn <b>overly_documented</b>()</pre>
        <p>Outer comment
        111
        outer attribute
        Second outer comment
        222
        second outer attribute
        inner attribute
        Inner comment</p>
    """)

    fun testEnumVariant() = doTest("""
        enum Foo {
            /// I am a well documented enum variant
            Bar { field: i32 },
            Baz(i32),
        }

        fn main() {
            let _ = Foo::Bar;
                       //^
        }
    """, """
        <pre>test_package::Foo::Bar</pre>
        <p>I am a well documented enum variant</p>
    """)

    fun testTraitAssocType() = doTest("""
        trait MyTrait {
            /// Documented
            type Awesome;
               //^
        }
    """, """
        <pre>test_package::Awesome</pre>
        <p>Documented</p>
    """)

    fun testTraitConst() = doTest("""
        trait MyTrait {
            /// Documented
            const AWESOME: i32;
                //^
        }
    """, """
        <pre>AWESOME</pre>
        <p>Documented</p>
    """)

    fun `test trait method`() = doTest("""
        trait MyTrait {
            /// Documented
            fn my_func();
             //^
        }
    """, """
        <pre>test_package::MyTrait</pre>
        <pre>fn <b>my_func</b>()</pre>
        <p>Documented</p>
    """)

    fun `test generic trait method`() = doTest("""
        trait MyTrait<T> {
            /// Documented
            fn my_func();
             //^
        }
    """, """
        <pre>test_package::MyTrait&lt;T&gt;</pre>
        <pre>fn <b>my_func</b>()</pre>
        <p>Documented</p>
    """)

    fun `test generic trait method with where clause`() = doTest("""
        trait MyTrait<T> where T: Into<String> {
            /// Documented
            fn my_func();
             //^
        }
    """, """
        <pre>test_package::MyTrait&lt;T&gt;</pre>
        <pre>where<br>&nbsp;&nbsp;&nbsp;&nbsp;T: Into&lt;String&gt;,</pre>
        <pre>fn <b>my_func</b>()</pre>
        <p>Documented</p>
    """)

    fun `test trait method impl`() = doTest("""
        trait Trait {
            fn foo();
        }
        struct Foo;
        impl Trait for Foo {
            fn foo() {}
              //^
        }
    """, """
        <pre>test_package</pre>
        <pre>impl Trait for Foo</pre>
        <pre>fn <b>foo</b>()</pre>
    """)

    fun `test generic trait method impl`() = doTest("""
        trait Trait<T> {
            fn foo();
        }
        struct Foo<T>(T);
        impl<T, F> Trait<T> for Foo<F> {
            fn foo() {}
              //^
        }
    """, """
        <pre>test_package</pre>
        <pre>impl&lt;T, F&gt; Trait&lt;T&gt; for Foo&lt;F&gt;</pre>
        <pre>fn <b>foo</b>()</pre>
    """)

    fun `test generic trait method impl with where clause`() = doTest("""
        trait Trait<T> {
            fn foo();
        }
        struct Foo<T>(T);
        impl<T, F> Trait<T> for Foo<F> where T: Ord, F: Into<String> {
            fn foo() {}
              //^
        }
    """, """
        <pre>test_package</pre>
        <pre>impl&lt;T, F&gt; Trait&lt;T&gt; for Foo&lt;F&gt;<br>where<br>&nbsp;&nbsp;&nbsp;&nbsp;T: Ord,<br>&nbsp;&nbsp;&nbsp;&nbsp;F: Into&lt;String&gt;,</pre>
        <pre>fn <b>foo</b>()</pre>
    """)

    fun testTraitMethodProvided() = doTest("""
        trait MyTrait {
            fn my_func() {
             //^
                //! Inner doc
            }
        }
    """, """
        <pre>test_package::MyTrait</pre>
        <pre>fn <b>my_func</b>()</pre>
        <p>Inner doc</p>
    """)

    fun testModInnerDocstring() = doTest("""
        /// *outer*
        mod foo {
            //! **inner**
            fn bar() {}
        }

        fn main() {
            foo::bar()
          //^
        }
    """, """
        <pre>test_package::foo</pre>
        <p><em>outer</em>
        <strong>inner</strong></p>
    """)

    fun testFnInnerDocstring() = doTest("""
        fn foo() {
            //! Inner doc.
        }

        fn main() {
            foo()
          //^
        }
    """, """
        <pre>test_package</pre>
        <pre>fn <b>foo</b>()</pre>
        <p>Inner doc.</p>
    """)

    fun testFileInnerDocstring() = doTest("""
//^
        //! Module level docs.

        fn foo() { }

        fn main() {
            self::foo();
        }
    """, """
        <pre>test_package</pre>
        <p>Module level docs.</p>
    """)

    fun testQualifiedName() = doTest("""
        mod q {
            /// Blurb.
            fn foo() {

            }
        }

        fn main() {
            q::foo();
                //^
        }
    """, """
        <pre>test_package::q</pre>
        <pre>fn <b>foo</b>()</pre>
        <p>Blurb.</p>
    """)

    // https://github.com/intellij-rust/intellij-rust/issues/495
    fun testIssue495() = doTest("""
        /// A cheap, reference-to-reference conversion.
        ///
        /// `AsRef` is very similar to, but different than, `Borrow`. See
        /// [the book][book] for more.
        ///
        /// [book]: ../../book/borrow-and-asref.html
        ///
        /// **Note: this trait must not fail**. If the conversion can fail, use a dedicated method which
        /// returns an `Option<T>` or a `Result<T, E>`.
        ///
        /// # Examples
        ///
        /// Both `String` and `&str` implement `AsRef<str>`:
        ///
        /// ```
        /// fn is_hello<T: AsRef<str>>(s: T) {
        ///    assert_eq!("hello", s.as_ref());
        /// }
        ///
        /// let s = "hello";
        /// is_hello(s);
        ///
        /// let s = "hello".to_string();
        /// is_hello(s);
        /// ```
        ///
        /// # Generic Impls
        ///
        /// - `AsRef` auto-dereference if the inner type is a reference or a mutable
        /// reference (eg: `foo.as_ref()` will work the same if `foo` has type `&mut Foo` or `&&mut Foo`)
        ///
        #[stable(feature = "rust1", since = "1.0.0")]
        pub trait AsRef<T: ?Sized> {
            /// Performs the conversion.
            #[stable(feature = "rust1", since = "1.0.0")]
            fn as_ref(&self) -> &T;
        }

        impl <T> AsRef<T> {}
                  //^
    """, """
        <pre>test_package::AsRef</pre>
        <p>A cheap, reference-to-reference conversion.</p><p><code>AsRef</code> is very similar to, but different than, <code>Borrow</code>. See
        <a href="../../book/borrow-and-asref.html">the book</a> for more.</p><p><strong>Note: this trait must not fail</strong>. If the conversion can fail, use a dedicated method which
        returns an <code>Option&lt;T&gt;</code> or a <code>Result&lt;T, E&gt;</code>.</p><h1>Examples</h1><p>Both <code>String</code> and <code>&amp;str</code> implement <code>AsRef&lt;str&gt;</code>:</p><pre><code>fn is_hello&lt;T: AsRef&lt;str&gt;&gt;(s: T) {
           assert_eq!(&quot;hello&quot;, s.as_ref());
        }

        let s = &quot;hello&quot;;
        is_hello(s);

        let s = &quot;hello&quot;.to_string();
        is_hello(s);
        </code></pre><h1>Generic Impls</h1><ul><li><code>AsRef</code> auto-dereference if the inner type is a reference or a mutable
        reference (eg: <code>foo.as_ref()</code> will work the same if <code>foo</code> has type <code>&amp;mut Foo</code> or <code>&amp;&amp;mut Foo</code>)</li></ul>
    """)

    fun testFnArg() = doTest("""
        fn foo(x: i32) -> i32 { x }
             //^
    """, """
        <pre>value parameter <b>x</b>: i32</pre>
    """)

    fun testVariable() = doTest("""
        fn main() {
            let x = "bar";
            println!(x);
                   //^
        }
    """, """
        <pre>variable <b>x</b>: &amp;str</pre>
    """)

    fun testGenericEnumVariable() = doTest("""
        enum E<T> {
            L,
            R(T),
        }

        fn main() {
            let x = E::R(123);
            x;
          //^
        }
    """, """
        <pre>variable <b>x</b>: E&lt;i32&gt;</pre>
    """)

    fun testGenericStructVariable() = doTest("""
        struct S<T> { s: T }

        fn main() {
            let x = S { s: 123 };
            x;
          //^
        }
    """, """
        <pre>variable <b>x</b>: S&lt;i32&gt;</pre>
    """)

    fun testTupleDestructuring() = doTest("""
        fn main() {
            let (a, b) = (1, ("foo", 1));
                  //^
        }
    """, """
        <pre>variable <b>b</b>: (&amp;str, i32)</pre>
    """)

    fun testConditionalBinding() = doTest("""
        fn main() {
            let x: (i32, f64) = unimplemented!();
            if let (1, y) = x {
                     //^
            }
        }
    """, """
        <pre>condition binding <b>y</b>: f64</pre>
    """)

    fun testStructField() = doTest("""
        struct Foo {
            /// Documented
            pub foo: i32
        }

        fn main() {
            let x = Foo { foo: 8 };
            x.foo
              //^
        }
    """, """
        <pre>test_package::Foo</pre>
        <pre>pub <b>foo</b>: i32</pre>
        <p>Documented</p>
    """)

    fun `test type parameter`() = doTest("""
        fn foo<T>() { unimplemented!() }
             //^
    """, """
        <pre>type parameter <b>T</b></pre>
    """)

    fun `test type parameter with single bound`() = doTest("""
        fn foo<T: Borrow<Q>>() { unimplemented!() }
             //^
    """, """
        <pre>type parameter <b>T</b>: Borrow&lt;Q&gt;</pre>
    """)

    fun `test type parameter with multiple bounds`() = doTest("""
        fn foo<Q, T: Eq + Hash + Borrow<Q>>() { unimplemented!() }
                //^
    """, """
        <pre>type parameter <b>T</b>: Eq + Hash + Borrow&lt;Q&gt;</pre>
    """)

    fun `test type parameter with lifetime bound`() = doTest("""
        fn foo<'a, T: 'a>() { unimplemented!() }
                 //^
    """, """
        <pre>type parameter <b>T</b>: &#39;a</pre>
    """)

    fun `test type parameter with default value`() = doTest("""
        fn foo<T = RandomState>() { unimplemented!() }
             //^
    """, """
        <pre>type parameter <b>T</b> = RandomState</pre>
    """)

    fun `test type parameter with bounds in where clause`() = doTest("""
        fn foo<Q, T>() where T: Eq + Hash + Borrow<Q> { unimplemented!() }
                //^
    """, """
        <pre>type parameter <b>T</b>: Eq + Hash + Borrow&lt;Q&gt;</pre>
    """)

    fun `test type parameter with complex bounds`() = doTest("""
        fn foo<'a, Q, T: 'a + Eq>() where T: Hash + Borrow<Q> { unimplemented!() }
                    //^
    """, """
        <pre>type parameter <b>T</b>: &#39;a + Eq + Hash + Borrow&lt;Q&gt;</pre>
    """)

    private fun doTest(@Language("Rust") code: String, @Language("Html") expected: String)
        = doTest(code, expected, RsDocumentationProvider::generateDoc)
}
