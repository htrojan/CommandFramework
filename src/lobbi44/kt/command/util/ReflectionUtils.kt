package lobbi44.kt.command.util

import java.lang.reflect.Method

class MethodSignature(val parameters: List<Class<out Any>>, val returnType: Class<out Any> = Void.TYPE) {

    fun matches(m: Method): Boolean {
        var paramsMatch: Boolean = true

        parameters.forEachIndexed { index, it -> paramsMatch = paramsMatch.and(it == m.parameterTypes[index]) }
        return (m.returnType == returnType && paramsMatch)
    }

}