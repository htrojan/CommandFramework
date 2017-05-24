package lobbi44.kt.command.util

import org.junit.jupiter.api.Test

/**
 * Created by HT on 22.05.2017.
 */
internal class MethodSignatureTest {
    @Test
    fun matches() {
        val sig = MethodSignature(listOf(Int::class.java, Boolean::class.java))
        val method = MethodSignatureTest::class.java.methods.find { it.name == "TestMethod" }!!
        val res = sig.matches(method)
        assert(res)
    }

    fun TestMethod(arg1: Int, arg2: Boolean) {}

}