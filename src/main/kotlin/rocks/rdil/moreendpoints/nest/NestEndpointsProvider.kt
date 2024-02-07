package rocks.rdil.moreendpoints.nest

import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.microservices.endpoints.*
import com.intellij.microservices.endpoints.presentation.HttpMethodPresentation
import com.intellij.microservices.url.UrlPath
import com.intellij.microservices.url.UrlTargetInfo
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import icons.JavaScriptLanguageIcons.Nodejs
import com.intellij.microservices.url.Authority

class NestEndpointsProvider : EndpointsUrlTargetProvider<PsiFile, NestMapping> {
    override val endpointType: EndpointType = HTTP_SERVER_TYPE
    override val presentation: FrameworkPresentation = FrameworkPresentation("NestJS", "NestJS", Nodejs.Nodejs)

    override fun getEndpointGroups(project: Project, filter: EndpointsFilter): Iterable<PsiFile> {
        if (!NestModel.hasNestJs(project)) {
            return emptyList()
        }

        if (filter !is SearchScopeEndpointsFilter) {
            return emptyList()
        }

        return NestModel.getEndpointGroups(project, filter.transitiveSearchScope)
    }

    override fun getModificationTracker(project: Project): ModificationTracker {
        return PsiManager.getInstance(project).modificationTracker.forLanguage(JavascriptLanguage.INSTANCE)
    }

    override fun getStatus(project: Project): EndpointsProvider.Status {
        return when {
            !NestModel.hasNestJs(project) -> EndpointsProvider.Status.UNAVAILABLE
            else -> EndpointsProvider.Status.HAS_ENDPOINTS
        }
    }

    override fun getUrlTargetInfo(group: PsiFile, endpoint: NestMapping): Iterable<UrlTargetInfo> {
        return listOf(
            NestUrlTargetInfo(
                // TODO: nest supports others!
                listOf("http", "https"),
                listOf(Authority.Placeholder()),
                UrlPath.fromExactString(endpoint.getFullPath()),
                setOf(endpoint.method),
                endpoint.pointer.element!!
            )
        )
    }

    override fun isValidEndpoint(group: PsiFile, endpoint: NestMapping): Boolean {
        return endpoint.pointer.element?.isValid ?: false
    }

    override fun getEndpoints(group: PsiFile): Iterable<NestMapping> {
        return NestModel.getEndpoints(group.project, group)
    }

    override fun getEndpointPresentation(group: PsiFile, endpoint: NestMapping): ItemPresentation {
        return HttpMethodPresentation(endpoint.getFullPath(), listOf(endpoint.method), endpoint.source, Nodejs.Nodejs, null)
    }
}
