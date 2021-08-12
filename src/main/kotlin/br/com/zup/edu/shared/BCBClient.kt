package br.com.zup.edu.shared

import br.com.zup.edu.shared.request.BCBPixDeleteRequest
import br.com.zup.edu.shared.response.BCBPixResponse
import io.micronaut.http.HttpResponse
import io.micronaut.http.MediaType
import io.micronaut.http.annotation.*
import io.micronaut.http.client.annotation.Client

@Client("\${bcb.uri}")
interface BCBClient {
    @Post("/pix/keys/")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    fun gerarChavePix(@Body bcbPixRequest: BCBPixRequest): HttpResponse<BCBPixResponse>

    @Delete("/pix/keys/{key}")
    @Produces(MediaType.APPLICATION_XML)
    @Consumes(MediaType.APPLICATION_XML)
    fun deletarChavePix(key: String, @Body bcbPixRequest: BCBPixDeleteRequest): HttpResponse<Any>
}