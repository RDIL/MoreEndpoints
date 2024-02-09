package rocks.rdil.moreendpoints.nest

import com.intellij.javascript.nodejs.PackageJsonData
import com.intellij.javascript.nodejs.packageJson.PackageJsonFileManager
import com.intellij.lang.javascript.*
import com.intellij.lang.javascript.psi.JSLiteralExpression
import com.intellij.lang.javascript.psi.ecma6.TypeScriptClass
import com.intellij.lang.javascript.psi.ecma6.impl.TypeScriptFunctionImpl
import com.intellij.lang.javascript.psi.impl.JSReferenceExpressionImpl
import com.intellij.lang.javascript.psi.stubs.JSFrameworkMarkersIndex.Companion.getElements
import com.intellij.lang.typescript.psi.impl.ES6DecoratorImpl
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.DumbService.Companion.isDumb
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import java.lang.IllegalStateException
import java.util.*

private fun removeStringWrapping(text: String): String {
    return text
        .removeSurrounding("'")
        .removeSurrounding("\"")
        .removeSurrounding("`")
}

internal object NestModel {
    @JvmStatic
    var VERBS = arrayOf("All", "Get", "Post", "Put", "Delete", "Patch", "Options", "Head")

    @JvmStatic
    val LOGGER = Logger.getInstance(NestModel::class.java)

    fun getEndpointGroups(project: Project, scope: GlobalSearchScope): Collection<PsiFile> {
        if (!hasNestJs(project)) {
            return Collections.emptyList()
        }

        val destination = ArrayList<PsiFile>()
        getEndpointsCached(project).forEach { endpoint ->
            var containingFile = endpoint.pointer.containingFile

            containingFile = if (containingFile?.let { scope.contains(it.virtualFile) } == true) {
                containingFile
            } else null

            if (containingFile != null) {
                destination.add(containingFile)
            }
        }

        return destination.distinct()
    }

    fun hasNestJs(project: Project): Boolean {
        val packageJsons = PackageJsonFileManager.getInstance(project).validPackageJsonFiles

        if (packageJsons.isEmpty()) {
            return false
        }

        for (packageJson in packageJsons) {
            val packageJsonData = PackageJsonData.getOrCreate(packageJson)
            val dependencies = arrayOf("@nestjs/core")

            if (packageJsonData.containsOneOfDependencyOfAnyType(*dependencies)) {
                return true
            }
        }

        return false
    }

    /*
    TS:
    @Post()
    test() {}

    PSI structure (with depths):
    // TypeScriptFunction:test             FUNCTION 1
    //   JSAttributeList                   DECORATORS 2
    //     ES6Decorator                    DECORATOR 3
    //       PsiElement(JS:AT)             @ 4
    //       JSCallExpression              4
    //         JSReferenceExpression       Post 5
    //           PsiElement(JS:IDENTIFIER) Post 6
    //         JSArgumentList              () 5
    //           PsiElement(JS:LPAR)       ( 7
    //           PsiElement(JS:RPAR)       ) 7
     */

    private fun tryCreateMappingFor(expr: ES6DecoratorImpl): NestMapping? {
        val qualifierElement = JSReferenceExpressionImpl.getQualifierNode(expr.node)

        val verbText = qualifierElement?.firstChildNode?.text ?: return null

        val callExpr = expr.node.findChildByType(JSElementTypes.CALL_EXPRESSION)
        val params = callExpr?.findChildByType(JSElementTypes.ARGUMENT_LIST)
        var pathName: String? = null
        var pathPsi: JSLiteralExpression? = null

        params?.findChildByType(JSElementTypes.LITERAL_EXPRESSION)?.let {
            pathName = removeStringWrapping(it.text)
            try {
                pathPsi = it.psi as JSLiteralExpression?
            } catch (e: Exception) {
                // oh no! anyway
            }
        }

        // fall back to the method name if the path is not specified
        pathName = pathName ?: PsiTreeUtil.getParentOfType(expr, TypeScriptFunctionImpl::class.java)?.name

        if (pathName == null) {
            throw IllegalStateException("Path name is null - this should not happen")
        }

        // get containing controller
        var controller: NestController? = null
        val parent = PsiTreeUtil.getParentOfType(expr, TypeScriptClass::class.java)

        if (parent !== null) {
            val parentAttributes = parent.attributeList
            val controllerAnnotation = parentAttributes?.decorators?.find {
                it.decoratorName == "Controller"
            }

            val string = PsiTreeUtil.findChildOfType(controllerAnnotation, JSLiteralExpression::class.java)

            string?.text?.let {
                controller = NestController(removeStringWrapping(it), null)
            }
        }

        return NestMapping(
            SmartPointerManager.createPointer(expr),
            if (pathPsi !== null) SmartPointerManager.createPointer(pathPsi!!) else null,
            controller,
            verbText,
            pathName!!
        )
    }

    private fun getJsSearchScope(project: Project): GlobalSearchScope {
        val types = arrayOf<FileType>(JavaScriptFileType.INSTANCE, TypeScriptFileType.INSTANCE)
        return GlobalSearchScope.getScopeRestrictedByFileTypes(GlobalSearchScope.allScope(project), *types)
    }

    private fun getLiterals(project: Project): Collection<ES6DecoratorImpl> {
        return if (isDumb(project)) {
            emptySet()
        } else {
            getElements(NestFrameworkHandler.KEY, ES6DecoratorImpl::class.java, project, getJsSearchScope(project))
        }
    }

    private fun getMappings(project: Project): Collection<NestMapping> {
        val destination = ArrayList<NestMapping>()

        val literals = getLiterals(project)

        literals.forEach {
            val mapping = tryCreateMappingFor(it)

            if (mapping != null) {
                destination.add(mapping)
            } else {
                LOGGER.warn("Failed to create mapping for $it")
            }
        }

        return destination
    }

    fun getEndpoints(project: Project, file: PsiFile): Collection<NestMapping> {
        val forProject = getEndpointsCached(project)

        return forProject.filter { it.pointer.containingFile == file }
    }

    fun getEndpoints(project: Project): Collection<NestMapping> {
        return getEndpointsCached(project)
    }

    private fun getEndpointsCached(project: Project): Collection<NestMapping> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            CachedValueProvider.Result.create(
                getMappings(project),
                PsiManager.getInstance(project).modificationTracker.forLanguage(JavascriptLanguage.INSTANCE)
            )
        }
    }
}
