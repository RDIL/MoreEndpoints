package rocks.rdil.moreendpoints.nest

import com.intellij.lang.ASTNode
import com.intellij.lang.javascript.JSElementTypes
import com.intellij.lang.javascript.JSTokenTypes
import com.intellij.lang.javascript.index.FrameworkIndexingHandler
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl
import com.intellij.psi.PsiElement

class NestFrameworkHandler : FrameworkIndexingHandler() {
    override fun shouldCreateStubForLiteral(node: ASTNode): Boolean {
        return this.hasSignificantValue(node)
    }

    override fun hasSignificantValue(expression: JSLiteralExpression): Boolean {
        val decorator = expression.node?.treeParent?.treeParent?.treeParent ?: return false

        return this.hasSignificantValue(decorator)
    }

    // this function will capture routes like @Get("id")
    private fun hasSignificantValue(maybeDecorator: ASTNode?): Boolean {
        if (maybeDecorator == null) {
            return false
        }

        if (maybeDecorator.firstChildNode?.elementType != JSTokenTypes.AT || maybeDecorator.elementType !== JSElementTypes.ES6_DECORATOR) {
            return false
        }

        val qualifierElement = JSReferenceExpressionImpl.getQualifierNode(maybeDecorator) ?: return false

        val refText = qualifierElement.firstChildNode?.text
        return NestModel.VERBS.contains(refText)
    }

    override fun getMarkers(elementToIndex: PsiElement): MutableList<String> {
        val significant = this.hasSignificantValue(elementToIndex.node)

        return if (significant) {
            mutableListOf(KEY)
        } else mutableListOf()
    }

    companion object {
        const val KEY: String = "NestJS"
    }
}
