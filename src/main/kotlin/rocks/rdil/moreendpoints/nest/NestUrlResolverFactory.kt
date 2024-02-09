package rocks.rdil.moreendpoints.nest

import com.intellij.microservices.url.*
import com.intellij.openapi.project.Project

class NestUrlResolverFactory : UrlResolverFactory {
    override fun forProject(project: Project): UrlResolver? {
        if (!NestModel.hasNestJs(project)) {
            return null
        }

        return NestUrlResolver(project)
    }

    internal class NestUrlResolver(private val project: Project) : HttpUrlResolver() {
        override fun getVariants(): Iterable<UrlTargetInfo> {
            val endpoints = NestModel.getEndpoints(this.project)

            return endpoints.mapNotNull {
                this.toUrlTargetInfo(it)
            }
        }

        private fun toUrlTargetInfo(it: NestMapping): NestUrlTargetInfo? {
            val element = it.pointer.element
            return if (element !== null) {
                NestUrlTargetInfo(
                    this.supportedSchemes,
                    this.getAuthorityHints(null),
                    UrlPath.fromExactString(it.path),
                    setOf(it.method),
                    element
                )
            } else null
        }

        override fun resolve(request: UrlResolveRequest): Iterable<UrlTargetInfo> = emptyList()
    }
}
