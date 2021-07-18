package altchain.explorer.verifier.api.controller;

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute

interface ApiController {
    fun NormalOpenAPIRoute.registerApi()
}
