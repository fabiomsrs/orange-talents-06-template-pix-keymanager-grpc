package br.com.zup.edu.shared

import io.micronaut.http.HttpResponse
import io.micronaut.http.annotation.Get
import io.micronaut.http.client.annotation.Client

@Client("\${itau.uri}")
interface ItauERPClient {
    @Get("/clientes/{clienteId}/")
    fun consultarCliente(clienteId: String): HttpResponse<ClienteItauResponse>
}

data class ClienteItauResponse(val id: String, val nome: String, val cpf: String)