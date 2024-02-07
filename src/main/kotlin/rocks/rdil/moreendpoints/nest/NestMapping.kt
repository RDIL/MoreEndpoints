package rocks.rdil.moreendpoints.nest

import com.intellij.lang.typescript.psi.impl.ES6DecoratorImpl
import com.intellij.psi.SmartPsiElementPointer

/**
 * A mapping of a NestJS endpoint to its URL.
 *
 * @property pointer A pointer to the decorator that defines the endpoint.
 * @property parent The parent mapping, if any.
 * @property method The HTTP verb used by the endpoint.
 * @property path The path of the endpoint.
 */
class NestMapping(
    val pointer: SmartPsiElementPointer<ES6DecoratorImpl>,
    private val parent: NestController?,
    val method: String,
    private val path: String
) {
    /**
     * The name of the source file of the endpoint.
     */
    val source: String

    override fun toString(): String {
        return "NestMapping(pointer=${pointer}, parent=${parent}, method=${method}, path=${path}, source=${source})"
    }

    init {
        val f = pointer.containingFile
        this.source = f?.name ?: ""
    }

    fun getFullPath(): String {
        val base = parent?.getBaseUrl() ?: "/"

        return (base + path).replace("//", "/")
    }
}
