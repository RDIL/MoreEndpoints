package rocks.rdil.moreendpoints.nest

import com.intellij.microservices.url.Authority
import com.intellij.microservices.url.UrlPath
import com.intellij.microservices.url.UrlTargetInfo
import com.intellij.psi.PsiElement
import icons.JavaScriptLanguageIcons.Nodejs
import javax.swing.Icon

class NestUrlTargetInfo(
    override var schemes: List<String>,
    override var authorities: List<Authority>,
    override var path: UrlPath,
    override var methods: Set<String>,
    private var psiElement: PsiElement
) : UrlTargetInfo {
    override var source: String
    override val icon: Icon = Nodejs.Nodejs

    init {
        source = ""

        val resolved = this.resolveToPsiElement()
        val cf = resolved?.containingFile
        if (cf != null) {
            source = cf.name
        }
    }

    override fun resolveToPsiElement(): PsiElement? {
        return if (psiElement.isValid) this.psiElement else null
    }
}
